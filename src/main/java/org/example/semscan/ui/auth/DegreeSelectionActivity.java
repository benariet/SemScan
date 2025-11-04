package org.example.semscan.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.example.semscan.R;
import org.example.semscan.ui.RolePickerActivity;
import org.example.semscan.ui.teacher.PresenterHomeActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

public class DegreeSelectionActivity extends AppCompatActivity {
    
    private CardView cardMSc;
    private CardView cardPhD;
    private PreferencesManager preferencesManager;
    
    private static final String DEGREE_MSC = "MSc";
    private static final String DEGREE_PHD = "PhD";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_degree_selection);
        
        Logger.i(Logger.TAG_UI, "DegreeSelectionActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        
        initializeViews();
        setupClickListeners();
    }
    
    private void initializeViews() {
        cardMSc = findViewById(R.id.card_msc);
        cardPhD = findViewById(R.id.card_phd);
    }
    
    private void setupClickListeners() {
        cardMSc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDegree(DEGREE_MSC);
            }
        });
        
        cardPhD.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectDegree(DEGREE_PHD);
            }
        });
    }
    
    private void selectDegree(String degree) {
        Logger.userAction("Select Degree", "User selected degree: " + degree);
        Logger.i(Logger.TAG_UI, "Setting user degree to: " + degree);
        
        preferencesManager.setUserDegree(degree);
        // Mark that user has completed first-time login
        preferencesManager.setFirstTimeLogin(false);
        
        // Route based on degree
        if (DEGREE_PHD.equals(degree)) {
            // PhD → Presenter path only
            Logger.i(Logger.TAG_UI, "PhD selected, navigating directly to Presenter");
            preferencesManager.setUserRole("PRESENTER");
            navigateToPresenter();
        } else {
            // MSc → Choose role (Participant, Presenter, or Both)
            Logger.i(Logger.TAG_UI, "MSc selected, navigating to MSc role selection");
            navigateToMScRoleSelection();
        }
    }
    
    private void navigateToMScRoleSelection() {
        Intent intent = new Intent(this, MScRoleSelectionActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void navigateToPresenter() {
        Intent intent = new Intent(this, PresenterHomeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    @Override
    public void onBackPressed() {
        // Prevent going back from degree selection
        moveTaskToBack(true);
    }
}

