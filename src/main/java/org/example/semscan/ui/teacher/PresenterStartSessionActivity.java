package org.example.semscan.ui.teacher;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Seminar;
import org.example.semscan.data.model.Session;
import org.example.semscan.ui.qr.QRDisplayActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.ServerLogger;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresenterStartSessionActivity extends AppCompatActivity {
    
    private Spinner spinnerSeminar;
    private Button btnStartSession;
    private Button btnCreateSeminar;
    private Button btnTestApi;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private ServerLogger serverLogger;
    
    private List<Seminar> seminars = new ArrayList<>();
    private String selectedSeminarId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_start_session);
        
        Logger.i(Logger.TAG_UI, "PresenterStartSessionActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        serverLogger = ServerLogger.getInstance(this);
        
        initializeViews();
        setupToolbar();
        setupSpinner();
        setupClickListeners();
        
        // Check if settings are configured
        if (!checkSettings()) {
            Logger.w(Logger.TAG_UI, "Settings not configured, returning from onCreate");
            return;
        }
        
        loadSeminars();
    }
    
    private void initializeViews() {
        spinnerSeminar = findViewById(R.id.spinner_course);
        btnStartSession = findViewById(R.id.btn_start_session);
        btnCreateSeminar = findViewById(R.id.btn_create_seminar);
        btnTestApi = findViewById(R.id.btn_test_api);
    }
    
    private void showLongToast(String message) {
        Toast toast = Toast.makeText(this, message, Toast.LENGTH_LONG);
        toast.show();
        
        // Show it again after a delay to make it really long
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        }, 3500);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupSpinner() {
        spinnerSeminar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                try {
                    if (position > 0 && position - 1 < seminars.size()) { // Skip "Select Seminar" placeholder
                        Seminar selectedSeminar = seminars.get(position - 1);
                        if (selectedSeminar != null) {
                            selectedSeminarId = selectedSeminar.getSeminarId();
                        }
                    } else {
                        selectedSeminarId = null;
                    }
                } catch (Exception e) {
                    selectedSeminarId = null;
                    showLongToast("❌ Error selecting seminar: " + e.getMessage());
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSeminarId = null;
            }
        });
    }
    
    private void setupClickListeners() {
        btnStartSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSession();
            }
        });
        
        btnCreateSeminar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateSeminarDialog();
            }
        });
        
        btnTestApi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testApiResponse();
            }
        });
    }
    
    private boolean checkSettings() {
        String apiKey = preferencesManager.getPresenterApiKey();
        String userId = preferencesManager.getUserId();
        String baseUrl = preferencesManager.getApiBaseUrl();
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            Toast.makeText(this, "Please configure your API key in Settings first.", Toast.LENGTH_LONG).show();
            return false;
        }
        
        if (userId == null || userId.trim().isEmpty()) {
            Toast.makeText(this, "Please configure your User ID in Settings first.", Toast.LENGTH_LONG).show();
            return false;
        }
        
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            Toast.makeText(this, "Please configure your API Base URL in Settings first.", Toast.LENGTH_LONG).show();
            return false;
        }
        
        return true;
    }
    
    private void loadSeminars() {
        Logger.i(Logger.TAG_UI, "Loading seminars");
        
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            Logger.e(Logger.TAG_UI, "API key not configured");
            Toast.makeText(this, "API key not configured. Please set it in Settings.", Toast.LENGTH_LONG).show();
            return;
        }
        
        String baseUrl = preferencesManager.getApiBaseUrl();
        String userId = preferencesManager.getUserId();
        
        Logger.api("GET", baseUrl + "api/v1/seminars", null);
        Logger.d(Logger.TAG_UI, "Loading seminars from: " + baseUrl + ", User ID: " + userId);
        
        Toast.makeText(this, "Loading seminars from: " + baseUrl + "\nUser ID: " + userId, Toast.LENGTH_LONG).show();
        
        Call<List<Seminar>> call = apiService.getSeminars(apiKey);
        call.enqueue(new Callback<List<Seminar>>() {
            @Override
            public void onResponse(Call<List<Seminar>> call, Response<List<Seminar>> response) {
                // Run UI updates on main thread
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        Logger.apiResponse("GET", "api/v1/seminars", response.code(), "Success - " + response.body().size() + " seminars");
                        
                        seminars.clear();
                        seminars.addAll(response.body());
                        
                        // Log seminar data
                        for (Seminar seminar : seminars) {
                            Logger.d(Logger.TAG_UI, "Seminar loaded: " + seminar.toString());
                        }
                        
                        // Debug: Show detailed info about first seminar
                        if (!seminars.isEmpty()) {
                            Seminar firstSeminar = seminars.get(0);
                            String debugInfo = "Name: " + (firstSeminar.getSeminarName() != null ? firstSeminar.getSeminarName() : "NULL") + 
                                             "\nID: " + (firstSeminar.getSeminarId() != null ? firstSeminar.getSeminarId() : "NULL") +
                                             "\nCode: " + (firstSeminar.getSeminarCode() != null ? firstSeminar.getSeminarCode() : "NULL");
                            Toast.makeText(PresenterStartSessionActivity.this, debugInfo, Toast.LENGTH_LONG).show();
                        }
                        
                        updateSeminarSpinner();
                        
                        // Debug: Show how many seminars were loaded
                        if (seminars.isEmpty()) {
                            Logger.i(Logger.TAG_UI, "No seminars found");
                            Toast.makeText(PresenterStartSessionActivity.this, 
                                    getString(R.string.no_seminars_found), Toast.LENGTH_LONG).show();
                        } else {
                            Logger.i(Logger.TAG_UI, "Loaded " + seminars.size() + " seminars successfully");
                            Toast.makeText(PresenterStartSessionActivity.this, 
                                    "Loaded " + seminars.size() + " seminars", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String errorBody = null;
                        if (response.errorBody() != null) {
                            try {
                                errorBody = response.errorBody().string();
                            } catch (Exception e) {
                                Logger.e(Logger.TAG_UI, "Error reading response body", e);
                            }
                        }
                        
                        Logger.apiError("GET", "api/v1/seminars", response.code(), errorBody);
                        
                        String errorMsg = "Failed to load seminars. Response code: " + response.code();
                        if (errorBody != null) {
                            errorMsg += " - " + errorBody;
                        }
                        showLongToast("❌ " + errorMsg);
                    }
                });
            }
            
            @Override
            public void onFailure(Call<List<Seminar>> call, Throwable t) {
                // Run UI updates on main thread
                runOnUiThread(() -> {
                    Logger.e(Logger.TAG_UI, "Failed to load seminars", t);
                    
                    String errorMsg = "Network error: " + t.getMessage();
                    if (t.getMessage().contains("Failed to connect")) {
                        errorMsg += "\nPlease check if the server is running at: " + preferencesManager.getApiBaseUrl();
                    }
                    Toast.makeText(PresenterStartSessionActivity.this, "❌ " + errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void updateSeminarSpinner() {
        try {
            if (spinnerSeminar == null) {
                showLongToast("❌ Spinner not initialized");
                return;
            }
            
            List<String> seminarNames = new ArrayList<>();
            seminarNames.add("Select Seminar");
            
            for (Seminar seminar : seminars) {
                if (seminar != null && seminar.getSeminarName() != null && !seminar.getSeminarName().trim().isEmpty()) {
                    seminarNames.add(seminar.getSeminarName());
                }
            }
            
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                    android.R.layout.simple_spinner_item, seminarNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            
            spinnerSeminar.setAdapter(adapter);
            
            // Reset selection to first item
            spinnerSeminar.setSelection(0);
            
            Toast.makeText(this, "Spinner updated with " + (seminarNames.size() - 1) + " seminars", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            showLongToast("❌ Error updating seminar list: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void startSession() {
        Logger.userAction("Start Session", "User clicked start session button");
        serverLogger.userAction("Start Session", "User clicked start session button");
        
        if (selectedSeminarId == null) {
            Logger.w(Logger.TAG_UI, "Start session attempted without selecting seminar");
            serverLogger.w(ServerLogger.TAG_UI, "Start session attempted without selecting seminar");
            Toast.makeText(this, "Please select a seminar", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            Logger.e(Logger.TAG_UI, "Start session attempted without API key");
            serverLogger.e(ServerLogger.TAG_UI, "Start session attempted without API key");
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Check for existing open sessions before creating a new one
        checkForOpenSessions(apiKey);
    }
    
    private void checkForOpenSessions(String apiKey) {
        Logger.userAction("Check Open Sessions", "Checking for existing open sessions before creating new one");
        
        Call<List<Session>> call = apiService.getOpenSessions(apiKey);
        call.enqueue(new Callback<List<Session>>() {
            @Override
            public void onResponse(Call<List<Session>> call, Response<List<Session>> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        List<Session> openSessions = response.body();
                        Logger.session("Open Sessions Check", "Found " + openSessions.size() + " open sessions");
                        
                        if (openSessions.isEmpty()) {
                            // No open sessions, proceed with creating new session
                            serverLogger.session("Open Sessions Check", "Found " + openSessions.size() + " open sessions");
                            createNewSession(apiKey);
                        } else {
                            // Show session selection dialog
                            serverLogger.session("Open Sessions Check", "Found " + openSessions.size() + " open sessions");
                            showSessionSelectionDialog(openSessions, apiKey);
                        }
                    } else {
                        Logger.apiError("GET", "api/v1/sessions/open", response.code(), "Failed to check open sessions");
                        Toast.makeText(PresenterStartSessionActivity.this, 
                                "Failed to check existing sessions", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(Call<List<Session>> call, Throwable t) {
                runOnUiThread(() -> {
                    Logger.e(Logger.TAG_UI, "Failed to check open sessions", t);
                    Toast.makeText(PresenterStartSessionActivity.this, 
                            "Network error checking sessions: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void showSessionSelectionDialog(List<Session> openSessions, String apiKey) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⚠️ Active Session Found");
        
        StringBuilder message = new StringBuilder();
        message.append("You have ").append(openSessions.size()).append(" active session(s):\n\n");
        
        for (int i = 0; i < openSessions.size(); i++) {
            Session session = openSessions.get(i);
            message.append("• Session: ").append(session.getSessionId()).append("\n");
            message.append("  Started: ").append(session.getStartTime()).append("\n");
            message.append("  Seminar: ").append(session.getSeminarId()).append("\n\n");
        }
        
        message.append("What would you like to do?");
        
        builder.setMessage(message.toString());
        
        builder.setPositiveButton("End Current & Start New", (dialog, which) -> {
            Logger.userAction("End Current Sessions", "User chose to end current sessions and start new one");
            endCurrentSessionsAndCreateNew(openSessions, apiKey);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Logger.userAction("Cancel New Session", "User cancelled new session creation");
            dialog.dismiss();
        });
        
        builder.setNeutralButton("Manage Current", (dialog, which) -> {
            Logger.userAction("Manage Current Sessions", "User chose to manage current sessions");
            // Navigate to session management or show current session QR
            if (!openSessions.isEmpty()) {
                openQRDisplay(openSessions.get(0)); // Show QR for first session
            }
        });
        
        builder.setCancelable(false);
        builder.show();
    }
    
    private void endCurrentSessionsAndCreateNew(List<Session> openSessions, String apiKey) {
        Logger.userAction("End All Sessions", "Ending " + openSessions.size() + " current sessions");
        
        // Close all open sessions first
        int sessionsToClose = openSessions.size();
        final int[] closedCount = {0};
        
        for (Session session : openSessions) {
            Call<Session> closeCall = apiService.closeSession(apiKey, session.getSessionId());
            closeCall.enqueue(new Callback<Session>() {
                @Override
                public void onResponse(Call<Session> call, Response<Session> response) {
                    runOnUiThread(() -> {
                        closedCount[0]++;
                        if (response.isSuccessful()) {
                            Logger.session("Session Closed", "Closed session: " + session.getSessionId());
                        } else {
                            Logger.apiError("PUT", "api/v1/sessions/" + session.getSessionId() + "/close", 
                                    response.code(), "Failed to close session");
                        }
                        
                        // When all sessions are processed, create new one
                        if (closedCount[0] == sessionsToClose) {
                            createNewSession(apiKey);
                        }
                    });
                }
                
                @Override
                public void onFailure(Call<Session> call, Throwable t) {
                    runOnUiThread(() -> {
                        closedCount[0]++;
                        Logger.e(Logger.TAG_UI, "Failed to close session: " + session.getSessionId(), t);
                        
                        // When all sessions are processed, create new one
                        if (closedCount[0] == sessionsToClose) {
                            createNewSession(apiKey);
                        }
                    });
                }
            });
        }
    }
    
    private void createNewSession(String apiKey) {
        Logger.userAction("Create New Session", "Creating new session for seminar: " + selectedSeminarId);
        
        // Format start time as ISO 8601 string
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
        String startTime = sdf.format(new java.util.Date());
        
        ApiService.CreateSessionRequest request = new ApiService.CreateSessionRequest(
                selectedSeminarId, startTime, "OPEN");
        
        // Log the request details (server will generate session ID)
        Logger.session("Creating Session", "Seminar ID: " + selectedSeminarId + ", Start Time: " + startTime + " (Server will generate session ID)");
        Logger.api("POST", "api/v1/sessions", "Seminar ID: " + selectedSeminarId + ", Start Time: " + startTime + ", Status: OPEN");
        
        Toast.makeText(this, "Creating session for seminar ID: " + selectedSeminarId, Toast.LENGTH_SHORT).show();
        
        Call<Session> call = apiService.createSession(apiKey, request);
        call.enqueue(new Callback<Session>() {
            @Override
            public void onResponse(Call<Session> call, Response<Session> response) {
                // Run UI updates on main thread
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        Session session = response.body();
                        
                        // Log the full session object for debugging
                        Logger.d(Logger.TAG_SESSION, "Session response: " + session.toString());
                        
                        // Log the server-generated session details
                        serverLogger.session("Session Created", "Session ID: " + session.getSessionId());
                        serverLogger.flushLogs(); // Force send logs after session creation
                        if (session.getSessionId() != null && !session.getSessionId().isEmpty()) {
                            Logger.session("Session Created", "Server-generated Session ID: " + session.getSessionId() + ", Seminar ID: " + session.getSeminarId());
                            Logger.apiResponse("POST", "api/v1/sessions", response.code(), "Session created successfully with ID: " + session.getSessionId());
                        } else {
                            Logger.w(Logger.TAG_SESSION, "Session created but no session ID returned from server");
                            Logger.d(Logger.TAG_SESSION, "Full session object: " + session.toString());
                        }
                        
                        Toast.makeText(PresenterStartSessionActivity.this, 
                                "Session created successfully! ID: " + (session.getSessionId() != null ? session.getSessionId() : "Unknown"), Toast.LENGTH_SHORT).show();
                        openQRDisplay(session);
                    } else {
                        String errorBody = null;
                        if (response.errorBody() != null) {
                            try {
                                errorBody = response.errorBody().string();
                            } catch (Exception e) {
                                Logger.e(Logger.TAG_UI, "Error reading session creation response body", e);
                            }
                        }
                        
                        Logger.apiError("POST", "api/v1/sessions", response.code(), errorBody);
                        Logger.session("Session Creation Failed", "Response code: " + response.code() + ", Error: " + errorBody);
                        
                        String errorMsg = "Failed to create session. Response code: " + response.code();
                        if (errorBody != null) {
                            errorMsg += " - " + errorBody;
                        }
                        showLongToast("❌ " + errorMsg);
                    }
                });
            }
            
            @Override
            public void onFailure(Call<Session> call, Throwable t) {
                // Run UI updates on main thread
                runOnUiThread(() -> {
                    Logger.e(Logger.TAG_UI, "Session creation network failure", t);
                    Logger.session("Session Creation Failed", "Network error: " + t.getMessage());
                    
                    String errorMsg = "Network error: " + t.getMessage();
                    if (t.getMessage().contains("Failed to connect")) {
                        errorMsg += "\nPlease check if the server is running at: " + preferencesManager.getApiBaseUrl();
                    }
                    Toast.makeText(PresenterStartSessionActivity.this, "❌ " + errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    private void openQRDisplay(Session session) {
        Intent intent = new Intent(this, QRDisplayActivity.class);
        intent.putExtra("sessionId", session.getSessionId());
        intent.putExtra("seminarId", session.getSeminarId());
        intent.putExtra("startTime", session.getStartTime());
        intent.putExtra("endTime", session.getEndTime() != null ? session.getEndTime() : 0L);
        intent.putExtra("status", session.getStatus());
        startActivity(intent);
        finish();
    }
    
    private void showCreateSeminarDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_create_seminar, null);
        builder.setView(dialogView);
        
        EditText etSeminarName = dialogView.findViewById(R.id.et_seminar_name);
        EditText etSeminarCode = dialogView.findViewById(R.id.et_seminar_code);
        EditText etDescription = dialogView.findViewById(R.id.et_description);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnCreate = dialogView.findViewById(R.id.btn_create);
        
        AlertDialog dialog = builder.create();
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnCreate.setOnClickListener(v -> {
            String seminarName = etSeminarName.getText().toString().trim();
            String seminarCode = etSeminarCode.getText().toString().trim();
            String description = etDescription.getText().toString().trim();
            
            if (TextUtils.isEmpty(seminarName)) {
                etSeminarName.setError("Seminar name is required");
                return;
            }
            
            if (TextUtils.isEmpty(seminarCode)) {
                etSeminarCode.setError("Seminar code is required");
                return;
            }
            
            createSeminar(seminarName, seminarCode, description);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void createSeminar(String seminarName, String seminarCode, String description) {
        Logger.userAction("Create Seminar", "Name: " + seminarName + ", Code: " + seminarCode);
        
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            Logger.e(Logger.TAG_UI, "Seminar creation attempted without API key");
            Toast.makeText(this, "API key not configured. Please set it in Settings.", Toast.LENGTH_LONG).show();
            return;
        }
        
        String presenterId = preferencesManager.getUserId();
        if (presenterId == null) {
            Logger.e(Logger.TAG_UI, "Seminar creation attempted without User ID");
            Toast.makeText(this, "User ID not configured. Please set it in Settings.", Toast.LENGTH_LONG).show();
            return;
        }
        
        ApiService.CreateSeminarRequest request = new ApiService.CreateSeminarRequest(
                seminarName, seminarCode, description, presenterId);
        
        Logger.api("POST", "api/v1/seminars", "Name: " + seminarName + ", Code: " + seminarCode + ", Presenter: " + presenterId);
        
        Toast.makeText(this, "Creating seminar...", Toast.LENGTH_SHORT).show();
        
        Call<Seminar> call = apiService.createSeminar(apiKey, request);
        call.enqueue(new Callback<Seminar>() {
            @Override
            public void onResponse(Call<Seminar> call, Response<Seminar> response) {
                // Run UI updates on main thread
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        Toast.makeText(PresenterStartSessionActivity.this, 
                                getString(R.string.seminar_created_successfully), Toast.LENGTH_SHORT).show();
                        // Reload seminars to include the new one
                        loadSeminars();
                    } else {
                        String errorMsg = "Failed to create seminar. Response code: " + response.code();
                        if (response.errorBody() != null) {
                            try {
                                errorMsg += " - " + response.errorBody().string();
                            } catch (Exception e) {
                                errorMsg += " - Error reading response body";
                            }
                        }
                        showLongToast("❌ " + errorMsg);
                    }
                });
            }
            
            @Override
            public void onFailure(Call<Seminar> call, Throwable t) {
                // Run UI updates on main thread
                runOnUiThread(() -> {
                    String errorMsg = "Network error: " + t.getMessage();
                    if (t.getMessage().contains("Failed to connect")) {
                        errorMsg += "\nPlease check if the server is running at: " + preferencesManager.getApiBaseUrl();
                    }
                    Toast.makeText(PresenterStartSessionActivity.this, "❌ " + errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    // Test method to debug API response
    private void testApiResponse() {
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Call<List<Seminar>> call = apiService.getSeminars(apiKey);
        call.enqueue(new Callback<List<Seminar>>() {
            @Override
            public void onResponse(Call<List<Seminar>> call, Response<List<Seminar>> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        String debugInfo = "API Response:\n";
                        debugInfo += "Status: " + response.code() + "\n";
                        debugInfo += "Seminars count: " + response.body().size() + "\n";
                        
                        if (!response.body().isEmpty()) {
                            Seminar first = response.body().get(0);
                            debugInfo += "First seminar fields:\n";
                            debugInfo += "- ID: " + (first.getSeminarId() != null ? first.getSeminarId() : "NULL") + "\n";
                            debugInfo += "- Name: " + (first.getSeminarName() != null ? first.getSeminarName() : "NULL") + "\n";
                            debugInfo += "- Code: " + (first.getSeminarCode() != null ? first.getSeminarCode() : "NULL") + "\n";
                            debugInfo += "- Description: " + (first.getDescription() != null ? first.getDescription() : "NULL") + "\n";
                            debugInfo += "- Presenter ID: " + (first.getPresenterId() != null ? first.getPresenterId() : "NULL");
                        }
                        
                        Toast.makeText(PresenterStartSessionActivity.this, debugInfo, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(PresenterStartSessionActivity.this, "API Error: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                });
            }
            
            @Override
            public void onFailure(Call<List<Seminar>> call, Throwable t) {
                runOnUiThread(() -> {
                    Toast.makeText(PresenterStartSessionActivity.this, "API Failure: " + t.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
    
    @Override
    protected void onDestroy() {
        if (serverLogger != null) {
            serverLogger.flushLogs();
        }
        super.onDestroy();
    }
}
