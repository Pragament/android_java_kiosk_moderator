package com.example.teacherapp;

import android.content.Context;
import android.content.SharedPreferences;

public class PrefManager {

    private final SharedPreferences preferences;
    private static final  String PREF_NAME = "Login_Pref";

    public PrefManager(Context context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveLogin(String userId) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("userId", userId);
        editor.putBoolean("isLogin", true);
        editor.apply();
    }

    public String getUserId() {
        return preferences.getString("userId", null);
    }

    public boolean isLogin() {
        return preferences.getBoolean("isLogin", false);
    }
}
