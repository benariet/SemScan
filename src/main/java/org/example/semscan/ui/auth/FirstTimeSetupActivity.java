package org.example.semscan.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.User;
import org.example.semscan.ui.RolePickerActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.ServerLogger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FirstTimeSetupActivity extends AppCompatActivity {

    public static final String EXTRA_SELECTED_DEGREE = "extra_selected_degree";
    public static final String EXTRA_SELECTED_PARTICIPATION = "extra_selected_participation";
    public static final String EXTRA_SELECTED_LABEL = "extra_selected_label";

    public static final String DEGREE_MSC = "MSc";
    public static final String DEGREE_PHD = "PhD";

    public static final String PARTICIPATION_PRESENTER_ONLY = "PRESENTER_ONLY";
    public static final String PARTICIPATION_PARTICIPANT_ONLY = "PARTICIPANT_ONLY";
    public static final String PARTICIPATION_BOTH = "BOTH";

    private TextInputEditText editFirstName;
    private TextInputEditText editLastName;
    private MaterialCardView cardDegree;
    private MaterialCardView cardParticipation;
    private TextView textSelectedDegree;
    private TextView textSelectedParticipation;
    private View btnSubmit;
    private CircularProgressIndicator progressBar;

    private PreferencesManager preferencesManager;
    private ServerLogger serverLogger;
    private ApiService apiService;

    private String selectedDegree;
    private String selectedParticipation;
    private String selectedParticipationLabel;

    private ActivityResultLauncher<Intent> degreeLauncher;
    private ActivityResultLauncher<Intent> participationLauncher;
    private ActivityResultLauncher<Intent> mscLauncher;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_time_setup);

        preferencesManager = PreferencesManager.getInstance(this);
        serverLogger = ServerLogger.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();

        initViews();
        initActivityResults();
        setupListeners();

        Logger.i(Logger.TAG_UI, "FirstTimeSetupActivity created for username=" + preferencesManager.getUserName());
        serverLogger.userAction("FirstTimeSetup", "Onboarding started");
    }

    private void initViews() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (toolbar != null) {
            toolbar.setNavigationOnClickListener(v -> onBackPressed());
        }

        editFirstName = findViewById(R.id.edit_first_name);
        editLastName = findViewById(R.id.edit_last_name);
        cardDegree = findViewById(R.id.card_degree);
        cardParticipation = findViewById(R.id.card_role_context);
        textSelectedDegree = findViewById(R.id.text_selected_degree);
        textSelectedParticipation = findViewById(R.id.text_selected_participation);
        btnSubmit = findViewById(R.id.btn_submit_setup);
        progressBar = findViewById(R.id.progress_loading);

        if (!TextUtils.isEmpty(preferencesManager.getFirstName())) {
            editFirstName.setText(preferencesManager.getFirstName());
        }
        if (!TextUtils.isEmpty(preferencesManager.getLastName())) {
            editLastName.setText(preferencesManager.getLastName());
        }
        if (!TextUtils.isEmpty(preferencesManager.getDegree())) {
            selectedDegree = preferencesManager.getDegree();
            textSelectedDegree.setText(selectedDegree);
        }
        if (!TextUtils.isEmpty(preferencesManager.getParticipationPreference())) {
            selectedParticipation = preferencesManager.getParticipationPreference();
            updateParticipationLabel();
        }
        updateParticipationCardState();
    }

    private void initActivityResults() {
        degreeLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedDegree = result.getData().getStringExtra(EXTRA_SELECTED_DEGREE);
                if (!TextUtils.isEmpty(selectedDegree)) {
                    textSelectedDegree.setText(selectedDegree);
                    serverLogger.userAction("FirstTimeSetup", "Degree selected=" + selectedDegree);
                    applyDegreeConstraints();
                }
            }
        });

        participationLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedParticipation = result.getData().getStringExtra(EXTRA_SELECTED_PARTICIPATION);
                selectedParticipationLabel = result.getData().getStringExtra(EXTRA_SELECTED_LABEL);
                updateParticipationLabel();
            }
        });

        mscLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                selectedParticipation = result.getData().getStringExtra(EXTRA_SELECTED_PARTICIPATION);
                selectedParticipationLabel = result.getData().getStringExtra(EXTRA_SELECTED_LABEL);
                updateParticipationLabel();
            }
        });
    }

    private void setupListeners() {
        cardDegree.setOnClickListener(v -> openDegreeSelection());
        cardParticipation.setOnClickListener(v -> openParticipationSelector());
        btnSubmit.setOnClickListener(v -> submitProfile());
    }

    private void openDegreeSelection() {
        degreeLauncher.launch(new Intent(this, DegreeSelectionActivity.class));
    }

    private void openParticipationSelector() {
        if (DEGREE_PHD.equals(selectedDegree)) {
            Toast.makeText(this, R.string.setup_participation_presenter_locked, Toast.LENGTH_SHORT).show();
            return;
        }
        if (DEGREE_MSC.equals(selectedDegree)) {
            mscLauncher.launch(new Intent(this, MScRoleSelectionActivity.class));
        } else {
            participationLauncher.launch(new Intent(this, RoleContextActivity.class));
        }
    }

    private void applyDegreeConstraints() {
        if (DEGREE_PHD.equals(selectedDegree)) {
            selectedParticipation = PARTICIPATION_PRESENTER_ONLY;
            selectedParticipationLabel = getString(R.string.setup_participation_presenter);
            updateParticipationLabel();
            updateParticipationCardState();
        } else {
            updateParticipationCardState();
            if (TextUtils.isEmpty(selectedParticipation)) {
                textSelectedParticipation.setText(R.string.setup_participation_placeholder);
            }
        }
    }

    private void updateParticipationCardState() {
        boolean phd = DEGREE_PHD.equals(selectedDegree);
        cardParticipation.setEnabled(!phd);
        cardParticipation.setAlpha(phd ? 0.6f : 1f);
    }

    private void updateParticipationLabel() {
        if (!TextUtils.isEmpty(selectedParticipationLabel)) {
            textSelectedParticipation.setText(selectedParticipationLabel);
        } else if (!TextUtils.isEmpty(selectedParticipation)) {
            switch (selectedParticipation) {
                case PARTICIPATION_PRESENTER_ONLY:
                    textSelectedParticipation.setText(R.string.setup_participation_presenter);
                    break;
                case PARTICIPATION_PARTICIPANT_ONLY:
                    textSelectedParticipation.setText(R.string.setup_participation_participant);
                    break;
                case PARTICIPATION_BOTH:
                    textSelectedParticipation.setText(R.string.setup_participation_both);
                    break;
                default:
                    textSelectedParticipation.setText(selectedParticipation);
                    break;
            }
        } else {
            textSelectedParticipation.setText(R.string.setup_participation_placeholder);
        }
        serverLogger.userAction("FirstTimeSetup", "Participation preference=" + selectedParticipation);
    }

    private void submitProfile() {
        String firstName = trim(editFirstName);
        String lastName = trim(editLastName);

        boolean valid = true;

        if (TextUtils.isEmpty(firstName)) {
            editFirstName.setError(getString(R.string.setup_error_first_name));
            valid = false;
        } else {
            editFirstName.setError(null);
        }

        if (TextUtils.isEmpty(lastName)) {
            editLastName.setError(getString(R.string.setup_error_last_name));
            valid = false;
        } else {
            editLastName.setError(null);
        }

        if (TextUtils.isEmpty(selectedDegree)) {
            Toast.makeText(this, R.string.setup_error_degree, Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (TextUtils.isEmpty(selectedParticipation)) {
            Toast.makeText(this, R.string.setup_error_participation, Toast.LENGTH_SHORT).show();
            valid = false;
        }

        if (!valid) {
            return;
        }

        toggleLoading(true);

        String username = preferencesManager.getUserName();
        
        // CRITICAL: Check if username exists before proceeding
        if (username == null || username.isEmpty()) {
            Logger.e(Logger.TAG_UI, "ERROR: Username is NULL or empty in FirstTimeSetupActivity!");
            Logger.e(Logger.TAG_UI, "Cannot create user profile without username. User must log in first.");
            toggleLoading(false);
            Toast.makeText(this, "Username not found. Please log in again.", Toast.LENGTH_LONG).show();
            // Navigate back to login
            Intent intent = new Intent(this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        
        Logger.d(Logger.TAG_UI, "Creating user profile with username: " + username);
        String email = username != null ? username + "@bgu.ac.il" : null;

        ApiService.UserProfileUpdateRequest request = new ApiService.UserProfileUpdateRequest(
                username,
                email,
                firstName,
                lastName,
                selectedDegree,
                selectedParticipation
        );

        // Create user directly - this is the first time creating the user
        serverLogger.api("POST", "/api/v1/users", "Creating new user profile for " + username);
        
        apiService.upsertUser(request).enqueue(new Callback<User>() {
            @Override
            public void onResponse(Call<User> call, Response<User> response) {
                toggleLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    String errorMsg = "Failed to create user profile";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            errorMsg = errorBody.length() > 100 ? errorBody.substring(0, 100) : errorBody;
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                    Logger.apiError("POST", "/api/v1/users", response.code(), errorMsg);
                    serverLogger.e(ServerLogger.TAG_API, "Failed to create user profile: " + errorMsg);
                    Toast.makeText(FirstTimeSetupActivity.this, getString(R.string.error) + ": " + errorMsg, Toast.LENGTH_LONG).show();
                    return;
                }

                persistProfile(firstName, lastName, email, selectedDegree, selectedParticipation);
                serverLogger.updateUserContext(preferencesManager.getUserName(), preferencesManager.getUserRole());
                Toast.makeText(FirstTimeSetupActivity.this, R.string.setup_success, Toast.LENGTH_LONG).show();
                Logger.userAction("FirstTimeSetup", "Onboarding complete");
                serverLogger.userAction("FirstTimeSetup", "Onboarding complete");
                navigateToRolePicker();
            }

            @Override
            public void onFailure(Call<User> call, Throwable t) {
                toggleLoading(false);
                Logger.e(Logger.TAG_API, "Failed to create user profile", t);
                serverLogger.e(ServerLogger.TAG_API, "Failed to create user profile", t);
                Toast.makeText(FirstTimeSetupActivity.this, getString(R.string.network_error_generic, t.getMessage()), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void persistProfile(String firstName, String lastName, String email, String degree, String participation) {
        // CRITICAL: Ensure username is preserved - it should already be set from login
        String username = preferencesManager.getUserName();
        if (username == null || username.isEmpty()) {
            Logger.e(Logger.TAG_UI, "WARNING: Username is NULL in persistProfile! This should not happen.");
            Logger.e(Logger.TAG_UI, "Username should have been set during login. User needs to log in again.");
        } else {
            // Explicitly preserve username (should already be set, but ensure it's not lost)
            preferencesManager.setUserName(username);
            Logger.d(Logger.TAG_UI, "Preserving username in persistProfile: " + username);
        }
        
        preferencesManager.setFirstName(firstName);
        preferencesManager.setLastName(lastName);
        preferencesManager.setEmail(email);
        preferencesManager.setDegree(degree);
        preferencesManager.setParticipationPreference(participation);
        preferencesManager.setInitialSetupCompleted(true);

        if (PARTICIPATION_PRESENTER_ONLY.equals(participation)) {
            preferencesManager.setUserRole("PRESENTER");
        } else if (PARTICIPATION_PARTICIPANT_ONLY.equals(participation)) {
            preferencesManager.setUserRole("PARTICIPANT"); // Changed from "STUDENT" to "PARTICIPANT" for consistency
        } else {
            preferencesManager.setUserRole(null);
        }
        
        // Final check: Log all saved values
        Logger.d(Logger.TAG_UI, "=== Profile Persisted ===");
        Logger.d(Logger.TAG_UI, "Username: " + preferencesManager.getUserName());
        Logger.d(Logger.TAG_UI, "Role: " + preferencesManager.getUserRole());
        Logger.d(Logger.TAG_UI, "First Name: " + preferencesManager.getFirstName());
        Logger.d(Logger.TAG_UI, "Last Name: " + preferencesManager.getLastName());
        Logger.d(Logger.TAG_UI, "Email: " + preferencesManager.getEmail());
        Logger.d(Logger.TAG_UI, "Degree: " + preferencesManager.getDegree());
        Logger.d(Logger.TAG_UI, "Participation: " + preferencesManager.getParticipationPreference());
        Logger.d(Logger.TAG_UI, "=========================");
    }

    private void navigateToRolePicker() {
        Intent intent = new Intent(this, RolePickerActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void toggleLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSubmit.setEnabled(!loading);
        cardDegree.setEnabled(!loading);
        if (loading) {
            cardParticipation.setEnabled(false);
            cardParticipation.setAlpha(0.6f);
        } else {
            updateParticipationCardState();
        }
        editFirstName.setEnabled(!loading);
        editLastName.setEnabled(!loading);
    }

    private String trim(@Nullable TextInputEditText editText) {
        if (editText == null || editText.getText() == null) {
            return null;
        }
        return editText.getText().toString().trim();
    }
}

