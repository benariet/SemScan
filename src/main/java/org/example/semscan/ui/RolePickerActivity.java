package org.example.semscan.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.example.semscan.R;
import org.example.semscan.ui.student.StudentHomeActivity;
import org.example.semscan.ui.teacher.TeacherHomeActivity;
import org.example.semscan.utils.PreferencesManager;

public class RolePickerActivity extends AppCompatActivity {
    
    private CardView cardTeacher;
    private CardView cardStudent;
    private Button btnSettings;
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_role_picker);
        
        preferencesManager = PreferencesManager.getInstance(this);
        
        initializeViews();
        setupClickListeners();
        
        // Check if user already has a role selected
        if (preferencesManager.hasRole()) {
            navigateToHome();
        }
    }
    
    private void initializeViews() {
        cardTeacher = findViewById(R.id.card_teacher);
        cardStudent = findViewById(R.id.card_student);
        btnSettings = findViewById(R.id.btn_settings);
    }
    
    private void setupClickListeners() {
        cardTeacher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRole("teacher");
            }
        });
        
        cardStudent.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectRole("student");
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
        preferencesManager.setUserRole(role);
        navigateToHome();
    }
    
    private void navigateToHome() {
        Intent intent;
        if (preferencesManager.isTeacher()) {
            intent = new Intent(this, TeacherHomeActivity.class);
        } else if (preferencesManager.isStudent()) {
            intent = new Intent(this, StudentHomeActivity.class);
        } else {
            // This shouldn't happen, but just in case
            Toast.makeText(this, "Please select a role", Toast.LENGTH_SHORT).show();
            return;
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    @Override
    public void onBackPressed() {
        // Prevent going back from role picker
        moveTaskToBack(true);
    }
}
