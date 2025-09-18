package org.example.semscan.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "semscan_prefs";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_API_BASE_URL = "api_base_url";
    private static final String KEY_TEACHER_API_KEY = "teacher_api_key";
    
    private static PreferencesManager instance;
    private SharedPreferences prefs;
    
    private PreferencesManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized PreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new PreferencesManager(context.getApplicationContext());
        }
        return instance;
    }
    
    // User Role
    public void setUserRole(String role) {
        prefs.edit().putString(KEY_USER_ROLE, role).apply();
    }
    
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }
    
    public boolean isTeacher() {
        return "teacher".equals(getUserRole());
    }
    
    public boolean isStudent() {
        return "student".equals(getUserRole());
    }
    
    public boolean hasRole() {
        return getUserRole() != null;
    }
    
    // User ID
    public void setUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }
    
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    // API Base URL
    public void setApiBaseUrl(String baseUrl) {
        prefs.edit().putString(KEY_API_BASE_URL, baseUrl).apply();
    }
    
    public String getApiBaseUrl() {
        return prefs.getString(KEY_API_BASE_URL, "http://10.0.2.2:8080/");
    }
    
    // Teacher API Key
    public void setTeacherApiKey(String apiKey) {
        prefs.edit().putString(KEY_TEACHER_API_KEY, apiKey).apply();
    }
    
    public String getTeacherApiKey() {
        return prefs.getString(KEY_TEACHER_API_KEY, null);
    }
    
    // Clear all preferences
    public void clearAll() {
        prefs.edit().clear().apply();
    }
    
    // Clear user data but keep settings
    public void clearUserData() {
        prefs.edit()
                .remove(KEY_USER_ROLE)
                .remove(KEY_USER_ID)
                .apply();
    }
}
