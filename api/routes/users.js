// routes/users.js
const express  = require('express');
const oracledb = require('oracledb');
const { getConnection } = require('../config/db');
const { authMiddleware, requireRole } = require('../middleware/auth');
const router = express.Router();

// GET /api/users/deliverers — liste des livreurs actifs pour le spinner filtre
router.get('/deliverers', authMiddleware, requireRole('CONTROLLER'), async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT user_id, full_name FROM users
             WHERE role = 'DELIVERER' AND is_active = 1
             ORDER BY full_name ASC`,
            {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        res.json(result.rows.map(r => ({
            user_id:   r.USER_ID,
            full_name: r.FULL_NAME
        })));
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

module.exports = router;