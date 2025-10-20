package org.example.semscan.ui.teacher;

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
import org.example.semscan.ui.teacher.PresenterStartSessionActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresenterHomeActivity extends AppCompatActivity {
    
    private CardView cardStartSession;
    private Button btnSettings;
    private Button btnChangeRole;
    private TextView textWelcomeMessage;
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_home);
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        
        // Check if user is actually a presenter
        if (!preferencesManager.isPresenter()) {
            navigateToRolePicker();
            return;
        }
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        updateWelcomeMessage();
    }
    
    private void initializeViews() {
        cardStartSession = findViewById(R.id.card_start_session);
        btnSettings = findViewById(R.id.btn_settings);
        btnChangeRole = findViewById(R.id.btn_change_role);
        textWelcomeMessage = findViewById(R.id.text_welcome_message);
    }
    
    private void updateWelcomeMessage() {
        String userId = preferencesManager.getUserId();
        
        if (userId == null || userId.trim().isEmpty()) {
            textWelcomeMessage.setText("Welcome, Presenter!");
            Logger.w(Logger.TAG_UI, "No user ID found, using generic welcome message");
            return;
        }
        
        // For presenters, use a simple mapping approach to get their actual names
        // This is more appropriate than showing seminar names
        String displayName = getPresenterDisplayName(userId);
        
        if (displayName != null && !displayName.trim().isEmpty()) {
            textWelcomeMessage.setText("Welcome, " + displayName + "!");
            Logger.i(Logger.TAG_UI, "Welcome message updated with presenter name: " + displayName);
        } else {
            textWelcomeMessage.setText("Welcome, Presenter!");
            Logger.w(Logger.TAG_UI, "No display name found for user: " + userId);
        }
    }
    
    private String getPresenterDisplayName(String userId) {
        // Simple mapping for common presenter IDs
        // This can be expanded or made configurable
        switch (userId.toLowerCase()) {
            case "presenter-001":
                return "Dr. Johnson";
            case "presenter-002":
                return "Prof. Davis";
            case "presenter-003":
                return "Dr. Smith";
            case "presenter-004":
                return "Prof. Wilson";
            case "presenter-005":
                return "Dr. Brown";
            default:
                // Try to extract a name from the user ID
                if (userId.contains("-")) {
                    String[] parts = userId.split("-");
                    if (parts.length > 1) {
                        return "Presenter " + parts[1];
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
        cardStartSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openStartSession();
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
        Intent intent = new Intent(this, PresenterStartSessionActivity.class);
        startActivity(intent);
    }
    
    // Export removed from home - accessed directly from QR display after session ends
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
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
        getMenuInflater().inflate(R.menu.menu_presenter_home, menu);
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
