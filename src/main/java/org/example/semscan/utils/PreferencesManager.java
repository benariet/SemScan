package org.example.semscan.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.example.semscan.constants.ApiConstants;

public class PreferencesManager {
    private static final String PREFS_NAME = "semscan_prefs";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_API_BASE_URL = "api_base_url";
    
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
    public void setUserId(Long userId) {
        Logger.prefs(KEY_USER_ID, userId == null ? "null" : String.valueOf(userId));
        if (userId == null) {
            prefs.edit().remove(KEY_USER_ID).apply();
        } else {
            prefs.edit().putLong(KEY_USER_ID, userId).apply();
        }
    }
    
    public Long getUserId() {
        if (!prefs.contains(KEY_USER_ID)) {
            return null;
        }
        try {
            return prefs.getLong(KEY_USER_ID, -1L);
        } catch (ClassCastException classCastException) {
            // Handle migration from legacy string IDs
            String stored = prefs.getString(KEY_USER_ID, null);
            if (stored == null) {
                return null;
            }
            try {
                Long parsed = Long.parseLong(stored.replaceAll("\\D", ""));
                setUserId(parsed);
                return parsed;
            } catch (NumberFormatException ignore) {
                Logger.e(Logger.TAG_PREFS, "Failed to parse legacy user ID: " + stored, ignore);
                prefs.edit().remove(KEY_USER_ID).apply();
                return null;
            }
        }
    }
    
    // User Name
    public void setUserName(String userName) {
        Logger.prefs(KEY_USER_NAME, userName);
        prefs.edit().putString(KEY_USER_NAME, userName).apply();
    }
    
    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, null);
    }
    
    // API Base URL
    public void setApiBaseUrl(String baseUrl) {
        Logger.prefs(KEY_API_BASE_URL, baseUrl);
        prefs.edit().putString(KEY_API_BASE_URL, baseUrl).apply();
    }
    
    public String getApiBaseUrl() {
        return prefs.getString(KEY_API_BASE_URL, ApiConstants.SERVER_URL + "/");
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
                .remove(KEY_USER_NAME)
                .apply();
    }
}
