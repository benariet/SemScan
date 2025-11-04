package org.example.semscan.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.example.semscan.R;
import org.example.semscan.ui.student.StudentHomeActivity;
import org.example.semscan.ui.teacher.PresenterHomeActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

public class RoleContextActivity extends AppCompatActivity {

    private MaterialButtonToggleGroup toggleContextRole;
    private MaterialButton btnContinue;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_context);

        preferencesManager = PreferencesManager.getInstance(this);

        if (!preferencesManager.hasBothRoles()) {
            Logger.w(Logger.TAG_UI, "RoleContextActivity launched but user does not have BOTH roles. Redirecting to home.");
            navigateAccordingToRole(preferencesManager.getUserRole());
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        initializeSelection();
        setupListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void initializeViews() {
        toggleContextRole = findViewById(R.id.toggle_context_role);
        btnContinue = findViewById(R.id.btn_context_continue);
    }

    private void initializeSelection() {
        String activeRole = preferencesManager.getActiveRole();
        if ("PRESENTER".equals(activeRole)) {
            toggleContextRole.check(R.id.btn_context_presenter);
        } else {
            toggleContextRole.check(R.id.btn_context_student);
        }
    }

    private void setupListeners() {
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selection = toggleContextRole.getCheckedButtonId();
                if (selection == View.NO_ID) {
                    Toast.makeText(RoleContextActivity.this, getString(R.string.setup_select_context_warning), Toast.LENGTH_SHORT).show();
                    return;
                }

                if (selection == R.id.btn_context_presenter) {
                    preferencesManager.setActiveRole("PRESENTER");
                    navigateAccordingToRole("PRESENTER");
                } else {
                    preferencesManager.setActiveRole("STUDENT");
                    navigateAccordingToRole("STUDENT");
                }
            }
        });
    }

    private void navigateAccordingToRole(String role) {
        Intent intent;
        if ("PRESENTER".equals(role)) {
            Logger.i(Logger.TAG_UI, "RoleContextActivity navigating to PresenterHomeActivity");
            intent = new Intent(this, PresenterHomeActivity.class);
        } else {
            Logger.i(Logger.TAG_UI, "RoleContextActivity navigating to StudentHomeActivity");
            intent = new Intent(this, StudentHomeActivity.class);
        }
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

