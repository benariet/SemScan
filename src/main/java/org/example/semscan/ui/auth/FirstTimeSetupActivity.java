package org.example.semscan.ui.auth;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;

import org.example.semscan.R;
import org.example.semscan.ui.student.StudentHomeActivity;
import org.example.semscan.ui.teacher.PresenterHomeActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

public class FirstTimeSetupActivity extends AppCompatActivity {

    private RadioGroup radioDegree;
    private RadioButton radioDegreeMsc;
    private RadioButton radioDegreePhd;
    private MaterialButtonToggleGroup toggleRole;
    private MaterialButton btnContinue;

    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_time_setup);

        preferencesManager = PreferencesManager.getInstance(this);

        setupToolbar();
        initializeViews();
        setupListeners();
        initializeSelections();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void initializeViews() {
        radioDegree = findViewById(R.id.radio_degree);
        radioDegreeMsc = findViewById(R.id.radio_degree_msc);
        radioDegreePhd = findViewById(R.id.radio_degree_phd);
        toggleRole = findViewById(R.id.toggle_role);
        btnContinue = findViewById(R.id.btn_continue);
    }

    private void setupListeners() {
        radioDegree.setOnCheckedChangeListener((group, checkedId) -> handleDegreeSelection(checkedId));

        btnContinue.setOnClickListener(v -> attemptSave());
    }

    private void initializeSelections() {
        // Default selections
        if (radioDegree.getCheckedRadioButtonId() == View.NO_ID) {
            radioDegree.check(R.id.radio_degree_msc);
        }
        if (toggleRole.getCheckedButtonId() == View.NO_ID) {
            toggleRole.check(R.id.btn_role_participant);
        }
    }

    private void handleDegreeSelection(int checkedId) {
        if (checkedId == R.id.radio_degree_phd) {
            toggleRole.check(R.id.btn_role_presenter);
            setRoleButtonsEnabled(false);
        } else {
            setRoleButtonsEnabled(true);
        }
    }

    private void setRoleButtonsEnabled(boolean enableParticipantAndBoth) {
        MaterialButton participant = findViewById(R.id.btn_role_participant);
        MaterialButton both = findViewById(R.id.btn_role_both);

        participant.setEnabled(enableParticipantAndBoth);
        both.setEnabled(enableParticipantAndBoth);

        if (!enableParticipantAndBoth) {
            participant.setAlpha(0.4f);
            both.setAlpha(0.4f);
        } else {
            participant.setAlpha(1f);
            both.setAlpha(1f);
        }
    }

    private void attemptSave() {
        int degreeSelection = radioDegree.getCheckedRadioButtonId();
        int roleSelection = toggleRole.getCheckedButtonId();

        if (degreeSelection == View.NO_ID || roleSelection == View.NO_ID) {
            Toast.makeText(this, getString(R.string.setup_select_options_warning), Toast.LENGTH_SHORT).show();
            return;
        }

        String degree = degreeSelection == R.id.radio_degree_phd ? "PhD" : "MSc";
        String role;
        if (roleSelection == R.id.btn_role_both) {
            role = "BOTH";
        } else if (roleSelection == R.id.btn_role_presenter) {
            role = "PRESENTER";
        } else {
            role = "STUDENT";
        }

        String message = getString(R.string.confirm_setup_message, degree, role);

        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_setup_title)
                .setMessage(message)
                .setPositiveButton(R.string.save_and_continue, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveConfiguration(degree, role);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void saveConfiguration(String degree, String role) {
        Logger.i(Logger.TAG_UI, "Saving first-time configuration: degree=" + degree + ", role=" + role);

        preferencesManager.setUserDegree(degree);
        preferencesManager.setUserRole(role);
        preferencesManager.setFirstTimeLogin(false);

        if ("BOTH".equals(role)) {
            preferencesManager.setActiveRole("STUDENT");
            launchRoleContextPicker();
        } else if ("PRESENTER".equals(role)) {
            preferencesManager.setActiveRole("PRESENTER");
            navigateToPresenterHome();
        } else {
            preferencesManager.setActiveRole("STUDENT");
            navigateToStudentHome();
        }
    }

    private void navigateToPresenterHome() {
        Intent intent = new Intent(this, PresenterHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void navigateToStudentHome() {
        Intent intent = new Intent(this, StudentHomeActivity.class);
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
}

