# DeliverPro API — Documentation

## Installation

```bash
# 1. Installer les dépendances
npm install

# 2. Configurer la base de données
# Éditer .env avec tes identifiants Oracle

# 3. Démarrer en développement
npm run dev

# 4. Démarrer en production
npm start
```

---

## Endpoints

### Authentification

| Méthode | URL | Description | Écran |
|---------|-----|-------------|-------|
| POST | `/api/auth/login` | Connexion (retourne JWT + rôle) | 01 |
| POST | `/api/auth/forgot-password` | Demande reset mot de passe | 00 |

**Body login :**
```json
{ "username": "ali.jebali", "password": "monmotdepasse" }
```
**Réponse :**
```json
{
  "token": "eyJ...",
  "user": { "user_id": 3, "full_name": "Ali Jebali", "role": "DELIVERER" }
}
```

---

### Dashboard (Contrôleur uniquement)

| Méthode | URL | Description | Écran |
|---------|-----|-------------|-------|
| GET | `/api/dashboard/by-deliverer` | Stats du jour par livreur | 02 |
| GET | `/api/dashboard/by-client` | Clients du jour PENDING+FAILED | 03 |

---

### Livraisons

| Méthode | URL | Description | Écran |
|---------|-----|-------------|-------|
| GET | `/api/deliveries/today` | Livraisons actives aujourd'hui | 04 |
| GET | `/api/deliveries/search` | Recherche avec filtres | 05 |
| GET | `/api/deliveries/:id` | Détail d'une livraison | 06 |
| GET | `/api/deliveries/driver/tour` | Tournée du livreur connecté | 08 |
| PATCH | `/api/deliveries/:id/status` | Modifier statut livraison | 10 |

**Paramètres de recherche (GET /search) :**
```
?date_start=2026-04-01
&date_end=2026-04-07
&deliverer_id=3
&status=FAILED         (PENDING | DELIVERED | FAILED | ALL)
&search=DEL-2841       (numéro commande ou nom client)
```

**Body PATCH /status :**
```json
{ "status": "FAILED", "failure_reason": "Client absent" }
```

---

### Messagerie

| Méthode | URL | Description | Écran |
|---------|-----|-------------|-------|
| GET | `/api/messages/conversations` | Liste des conversations | 07 |
| GET | `/api/messages/:userId` | Messages avec un utilisateur | 07 |
| POST | `/api/messages` | Envoyer un message | 07 |

---

### Alertes urgence

| Méthode | URL | Description | Écran |
|---------|-----|-------------|-------|
| POST | `/api/alerts` | Envoyer une alerte (livreur) | 11 |
| GET | `/api/alerts` | Voir les alertes actives (contrôleur) | — |
| PATCH | `/api/alerts/:id/resolve` | Résoudre une alerte | — |

---

## Authentification

Toutes les routes (sauf `/api/auth/login`) nécessitent le header :
```
Authorization: Bearer <token>
```

## Structure du projet

```
deliverpro-api/
├── server.js            ← Point d'entrée
├── .env.example         ← Variables d'environnement
├── config/
│   └── db.js            ← Pool de connexion Oracle
├── middleware/
│   └── auth.js          ← Vérification JWT + rôles
└── routes/
    ├── auth.js          ← Login, forgot password
    ├── dashboard.js     ← Écrans 02, 03
    ├── deliveries.js    ← Écrans 04, 05, 06, 08, 09, 10
    ├── messages.js      ← Écran 07
    └── alerts.js        ← Écran 11
```
