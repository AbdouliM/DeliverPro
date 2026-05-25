package com.deliverpro.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.deliverpro.app.model.Delivery;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocalDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "deliverpro_local.db";
    private static final int    DB_VERSION = 2; // bumped to trigger onUpgrade

    public static final String TABLE_DELIVERIES   = "local_deliveries";
    public static final String COL_DELIVERY_ID    = "delivery_id";
    public static final String COL_ORDER_NUMBER   = "order_number";
    public static final String COL_STATUS         = "status";
    public static final String COL_CLIENT_NAME    = "client_name";
    public static final String COL_CLIENT_PHONE   = "client_phone";
    public static final String COL_ADDRESS        = "delivery_address";
    public static final String COL_CITY           = "delivery_city";
    public static final String COL_AMOUNT         = "amount";
    public static final String COL_PAYMENT        = "payment_method";
    public static final String COL_NB_ARTICLES    = "nb_articles";
    public static final String COL_STOP_ORDER     = "stop_order";
    public static final String COL_FAILURE_REASON = "failure_reason";
    public static final String COL_SYNCED         = "is_synced";
    public static final String COL_UPDATED_AT     = "updated_at";

    // Metadata table — stores when the tour was last loaded
    private static final String TABLE_META        = "meta";
    private static final String COL_KEY           = "key";
    private static final String COL_VALUE         = "value";
    private static final String META_TOUR_DATE    = "tour_date"; // value = "yyyy-MM-dd"

    private static LocalDatabase instance;

    public static synchronized LocalDatabase getInstance(Context ctx) {
        if (instance == null)
            instance = new LocalDatabase(ctx.getApplicationContext());
        return instance;
    }

    private LocalDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_DELIVERIES + " (" +
                COL_DELIVERY_ID    + " INTEGER PRIMARY KEY, " +
                COL_ORDER_NUMBER   + " TEXT NOT NULL, " +
                COL_STATUS         + " TEXT DEFAULT 'PENDING', " +
                COL_CLIENT_NAME    + " TEXT, " +
                COL_CLIENT_PHONE   + " TEXT, " +
                COL_ADDRESS        + " TEXT, " +
                COL_CITY           + " TEXT, " +
                COL_AMOUNT         + " REAL DEFAULT 0, " +
                COL_PAYMENT        + " TEXT, " +
                COL_NB_ARTICLES    + " INTEGER DEFAULT 1, " +
                COL_STOP_ORDER     + " INTEGER DEFAULT 0, " +
                COL_FAILURE_REASON + " TEXT, " +
                COL_SYNCED         + " INTEGER DEFAULT 0, " +
                COL_UPDATED_AT     + " TEXT" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_META + " (" +
                COL_KEY   + " TEXT PRIMARY KEY, " +
                COL_VALUE + " TEXT" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DELIVERIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_META);
        onCreate(db);
    }

    // ── WRITE ────────────────────────────────────────────────

    public void saveTour(List<Delivery> deliveries) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(TABLE_DELIVERIES, null, null);

            for (Delivery d : deliveries) {
                ContentValues cv = new ContentValues();
                cv.put(COL_DELIVERY_ID,  d.getDeliveryId());
                cv.put(COL_ORDER_NUMBER, d.getOrderNumber());
                cv.put(COL_STATUS,       d.getStatus());
                cv.put(COL_CLIENT_NAME,  d.getClientName());
                cv.put(COL_CLIENT_PHONE, d.getClientPhone());
                cv.put(COL_ADDRESS,      d.getDeliveryAddress());
                cv.put(COL_CITY,         d.getDeliveryCity());
                cv.put(COL_AMOUNT,       d.getAmount());
                cv.put(COL_PAYMENT,      d.getPaymentMethod());
                cv.put(COL_NB_ARTICLES,  d.getNbArticles());
                cv.put(COL_STOP_ORDER,   d.getStopOrder());
                cv.put(COL_SYNCED,       1);
                db.insert(TABLE_DELIVERIES, null, cv);
            }

            // Save today's date as the tour date
            ContentValues meta = new ContentValues();
            meta.put(COL_KEY,   META_TOUR_DATE);
            meta.put(COL_VALUE, todayString());
            db.insertWithOnConflict(TABLE_META, null, meta,
                    SQLiteDatabase.CONFLICT_REPLACE);

            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void updateStatusLocal(int deliveryId, String status, String failureReason) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_STATUS,         status);
        cv.put(COL_FAILURE_REASON, failureReason);
        cv.put(COL_SYNCED,         0);
        cv.put(COL_UPDATED_AT,     getCurrentTimestamp());
        db.update(TABLE_DELIVERIES, cv,
                COL_DELIVERY_ID + " = ?",
                new String[]{String.valueOf(deliveryId)});
    }

    public void markAsSynced(int deliveryId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_SYNCED, 1);
        db.update(TABLE_DELIVERIES, cv,
                COL_DELIVERY_ID + " = ?",
                new String[]{String.valueOf(deliveryId)});
    }

    // ── READ ─────────────────────────────────────────────────

    public List<Delivery> getTour() {
        List<Delivery> list = new ArrayList<>();
        SQLiteDatabase db   = getReadableDatabase();
        Cursor c = db.query(TABLE_DELIVERIES, null, null, null,
                null, null, COL_STOP_ORDER + " ASC");
        while (c.moveToNext()) list.add(cursorToDelivery(c));
        c.close();
        return list;
    }

    public Delivery getDelivery(int deliveryId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_DELIVERIES, null,
                COL_DELIVERY_ID + " = ?",
                new String[]{String.valueOf(deliveryId)},
                null, null, null);
        Delivery d = null;
        if (c.moveToFirst()) d = cursorToDelivery(c);
        c.close();
        return d;
    }

    public List<Delivery> getUnsyncedDelivered() { return getUnsyncedByStatus("DELIVERED"); }
    public List<Delivery> getUnsyncedFailed()    { return getUnsyncedByStatus("FAILED");    }

    private List<Delivery> getUnsyncedByStatus(String status) {
        List<Delivery> list = new ArrayList<>();
        SQLiteDatabase db   = getReadableDatabase();
        Cursor c = db.query(TABLE_DELIVERIES, null,
                COL_STATUS + " = ? AND " + COL_SYNCED + " = 0",
                new String[]{status}, null, null, null);
        while (c.moveToNext()) list.add(cursorToDelivery(c));
        c.close();
        return list;
    }

    /**
     * Returns true only if the cached tour was saved TODAY.
     * If it was saved on a previous day, returns false → forces API reload.
     */
    public boolean hasTourLoaded() {
        SQLiteDatabase db = getReadableDatabase();

        // Check count first
        Cursor countCursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_DELIVERIES, null);
        int count = 0;
        if (countCursor.moveToFirst()) count = countCursor.getInt(0);
        countCursor.close();
        if (count == 0) return false;

        // Check that the saved date matches today
        Cursor dateCursor = db.query(TABLE_META, new String[]{COL_VALUE},
                COL_KEY + " = ?", new String[]{META_TOUR_DATE},
                null, null, null);
        String savedDate = null;
        if (dateCursor.moveToFirst()) savedDate = dateCursor.getString(0);
        dateCursor.close();

        return todayString().equals(savedDate);
    }

    // Clear the cache so the next launch forces an API call
    public void clearTour() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_DELIVERIES, null, null);
        db.delete(TABLE_META, null, null);
    }

    // ── HELPERS ──────────────────────────────────────────────

    private Delivery cursorToDelivery(Cursor c) {
        return DeliveryBuilder.fromCursor(c);
    }

    private String todayString() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
    }

    private String getCurrentTimestamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",
                Locale.getDefault()).format(new Date());
    }
}
/*// database/LocalDatabase.java
// Base SQLite locale sur le téléphone du livreur
// Stocke les livraisons du jour et les changements de statut en attente de sync

package com.deliverpro.app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.deliverpro.app.model.Delivery;

import java.util.ArrayList;
import java.util.List;

public class LocalDatabase extends SQLiteOpenHelper {

    private static final String DB_NAME    = "deliverpro_local.db";
    private static final int    DB_VERSION = 1;

    // Table des livraisons locales
    public static final String TABLE_DELIVERIES = "local_deliveries";
    public static final String COL_DELIVERY_ID  = "delivery_id";
    public static final String COL_ORDER_NUMBER = "order_number";
    public static final String COL_STATUS       = "status";
    public static final String COL_CLIENT_NAME  = "client_name";
    public static final String COL_CLIENT_PHONE = "client_phone";
    public static final String COL_ADDRESS      = "delivery_address";
    public static final String COL_CITY         = "delivery_city";
    public static final String COL_AMOUNT       = "amount";
    public static final String COL_PAYMENT      = "payment_method";
    public static final String COL_NB_ARTICLES  = "nb_articles";
    public static final String COL_STOP_ORDER   = "stop_order";
    public static final String COL_FAILURE_REASON = "failure_reason";
    public static final String COL_SYNCED       = "is_synced";      // 0 = en attente, 1 = synchronisé
    public static final String COL_UPDATED_AT   = "updated_at";

    private static LocalDatabase instance;

    public static synchronized LocalDatabase getInstance(Context ctx) {
        if (instance == null) {
            instance = new LocalDatabase(ctx.getApplicationContext());
        }
        return instance;
    }

    private LocalDatabase(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_DELIVERIES + " (" +
                COL_DELIVERY_ID    + " INTEGER PRIMARY KEY, " +
                COL_ORDER_NUMBER   + " TEXT NOT NULL, " +
                COL_STATUS         + " TEXT DEFAULT 'PENDING', " +
                COL_CLIENT_NAME    + " TEXT, " +
                COL_CLIENT_PHONE   + " TEXT, " +
                COL_ADDRESS        + " TEXT, " +
                COL_CITY           + " TEXT, " +
                COL_AMOUNT         + " REAL DEFAULT 0, " +
                COL_PAYMENT        + " TEXT, " +
                COL_NB_ARTICLES    + " INTEGER DEFAULT 1, " +
                COL_STOP_ORDER     + " INTEGER DEFAULT 0, " +
                COL_FAILURE_REASON + " TEXT, " +
                COL_SYNCED         + " INTEGER DEFAULT 0, " +
                COL_UPDATED_AT     + " TEXT" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_DELIVERIES);
        onCreate(db);
    }

    // ── ÉCRITURE ─────────────────────────────────────────────

    // Sauvegarder la tournée du jour (appelé au démarrage de l'app livreur)
    public void saveTour(List<Delivery> deliveries) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            // Effacer l'ancienne tournée
            db.delete(TABLE_DELIVERIES, null, null);

            for (Delivery d : deliveries) {
                ContentValues cv = new ContentValues();
                cv.put(COL_DELIVERY_ID,  d.getDeliveryId());
                cv.put(COL_ORDER_NUMBER, d.getOrderNumber());
                cv.put(COL_STATUS,       d.getStatus());
                cv.put(COL_CLIENT_NAME,  d.getClientName());
                cv.put(COL_CLIENT_PHONE, d.getClientPhone());
                cv.put(COL_ADDRESS,      d.getDeliveryAddress());
                cv.put(COL_CITY,         d.getDeliveryCity());
                cv.put(COL_AMOUNT,       d.getAmount());
                cv.put(COL_PAYMENT,      d.getPaymentMethod());
                cv.put(COL_NB_ARTICLES,  d.getNbArticles());
                cv.put(COL_STOP_ORDER,   d.getStopOrder());
                cv.put(COL_SYNCED,       1); // Données fraîches du serveur = déjà sync
                db.insert(TABLE_DELIVERIES, null, cv);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // Mettre à jour le statut localement
    // is_synced = 0 → sera synchronisé selon la logique métier
    public void updateStatusLocal(int deliveryId, String status, String failureReason) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_STATUS,         status);
        cv.put(COL_FAILURE_REASON, failureReason);
        cv.put(COL_SYNCED,         0); // Marqué comme non synchronisé
        cv.put(COL_UPDATED_AT,     getCurrentTimestamp());
        db.update(TABLE_DELIVERIES, cv,
                COL_DELIVERY_ID + " = ?",
                new String[]{String.valueOf(deliveryId)});
    }

    // Marquer une livraison comme synchronisée
    public void markAsSynced(int deliveryId) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_SYNCED, 1);
        db.update(TABLE_DELIVERIES, cv,
                COL_DELIVERY_ID + " = ?",
                new String[]{String.valueOf(deliveryId)});
    }

    // ── LECTURE ──────────────────────────────────────────────

    // Lire toute la tournée du jour depuis la base locale
    public List<Delivery> getTour() {
        List<Delivery> list = new ArrayList<>();
        SQLiteDatabase db   = getReadableDatabase();
        Cursor c = db.query(TABLE_DELIVERIES, null, null, null,
                null, null, COL_STOP_ORDER + " ASC");

        while (c.moveToNext()) {
            list.add(cursorToDelivery(c));
        }
        c.close();
        return list;
    }

    // Récupérer une livraison par ID
    public Delivery getDelivery(int deliveryId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_DELIVERIES, null,
                COL_DELIVERY_ID + " = ?",
                new String[]{String.valueOf(deliveryId)},
                null, null, null);
        Delivery d = null;
        if (c.moveToFirst()) d = cursorToDelivery(c);
        c.close();
        return d;
    }

    // Livraisons DELIVERED non encore synchronisées → batch 23h59
    public List<Delivery> getUnsyncedDelivered() {
        return getUnsyncedByStatus("DELIVERED");
    }

    // Livraisons FAILED non encore synchronisées → remontée immédiate
    public List<Delivery> getUnsyncedFailed() {
        return getUnsyncedByStatus("FAILED");
    }

    private List<Delivery> getUnsyncedByStatus(String status) {
        List<Delivery> list = new ArrayList<>();
        SQLiteDatabase db   = getReadableDatabase();
        Cursor c = db.query(TABLE_DELIVERIES, null,
                COL_STATUS + " = ? AND " + COL_SYNCED + " = 0",
                new String[]{status},
                null, null, null);
        while (c.moveToNext()) list.add(cursorToDelivery(c));
        c.close();
        return list;
    }

    // Vérifier si la tournée est déjà chargée pour aujourd'hui
    public boolean hasTourLoaded() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_DELIVERIES, null);
        int count = 0;
        if (c.moveToFirst()) count = c.getInt(0);
        c.close();
        return count > 0;
    }

    // ── HELPERS ──────────────────────────────────────────────

    private Delivery cursorToDelivery(Cursor c) {
        // Utilisation de la reflection pour remplir le modèle
        // On crée un objet Delivery via un constructeur dédié
        return DeliveryBuilder.fromCursor(c);
    }

    private String getCurrentTimestamp() {
        return new java.text.SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss",
                java.util.Locale.getDefault())
                .format(new java.util.Date());
    }
}*/