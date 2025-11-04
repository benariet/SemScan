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
import org.example.semscan.ui.RolePickerActivity;
import org.example.semscan.ui.SettingsActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.ServerLogger;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import org.example.semscan.ui.teacher.AddAvailabilityActivity;

public class PresenterHomeActivity extends AppCompatActivity {
    
    private CardView cardStartSession;
    private CardView cardCreateSeminar;
    private CardView cardCreateInstance;
    private Button btnSettings;
    private Button btnChangeRole;
    private TextView textWelcomeMessage;
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private ServerLogger serverLogger;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_home);
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        serverLogger = ServerLogger.getInstance(this);
        
        // Check if user is actually a presenter
        if (!preferencesManager.isPresenter()) {
            navigateToRolePicker();
            return;
        }

        preferencesManager.setActiveRole("PRESENTER");

        initializeViews();
        setupToolbar();
        setupClickListeners();
        updateWelcomeMessage();

        Logger.userAction("Open Presenter Home", "Presenter home screen opened");
        if (serverLogger != null) {
            serverLogger.userAction("Open Presenter Home", "Presenter home screen opened");
        }
    }
    
    private void initializeViews() {
        cardStartSession = findViewById(R.id.card_start_session);
        cardCreateSeminar = findViewById(R.id.card_create_seminar);
        cardCreateInstance = findViewById(R.id.card_create_instance);
        btnSettings = findViewById(R.id.btn_settings);
        btnChangeRole = findViewById(R.id.btn_change_role);
        textWelcomeMessage = findViewById(R.id.text_welcome_message);
    }
    
    private void updateWelcomeMessage() {
        Long userId = preferencesManager.getUserId();
        
        if (userId == null || userId <= 0) {
            textWelcomeMessage.setText("Welcome, Presenter!");
            Logger.w(Logger.TAG_UI, "No user ID found, using generic welcome message");
            return;
        }
        
        textWelcomeMessage.setText("Welcome, Presenter!");

        apiService.getUserById(userId).enqueue(new retrofit2.Callback<org.example.semscan.data.model.User>() {
            @Override
            public void onResponse(Call<org.example.semscan.data.model.User> call, Response<org.example.semscan.data.model.User> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Logger.w(Logger.TAG_UI, "Failed to fetch presenter name. Code: " + response.code());
                    return;
                }

                org.example.semscan.data.model.User user = response.body();
                String fullName = user.getFullName();
                if (fullName != null) {
                    fullName = fullName.trim();
                }

                if (fullName != null && !fullName.isEmpty()) {
                    textWelcomeMessage.setText("Welcome, " + fullName + "!");
                    Logger.i(Logger.TAG_UI, "Welcome message updated with presenter name: " + fullName);
                    if (serverLogger != null) {
                        serverLogger.i(ServerLogger.TAG_UI, "Presenter resolved to: " + fullName);
                    }
                }
            }

            @Override
            public void onFailure(Call<org.example.semscan.data.model.User> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to load presenter profile", t);
                if (serverLogger != null) {
                    serverLogger.e(ServerLogger.TAG_UI, "Failed to load presenter profile", t);
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
        cardStartSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.userAction("Open Start Session", "Presenter tapped start session card");
                if (serverLogger != null) {
                    serverLogger.userAction("Open Start Session", "Presenter tapped start session card");
                }
                openStartSession();
            }
        });

        cardCreateSeminar.setOnClickListener(v -> {
            Logger.userAction("Create Seminar", "Presenter tapped create seminar card");
            if (serverLogger != null) {
                serverLogger.userAction("Create Seminar", "Presenter tapped create seminar card");
            }
            Intent intent = new Intent(PresenterHomeActivity.this, AddSeminarActivity.class);
            startActivity(intent);
        });

        cardCreateInstance.setOnClickListener(v -> {
            Logger.userAction("Create Seminar Instance", "Presenter tapped create instance card");
            if (serverLogger != null) {
                serverLogger.userAction("Create Seminar Instance", "Presenter tapped create instance card");
            }
            Intent intent = new Intent(PresenterHomeActivity.this, AddAvailabilityActivity.class);
            startActivity(intent);
        });
        
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.userAction("Open Settings", "Presenter tapped settings button");
                if (serverLogger != null) {
                    serverLogger.userAction("Open Settings", "Presenter tapped settings button");
                }
                openSettings();
            }
        });
        
        btnChangeRole.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Logger.userAction("Change Role", "Presenter tapped change role button");
                if (serverLogger != null) {
                    serverLogger.userAction("Change Role", "Presenter tapped change role button");
                }
                changeRole();
            }
        });
    }
    
    private void openStartSession() {
        Intent intent = new Intent(this, PresenterStartSessionActivity.class);
        startActivity(intent);
        Logger.userAction("Navigate", "Opened PresenterStartSessionActivity");
        if (serverLogger != null) {
            serverLogger.userAction("Navigate", "Opened PresenterStartSessionActivity");
        }
    }
    
    // Export removed from home - accessed directly from QR display after session ends
    
    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
        Logger.userAction("Navigate", "Opened SettingsActivity from presenter home");
        if (serverLogger != null) {
            serverLogger.userAction("Navigate", "Opened SettingsActivity from presenter home");
        }
    }
    
    private void changeRole() {
        preferencesManager.clearUserData();
        Logger.userAction("Change Role", "Presenter cleared user data and switched role");
        if (serverLogger != null) {
            serverLogger.userAction("Change Role", "Presenter cleared user data and switched role");
        }
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
        Logger.userAction("Open Menu", "Presenter opened home menu");
        if (serverLogger != null) {
            serverLogger.userAction("Open Menu", "Presenter opened home menu");
        }
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
        } else if (id == R.id.action_create_seminar) {
            Logger.userAction("Menu", "Presenter selected Create seminar");
            startActivity(new Intent(this, AddAvailabilityActivity.class));
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
