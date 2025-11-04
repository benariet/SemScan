package org.example.semscan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.example.semscan.R;
import org.example.semscan.ui.auth.FirstTimeSetupActivity;
import org.example.semscan.ui.auth.RoleContextActivity;
import org.example.semscan.ui.student.StudentHomeActivity;
import org.example.semscan.ui.teacher.PresenterHomeActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

public class RolePickerActivity extends AppCompatActivity {
    
    private CardView cardPresenter;
    private CardView cardStudent;
    private Button btnSettings;
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_picker);
        
        Logger.i(Logger.TAG_UI, "RolePickerActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        
        boolean hasDegree = preferencesManager.hasDegree();
        if (!hasDegree) {
            Logger.w(Logger.TAG_UI, "No degree stored for user, redirecting to first time setup");
            Intent intent = new Intent(this, FirstTimeSetupActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        if (preferencesManager.isPhD()) {
            Logger.i(Logger.TAG_UI, "PhD users bypass role picker and navigate to presenter home");
            Intent intent = new Intent(this, PresenterHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        // Continue with MSc role selection UI
        
        initializeViews();
        setupClickListeners();
        
        // Check if user already has a role selected
        if (preferencesManager.hasRole()) {
            String currentRole = preferencesManager.getUserRole();
            Logger.i(Logger.TAG_UI, "User already has role: " + currentRole + ", navigating to home");
            navigateToHome();
        } else {
            Logger.i(Logger.TAG_UI, "No role selected, showing role picker");
        }
    }
    
    private void initializeViews() {
        cardPresenter = findViewById(R.id.card_presenter);
        cardStudent = findViewById(R.id.card_student);
        btnSettings = findViewById(R.id.btn_settings);
    }
    
    private void setupClickListeners() {
        cardPresenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRole("PRESENTER");
            }
        });
        
        cardStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRole("STUDENT");
            }
        });
        
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });
    }
    
    private void selectRole(String role) {
        Logger.userAction("Select Role", "User selected role: " + role);
        Logger.i(Logger.TAG_UI, "Setting user role to: " + role);
        
        preferencesManager.setUserRole(role);
        if ("BOTH".equals(role)) {
            preferencesManager.setActiveRole("STUDENT");
            Intent intent = new Intent(this, RoleContextActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            preferencesManager.setActiveRole(role);
            navigateToHome();
        }
    }
    
    private void navigateToHome() {
        Intent intent;
        String targetActivity;
        
        if (preferencesManager.hasBothRoles()) {
            String activeRole = preferencesManager.getActiveRole();
            if ("PRESENTER".equals(activeRole)) {
                intent = new Intent(this, PresenterHomeActivity.class);
                targetActivity = "PresenterHomeActivity";
            } else {
                intent = new Intent(this, StudentHomeActivity.class);
                targetActivity = "StudentHomeActivity";
            }
        } else if (preferencesManager.isPresenter()) {
            intent = new Intent(this, PresenterHomeActivity.class);
            targetActivity = "PresenterHomeActivity";
        } else if (preferencesManager.isStudent()) {
            intent = new Intent(this, StudentHomeActivity.class);
            targetActivity = "StudentHomeActivity";
        } else {
            // This shouldn't happen, but just in case
            Logger.w(Logger.TAG_UI, "No valid role found for navigation");
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Logger.i(Logger.TAG_UI, "Navigating to: " + targetActivity);
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void openSettings() {
        Logger.userAction("Open Settings", "User clicked settings from role picker");
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    @Override
    public void onBackPressed() {
        // Prevent going back from role picker
        moveTaskToBack(true);
    }
}
