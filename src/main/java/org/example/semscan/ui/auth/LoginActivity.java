package org.example.semscan.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import org.example.semscan.R;
import org.example.semscan.ui.RolePickerActivity;
import org.example.semscan.ui.teacher.PresenterHomeActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

public class LoginActivity extends AppCompatActivity {

    private static final String DEFAULT_SKIP_USERNAME = "skiptester";

    private TextInputEditText editUsername;
    private TextInputEditText editPassword;
    private MaterialButton btnLogin;
    private MaterialButton btnSkipAuth;

    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        preferencesManager = PreferencesManager.getInstance(this);

        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        btnSkipAuth = findViewById(R.id.btn_skip_auth);

        btnLogin.setOnClickListener(v -> handleLogin());
        btnSkipAuth.setOnClickListener(v -> handleSkipAuth());

        Logger.i(Logger.TAG_UI, "LoginActivity created");
    }

    private void handleLogin() {
        String normalized = normalizeInput(editUsername != null ? editUsername.getText() : null);
        if (TextUtils.isEmpty(normalized)) {
            if (editUsername != null) {
                editUsername.setError(getString(R.string.login_username_required));
                editUsername.requestFocus();
            }
            return;
        }

        if (editPassword != null) {
            String password = editPassword.getText() != null ? editPassword.getText().toString() : null;
            if (TextUtils.isEmpty(password)) {
                editPassword.setError(getString(R.string.login_password_required));
                editPassword.requestFocus();
                return;
            } else {
                editPassword.setError(null);
            }
        }

        preferencesManager.clearUserData();
        preferencesManager.setUserName(normalized);
        preferencesManager.setInitialSetupCompleted(false);

        Logger.userAction("Login", "User logged in with username=" + normalized);

        Intent intent;
        if (preferencesManager.hasCompletedInitialSetup()) {
            intent = new Intent(this, RolePickerActivity.class);
        } else {
            intent = new Intent(this, FirstTimeSetupActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void handleSkipAuth() {
        String normalized = normalizeInput(editUsername != null ? editUsername.getText() : null);
        if (TextUtils.isEmpty(normalized)) {
            normalized = DEFAULT_SKIP_USERNAME;
        }

        preferencesManager.clearUserData();
        preferencesManager.setUserName(normalized);
        preferencesManager.setUserRole("PRESENTER");
        preferencesManager.setFirstName("Skip");
        preferencesManager.setLastName("Tester");
        preferencesManager.setEmail(normalized + "@bgu.ac.il");
        preferencesManager.setDegree(FirstTimeSetupActivity.DEGREE_PHD);
        preferencesManager.setParticipationPreference(FirstTimeSetupActivity.PARTICIPATION_PRESENTER_ONLY);
        preferencesManager.setInitialSetupCompleted(true);

        Logger.userAction("SkipAuth", "Skip auth used with username=" + normalized);

        Toast.makeText(this, getString(R.string.login_skip_auth_success, normalized), Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(this, PresenterHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private String normalizeInput(@Nullable CharSequence raw) {
        if (raw == null) {
            return null;
        }
        String value = raw.toString().trim();
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        return value.toLowerCase(java.util.Locale.US).replaceAll("\\s+", "");
    }
}


