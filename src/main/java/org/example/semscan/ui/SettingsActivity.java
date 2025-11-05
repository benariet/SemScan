package org.example.semscan.ui;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import org.example.semscan.R;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.ServerLogger;
import org.example.semscan.data.api.ApiClient;

public class SettingsActivity extends AppCompatActivity {
    
    private EditText editUserId;
    private EditText editApiUrl;
    private Button btnSave;
    private Button btnClearData;
    private Button btnLoggingSettings;
    private Button btnLoginTest;
    
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        Logger.i(Logger.TAG_UI, "SettingsActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        loadCurrentSettings();
    }
    
    private void initializeViews() {
        editUserId = findViewById(R.id.edit_user_id);
        editApiUrl = findViewById(R.id.edit_api_url);
        btnSave = findViewById(R.id.btn_save);
        btnClearData = findViewById(R.id.btn_clear_data);
        btnLoggingSettings = findViewById(R.id.btn_logging_settings);
        btnLoginTest = findViewById(R.id.btn_login_test);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupClickListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
        
        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearDataDialog();
            }
        });
        
        btnLoggingSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLoggingSettingsDialog();
            }
        });
        
        btnLoginTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openLoginTest();
            }
        });
    }
    
    private void loadCurrentSettings() {
        String email = preferencesManager.getUserEmail();
        String bguUsername = preferencesManager.getBguUsername();
        Long userId = preferencesManager.getUserId();
        String apiUrl = preferencesManager.getApiBaseUrl();
        String degree = preferencesManager.getUserDegree();
        
        Logger.i(Logger.TAG_UI, "Loading current settings");
        Logger.d(Logger.TAG_UI, "Current User ID: " + userId + ", email: " + email + ", bguUsername: " + bguUsername);
        Logger.d(Logger.TAG_UI, "Current API URL: " + apiUrl);
        
        if (!TextUtils.isEmpty(bguUsername)) {
            editUserId.setText(bguUsername);
        } else if (!TextUtils.isEmpty(email)) {
            editUserId.setText(email);
        } else if (userId != null && userId > 0) {
            editUserId.setText(String.valueOf(userId));
        } else {
            editUserId.setText("");
        }
        editApiUrl.setText(apiUrl);
        
        if ("PhD".equalsIgnoreCase(degree)) {
            radioDegreePhd.setChecked(true);
            radioDegreeMsc.setChecked(false);
        } else {
            radioDegreeMsc.setChecked(true);
            radioDegreePhd.setChecked(false);
        }
    }
    
    private void saveSettings() {
        Logger.userAction("Save Settings", "User clicked save settings button");
        
        String userIdInput = editUserId.getText().toString().trim();
        String apiUrl = editApiUrl.getText().toString().trim();
        int selectedDegreeId = radioDegree.getCheckedRadioButtonId();
        
        Logger.d(Logger.TAG_UI, "Attempting to save settings - User ID: " + userIdInput + ", API URL: " + apiUrl);
        
        // Validate inputs
        if (userIdInput.isEmpty()) {
            Logger.w(Logger.TAG_UI, "Save settings failed - User ID (email) is empty");
            Toast.makeText(this, "User ID (email) is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (apiUrl.isEmpty()) {
            Logger.w(Logger.TAG_UI, "Save settings failed - API URL is empty");
            Toast.makeText(this, "API URL is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedDegreeId == View.NO_ID) {
            Logger.w(Logger.TAG_UI, "Save settings failed - Degree not selected");
            Toast.makeText(this, "Please select your degree", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            // Save settings
            preferencesManager.setBguUsername(userIdInput);
            preferencesManager.setUserEmail(userIdInput);
            Long derivedUserId;
            try {
                derivedUserId = Long.parseLong(userIdInput);
            } catch (NumberFormatException numberFormatException) {
                // Derive a stable numeric ID from the email string
                derivedUserId = (long) Math.abs(userIdInput.hashCode());
                Logger.i(Logger.TAG_UI, "Derived numeric user ID from email: " + derivedUserId);
            }
            preferencesManager.setUserId(derivedUserId);
            preferencesManager.setApiBaseUrl(apiUrl);
            String selectedDegree = selectedDegreeId == R.id.radio_degree_phd ? "PhD" : "MSc";
            preferencesManager.setUserDegree(selectedDegree);
            if ("PhD".equals(selectedDegree)) {
                preferencesManager.setUserRole("PRESENTER");
            } else {
                preferencesManager.setUserRole(null);
            }

            String loggerRole = preferencesManager.getActiveRole();
            if (loggerRole == null) {
                loggerRole = preferencesManager.getUserRole();
            }
            ServerLogger.getInstance(this).updateUserContext(preferencesManager.getUserId(), loggerRole, userIdInput);
            
            // Update API client with new base URL
            ApiClient.getInstance(this).updateBaseUrl(this);
            
            Logger.i(Logger.TAG_UI, "Settings saved successfully");
            
            // Show success message with longer duration
            Toast.makeText(this, "✅ Settings saved successfully!", Toast.LENGTH_LONG).show();
            
            // Also show in logs for debugging
            Logger.i(Logger.TAG_UI, "Settings saved - User ID: " + derivedUserId + ", BGU Username: " + userIdInput + ", Degree: " + selectedDegree + ", API URL: " + apiUrl);
            
        } catch (Exception e) {
            Logger.e(Logger.TAG_UI, "Failed to save settings", e);
            Toast.makeText(this, "❌ Failed to save settings: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void showClearDataDialog() {
        Logger.userAction("Clear Data Dialog", "User clicked clear data button");
        
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("This will clear all user data and settings. Are you sure?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearAllData();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Logger.i(Logger.TAG_UI, "Clear data cancelled by user");
                    }
                })
                .show();
    }
    
    private void clearAllData() {
        Logger.userAction("Clear All Data", "User confirmed clearing all data");
        Logger.i(Logger.TAG_UI, "Clearing all user data and settings");
        
        preferencesManager.clearAll();
        
        Logger.i(Logger.TAG_UI, "All data cleared successfully");
        Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
        
        // Navigate back to role picker
        finish();
    }
    
    private void showLoggingSettingsDialog() {
        Logger.userAction("Logging Settings", "User clicked logging settings button");
        
        new AlertDialog.Builder(this)
                .setTitle("Logging Settings")
                .setMessage("This feature allows you to configure logging levels and settings for debugging purposes.\n\n" +
                           "Current logging is automatically configured for optimal performance.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Logger.i(Logger.TAG_UI, "Logging settings dialog closed");
                    }
                })
                .show();
    }
    
    private void openLoginTest() {
        Logger.userAction("Login Test", "User clicked login test button");
        
        Intent intent = new Intent(this, org.example.semscan.ui.auth.LoginActivity.class);
        startActivity(intent);
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
