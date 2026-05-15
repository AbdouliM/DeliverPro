// routes/deliveries.js — Écrans 04, 05, 06 (contrôleur) + 08, 09, 10 (livreur)
const express  = require('express');
const oracledb = require('oracledb');
const { getConnection } = require('../config/db');
const { authMiddleware, requireRole } = require('../middleware/auth');
const router = express.Router();

// ============================================================
// CÔTÉ CONTRÔLEUR
// ============================================================

// GET /api/deliveries/today — Écran 04
// Livraisons du jour PENDING + FAILED uniquement
router.get('/today', authMiddleware, requireRole('CONTROLLER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT * FROM v_active_deliveries_today`,
            {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        res.json(result.rows.map(formatDelivery));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// GET /api/deliveries/search — Écran 05 (recherche par période + filtres)
// Query params : date_start, date_end, deliverer_id, status, search (n° commande ou client)
router.get('/search', authMiddleware, requireRole('CONTROLLER'), async (req, res) => {
    const { date_start, date_end, deliverer_id, status, search } = req.query;
    let conn;
    try {
        conn = await getConnection();

        let sql = `
            SELECT
                d.delivery_id, d.order_number, d.status, d.amount,
                d.payment_method, d.scheduled_date, d.failure_reason,
                c.full_name AS client_name, c.phone AS client_phone,
                u.full_name AS deliverer_name, u.user_id AS deliverer_id,
                d.delivery_address, d.delivery_city, d.failed_at, d.delivered_at
            FROM deliveries d
            JOIN clients c ON c.client_id = d.client_id
            JOIN users u   ON u.user_id   = d.deliverer_id
            WHERE 1=1
        `;
        const binds = {};

        if (date_start) {
            sql += ` AND d.scheduled_date >= TO_DATE(:date_start, 'YYYY-MM-DD')`;
            binds.date_start = date_start;
        }
        if (date_end) {
            sql += ` AND d.scheduled_date <= TO_DATE(:date_end, 'YYYY-MM-DD')`;
            binds.date_end = date_end;
        }
        if (deliverer_id) {
            sql += ` AND d.deliverer_id = :deliverer_id`;
            binds.deliverer_id = parseInt(deliverer_id);
        }
        if (status && status !== 'ALL') {
            sql += ` AND d.status = :status`;
            binds.status = status;
        }
        if (search) {
            sql += ` AND (UPPER(d.order_number) LIKE UPPER(:search) OR UPPER(c.full_name) LIKE UPPER(:search))`;
            binds.search = `%${search}%`;
        }

        sql += ` ORDER BY d.scheduled_date DESC, d.stop_order`;

        const result = await conn.execute(sql, binds, { outFormat: oracledb.OUT_FORMAT_OBJECT });
        res.json(result.rows.map(formatDelivery));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// GET /api/deliveries/:id — Écran 06 (détail livraison contrôleur)
router.get('/:id', authMiddleware, async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT
                d.delivery_id, d.order_number, d.status, d.amount,
                d.payment_method, d.nb_articles, d.scheduled_date,
                d.delivery_address, d.delivery_city, d.latitude, d.longitude,
                d.failure_reason, d.failed_at, d.delivered_at, d.stop_order,
                c.full_name AS client_name, c.phone AS client_phone,
                u.full_name AS deliverer_name, u.user_id AS deliverer_id, u.phone AS deliverer_phone
             FROM deliveries d
             JOIN clients c ON c.client_id = d.client_id
             JOIN users u   ON u.user_id   = d.deliverer_id
             WHERE d.delivery_id = :id`,
            { id: parseInt(req.params.id) },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Livraison non trouvee' });
        }

        const r = result.rows[0];
        res.json({
            delivery_id:      r.DELIVERY_ID,
            order_number:     r.ORDER_NUMBER,
            status:           r.STATUS,
            failure_reason:   r.FAILURE_REASON,
            amount:           r.AMOUNT,
            payment_method:   r.PAYMENT_METHOD,
            nb_articles:      r.NB_ARTICLES,
            scheduled_date:   r.SCHEDULED_DATE,
            delivery_address: r.DELIVERY_ADDRESS,
            delivery_city:    r.DELIVERY_CITY,
            latitude:         r.LATITUDE,
            longitude:        r.LONGITUDE,
            failed_at:        r.FAILED_AT,
            delivered_at:     r.DELIVERED_AT,
            stop_order:       r.STOP_ORDER,
            client: {
                name:  r.CLIENT_NAME,
                phone: r.CLIENT_PHONE
            },
            deliverer: {
                user_id: r.DELIVERER_ID,
                name:    r.DELIVERER_NAME,
                phone:   r.DELIVERER_PHONE
            }
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// ============================================================
// CÔTÉ LIVREUR
// ============================================================

// GET /api/deliveries/driver/tour — Écran 08 (tournée du livreur)
router.get('/driver/tour', authMiddleware, requireRole('DELIVERER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT * FROM v_driver_tour WHERE deliverer_id = :id`,
            { id: req.user.user_id },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        const total     = result.rows.length;
        const done      = result.rows.filter(r => r.STATUS === 'DELIVERED').length;
        const remaining = result.rows.filter(r => r.STATUS !== 'DELIVERED').length;

        res.json({
            summary: { total, done, remaining },
            stops: result.rows.map(r => ({
                delivery_id:      r.DELIVERY_ID,
                order_number:     r.ORDER_NUMBER,
                status:           r.STATUS,
                stop_order:       r.STOP_ORDER,
                client_name:      r.CLIENT_NAME,
                client_phone:     r.CLIENT_PHONE,
                delivery_city:    r.DELIVERY_CITY,
                delivery_address: r.DELIVERY_ADDRESS,
                amount:           r.AMOUNT,
                payment_method:   r.PAYMENT_METHOD,
                nb_articles:      r.NB_ARTICLES,
                latitude:         r.LATITUDE,
                longitude:        r.LONGITUDE
            }))
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// PATCH /api/deliveries/:id/status — Écran 10 (modifier statut livraison)
// Logique métier :
//   DELIVERED → base centrale uniquement (sera synchro via batch 23h59 en prod)
//   FAILED    → remontée immédiate dans la base centrale
router.patch('/:id/status', authMiddleware, requireRole('DELIVERER'), async (req, res) => {
    const { status, failure_reason } = req.body;
    const deliveryId = parseInt(req.params.id);

    if (!['PENDING', 'DELIVERED', 'FAILED'].includes(status)) {
        return res.status(400).json({ error: 'Statut invalide' });
    }
    if (status === 'FAILED' && !failure_reason) {
        return res.status(400).json({ error: 'La remarque est obligatoire pour Non livre' });
    }

    let conn;
    try {
        conn = await getConnection();

        // Vérifier que la livraison appartient bien à ce livreur
        const check = await conn.execute(
            `SELECT delivery_id FROM deliveries WHERE delivery_id = :p_check_id AND deliverer_id = :p_check_uid`,
            { p_check_id: deliveryId, p_check_uid: parseInt(req.user.user_id) },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        if (check.rows.length === 0) {
            return res.status(403).json({ error: 'Livraison non autorisee' });
        }

        await conn.execute(
            `UPDATE deliveries
             SET status = :p_status,
                 failure_reason = :p_reason,
                 updated_at = SYSDATE
             WHERE delivery_id = :p_id`,
            {
                p_status: status,
                p_reason: failure_reason || null,
                p_id:     deliveryId
            },
            { autoCommit: true }
        );

        res.json({
            message: 'Statut mis a jour',
            delivery_id: deliveryId,
            status,
            sync_type: status === 'FAILED' ? 'IMMEDIATE' : 'BATCH_23H59'
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// ============================================================
// HELPER
// ============================================================
function formatDelivery(r) {
    return {
        delivery_id:      r.DELIVERY_ID,
        order_number:     r.ORDER_NUMBER,
        status:           r.STATUS,
        failure_reason:   r.FAILURE_REASON,
        amount:           r.AMOUNT,
        payment_method:   r.PAYMENT_METHOD,
        scheduled_date:   r.SCHEDULED_DATE,
        stop_order:       r.STOP_ORDER,
        delivery_address: r.DELIVERY_ADDRESS,
        delivery_city:    r.DELIVERY_CITY,
        latitude:         r.LATITUDE,
        longitude:        r.LONGITUDE,
        client_name:      r.CLIENT_NAME,
        client_phone:     r.CLIENT_PHONE,
        deliverer_name:   r.DELIVERER_NAME,
        deliverer_id:     r.DELIVERER_ID,
        failed_at:        r.FAILED_AT,
        delivered_at:     r.DELIVERED_AT
    };
}

module.exports = router;