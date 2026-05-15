// routes/alerts.js — Compatible Oracle 11g
const express  = require('express');
const oracledb = require('oracledb');
const { getConnection } = require('../config/db');
const { authMiddleware, requireRole } = require('../middleware/auth');
const router = express.Router();

// POST /api/alerts — Livreur envoie une alerte urgence
// Inserts the alert AND sends it as a message to the relevant controller
router.post('/', authMiddleware, requireRole('DELIVERER'), async (req, res) => {
    const { delivery_id, alert_type, description, latitude, longitude, location_name } = req.body;
    if (!alert_type) return res.status(400).json({ error: 'alert_type requis' });

    let conn;
    try {
        conn = await getConnection();
        const delivererId = parseInt(req.user.user_id);

        // 1. Find the controller who has spoken with this deliverer before.
        //    If none, fall back to the first active controller.
        const ctrlResult = await conn.execute(
            `SELECT user_id FROM (
                SELECT u.user_id
                FROM users u
                WHERE u.role = 'CONTROLLER' AND u.is_active = 1
                  AND EXISTS (
                      SELECT 1 FROM messages m
                      WHERE (m.sender_id = :did1 AND m.receiver_id = u.user_id)
                         OR (m.sender_id = u.user_id AND m.receiver_id = :did2)
                  )
                UNION ALL
                SELECT u2.user_id FROM users u2
                WHERE u2.role = 'CONTROLLER' AND u2.is_active = 1
             )
             WHERE ROWNUM = 1`,
            { did1: delivererId, did2: delivererId },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        if (ctrlResult.rows.length === 0) {
            return res.status(500).json({ error: 'Aucun contrôleur disponible' });
        }
        const controllerId = ctrlResult.rows[0].USER_ID;

        // 2. Build the alert message text
        const alertLabels = {
            ACCIDENT:          '🚨 ACCIDENT',
            CLIENT_ISSUE:      '🚨 PROBLÈME CLIENT',
            VEHICLE_BREAKDOWN: '🚨 PANNE VÉHICULE',
            ADDRESS_NOT_FOUND: '🚨 ADRESSE INTROUVABLE',
            SAFETY_CONCERN:    '🚨 PROBLÈME SÉCURITÉ'
        };
        const label = alertLabels[alert_type] || '🚨 URGENCE';
        // Get order number from deliveries table if delivery_id provided
        let orderInfo = '';
        if (delivery_id) {
            try {
                const orderResult = await conn.execute(
                    `SELECT order_number FROM deliveries WHERE delivery_id = :p_did`,
                    { p_did: delivery_id },
                    { outFormat: oracledb.OUT_FORMAT_OBJECT }
                );
                if (orderResult.rows.length > 0) {
                    orderInfo = ` — Commande ${orderResult.rows[0].ORDER_NUMBER}`;
                }
            } catch (_) {}
        }

        // Build location string
        const locInfo = (latitude && longitude)
            ? `
Position: ${latitude}, ${longitude}`
            : location_name ? `
Position: ${location_name}` : '';

        const msgContent = `${label}${orderInfo}: ${description}${locInfo}`;

        // 3. Insert the alert record
        const seqAlert = await conn.execute(
            `SELECT SEQ_ALERT_ID.NEXTVAL AS new_id FROM DUAL`, {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        const alertId = seqAlert.rows[0].NEW_ID;

        await conn.execute(
            `INSERT INTO emergency_alerts
                (alert_id, deliverer_id, delivery_id, alert_type, description,
                 latitude, longitude, location_name, sent_at)
             VALUES (:p_id, :p_del, :p_dev, :p_type, :p_desc, :p_lat, :p_lng, :p_loc, SYSDATE)`,
            {
                p_id: alertId, p_del: delivererId,
                p_dev: delivery_id || null, p_type: alert_type,
                p_desc: description || null, p_lat: latitude || null,
                p_lng: longitude || null, p_loc: location_name || null
            },
            { autoCommit: false }
        );

        // 4. Insert it as a message so it appears in the conversation
        const seqMsg = await conn.execute(
            `SELECT SEQ_MESSAGE_ID.NEXTVAL AS new_id FROM DUAL`, {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        const msgId = seqMsg.rows[0].NEW_ID;

        await conn.execute(
            `INSERT INTO messages (message_id, sender_id, receiver_id, content, is_read, sent_at)
             VALUES (:p_id, :p_sender, :p_receiver, :p_content, 0, SYSDATE)`,
            {
                p_id:       msgId,
                p_sender:   delivererId,
                p_receiver: controllerId,
                p_content:  msgContent
            },
            { autoCommit: false }
        );

        await conn.execute(`COMMIT`, {});

        res.status(201).json({
            alert_id:      alertId,
            controller_id: controllerId,
            message:       'Alerte envoyée au contrôleur',
        });
    } catch (err) {
        console.error(err);
        try { await conn.execute(`ROLLBACK`, {}); } catch (_) {}
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// GET /api/alerts/unread — Alertes non résolues pour le livreur connecté
// Returns controllers whose conversation should be highlighted as urgent
router.get('/unread', authMiddleware, requireRole('DELIVERER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const delivererId = parseInt(req.user.user_id);

        // Find unresolved alerts and the controller they were sent to
        // (identified by the message sent at the same time)
        const result = await conn.execute(
            `SELECT DISTINCT
                u.user_id   AS controller_id,
                u.full_name AS controller_name,
                a.description,
                a.sent_at
             FROM emergency_alerts a
             JOIN messages m ON (
                 m.sender_id = a.deliverer_id
                 AND m.content LIKE '🚨%'
             )
             JOIN users u ON u.user_id = m.receiver_id
             WHERE a.deliverer_id = :p_uid
               AND a.is_resolved = 0
               AND u.role = 'CONTROLLER'
             ORDER BY a.sent_at DESC`,
            { p_uid: delivererId },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        res.json(result.rows.map(r => ({
            sender_id:   r.CONTROLLER_ID,
            sender_name: r.CONTROLLER_NAME,
            description: r.DESCRIPTION,
            sent_at:     r.SENT_AT
        })));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// GET /api/alerts — Contrôleur voit toutes les alertes non résolues
router.get('/', authMiddleware, requireRole('CONTROLLER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT a.alert_id, a.alert_type, a.description, a.location_name,
                a.latitude, a.longitude, a.is_resolved, a.sent_at,
                u.full_name AS deliverer_name, u.phone AS deliverer_phone,
                d.order_number
             FROM emergency_alerts a
             JOIN users u ON u.user_id = a.deliverer_id
             LEFT JOIN deliveries d ON d.delivery_id = a.delivery_id
             WHERE a.is_resolved = 0
             ORDER BY a.sent_at DESC`,
            {}, { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        res.json(result.rows.map(r => ({
            alert_id: r.ALERT_ID, alert_type: r.ALERT_TYPE,
            description: r.DESCRIPTION, location_name: r.LOCATION_NAME,
            latitude: r.LATITUDE, longitude: r.LONGITUDE,
            is_resolved: r.IS_RESOLVED, sent_at: r.SENT_AT,
            order_number: r.ORDER_NUMBER,
            deliverer: { name: r.DELIVERER_NAME, phone: r.DELIVERER_PHONE }
        })));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// PATCH /api/alerts/:id/resolve
router.patch('/:id/resolve', authMiddleware, requireRole('CONTROLLER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        await conn.execute(
            `UPDATE emergency_alerts SET is_resolved = 1, resolved_at = SYSDATE
             WHERE alert_id = :p_id`,
            { p_id: parseInt(req.params.id) },
            { autoCommit: true }
        );
        res.json({ message: 'Alerte resolue' });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

module.exports = router;
/*
// routes/alerts.js — Compatible Oracle 11g
const express  = require('express');
const oracledb = require('oracledb');
const { getConnection } = require('../config/db');
const { authMiddleware, requireRole } = require('../middleware/auth');
const router = express.Router();

// POST /api/alerts — Livreur envoie une alerte urgence
// Inserts the alert AND sends it as a message to the relevant controller
router.post('/', authMiddleware, requireRole('DELIVERER'), async (req, res) => {
    const { delivery_id, alert_type, description, latitude, longitude, location_name } = req.body;
    if (!alert_type) return res.status(400).json({ error: 'alert_type requis' });

    let conn;
    try {
        conn = await getConnection();
        const delivererId = parseInt(req.user.user_id);

        // 1. Find the controller who has spoken with this deliverer before.
        //    If none, fall back to the first active controller.
        const ctrlResult = await conn.execute(
            `SELECT user_id FROM (
                SELECT u.user_id
                FROM users u
                WHERE u.role = 'CONTROLLER' AND u.is_active = 1
                  AND EXISTS (
                      SELECT 1 FROM messages m
                      WHERE (m.sender_id = :did1 AND m.receiver_id = u.user_id)
                         OR (m.sender_id = u.user_id AND m.receiver_id = :did2)
                  )
                UNION ALL
                SELECT u2.user_id FROM users u2
                WHERE u2.role = 'CONTROLLER' AND u2.is_active = 1
             )
             WHERE ROWNUM = 1`,
            { did1: delivererId, did2: delivererId },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        if (ctrlResult.rows.length === 0) {
            return res.status(500).json({ error: 'Aucun contrôleur disponible' });
        }
        const controllerId = ctrlResult.rows[0].USER_ID;

        // 2. Build the alert message text
        const alertLabels = {
            ACCIDENT:          '🚨 ACCIDENT',
            CLIENT_ISSUE:      '🚨 PROBLÈME CLIENT',
            VEHICLE_BREAKDOWN: '🚨 PANNE VÉHICULE',
            ADDRESS_NOT_FOUND: '🚨 ADRESSE INTROUVABLE',
            SAFETY_CONCERN:    '🚨 PROBLÈME SÉCURITÉ'
        };
        const label = alertLabels[alert_type] || '🚨 URGENCE';
        const orderInfo = delivery_id ? ` — Commande #${delivery_id}` : '';
        const msgContent = `${label}${orderInfo}: ${description}`;

        // 3. Insert the alert record
        const seqAlert = await conn.execute(
            `SELECT SEQ_ALERT_ID.NEXTVAL AS new_id FROM DUAL`, {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        const alertId = seqAlert.rows[0].NEW_ID;

        await conn.execute(
            `INSERT INTO emergency_alerts
                (alert_id, deliverer_id, delivery_id, alert_type, description,
                 latitude, longitude, location_name, sent_at)
             VALUES (:p_id, :p_del, :p_dev, :p_type, :p_desc, :p_lat, :p_lng, :p_loc, SYSDATE)`,
            {
                p_id: alertId, p_del: delivererId,
                p_dev: delivery_id || null, p_type: alert_type,
                p_desc: description || null, p_lat: latitude || null,
                p_lng: longitude || null, p_loc: location_name || null
            },
            { autoCommit: false }
        );

        // 4. Insert it as a message so it appears in the conversation
        const seqMsg = await conn.execute(
            `SELECT SEQ_MESSAGE_ID.NEXTVAL AS new_id FROM DUAL`, {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        const msgId = seqMsg.rows[0].NEW_ID;

        await conn.execute(
            `INSERT INTO messages (message_id, sender_id, receiver_id, content, is_read, sent_at)
             VALUES (:p_id, :p_sender, :p_receiver, :p_content, 0, SYSDATE)`,
            {
                p_id:       msgId,
                p_sender:   delivererId,
                p_receiver: controllerId,
                p_content:  msgContent
            },
            { autoCommit: false }
        );

        await conn.execute(`COMMIT`, {});

        res.status(201).json({
            alert_id:      alertId,
            controller_id: controllerId,
            message:       'Alerte envoyée au contrôleur',
        });
    } catch (err) {
        console.error(err);
        try { await conn.execute(`ROLLBACK`, {}); } catch (_) {}
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// GET /api/alerts/unread — Alertes non résolues pour le livreur connecté
// Returns controllers whose conversation should be highlighted as urgent
router.get('/unread', authMiddleware, requireRole('DELIVERER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const delivererId = parseInt(req.user.user_id);

        // Find unresolved alerts and the controller they were sent to
        // (identified by the message sent at the same time)
        const result = await conn.execute(
            `SELECT DISTINCT
                u.user_id   AS controller_id,
                u.full_name AS controller_name,
                a.description,
                a.sent_at
             FROM emergency_alerts a
             JOIN messages m ON (
                 m.sender_id = a.deliverer_id
                 AND m.content LIKE '🚨%'
             )
             JOIN users u ON u.user_id = m.receiver_id
             WHERE a.deliverer_id = :p_uid
               AND a.is_resolved = 0
               AND u.role = 'CONTROLLER'
             ORDER BY a.sent_at DESC`,
            { p_uid: delivererId },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        res.json(result.rows.map(r => ({
            sender_id:   r.CONTROLLER_ID,
            sender_name: r.CONTROLLER_NAME,
            description: r.DESCRIPTION,
            sent_at:     r.SENT_AT
        })));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// GET /api/alerts — Contrôleur voit toutes les alertes non résolues
router.get('/', authMiddleware, requireRole('CONTROLLER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT a.alert_id, a.alert_type, a.description, a.location_name,
                a.latitude, a.longitude, a.is_resolved, a.sent_at,
                u.full_name AS deliverer_name, u.phone AS deliverer_phone,
                d.order_number
             FROM emergency_alerts a
             JOIN users u ON u.user_id = a.deliverer_id
             LEFT JOIN deliveries d ON d.delivery_id = a.delivery_id
             WHERE a.is_resolved = 0
             ORDER BY a.sent_at DESC`,
            {}, { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        res.json(result.rows.map(r => ({
            alert_id: r.ALERT_ID, alert_type: r.ALERT_TYPE,
            description: r.DESCRIPTION, location_name: r.LOCATION_NAME,
            latitude: r.LATITUDE, longitude: r.LONGITUDE,
            is_resolved: r.IS_RESOLVED, sent_at: r.SENT_AT,
            order_number: r.ORDER_NUMBER,
            deliverer: { name: r.DELIVERER_NAME, phone: r.DELIVERER_PHONE }
        })));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// PATCH /api/alerts/:id/resolve
router.patch('/:id/resolve', authMiddleware, requireRole('CONTROLLER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        await conn.execute(
            `UPDATE emergency_alerts SET is_resolved = 1, resolved_at = SYSDATE
             WHERE alert_id = :p_id`,
            { p_id: parseInt(req.params.id) },
            { autoCommit: true }
        );
        res.json({ message: 'Alerte resolue' });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

module.exports = router;*/