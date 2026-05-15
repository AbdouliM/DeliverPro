// routes/messages.js — Messagerie temps réel écran 07
// Compatible Oracle 11g Express Edition
const express  = require('express');
const oracledb = require('oracledb');
const { getConnection } = require('../config/db');
const { authMiddleware } = require('../middleware/auth');
const router = express.Router();

// GET /api/messages/conversations
router.get('/conversations', authMiddleware, async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const uid = parseInt(req.user.user_id);

        const result = await conn.execute(
            `SELECT
                u.user_id,
                u.full_name,
                u.role,
                u.profile_photo,
                (SELECT MAX(msg1.content) KEEP (DENSE_RANK LAST ORDER BY msg1.sent_at)
                 FROM messages msg1
                 WHERE (msg1.sender_id = :uid1 AND msg1.receiver_id = u.user_id)
                    OR (msg1.sender_id = u.user_id AND msg1.receiver_id = :uid2)
                ) AS last_message,
                (SELECT MAX(msg2.sent_at)
                 FROM messages msg2
                 WHERE (msg2.sender_id = :uid3 AND msg2.receiver_id = u.user_id)
                    OR (msg2.sender_id = u.user_id AND msg2.receiver_id = :uid4)
                ) AS last_sent_at,
                (SELECT COUNT(*)
                 FROM messages msg3
                 WHERE msg3.sender_id = u.user_id
                   AND msg3.receiver_id = :uid5
                   AND msg3.is_read = 0
                ) AS unread_count
             FROM users u
             WHERE u.user_id != :uid6
               AND u.is_active = 1
             ORDER BY last_sent_at DESC NULLS LAST`,
            { uid1: uid, uid2: uid, uid3: uid, uid4: uid, uid5: uid, uid6: uid },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        res.json(result.rows.map(r => ({
            user_id:       r.USER_ID,
            full_name:     r.FULL_NAME,
            role:          r.ROLE,
            profile_photo: r.PROFILE_PHOTO,
            last_message:  r.LAST_MESSAGE,
            last_sent_at:  r.LAST_SENT_AT,
            unread_count:  r.UNREAD_COUNT
        })));
    } catch (err) {
        console.error('CONVERSATIONS ERROR:', err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// GET /api/messages/:userId
router.get('/:userId', authMiddleware, async (req, res) => {
    const otherId = parseInt(req.params.userId);
    const uid = parseInt(req.user.user_id);
    let conn;
    try {
        conn = await getConnection();

        const result = await conn.execute(
            `SELECT message_id, sender_id, receiver_id, content, is_read, sent_at
             FROM messages
             WHERE (sender_id = :uid1 AND receiver_id = :oid1)
                OR (sender_id = :oid2 AND receiver_id = :uid2)
             ORDER BY sent_at ASC`,
            { uid1: uid, uid2: uid, oid1: otherId, oid2: otherId },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        await conn.execute(
            `UPDATE messages
             SET is_read = 1
             WHERE sender_id = :p_oid
               AND receiver_id = :p_uid
               AND is_read = 0`,
            { p_oid: otherId, p_uid: uid },
            { autoCommit: true }
        );

        res.json(result.rows.map(r => ({
            message_id:  r.MESSAGE_ID,
            sender_id:   r.SENDER_ID,
            receiver_id: r.RECEIVER_ID,
            content:     r.CONTENT,
            is_read:     r.IS_READ,
            sent_at:     r.SENT_AT,
            is_mine:     r.SENDER_ID === uid
        })));
    } catch (err) {
        console.error('GET MESSAGES ERROR:', err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// POST /api/messages
router.post('/', authMiddleware, async (req, res) => {
    const { receiver_id, content } = req.body;
    if (!receiver_id || !content) {
        return res.status(400).json({ error: 'receiver_id et content requis' });
    }

    const uid = parseInt(req.user.user_id);
    let conn;
    try {
        conn = await getConnection();

        const seqResult = await conn.execute(
            `SELECT SEQ_MESSAGE_ID.NEXTVAL AS new_id FROM DUAL`,
            {},
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );
        const newId = seqResult.rows[0].NEW_ID;

        await conn.execute(
            `INSERT INTO messages (message_id, sender_id, receiver_id, content, is_read, sent_at)
             VALUES (:p_id, :p_sender, :p_receiver, :p_content, 0, SYSDATE)`,
            {
                p_id:       newId,
                p_sender:   uid,
                p_receiver: parseInt(receiver_id),
                p_content:  content
            },
            { autoCommit: true }
        );

        const inserted = await conn.execute(
            `SELECT message_id, sent_at FROM messages WHERE message_id = :p_id`,
            { p_id: newId },
            { outFormat: oracledb.OUT_FORMAT_OBJECT }
        );

        res.status(201).json({
            message_id:  inserted.rows[0].MESSAGE_ID,
            sender_id:   uid,
            receiver_id: parseInt(receiver_id),
            content,
            sent_at:     inserted.rows[0].SENT_AT,
            is_mine:     true
        });
    } catch (err) {
        console.error('POST MESSAGE ERROR:', err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

module.exports = router;