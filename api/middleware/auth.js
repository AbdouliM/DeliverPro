// middleware/auth.js — Vérification du token JWT
const jwt = require('jsonwebtoken');

function authMiddleware(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1]; // Bearer <token>

    if (!token) {
        return res.status(401).json({ error: 'Token manquant' });
    }

    try {
        const decoded = jwt.verify(token, process.env.JWT_SECRET);
        req.user = decoded; // { user_id, username, role }
        next();
    } catch (err) {
        return res.status(403).json({ error: 'Token invalide ou expire' });
    }
}

// Middleware pour vérifier le rôle
function requireRole(...roles) {
    return (req, res, next) => {
        if (!roles.includes(req.user.role)) {
            return res.status(403).json({ error: 'Acces interdit pour ce role' });
        }
        next();
    };
}

module.exports = { authMiddleware, requireRole };
