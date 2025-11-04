package org.example.semscan.utils;

import android.content.Context;
import android.content.SharedPreferences;
import org.example.semscan.constants.ApiConstants;

public class PreferencesManager {
    private static final String PREFS_NAME = "semscan_prefs";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_BGU_USERNAME = "bgu_username";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_API_BASE_URL = "api_base_url";
    private static final String KEY_USER_DEGREE = "user_degree";
    private static final String KEY_IS_FIRST_TIME_LOGIN = "is_first_time_login";
    private static final String KEY_ACTIVE_ROLE = "active_role";
    
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
        if ("BOTH".equals(role)) {
            if (getActiveRole() == null) {
                setActiveRole("STUDENT");
            }
        } else if (role != null) {
            setActiveRole(role);
        } else {
            setActiveRole(null);
        }
    }
    
    public String getUserRole() {
        return prefs.getString(KEY_USER_ROLE, null);
    }
    
    public boolean isPresenter() {
        String role = getUserRole();
        return "PRESENTER".equals(role) || "BOTH".equals(role);
    }
    
    public boolean isTeacher() {
        return isPresenter(); // For backward compatibility
    }
    
    public boolean isStudent() {
        String role = getUserRole();
        return "STUDENT".equals(role) || "BOTH".equals(role);
    }
    
    public boolean hasBothRoles() {
        return "BOTH".equals(getUserRole());
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

    // User Email (for display as User ID)
    public void setUserEmail(String email) {
        Logger.prefs(KEY_USER_EMAIL, email);
        if (email == null || email.trim().isEmpty()) {
            prefs.edit().remove(KEY_USER_EMAIL).apply();
        } else {
            prefs.edit().putString(KEY_USER_EMAIL, email.trim()).apply();
        }
    }

    public String getUserEmail() {
        return prefs.getString(KEY_USER_EMAIL, null);
    }

    public void setBguUsername(String username) {
        Logger.prefs(KEY_BGU_USERNAME, username);
        if (username == null || username.trim().isEmpty()) {
            prefs.edit().remove(KEY_BGU_USERNAME).apply();
        } else {
            prefs.edit().putString(KEY_BGU_USERNAME, username.trim()).apply();
        }
    }

    public String getBguUsername() {
        return prefs.getString(KEY_BGU_USERNAME, null);
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
    
    // User Degree
    public void setUserDegree(String degree) {
        Logger.prefs(KEY_USER_DEGREE, degree);
        if (degree == null) {
            prefs.edit().remove(KEY_USER_DEGREE).apply();
        } else {
            prefs.edit().putString(KEY_USER_DEGREE, degree).apply();
        }
    }
    
    public String getUserDegree() {
        return prefs.getString(KEY_USER_DEGREE, null);
    }
    
    public boolean hasDegree() {
        return getUserDegree() != null;
    }
    
    public boolean isMSc() {
        return "MSc".equals(getUserDegree());
    }
    
    public boolean isPhD() {
        return "PhD".equals(getUserDegree());
    }
    
    // First-time login flag
    public void setFirstTimeLogin(boolean isFirstTime) {
        Logger.prefs(KEY_IS_FIRST_TIME_LOGIN, String.valueOf(isFirstTime));
        prefs.edit().putBoolean(KEY_IS_FIRST_TIME_LOGIN, isFirstTime).apply();
    }
    
    public boolean isFirstTimeLogin() {
        return prefs.getBoolean(KEY_IS_FIRST_TIME_LOGIN, true);
    }
    
    // Clear user data but keep settings
    // Note: Degree is preserved - it's a fundamental property that doesn't change when changing roles
    public void clearUserData() {
        prefs.edit()
                .remove(KEY_USER_ROLE)
                .remove(KEY_USER_ID)
                .remove(KEY_USER_NAME)
                .remove(KEY_USER_EMAIL)
                .remove(KEY_BGU_USERNAME)
                .remove(KEY_ACTIVE_ROLE)
                // Don't remove KEY_USER_DEGREE - degree persists across role changes
                .apply();
    }

    // Active role helpers
    public void setActiveRole(String role) {
        Logger.prefs(KEY_ACTIVE_ROLE, role);
        if (role == null) {
            prefs.edit().remove(KEY_ACTIVE_ROLE).apply();
        } else {
            prefs.edit().putString(KEY_ACTIVE_ROLE, role).apply();
        }
    }

    public String getActiveRole() {
        return prefs.getString(KEY_ACTIVE_ROLE, null);
    }
}
