package com.zihowl.thecalendar.data.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREFS = "auth_prefs";
    private static final String KEY_TOKEN = "jwt_token";
    private static final String KEY_USER = "username";

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public void saveSession(String username, String token) {
        prefs.edit().putString(KEY_USER, username).putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public String getUsername() {
        return prefs.getString(KEY_USER, "");
    }

    public void clear() {
        prefs.edit().clear().apply();
    }
}
