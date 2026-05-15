// routes/dashboard.js — Écrans 02 (par livreur) et 03 (par client)
const express  = require('express');
const oracledb = require('oracledb');
const { getConnection } = require('../config/db');
const { authMiddleware, requireRole } = require('../middleware/auth');
const router = express.Router();

// GET /api/dashboard/by-deliverer — Écran 02
// Stats du jour par livreur : total, en attente, echoues
router.get('/by-deliverer', authMiddleware, requireRole('CONTROLLER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();

        // Totaux globaux (bandeau bleu)
        const totals = await conn.execute(
            `SELECT
                COUNT(*)                                          AS total,
                SUM(CASE WHEN status='PENDING'  THEN 1 ELSE 0 END) AS pending,
                SUM(CASE WHEN status='FAILED'   THEN 1 ELSE 0 END) AS failed,
                SUM(CASE WHEN status='DELIVERED'THEN 1 ELSE 0 END) AS delivered
             FROM deliveries
             WHERE scheduled_date = TRUNC(SYSDATE)`,
            {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        // Liste des livreurs avec leurs stats
        const byDeliverer = await conn.execute(
            `SELECT * FROM v_dashboard_today ORDER BY failed_count DESC, pending_count DESC`,
            {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        const t = totals.rows[0];
        res.json({
            summary: {
                total:     t.TOTAL,
                pending:   t.PENDING,
                failed:    t.FAILED,
                delivered: t.DELIVERED
            },
            deliverers: byDeliverer.rows.map(r => ({
                deliverer_id:   r.DELIVERER_ID,
                deliverer_name: r.DELIVERER_NAME,
                total:          r.TOTAL_DELIVERIES,
                pending:        r.PENDING_COUNT,
                failed:         r.FAILED_COUNT,
                delivered:      r.DELIVERED_COUNT
            }))
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// GET /api/dashboard/by-client — Écran 03
// Liste des clients du jour avec leur statut (FAILED ou PENDING uniquement)
router.get('/by-client', authMiddleware, requireRole('CONTROLLER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT
                d.delivery_id,
                d.order_number,
                d.status,
                d.failure_reason,
                c.client_id,
                c.full_name AS client_name,
                c.phone     AS client_phone,
                u.full_name AS deliverer_name
             FROM deliveries d
             JOIN clients c ON c.client_id  = d.client_id
             JOIN users u   ON u.user_id    = d.deliverer_id
             WHERE d.scheduled_date = TRUNC(SYSDATE)
               AND d.status IN ('PENDING', 'FAILED')
             ORDER BY d.status DESC, c.full_name`,
            {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        res.json(result.rows.map(r => ({
            delivery_id:    r.DELIVERY_ID,
            order_number:   r.ORDER_NUMBER,
            status:         r.STATUS,
            failure_reason: r.FAILURE_REASON,
            client: {
                client_id:   r.CLIENT_ID,
                name:        r.CLIENT_NAME,
                phone:       r.CLIENT_PHONE
            },
            deliverer_name: r.DELIVERER_NAME
        })));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

module.exports = router;
