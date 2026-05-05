# DeliverPro

Application mobile Android de supervision des opérations de livraison en temps réel, avec API REST sécurisée et base de données Oracle.

> Projet académique — ENICarthage, année universitaire 2025-2026.

## Contexte

DeliverPro s'adresse à deux catégories d'utilisateurs :
- **Contrôleurs** : pilotage des tournées depuis un tableau de bord, suivi en temps réel, gestion des alertes d'urgence.
- **Livreurs** : consultation de leur tournée du jour, mise à jour du statut de livraison, mode hors-ligne avec synchronisation différée.

L'objectif est de centraliser la communication entre contrôleurs et livreurs et de garantir la remontée immédiate des incidents, même en cas de connectivité intermittente sur le terrain.

## Architecture

```
┌─────────────────────┐      ┌──────────────────────┐      ┌─────────────────────┐
│   Android App       │      │    API Node.js        │      │   Oracle DB 11g      │
│   (Java, Retrofit2)  │ ───► │   (Express.js, JWT)    │ ───► │   + SQLite local      │
│   /android           │      │   /api                 │      │   (mode offline)      │
└─────────────────────┘      └──────────────────────┘      └─────────────────────┘
```

- **[`/android`](./android)** — Application Android native (Java), ViewBinding, Retrofit2, synchronisation SQLite offline. [Voir le README](./android/README.md)
- **[`/api`](./api)** — API REST Node.js / Express, authentification JWT, connexion Oracle 11g. [Voir le README](./api/README.md)

## Fonctionnalités principales

| Module | Fonctionnalités |
|---|---|
| **Contrôleur** | Tableau de bord par livreur/client, suivi temps réel, recherche avancée, messagerie, gestion des alertes d'urgence |
| **Livreur** | Tournée du jour ordonnée, mise à jour de statut (Livré/Non livré + motif), mode hors-ligne SQLite, alertes géolocalisées |
| **Synchronisation** | Remontée immédiate des échecs (FAILED), batch de fin de journée pour les succès (DELIVERED à 23h59) |

## Stack technique

**Frontend (Android)** : Java 11+, ViewBinding, Retrofit2, OkHttp3, Gson, RecyclerView, ViewPager2, WorkManager, SQLite

**Backend (API)** : Node.js 18+, Express.js, oracledb, jsonwebtoken, dotenv

**Base de données** : Oracle Database 11g Express Edition (vues SQL, séquences) + SQLite local côté livreur

## Difficultés techniques notables

- **Compatibilité Oracle 11g** : pas de variables bind nommées dupliquées dans une même requête SQL — nécessité de nommer chaque occurrence distinctement (`:search_order`, `:search_client`...).
- **Synchronisation offline/online** : logique différenciée selon le statut — remontée immédiate pour les échecs (intervention du contrôleur requise) vs batch de fin de journée pour les succès, gérée par un `SyncManager` dédié avec `WorkManager`.
- **Rotation du client Retrofit** : le singleton capturait le token au premier appel, posant problème après un logout/login — résolu via une méthode `ApiClient.reset()` appelée explicitement au logout.

## Documentation complète

Le rapport technique complet (diagrammes de cas d'utilisation, diagramme de classes, arbre des interfaces, captures d'écran de chaque module) est disponible sur demande.

## Auteur

Mayssa Abdouli — ENICarthage, Génie Informatique
