package org.example.semscan.ui.student;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;

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
import org.example.semscan.utils.ServerLogger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentHomeActivity extends AppCompatActivity {

    private CardView cardScanAttendance;
    private CardView cardManualAttendance;
    private TextView textWelcomeMessage;
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private ServerLogger serverLogger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        Logger.i(Logger.TAG_UI, "StudentHomeActivity created");
        Logger.userAction("Open Student Home", "Student home screen opened");

        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        serverLogger = ServerLogger.getInstance(this);
        
        // Update user context for student logging
        Long userId = preferencesManager.getUserId();
        String userRole = preferencesManager.getUserRole();
        serverLogger.updateUserContext(userId, userRole);
        
        // Test logging to verify student context
        serverLogger.i(ServerLogger.TAG_UI, "StudentHomeActivity created - User: " + userId + ", Role: " + userRole);

        // Check if user is actually a student
        if (!preferencesManager.isStudent()) {
            Logger.w(Logger.TAG_UI, "User is not a student, navigating to role picker");
            navigateToRolePicker();
            return;
        }

        Logger.i(Logger.TAG_UI, "Student user authenticated, setting up UI");
        if (serverLogger != null) {
            serverLogger.userAction("Student Authenticated", "Student home setup initialized");
        }
        initializeViews();
        setupToolbar();
        setupClickListeners();
    }

    private void initializeViews() {
        cardScanAttendance = findViewById(R.id.card_scan_attendance);
        cardManualAttendance = findViewById(R.id.card_manual_attendance);
        textWelcomeMessage = findViewById(R.id.text_welcome_message);
        
        // Set personalized welcome message
        updateWelcomeMessage();
        cardScanAttendance.setBackgroundResource(R.drawable.card_dashboard_background);
        cardManualAttendance.setBackgroundResource(R.drawable.card_dashboard_background);
    }
    
    private void updateWelcomeMessage() {
        Long userId = preferencesManager.getUserId();
        
        if (userId == null || userId <= 0) {
            textWelcomeMessage.setText("Welcome, Student!");
            Logger.w(Logger.TAG_UI, "No user ID found, using generic welcome message");
            return;
        }
        
        textWelcomeMessage.setText("Welcome, Student!");

        apiService.getStudentUserById(userId).enqueue(new retrofit2.Callback<org.example.semscan.data.model.User>() {
            @Override
            public void onResponse(Call<org.example.semscan.data.model.User> call, Response<org.example.semscan.data.model.User> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Logger.w(Logger.TAG_UI, "Failed to fetch student name. Code: " + response.code());
                    return;
                }

                org.example.semscan.data.model.User user = response.body();
                String fullName = user.getFullName();
                if (fullName != null) {
                    fullName = fullName.trim();
                }

                if (fullName != null && !fullName.isEmpty()) {
                    textWelcomeMessage.setText("Welcome, " + fullName + "!");
                    Logger.i(Logger.TAG_UI, "Welcome message updated with student name: " + fullName);
                    if (serverLogger != null) {
                        serverLogger.i(ServerLogger.TAG_UI, "Student resolved to: " + fullName);
                    }
                }
            }

            @Override
            public void onFailure(Call<org.example.semscan.data.model.User> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to load student profile", t);
                if (serverLogger != null) {
                    serverLogger.e(ServerLogger.TAG_UI, "Failed to load student profile", t);
                }
            }
        });
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
                Logger.userAction("Open QR Scanner", "Student tapped scan attendance card");
                if (serverLogger != null) {
                    serverLogger.userAction("Open QR Scanner", "Student tapped scan attendance card");
                }
                openQRScanner();
            }
        });

        cardManualAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.userAction("Open Manual Attendance", "Student tapped manual attendance card");
                if (serverLogger != null) {
                    serverLogger.userAction("Open Manual Attendance", "Student tapped manual attendance card");
                }
                openManualAttendanceRequest();
            }
        });
    }

    private void openQRScanner() {
        Logger.userAction("Navigate", "Launching ModernQRScannerActivity");
        if (serverLogger != null) {
            serverLogger.userAction("Navigate", "Launching ModernQRScannerActivity");
        }
        Intent intent = new Intent(this, ModernQRScannerActivity.class);
        startActivity(intent);
    }

    private void openManualAttendanceRequest() {
        Logger.userAction("Navigate", "Launching ManualAttendanceRequestActivity");
        if (serverLogger != null) {
            serverLogger.userAction("Navigate", "Launching ManualAttendanceRequestActivity");
        }
        Intent intent = new Intent(this, ManualAttendanceRequestActivity.class);
        startActivity(intent);
    }

    private void openSettings() {
        Logger.userAction("Navigate", "Launching SettingsActivity from student home");
        if (serverLogger != null) {
            serverLogger.userAction("Navigate", "Launching SettingsActivity from student home");
        }
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }

    private void changeRole() {
        Logger.userAction("Change Role", "Student clicked change role");
        serverLogger.userAction("Change Role", "Student clicked change role");
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
            Logger.userAction("Open Settings", "Student selected settings from menu");
            if (serverLogger != null) {
                serverLogger.userAction("Open Settings", "Student selected settings from menu");
            }
            openSettings();
            return true;
        } else if (id == R.id.action_change_role) {
            Logger.userAction("Change Role", "Student selected change role from menu");
            if (serverLogger != null) {
                serverLogger.userAction("Change Role", "Student selected change role from menu");
            }
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
    
    @Override
    protected void onDestroy() {
        if (serverLogger != null) {
            serverLogger.flushLogs();
        }
        super.onDestroy();
    }
}
