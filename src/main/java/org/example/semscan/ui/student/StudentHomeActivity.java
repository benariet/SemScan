package org.example.semscan.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.User;
import org.example.semscan.data.model.Seminar;
import java.util.List;
import org.example.semscan.ui.RolePickerActivity;
import org.example.semscan.ui.SettingsActivity;
import org.example.semscan.ui.qr.ModernQRScannerActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentHomeActivity extends AppCompatActivity {

    private CardView cardScanAttendance;
    private CardView cardManualAttendance;
    private Button btnSettings;
    private Button btnChangeRole;
    private TextView textWelcomeMessage;
    private PreferencesManager preferencesManager;
    private ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        Logger.i(Logger.TAG_UI, "StudentHomeActivity created");

        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();

        // Check if user is actually a student
        if (!preferencesManager.isStudent()) {
            Logger.w(Logger.TAG_UI, "User is not a student, navigating to role picker");
            navigateToRolePicker();
            return;
        }

        Logger.i(Logger.TAG_UI, "Student user authenticated, setting up UI");
        initializeViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initializeViews() {
        cardScanAttendance = findViewById(R.id.card_scan_attendance);
        cardManualAttendance = findViewById(R.id.card_manual_attendance);
        btnSettings = findViewById(R.id.btn_settings);
        btnChangeRole = findViewById(R.id.btn_change_role);
        textWelcomeMessage = findViewById(R.id.text_welcome_message);
        
        // Set personalized welcome message
        updateWelcomeMessage();
    }
    
    private void updateWelcomeMessage() {
        String userId = preferencesManager.getUserId();
        
        if (userId == null || userId.trim().isEmpty()) {
            textWelcomeMessage.setText("Welcome, Student!");
            Logger.w(Logger.TAG_UI, "No user ID found, using generic welcome message");
            return;
        }
        
        // For students, use a simple mapping approach since they don't have seminars
        // This is more appropriate for students who don't present seminars
        String displayName = getStudentDisplayName(userId);
        
        if (displayName != null && !displayName.trim().isEmpty()) {
            textWelcomeMessage.setText("Welcome, " + displayName + "!");
            Logger.i(Logger.TAG_UI, "Welcome message updated with student name: " + displayName);
        } else {
            textWelcomeMessage.setText("Welcome, Student!");
            Logger.w(Logger.TAG_UI, "No display name found for user: " + userId);
        }
    }
    
    private String getStudentDisplayName(String userId) {
        // Simple mapping for common student IDs
        // This can be expanded or made configurable
        switch (userId.toLowerCase()) {
            case "student-001":
                return "John Smith";
            case "student-002":
                return "Sarah Johnson";
            case "student-003":
                return "Mike Wilson";
            case "student-004":
                return "Emily Davis";
            case "student-005":
                return "Alex Brown";
            default:
                // Try to extract a name from the user ID
                if (userId.contains("-")) {
                    String[] parts = userId.split("-");
                    if (parts.length > 1) {
                        return "Student " + parts[1];
                    }
                }
                return null;
        }
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }

    private void setupClickListeners() {
        cardScanAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openQRScanner();
            }
        });

        cardManualAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openManualAttendanceRequest();
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openSettings();
            }
        });

        btnChangeRole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeRole();
            }
        });
    }

    private void openQRScanner() {
        Logger.userAction("Open QR Scanner", "Student clicked scan attendance");
        Intent intent = new Intent(this, ModernQRScannerActivity.class);
        startActivity(intent);
    }

    private void openManualAttendanceRequest() {
        Logger.userAction("Open Manual Attendance Request", "Student clicked manual attendance request");
        Intent intent = new Intent(this, ManualAttendanceRequestActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Logger.userAction("Open Settings", "Student clicked settings");
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void changeRole() {
        Logger.userAction("Change Role", "Student clicked change role");
        preferencesManager.clearUserData();
        navigateToRolePicker();
    }

    private void navigateToRolePicker() {
        Intent intent = new Intent(this, RolePickerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_student_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            openSettings();
            return true;
        } else if (id == R.id.action_change_role) {
            changeRole();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        // Prevent going back to role picker
        moveTaskToBack(true);
    }
}
