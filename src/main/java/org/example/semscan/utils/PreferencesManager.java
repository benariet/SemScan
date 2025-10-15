package org.example.semscan.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "semscan_prefs";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_API_BASE_URL = "api_base_url";
    private static final String KEY_PRESENTER_API_KEY = "presenter_api_key";
    
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
        Logger.prefs(KEY_USER_ROLE, role);
        prefs.edit().putString(KEY_USER_ROLE, role).apply();
    }
    
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }
    
    public boolean isPresenter() {
        return "PRESENTER".equals(getUserRole());
    }
    
    public boolean isTeacher() {
        return "PRESENTER".equals(getUserRole()); // For backward compatibility
    }
    
    public boolean isStudent() {
        return "STUDENT".equals(getUserRole());
    }
    
    public boolean hasRole() {
        return getUserRole() != null;
    }
    
    // User ID
    public void setUserId(String userId) {
        Logger.prefs(KEY_USER_ID, userId);
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }
    
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, "presenter-001");
    }
    
    // API Base URL
    public void setApiBaseUrl(String baseUrl) {
        Logger.prefs(KEY_API_BASE_URL, baseUrl);
        prefs.edit().putString(KEY_API_BASE_URL, baseUrl).apply();
    }
    
    public String getApiBaseUrl() {
        return prefs.getString(KEY_API_BASE_URL, "http://132.72.54.104:8080/");
    }
    
    // Presenter API Key
    public void setPresenterApiKey(String apiKey) {
        Logger.prefs(KEY_PRESENTER_API_KEY, apiKey != null ? "[HIDDEN]" : "null");
        prefs.edit().putString(KEY_PRESENTER_API_KEY, apiKey).apply();
    }
    
    public String getPresenterApiKey() {
        return prefs.getString(KEY_PRESENTER_API_KEY, "presenter-001-api-key-12345");
    }
    
    // For backward compatibility
    public void setTeacherApiKey(String apiKey) {
        setPresenterApiKey(apiKey);
    }
    
    public String getTeacherApiKey() {
        return getPresenterApiKey();
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
