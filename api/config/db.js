// config/db.js — Connexion Oracle via oracledb
const oracledb = require('oracledb');
require('dotenv').config();

// Mode "thin" : pas besoin d'installer le client Oracle
oracledb.initOracleClient(); // Commenter cette ligne si tu utilises le mode thin

let pool;

async function initPool() {
    try {
        pool = await oracledb.createPool({
            user:             process.env.DB_USER,
            password:         process.env.DB_PASSWORD,
            connectionString: process.env.DB_CONNECTION_STRING,
            poolMin:          2,
            poolMax:          10,
            poolIncrement:    1
        });
        console.log('Oracle DB pool cree avec succes');
    } catch (err) {
        console.error('Erreur creation pool Oracle :', err.message);
        process.exit(1);
    }
}

async function getConnection() {
    return await pool.getConnection();
}

async function closePool() {
    if (pool) await pool.close(0);
}

module.exports = { initPool, getConnection, closePool };
