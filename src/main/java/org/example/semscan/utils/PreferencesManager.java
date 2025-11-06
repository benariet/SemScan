package org.example.semscan.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Locale;

public class PreferencesManager {
    private static final String PREFS_NAME = "semscan_prefs";
    private static final String KEY_USER_ROLE = "user_role";
    private static final String KEY_USERNAME = "bgu_username";
    private static final String KEY_FIRST_NAME = "first_name";
    private static final String KEY_LAST_NAME = "last_name";
    private static final String KEY_EMAIL = "user_email";
    private static final String KEY_DEGREE = "user_degree";
    private static final String KEY_PARTICIPATION = "participation_preference";
    private static final String KEY_SETUP_COMPLETED = "initial_setup_completed";
    
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
    
    // User Name
    public void setUserName(String userName) {
        String normalized = userName;
        if (normalized != null) {
            normalized = normalized.trim().toLowerCase(Locale.US);
        }
        Logger.prefs(KEY_USERNAME, normalized);
        if (normalized == null || normalized.isEmpty()) {
            prefs.edit().remove(KEY_USERNAME).apply();
        } else {
            prefs.edit().putString(KEY_USERNAME, normalized).apply();
        }
    }
    
    public String getUserName() {
        return prefs.getString(KEY_USERNAME, null);
    }

    public void setFirstName(String firstName) {
        Logger.prefs(KEY_FIRST_NAME, firstName);
        prefs.edit().putString(KEY_FIRST_NAME, firstName).apply();
    }

    public String getFirstName() {
        return prefs.getString(KEY_FIRST_NAME, null);
    }

    public void setLastName(String lastName) {
        Logger.prefs(KEY_LAST_NAME, lastName);
        prefs.edit().putString(KEY_LAST_NAME, lastName).apply();
    }

    public String getLastName() {
        return prefs.getString(KEY_LAST_NAME, null);
    }

    public void setEmail(String email) {
        Logger.prefs(KEY_EMAIL, email);
        prefs.edit().putString(KEY_EMAIL, email).apply();
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public void setDegree(String degree) {
        Logger.prefs(KEY_DEGREE, degree);
        prefs.edit().putString(KEY_DEGREE, degree).apply();
    }

    public String getDegree() {
        return prefs.getString(KEY_DEGREE, null);
    }

    public void setParticipationPreference(String preference) {
        Logger.prefs(KEY_PARTICIPATION, preference);
        prefs.edit().putString(KEY_PARTICIPATION, preference).apply();
    }

    public String getParticipationPreference() {
        return prefs.getString(KEY_PARTICIPATION, null);
    }

    public void setInitialSetupCompleted(boolean completed) {
        Logger.prefs(KEY_SETUP_COMPLETED, String.valueOf(completed));
        prefs.edit().putBoolean(KEY_SETUP_COMPLETED, completed).apply();
    }

    public boolean hasCompletedInitialSetup() {
        return prefs.getBoolean(KEY_SETUP_COMPLETED, false);
    }
    
    
    // Clear all preferences
    public void clearAll() {
        prefs.edit().clear().apply();
    }
    
    // Clear user data but keep settings
    public void clearUserData() {
        prefs.edit()
                .remove(KEY_USER_ROLE)
                .remove(KEY_USERNAME)
                .remove(KEY_FIRST_NAME)
                .remove(KEY_LAST_NAME)
                .remove(KEY_EMAIL)
                .remove(KEY_DEGREE)
                .remove(KEY_PARTICIPATION)
                .remove(KEY_SETUP_COMPLETED)
                .apply();
    }
}
