package org.example.semscan.ui.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;

import org.example.semscan.R;
import org.example.semscan.ui.RolePickerActivity;
import org.example.semscan.ui.SettingsActivity;
import org.example.semscan.ui.teacher.CourseManagementActivity;
import org.example.semscan.ui.teacher.RecordsDashboardActivity;
import org.example.semscan.ui.teacher.TeacherAttendanceActivity;
import org.example.semscan.ui.teacher.TeacherStartSessionActivity;
import org.example.semscan.utils.PreferencesManager;

public class TeacherHomeActivity extends AppCompatActivity {
    
    private CardView cardStartSession;
    private CardView cardAttendance;
    private CardView cardExport;
    private CardView cardManageCourses;
    private CardView cardRecordsDashboard;
    private Button btnSettings;
    private Button btnChangeRole;
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_home);
        
        preferencesManager = PreferencesManager.getInstance(this);
        
        // Check if user is actually a teacher
        if (!preferencesManager.isTeacher()) {
            navigateToRolePicker();
            return;
        }
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
    }
    
    private void initializeViews() {
        cardStartSession = findViewById(R.id.card_start_session);
        cardAttendance = findViewById(R.id.card_attendance);
        cardExport = findViewById(R.id.card_export);
        cardManageCourses = findViewById(R.id.card_manage_courses);
        cardRecordsDashboard = findViewById(R.id.card_records_dashboard);
        btnSettings = findViewById(R.id.btn_settings);
        btnChangeRole = findViewById(R.id.btn_change_role);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        }
    }
    
    private void setupClickListeners() {
        cardStartSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStartSession();
            }
        });
        
        cardAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAttendanceManagement();
            }
        });
        
        cardExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openExport();
            }
        });
        
        cardManageCourses.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCourseManagement();
            }
        });
        
        cardRecordsDashboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openRecordsDashboard();
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
    
    private void openStartSession() {
        Intent intent = new Intent(this, TeacherStartSessionActivity.class);
        startActivity(intent);
    }
    
    private void openAttendanceManagement() {
        Intent intent = new Intent(this, TeacherAttendanceActivity.class);
        startActivity(intent);
    }
    
    private void openExport() {
        Intent intent = new Intent(this, ExportActivity.class);
        startActivity(intent);
    }
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    }
    
    private void openCourseManagement() {
        Intent intent = new Intent(this, CourseManagementActivity.class);
        startActivity(intent);
    }
    
    private void openRecordsDashboard() {
        Intent intent = new Intent(this, RecordsDashboardActivity.class);
        startActivity(intent);
    }
    
    private void changeRole() {
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
        getMenuInflater().inflate(R.menu.menu_teacher_home, menu);
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
