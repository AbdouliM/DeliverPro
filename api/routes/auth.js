// routes/auth.js — Connexion et mot de passe oublié
const express    = require('express');
const bcrypt     = require('bcryptjs');
const jwt        = require('jsonwebtoken');
const { authMiddleware } = require('../middleware/auth');

const { getConnection } = require('../config/db');
const router = express.Router();

// POST /api/auth/login — Écrans 01
router.post('/login', async (req, res) => {
    const { username, password } = req.body;
    if (!username || !password) {
        return res.status(400).json({ error: 'username et password requis' });
    }

    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT user_id, username, password_hash, full_name, role, phone, profile_photo
             FROM users
             WHERE username = :username AND is_active = 1`,
            { username },
            { outFormat: require('oracledb').OUT_FORMAT_OBJECT }
        );

        if (result.rows.length === 0) {
            return res.status(401).json({ error: 'Identifiants incorrects' });
        }

        const user = result.rows[0];
        //const validPassword = await bcrypt.compare(password, user.PASSWORD_HASH);
        const validPassword = password === user.PASSWORD_HASH;
        
        if (!validPassword) {
            return res.status(401).json({ error: 'Identifiants incorrects' });
        }

        const token = jwt.sign(
            { user_id: user.USER_ID, username: user.USERNAME, role: user.ROLE },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN }
        );

        res.json({
            token,
            user: {
                user_id:      user.USER_ID,
                username:     user.USERNAME,
                full_name:    user.FULL_NAME,
                role:         user.ROLE,         // 'CONTROLLER' ou 'DELIVERER'
                phone:        user.PHONE,
                profile_photo: user.PROFILE_PHOTO
            }
        });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});

// POST /api/auth/forgot-password — Écran 00
router.post('/forgot-password', async (req, res) => {
    const { email } = req.body;
    if (!email) return res.status(400).json({ error: 'Email requis' });

    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT user_id FROM users WHERE email = :email AND is_active = 1`,
            { email },
            { outFormat: require('oracledb').OUT_FORMAT_OBJECT }
        );

        // Toujours répondre OK (sécurité : ne pas révéler si l'email existe)
        if (result.rows.length === 0) {
            return res.json({ message: 'Si cet email existe, un lien a ete envoye' });
        }

        const token = require('crypto').randomBytes(32).toString('hex');
        const expires = new Date(Date.now() + 3600000); // +1h

        await conn.execute(
            `INSERT INTO password_reset_tokens (user_id, token, expires_at)
             VALUES (:user_id, :token, :expires_at)`,
            {
                user_id:    result.rows[0].USER_ID,
                token:      token,
                expires_at: expires
            },
            { autoCommit: true }
        );

        // TODO: envoyer l'email avec le lien de réinitialisation
        console.log(`Lien reset: http://localhost:3000/reset-password?token=${token}`);

        res.json({ message: 'Si cet email existe, un lien a ete envoye' });
    } catch (err) {
        console.error(err);
        res.status(500).json({ error: 'Erreur serveur' });
    } finally {
        if (conn) await conn.close();
    }
});
router.get('/profile', authMiddleware, async (req, res) => {
    let conn;
    try {
        conn = await getConnection();
        const result = await conn.execute(
            `SELECT user_id, username, full_name, role, phone,
                    address, hire_date, email, profile_photo
             FROM users
             WHERE user_id = :p_user_id AND is_active = 1`,
            { p_user_id: req.user.user_id },
            { outFormat: require('oracledb').OUT_FORMAT_OBJECT }
        );
 
        if (result.rows.length === 0) {
            return res.status(404).json({ error: 'Utilisateur non trouvé' });
        }
 
        const u = result.rows[0];
 
        // Formater la date hire_date en string lisible
        let hireDate = null;
        if (u.HIRE_DATE) {
            const d = new Date(u.HIRE_DATE);
            const day   = String(d.getDate()).padStart(2, '0');
            const month = String(d.getMonth() + 1).padStart(2, '0');
            const year  = d.getFullYear();
            hireDate = `${year}-${month}-${day}`; // format YYYY-MM-DD pour Android
        }
 
        res.json({
            user_id:       u.USER_ID,
            username:      u.USERNAME,
            full_name:     u.FULL_NAME,
            role:          u.ROLE,
            phone:         u.PHONE,
            address:       u.ADDRESS,
            hire_date:     hireDate,
            email:         u.EMAIL,
            profile_photo: u.PROFILE_PHOTO
        });
    } catch (err) {
        console.error('Erreur /profile :', err.message);
        res.status(500).json({ error: err.message });
    } finally {
        if (conn) await conn.close();
    }
});
module.exports = router;
