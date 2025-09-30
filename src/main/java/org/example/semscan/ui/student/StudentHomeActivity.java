package org.example.semscan.ui.student;

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
import org.example.semscan.ui.qr.QRScannerActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

public class StudentHomeActivity extends AppCompatActivity {

    private CardView cardScanAttendance;
    private Button btnSettings;
    private Button btnChangeRole;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        Logger.i(Logger.TAG_UI, "StudentHomeActivity created");

        preferencesManager = PreferencesManager.getInstance(this);

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
        cardScanAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openQRScanner();
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
        Intent intent = new Intent(this, QRScannerActivity.class);
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
