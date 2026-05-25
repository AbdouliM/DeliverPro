// utils/SessionManager.java
// Gestion de la session : stockage du token JWT et infos utilisateur

package com.deliverpro.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.deliverpro.app.network.ApiClient;
public class SessionManager {

    private static final String PREF_NAME    = "DeliverProSession";
    private static final String KEY_TOKEN    = "token";
    private static final String KEY_USER_ID  = "user_id";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_FULLNAME = "full_name";
    private static final String KEY_ROLE     = "role";
    private static final String KEY_PHONE    = "phone";

    private static SessionManager instance;
    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager getInstance(Context context) {
        if (instance == null) {
            instance = new SessionManager(context);
        }
        return instance;
    }

    // Sauvegarder la session après login
    public void saveSession(String token, int userId, String username,
                            String fullName, String role, String phone) {
        prefs.edit()
                .putString(KEY_TOKEN,    token)
                .putInt(KEY_USER_ID,     userId)
                .putString(KEY_USERNAME, username)
                .putString(KEY_FULLNAME, fullName)
                .putString(KEY_ROLE,     role)
                .putString(KEY_PHONE,    phone)
                .apply();
    }

    // Getters
    public String  getToken()    { return prefs.getString(KEY_TOKEN,    null); }
    public int     getUserId()   { return prefs.getInt(KEY_USER_ID,     -1);  }
    public String  getUsername() { return prefs.getString(KEY_USERNAME, null); }
    public String  getFullName() { return prefs.getString(KEY_FULLNAME, null); }
    public String  getRole()     { return prefs.getString(KEY_ROLE,     null); }
    public String  getPhone()    { return prefs.getString(KEY_PHONE,    null); }

    // Vérifier si l'utilisateur est connecté
    public boolean isLoggedIn() {
        return getToken() != null;
    }

    // Vérifier le rôle
    public boolean isController() { return "CONTROLLER".equals(getRole()); }
    public boolean isDeliverer()  { return "DELIVERER".equals(getRole());  }

    // Déconnexion — vider la session
    public void logout() {
        prefs.edit().clear().apply();
        ApiClient.reset();
    }
}