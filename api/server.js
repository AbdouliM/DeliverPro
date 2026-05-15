/*// server.js — Point d'entree DeliverPro API
require('dotenv').config();
const express    = require('express');
const cors       = require('cors');
const { initPool } = require('./config/db');

const authRoutes         = require('./routes/auth');
const dashboardRoutes    = require('./routes/dashboard');
const deliveriesRoutes   = require('./routes/deliveries');
const messagesRoutes     = require('./routes/messages');
const alertsRoutes       = require('./routes/alerts');

const app  = express();
const PORT = process.env.PORT || 3000;

// Middlewares globaux
app.use(cors());
app.use(express.json());

// Routes
app.use('/api/auth',       authRoutes);
app.use('/api/dashboard',  dashboardRoutes);
app.use('/api/deliveries', deliveriesRoutes);
app.use('/api/messages',   messagesRoutes);
app.use('/api/alerts',     alertsRoutes);

// Route de test
app.get('/', (req, res) => {
    res.json({ message: 'DeliverPro API is running', version: '1.0.0' });
});

// Gestionnaire d'erreurs global
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Erreur interne du serveur' });
});

// Démarrage : initialiser le pool Oracle puis lancer le serveur
initPool().then(() => {
    app.listen(PORT, () => {
        console.log(`DeliverPro API demarree sur http://localhost:${PORT}`);
    });
});
*/
// server.js — Point d'entree DeliverPro API
require('dotenv').config();
const express    = require('express');
const cors       = require('cors');
const { initPool } = require('./config/db');

const authRoutes         = require('./routes/auth');
const dashboardRoutes    = require('./routes/dashboard');
const deliveriesRoutes   = require('./routes/deliveries');
const messagesRoutes     = require('./routes/messages');
const alertsRoutes       = require('./routes/alerts');
const usersRoutes        = require('./routes/users');

const app  = express();
const PORT = process.env.PORT || 3000;

// Middlewares globaux
app.use(cors());
app.use(express.json());

// Routes
app.use('/api/auth',       authRoutes);
app.use('/api/dashboard',  dashboardRoutes);
app.use('/api/deliveries', deliveriesRoutes);
app.use('/api/messages',   messagesRoutes);
app.use('/api/alerts',     alertsRoutes);
app.use('/api/users',      usersRoutes);

// Route de test
app.get('/', (req, res) => {
    res.json({ message: 'DeliverPro API is running', version: '1.0.0' });
});

// Gestionnaire d'erreurs global
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({ error: 'Erreur interne du serveur' });
});

// Démarrage : initialiser le pool Oracle puis lancer le serveur
initPool().then(() => {
    app.listen(PORT, () => {
        console.log(`DeliverPro API demarree sur http://localhost:${PORT}`);
    });
});