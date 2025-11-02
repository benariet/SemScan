package org.example.semscan.ui.auth;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.ServerLogger;
import org.example.semscan.utils.ToastUtils;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    
    private EditText editUsername;
    private EditText editPassword;
    private Button btnLogin;
    private TextView textResponse;
    private CheckBox checkRememberCredentials;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private ServerLogger serverLogger;
    private SharedPreferences loginPrefs;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        Logger.i(Logger.TAG_UI, "LoginActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        serverLogger = ServerLogger.getInstance(this);
        loginPrefs = getSharedPreferences("login_credentials", MODE_PRIVATE);
        
        // Clear any existing saved credentials for testing consistency
        clearSavedCredentials();
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        loadSavedCredentials();
    }
    
    private void initializeViews() {
        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        textResponse = findViewById(R.id.text_response);
        checkRememberCredentials = findViewById(R.id.check_remember_credentials);
        
        // Don't pre-fill anything here - let loadSavedCredentials() handle it
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                getSupportActionBar().setTitle("Login");
            }
        }
    }
    
    private void setupClickListeners() {
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                performLogin();
            }
        });
    }
    
    /**
     * Load saved credentials from SharedPreferences
     */
    private void loadSavedCredentials() {
        // Always start with empty fields and unchecked checkbox
        editUsername.setText("");
        editPassword.setText("");
        checkRememberCredentials.setChecked(false);
        
        boolean credentialsSaved = loginPrefs.getBoolean("credentials_saved", false);
        if (credentialsSaved) {
            String savedUsername = loginPrefs.getString("saved_username", "");
            String savedPassword = loginPrefs.getString("saved_password", "");
            
            if (!TextUtils.isEmpty(savedUsername) && !TextUtils.isEmpty(savedPassword)) {
                editUsername.setText(savedUsername);
                editPassword.setText(savedPassword);
                checkRememberCredentials.setChecked(true);
                
                Logger.d(Logger.TAG_UI, "Loaded saved credentials for user: " + savedUsername);
            } else {
                // Invalid saved credentials, clear them
                clearSavedCredentials();
                // Fall back to test credentials
                editUsername.setText("student_username");
                editPassword.setText("student_password");
                Logger.d(Logger.TAG_UI, "Invalid saved credentials, using test credentials");
            }
        } else {
            // No saved credentials, pre-fill with test credentials
            editUsername.setText("student_username");
            editPassword.setText("student_password");
            Logger.d(Logger.TAG_UI, "No saved credentials, using test credentials");
        }
    }
    
    /**
     * Save credentials to SharedPreferences
     */
    private void saveCredentials(String username, String password) {
        SharedPreferences.Editor editor = loginPrefs.edit();
        editor.putString("saved_username", username);
        editor.putString("saved_password", password);
        editor.putBoolean("credentials_saved", true);
        editor.apply();
        
        Logger.d(Logger.TAG_UI, "Saved credentials for user: " + username);
    }
    
    /**
     * Clear saved credentials from SharedPreferences
     */
    private void clearSavedCredentials() {
        SharedPreferences.Editor editor = loginPrefs.edit();
        editor.remove("saved_username");
        editor.remove("saved_password");
        editor.putBoolean("credentials_saved", false);
        editor.apply();
        
        Logger.d(Logger.TAG_UI, "Cleared saved credentials");
    }
    
    /**
     * Show error dialog for wrong credentials
     */
    private void showWrongCredentialsDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Login Failed")
                .setMessage("Username or password is incorrect")
                .setPositiveButton("OK", null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
    
    /**
     * Navigate to RolePickerActivity after successful login
     */
    private void navigateToRolePicker() {
        Intent intent = new Intent(this, org.example.semscan.ui.RolePickerActivity.class);
        startActivity(intent);
        finish(); // Close the login activity so user can't go back
    }
    
    private void performLogin() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();
        
        // Validate input
        if (TextUtils.isEmpty(username)) {
            ToastUtils.showError(this, "Please enter username");
            editUsername.requestFocus();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            ToastUtils.showError(this, "Please enter password");
            editPassword.requestFocus();
            return;
        }
        
        // Disable button during login
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");
        
        // Clear previous response
        textResponse.setText("Sending login request...");
        
        Logger.userAction("Login Attempt", "Username: " + username);
        Logger.api("POST", "api/v1/auth/login", "Username: " + username);
        
        // Log the request details to server
        serverLogger.i(Logger.TAG_UI, "Login attempt started for user: " + username);
        
        // Create login request
        ApiService.LoginRequest loginRequest = new ApiService.LoginRequest(username, password);
        
        // Make API call
        Call<ApiService.LoginResponse> call = apiService.login(loginRequest);
        call.enqueue(new Callback<ApiService.LoginResponse>() {
            @Override
            public void onResponse(Call<ApiService.LoginResponse> call, Response<ApiService.LoginResponse> response) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                
                if (response.isSuccessful() && response.body() != null) {
                    ApiService.LoginResponse loginResponse = response.body();
                    
                    // Check if login was actually successful based on the 'ok' field
                    if (loginResponse.ok) {
                        // Login was successful
                        Logger.apiResponse("POST", "api/v1/auth/login", response.code(), 
                            "Login successful: " + loginResponse.message);
                        
                        // Log to server with comprehensive details
                        String responseDetails = String.format("ok: %s, message: %s", 
                            loginResponse.ok, 
                            loginResponse.message != null ? loginResponse.message : "null");
                        
                        // Log the full response data
                        serverLogger.i(Logger.TAG_UI, "Login successful for user: " + username + 
                            " - Response: " + responseDetails + 
                            " - Status Code: " + response.code());
                        
                        // Also log the raw response for debugging
                        serverLogger.d(Logger.TAG_UI, "Login API Response - Username: " + username + 
                            " - Full Response: {\"ok\":" + loginResponse.ok + 
                            ",\"message\":\"" + (loginResponse.message != null ? loginResponse.message : "null") + "\"}");
                        
                        serverLogger.flushLogs();
                        
                        // Display success response
                        String responseText = "✅ LOGIN SUCCESSFUL\n\n" +
                                           "Status Code: " + response.code() + "\n" +
                                           "Response: {\n" +
                                           "  \"ok\": " + loginResponse.ok + ",\n" +
                                           "  \"message\": \"" + loginResponse.message + "\"\n" +
                                           "}\n\n" +
                                           "Username: " + username + "\n" +
                                           "Timestamp: " + java.text.SimpleDateFormat.getDateTimeInstance().format(new java.util.Date());
                        
                        textResponse.setText(responseText);
                        ToastUtils.showSuccess(LoginActivity.this, "Login successful!");
                        
                        // Handle credential saving based on checkbox
                        if (checkRememberCredentials.isChecked()) {
                            saveCredentials(username, password);
                        } else {
                            clearSavedCredentials();
                        }
                        
                        // Navigate to RolePickerActivity after successful login
                        navigateToRolePicker();
                        
                    } else {
                        // Login failed (HTTP 200 but ok: false)
                        Logger.apiError("POST", "api/v1/auth/login", response.code(), 
                            "Login failed - ok: false, message: " + loginResponse.message);
                        
                        // Log failed login to server
                        serverLogger.e(Logger.TAG_UI, "Login failed for user: " + username + 
                            " - Response: ok: false, message: " + (loginResponse.message != null ? loginResponse.message : "null") + 
                            " - Status Code: " + response.code());
                        serverLogger.flushLogs();
                        
                        // Display failed login response
                        String responseText = "❌ LOGIN FAILED\n\n" +
                                           "Status Code: " + response.code() + "\n" +
                                           "Response: {\n" +
                                           "  \"ok\": " + loginResponse.ok + ",\n" +
                                           "  \"message\": \"" + loginResponse.message + "\"\n" +
                                           "}\n\n" +
                                           "Username: " + username + "\n" +
                                           "Timestamp: " + java.text.SimpleDateFormat.getDateTimeInstance().format(new java.util.Date());
                        
                        textResponse.setText(responseText);
                        showWrongCredentialsDialog();
                    }
                    
                } else {
                    // Handle error response
                    String errorBody = null;
                    if (response.errorBody() != null) {
                        try {
                            errorBody = response.errorBody().string();
                        } catch (Exception e) {
                            Logger.e(Logger.TAG_UI, "Error reading login response body", e);
                        }
                    }
                    
                    Logger.apiError("POST", "api/v1/auth/login", response.code(), errorBody);
                    
                    // Log error to server
                    serverLogger.e(Logger.TAG_UI, "Login failed for user: " + username + 
                        " - Code: " + response.code() + " - Error: " + errorBody);
                    serverLogger.flushLogs();
                    
                    // Display error response
                    String responseText = "❌ LOGIN FAILED\n\n" +
                                       "Status Code: " + response.code() + "\n" +
                                       "Error: " + (errorBody != null ? errorBody : "Unknown error") + "\n\n" +
                                       "Username: " + username + "\n" +
                                       "Timestamp: " + java.text.SimpleDateFormat.getDateTimeInstance().format(new java.util.Date());
                    
                    textResponse.setText(responseText);
                    
                    // Show appropriate error dialog based on status code
                    if (response.code() == 401 || response.code() == 403) {
                        showWrongCredentialsDialog();
                    } else {
                        ToastUtils.showError(LoginActivity.this, "Login failed: " + response.code());
                    }
                }
            }
            
            @Override
            public void onFailure(Call<ApiService.LoginResponse> call, Throwable t) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                
                Logger.e(Logger.TAG_UI, "Login network failure", t);
                
                // Log network error to server
                serverLogger.e(Logger.TAG_UI, "Login network failure for user: " + 
                    editUsername.getText().toString() + " - Error: " + t.getMessage());
                serverLogger.flushLogs();
                
                // Display network error
                String responseText = "❌ NETWORK ERROR\n\n" +
                                   "Error: " + t.getMessage() + "\n\n" +
                                   "Username: " + editUsername.getText().toString() + "\n" +
                                   "Timestamp: " + java.text.SimpleDateFormat.getDateTimeInstance().format(new java.util.Date());
                
                textResponse.setText(responseText);
                ToastUtils.showError(LoginActivity.this, "Network error: " + t.getMessage());
            }
        });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    protected void onDestroy() {
        if (serverLogger != null) {
            serverLogger.flushLogs();
        }
        super.onDestroy();
    }
}
