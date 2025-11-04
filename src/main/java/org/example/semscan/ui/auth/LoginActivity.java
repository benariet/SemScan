package org.example.semscan.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.User;
import org.example.semscan.ui.student.StudentHomeActivity;
import org.example.semscan.ui.teacher.PresenterHomeActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private View loginForm;
    private EditText editEmail;
    private EditText editPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        Logger.i(Logger.TAG_UI, "LoginActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        
        initializeViews();
        setupListeners();

        String storedEmail = preferencesManager.getUserEmail();
        if (!TextUtils.isEmpty(storedEmail)) {
            editEmail.setText(storedEmail);
        }

        showLoginForm(true);
    }

    private void initializeViews() {
        loginForm = findViewById(R.id.login_form);
        editEmail = findViewById(R.id.edit_email);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_login);
    }

    private void setupListeners() {
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    private void attemptLogin() {
        String emailInput = editEmail.getText().toString().trim();

        if (TextUtils.isEmpty(emailInput)) {
            editEmail.setError(getString(R.string.login_email_hint));
            editEmail.requestFocus();
            return;
        }

        showLoginForm(false);

        // Reset previous state and persist new credentials
        preferencesManager.clearUserData();
        preferencesManager.setUserDegree(null);
        preferencesManager.setFirstTimeLogin(true);
        preferencesManager.setUserEmail(emailInput);
        preferencesManager.setBguUsername(emailInput);

        Long derivedUserId;
        try {
            derivedUserId = Long.parseLong(emailInput);
        } catch (NumberFormatException numberFormatException) {
            derivedUserId = (long) Math.abs(emailInput.hashCode());
        }
        preferencesManager.setUserId(derivedUserId);

        Logger.i(Logger.TAG_UI, "User signed in with ID: " + derivedUserId);

        ApiService.CreateOrUpdateUserRequest request =
                new ApiService.CreateOrUpdateUserRequest(derivedUserId, emailInput, null, null);

        apiService.createOrUpdateUser(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                if (!response.isSuccessful()) {
                    Logger.e(Logger.TAG_API, "Failed to register user. Code: " + response.code());
                    Toast.makeText(LoginActivity.this, R.string.login_error_register, Toast.LENGTH_LONG).show();
                    showLoginForm(true);
                    return;
                }

                User user = response.body();
                if (user != null) {
                    if (user.getUserId() != null) {
                        preferencesManager.setUserId(user.getUserId());
                    }

                    if (!TextUtils.isEmpty(user.getEmail())) {
                        preferencesManager.setUserEmail(user.getEmail());
                        preferencesManager.setBguUsername(user.getEmail());
                    }

                    preferencesManager.setUserName(user.getFullName());
                }

                proceedAfterLogin();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                Logger.e(Logger.TAG_API, "Failed to register user", t);
                Toast.makeText(LoginActivity.this, R.string.login_error_register, Toast.LENGTH_LONG).show();
                showLoginForm(true);
            }
        });
    }
    
    private void proceedAfterLogin() {
        String degree = preferencesManager.getUserDegree();
        String role = preferencesManager.getUserRole();
        boolean isFirstTime = preferencesManager.isFirstTimeLogin();

        if (isFirstTime || degree == null || role == null) {
            Logger.i(Logger.TAG_UI, "First-time configuration required, launching setup");
            launchFirstTimeSetup();
            return;
        }

        if ("BOTH".equals(role)) {
            Logger.i(Logger.TAG_UI, "User has BOTH roles, launching context picker");
            launchRoleContextPicker();
            return;
        }

        navigateToHome();
    }

    private void launchFirstTimeSetup() {
        Intent intent = new Intent(this, FirstTimeSetupActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void launchRoleContextPicker() {
        Intent intent = new Intent(this, RoleContextActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void navigateToHome() {
        Intent intent;
        String targetActivity;
        
        String role = preferencesManager.getUserRole();

        if (preferencesManager.hasBothRoles()) {
            String activeRole = preferencesManager.getActiveRole();
            if ("PRESENTER".equals(activeRole)) {
                intent = new Intent(this, PresenterHomeActivity.class);
                targetActivity = "PresenterHomeActivity";
            } else {
                intent = new Intent(this, StudentHomeActivity.class);
                targetActivity = "StudentHomeActivity";
            }
        } else if (preferencesManager.isPresenter() && !preferencesManager.isStudent()) {
            intent = new Intent(this, PresenterHomeActivity.class);
            targetActivity = "PresenterHomeActivity";
        } else if (preferencesManager.isStudent() && !preferencesManager.isPresenter()) {
            intent = new Intent(this, StudentHomeActivity.class);
            targetActivity = "StudentHomeActivity";
        } else {
            Logger.w(Logger.TAG_UI, "No valid role found for navigation, returning to setup");
            launchFirstTimeSetup();
            return;
        }
        
        Logger.i(Logger.TAG_UI, "Navigating to: " + targetActivity);
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void showLoginForm(boolean show) {
        loginForm.setVisibility(show ? View.VISIBLE : View.INVISIBLE);
        progressBar.setVisibility(show ? View.GONE : View.VISIBLE);
        editEmail.setEnabled(show);
        editPassword.setEnabled(show);
        btnLogin.setEnabled(show);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}

