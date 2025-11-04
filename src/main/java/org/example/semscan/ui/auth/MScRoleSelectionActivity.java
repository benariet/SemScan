package org.example.semscan.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.google.android.material.card.MaterialCardView;

import org.example.semscan.R;
import org.example.semscan.ui.student.StudentHomeActivity;
import org.example.semscan.ui.teacher.PresenterHomeActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

public class MScRoleSelectionActivity extends AppCompatActivity {
    
    private MaterialCardView cardPresenter;
    private MaterialCardView cardParticipant;
    private MaterialCardView cardBoth;
    private CheckBox checkPresenter;
    private CheckBox checkParticipant;
    private CheckBox checkBoth;
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msc_role_selection);
        
        Logger.i(Logger.TAG_UI, "MScRoleSelectionActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        
        // Ensure user is MSc
        if (!preferencesManager.isMSc()) {
            Logger.w(Logger.TAG_UI, "User is not MSc, cannot select roles");
            finish();
            return;
        }
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        loadCurrentSelection();
        updateCardBackgrounds(); // Set initial card states
    }
    
    private void initializeViews() {
        cardPresenter = findViewById(R.id.card_presenter);
        cardParticipant = findViewById(R.id.card_participant);
        cardBoth = findViewById(R.id.card_both);
        checkPresenter = findViewById(R.id.check_presenter);
        checkParticipant = findViewById(R.id.check_participant);
        checkBoth = findViewById(R.id.check_both);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);
                getSupportActionBar().setDisplayShowHomeEnabled(true);
                getSupportActionBar().setTitle("Select Your Role");
            }
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    navigateBackToDegreeSelection();
                }
            });
        }
    }
    
    private void setupClickListeners() {
        // Card clicks toggle checkboxes
        cardPresenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPresenter.setChecked(!checkPresenter.isChecked());
                updateSelection();
            }
        });
        
        cardParticipant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkParticipant.setChecked(!checkParticipant.isChecked());
                updateSelection();
            }
        });
        
        cardBoth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkBoth.setChecked(!checkBoth.isChecked());
                updateSelection();
            }
        });
        
        // Checkbox changes update selection
        checkPresenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSelection();
            }
        });
        
        checkParticipant.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSelection();
            }
        });
        
        checkBoth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateSelection();
            }
        });
        
        // Confirm button
        View btnConfirm = findViewById(R.id.btn_confirm);
        if (btnConfirm != null) {
            btnConfirm.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    confirmSelection();
                }
            });
        }
    }
    
    private void loadCurrentSelection() {
        String role = preferencesManager.getUserRole();
        if (role != null) {
            if ("BOTH".equals(role)) {
                checkPresenter.setChecked(true);
                checkParticipant.setChecked(true);
                checkBoth.setChecked(true);
            } else if ("PRESENTER".equals(role)) {
                checkPresenter.setChecked(true);
                checkParticipant.setChecked(false);
                checkBoth.setChecked(false);
            } else if ("STUDENT".equals(role)) {
                checkPresenter.setChecked(false);
                checkParticipant.setChecked(true);
                checkBoth.setChecked(false);
            }
        }
    }
    
    private void updateSelection() {
        // If "both" is checked, automatically check presenter and participant
        if (checkBoth.isChecked()) {
            checkPresenter.setChecked(true);
            checkParticipant.setChecked(true);
        } else if (checkPresenter.isChecked() && checkParticipant.isChecked()) {
            // If both presenter and participant are checked, check "both"
            checkBoth.setChecked(true);
        } else {
            checkBoth.setChecked(false);
        }
        
        // Update card backgrounds based on selection state
        updateCardBackgrounds();
        
        Logger.d(Logger.TAG_UI, "Selection updated - Presenter: " + checkPresenter.isChecked() + 
            ", Participant: " + checkParticipant.isChecked() + ", Both: " + checkBoth.isChecked());
    }
    
    private void updateCardBackgrounds() {
        int selectedStroke = getResources().getDimensionPixelSize(R.dimen.role_card_stroke_selected);
        int defaultStroke = getResources().getDimensionPixelSize(R.dimen.role_card_stroke_default);
        
        // Update Presenter card
        if (checkPresenter.isChecked()) {
            cardPresenter.setStrokeColor(ContextCompat.getColor(this, R.color.primary_blue));
            cardPresenter.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_50));
            cardPresenter.setCardElevation(6f);
            cardPresenter.setStrokeWidth(selectedStroke);
        } else {
            cardPresenter.setStrokeColor(ContextCompat.getColor(this, R.color.gray_300));
            cardPresenter.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            cardPresenter.setCardElevation(2f);
            cardPresenter.setStrokeWidth(defaultStroke);
        }
        
        // Update Participant card
        if (checkParticipant.isChecked()) {
            cardParticipant.setStrokeColor(ContextCompat.getColor(this, R.color.primary_blue));
            cardParticipant.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_50));
            cardParticipant.setCardElevation(6f);
            cardParticipant.setStrokeWidth(selectedStroke);
        } else {
            cardParticipant.setStrokeColor(ContextCompat.getColor(this, R.color.gray_300));
            cardParticipant.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            cardParticipant.setCardElevation(2f);
            cardParticipant.setStrokeWidth(defaultStroke);
        }
        
        // Update Both card
        if (checkBoth.isChecked()) {
            cardBoth.setStrokeColor(ContextCompat.getColor(this, R.color.primary_blue));
            cardBoth.setCardBackgroundColor(ContextCompat.getColor(this, R.color.gray_50));
            cardBoth.setCardElevation(6f);
            cardBoth.setStrokeWidth(selectedStroke);
        } else {
            cardBoth.setStrokeColor(ContextCompat.getColor(this, R.color.gray_300));
            cardBoth.setCardBackgroundColor(ContextCompat.getColor(this, R.color.white));
            cardBoth.setCardElevation(2f);
            cardBoth.setStrokeWidth(defaultStroke);
        }
    }
    
    private void confirmSelection() {
        boolean presenterChecked = checkPresenter.isChecked();
        boolean participantChecked = checkParticipant.isChecked();
        boolean bothChecked = checkBoth.isChecked();
        
        if (!presenterChecked && !participantChecked && !bothChecked) {
            Toast.makeText(this, getString(R.string.please_select_role), Toast.LENGTH_SHORT).show();
            return;
        }
        
        String selectedRole;
        if (bothChecked || (presenterChecked && participantChecked)) {
            selectedRole = "BOTH";
            Logger.userAction("Select Role", "User selected: Both (Presenter and Participant) for this semester");
        } else if (presenterChecked) {
            selectedRole = "PRESENTER";
            Logger.userAction("Select Role", "User selected: Presenter for this semester");
        } else {
            selectedRole = "STUDENT";
            Logger.userAction("Select Role", "User selected: Participant for this semester");
        }
        
        Logger.i(Logger.TAG_UI, "Setting user role to: " + selectedRole + " for this semester");
        preferencesManager.setUserRole(selectedRole);
        
        // Mark that user has completed first-time login
        preferencesManager.setFirstTimeLogin(false);
        
        // Navigate based on selection
        navigateAfterSelection(selectedRole);
    }
    
    private void navigateAfterSelection(String role) {
        if ("BOTH".equals(role)) {
            // User has both roles - default to participant home, can switch later
            Logger.i(Logger.TAG_UI, "User selected both roles, navigating to participant home");
            Intent intent = new Intent(this, StudentHomeActivity.class);
            intent.putExtra("has_both_roles", true);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else if ("PRESENTER".equals(role)) {
            Logger.i(Logger.TAG_UI, "User selected presenter for this semester, navigating to presenter home");
            Intent intent = new Intent(this, PresenterHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            Logger.i(Logger.TAG_UI, "User selected participant for this semester, navigating to student home");
            Intent intent = new Intent(this, StudentHomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
    
    private void navigateBackToDegreeSelection() {
        Logger.i(Logger.TAG_UI, "User clicked back, navigating to degree selection");
        Intent intent = new Intent(this, DegreeSelectionActivity.class);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        navigateBackToDegreeSelection();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        navigateBackToDegreeSelection();
        return true;
    }
}

