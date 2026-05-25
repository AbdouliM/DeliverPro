// database/DeliveryBuilder.java
// Helper pour construire un objet Delivery depuis un Cursor SQLite

package com.deliverpro.app.database;

import android.database.Cursor;

import com.deliverpro.app.model.Delivery;

import org.json.JSONException;
import org.json.JSONObject;

public class DeliveryBuilder {

    public static Delivery fromCursor(Cursor c) {
        // Construire un JSON depuis le cursor puis le parser avec Gson
        // C'est la façon la plus propre sans modifier le modèle Delivery
        try {
            JSONObject json = new JSONObject();
            json.put("delivery_id",      getInt(c, LocalDatabase.COL_DELIVERY_ID));
            json.put("order_number",     getString(c, LocalDatabase.COL_ORDER_NUMBER));
            json.put("status",           getString(c, LocalDatabase.COL_STATUS));
            json.put("client_name",      getString(c, LocalDatabase.COL_CLIENT_NAME));
            json.put("client_phone",     getString(c, LocalDatabase.COL_CLIENT_PHONE));
            json.put("delivery_address", getString(c, LocalDatabase.COL_ADDRESS));
            json.put("delivery_city",    getString(c, LocalDatabase.COL_CITY));
            json.put("amount",           getDouble(c, LocalDatabase.COL_AMOUNT));
            json.put("payment_method",   getString(c, LocalDatabase.COL_PAYMENT));
            json.put("nb_articles",      getInt(c, LocalDatabase.COL_NB_ARTICLES));
            json.put("stop_order",       getInt(c, LocalDatabase.COL_STOP_ORDER));
            json.put("failure_reason",   getString(c, LocalDatabase.COL_FAILURE_REASON));

            com.google.gson.Gson gson = new com.google.gson.Gson();
            return gson.fromJson(json.toString(), Delivery.class);

        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String getString(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getString(idx) : null;
    }

    private static int getInt(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getInt(idx) : 0;
    }

    private static double getDouble(Cursor c, String col) {
        int idx = c.getColumnIndex(col);
        return idx >= 0 ? c.getDouble(idx) : 0.0;
    }
}