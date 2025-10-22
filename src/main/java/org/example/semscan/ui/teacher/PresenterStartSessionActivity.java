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
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.widget.Toast;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ListView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

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
    
    // Removed spinner - using grid only
    private RecyclerView recyclerSeminars;
    private ListView listSeminars;
    private SwipeRefreshLayout swipeRefreshSeminars;
    private Button btnStartSession;
    private Button btnCreateSeminar;
    private Button btnTestApi;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private ServerLogger serverLogger;
    
    private List<Seminar> seminars = new ArrayList<>();
    private String selectedSeminarId;
    private PresenterSeminarsAdapter seminarsAdapter;
    
    private static final int REQ_ADD_SEMINAR = 2001;
    
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
        // Removed spinner setup - using grid only
        setupClickListeners();
        
        // Check if settings are configured
        if (!checkSettings()) {
            Logger.w(Logger.TAG_UI, "Settings not configured, returning from onCreate");
            return;
        }
        
        // Only load presenter seminars - not all seminars
        loadPresenterSeminarsGrid();
    }
    
    private void initializeViews() {
        // Test with simple ListView
        listSeminars = findViewById(R.id.list_seminars);
        // recyclerSeminars = findViewById(R.id.recycler_seminars);
        // Temporarily removed SwipeRefreshLayout for testing
        // swipeRefreshSeminars = findViewById(R.id.swipe_refresh_seminars);
        // Grid layout (commented out for debugging)
        // GridLayoutManager grid = new GridLayoutManager(this, 2);
        // recyclerSeminars.setLayoutManager(grid);
        
        // Simple linear layout for easier debugging (commented out for ListView test)
        // LinearLayoutManager linear = new LinearLayoutManager(this);
        // recyclerSeminars.setLayoutManager(linear);
        
        // Ensure RecyclerView is properly initialized (commented out for ListView test)
        // recyclerSeminars.setHasFixedSize(true);
        // recyclerSeminars.setItemAnimator(null); // Disable animations to prevent layout issues
        
        // Debug: Add touch listener to RecyclerView (commented out for ListView test)
        // recyclerSeminars.setOnTouchListener((v, event) -> {
        //     Logger.d("PresenterSeminarsAdapter", "RecyclerView touched: " + event.getAction());
        //     return false; // Let the event propagate
        // });
        
        // Test with simple ListView - no adapter needed initially
        // seminarsAdapter = new PresenterSeminarsAdapter(new ArrayList<>(), item -> {
        //     selectedSeminarId = item.id;
        //     btnStartSession.setEnabled(true);
        //     Logger.userAction("Select Seminar Tile", item.seminarName);
        // });
        // recyclerSeminars.setAdapter(seminarsAdapter);

        btnStartSession = findViewById(R.id.btn_start_session);
        btnCreateSeminar = findViewById(R.id.btn_create_seminar);
        btnTestApi = findViewById(R.id.btn_test_api);
        
        // Setup pull-to-refresh (temporarily removed for testing)
        // swipeRefreshSeminars.setEnabled(false);
        // swipeRefreshSeminars.setOnRefreshListener(() -> {
        //     Logger.i(Logger.TAG_UI, "Pull-to-refresh triggered");
        //     loadPresenterSeminarsGrid();
        // });
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
        // Spinner removed in favor of grid; no-op to avoid referencing old ID
        return;
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
                Intent i = new Intent(PresenterStartSessionActivity.this, AddSeminarActivity.class);
                startActivityForResult(i, REQ_ADD_SEMINAR);
            }
        });
        
        btnTestApi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testPresenterSeminarsApi();
                
                // Additional debug info
                String debugInfo = "RecyclerView Debug:\n";
                debugInfo += "Item Count: " + (seminarsAdapter != null ? seminarsAdapter.getItemCount() : "null") + "\n";
                debugInfo += "Visibility: " + recyclerSeminars.getVisibility() + "\n";
                debugInfo += "Height: " + recyclerSeminars.getHeight() + "\n";
                debugInfo += "Width: " + recyclerSeminars.getWidth();
                
                Toast.makeText(PresenterStartSessionActivity.this, debugInfo, Toast.LENGTH_LONG).show();
                Logger.i(Logger.TAG_UI, "Debug Info: " + debugInfo);
            }
        });
    }
    
    private boolean checkSettings() {
        String userId = preferencesManager.getUserId();
        String baseUrl = preferencesManager.getApiBaseUrl();
        
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
        
        String baseUrl = preferencesManager.getApiBaseUrl();
        String userId = preferencesManager.getUserId();
        
        Logger.api("GET", baseUrl + "api/v1/seminars", null);
        Logger.d(Logger.TAG_UI, "Loading seminars from: " + baseUrl + ", User ID: " + userId);
        
        // Removed info toast - too cluttered
        
        Call<List<Seminar>> call = apiService.getSeminars();
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
                        
                        // Removed debug toast - too cluttered
                        
                        // Removed spinner update - using grid only
                        
                        // Debug: Show how many seminars were loaded
                        if (seminars.isEmpty()) {
                            Logger.i(Logger.TAG_UI, "No seminars found");
                            Toast.makeText(PresenterStartSessionActivity.this, 
                                    getString(R.string.no_seminars_found), Toast.LENGTH_LONG).show();
                        } else {
                            Logger.i(Logger.TAG_UI, "Loaded " + seminars.size() + " seminars successfully");
                            // Removed seminar count toast - too cluttered
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
    
    private void loadPresenterSeminarsGrid() {
        try {
            String presenterId = preferencesManager.getUserId();
            
            Logger.i(Logger.TAG_UI, "Loading presenter seminars for: " + presenterId);
            
            apiService.getPresenterSeminars(presenterId).enqueue(new Callback<java.util.List<ApiService.PresenterSeminarDto>>() {
                @Override
                public void onResponse(Call<java.util.List<ApiService.PresenterSeminarDto>> call, Response<java.util.List<ApiService.PresenterSeminarDto>> response) {
                    // Stop refresh indicator (temporarily removed)
                    // swipeRefreshSeminars.setRefreshing(false);
                    
                    if (!response.isSuccessful() || response.body() == null) {
                        Logger.w(Logger.TAG_UI, "Failed to load presenter seminars for grid - Response code: " + response.code());
                        String errorMsg = "Failed to load seminars - Code: " + response.code();
                        if (response.errorBody() != null) {
                            try {
                                errorMsg += "\nError: " + response.errorBody().string();
                            } catch (Exception e) {
                                errorMsg += "\nError: " + e.getMessage();
                            }
                        }
                        Toast.makeText(PresenterStartSessionActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    java.util.List<ApiService.PresenterSeminarDto> list = response.body();
                    Logger.i(Logger.TAG_UI, "Loaded " + list.size() + " presenter seminars");
                    
                    // Debug: Log each seminar
                    for (int i = 0; i < list.size(); i++) {
                        ApiService.PresenterSeminarDto seminar = list.get(i);
                        Logger.d(Logger.TAG_UI, "Seminar " + i + ": ID=" + seminar.id + ", Name=" + seminar.seminarName + ", Slots=" + (seminar.slots != null ? seminar.slots.size() : 0));
                    }
                    
                    // Removed debug toast - too cluttered
                    
                    // Test with simple ListView
                    Logger.i(Logger.TAG_UI, "Loading " + list.size() + " seminars into ListView");
                    
                    // Debug: Check if list is empty
                    if (list.isEmpty()) {
                        Logger.w(Logger.TAG_UI, "⚠️ WARNING: Presenter seminars list is EMPTY!");
                        Toast.makeText(PresenterStartSessionActivity.this, "No seminars found for this presenter", Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    // Filter out seminars with UUID IDs - only show proper PSM-* IDs
                    java.util.List<ApiService.PresenterSeminarDto> validSeminars = new java.util.ArrayList<>();
                    for (ApiService.PresenterSeminarDto seminar : list) {
                        if (seminar.id.startsWith("PSM-")) {
                            validSeminars.add(seminar);
                            Logger.i(Logger.TAG_UI, "Valid seminar: ID=" + seminar.id + ", Name='" + seminar.seminarName + "'");
                        } else {
                            Logger.w(Logger.TAG_UI, "Skipping UUID seminar: ID=" + seminar.id + ", Name='" + seminar.seminarName + "'");
                        }
                    }
                    
                    if (validSeminars.isEmpty()) {
                        Logger.w(Logger.TAG_UI, "⚠️ WARNING: No valid seminars found (all have UUIDs)!");
                        Toast.makeText(PresenterStartSessionActivity.this, "No valid seminars found. All seminars have UUID IDs.", Toast.LENGTH_LONG).show();
                        return;
                    }
                    
                    // Create simple string array for ListView with only valid seminars
                    String[] seminarNames = new String[validSeminars.size()];
                    for (int i = 0; i < validSeminars.size(); i++) {
                        ApiService.PresenterSeminarDto seminar = validSeminars.get(i);
                        seminarNames[i] = seminar.seminarName;
                    }
                    
                    // Set up ListView with custom adapter for better selection (using filtered list)
                    SeminarListAdapter adapter = new SeminarListAdapter(PresenterStartSessionActivity.this, validSeminars);
                    listSeminars.setAdapter(adapter);
                    
                    // Enable single choice mode for visual selection
                    listSeminars.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    
                    // Add click listener to ListView (using filtered list)
                    listSeminars.setOnItemClickListener((parent, view, position, id) -> {
                        ApiService.PresenterSeminarDto selectedSeminar = validSeminars.get(position);
                        
                        // Use the actual seminar ID from the API response
                        selectedSeminarId = selectedSeminar.id;
                        btnStartSession.setEnabled(true);
                        
                        // Debug: Log the actual seminar ID being used
                        Logger.d("ListView", "Using seminar ID: " + selectedSeminarId);
                        Logger.d("ListView", "Selected seminar name: " + selectedSeminar.seminarName);
                        
                        // Update adapter selection
                        adapter.setSelectedPosition(position);
                        
                        Logger.d("ListView", "Item clicked: " + selectedSeminar.seminarName);
                        Logger.userAction("Select Seminar", selectedSeminar.seminarName);
                        Toast.makeText(PresenterStartSessionActivity.this, "Selected: " + selectedSeminar.seminarName, Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onFailure(Call<java.util.List<ApiService.PresenterSeminarDto>> call, Throwable t) {
                    // Stop refresh indicator (temporarily removed)
                    // swipeRefreshSeminars.setRefreshing(false);
                    Logger.e(Logger.TAG_UI, "Failed to load presenter seminars", t);
                    Toast.makeText(PresenterStartSessionActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            // Stop refresh indicator
            swipeRefreshSeminars.setRefreshing(false);
            Logger.e(Logger.TAG_UI, "Error loading presenter seminars", e);
            Toast.makeText(this, "Error loading seminars", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Removed spinner method - using grid only
    
    private void startSession() {
        Logger.userAction("Start Session", "User clicked start session button");
        serverLogger.userAction("Start Session", "User clicked start session button");
        
        if (selectedSeminarId == null) {
            Logger.w(Logger.TAG_UI, "Start session attempted without selecting seminar");
            serverLogger.w(ServerLogger.TAG_UI, "Start session attempted without selecting seminar");
            Toast.makeText(this, "Please select a seminar", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // API key no longer required - removed authentication
        
        // Check for existing open sessions before creating a new one
        checkForOpenSessions();
    }
    
    private void checkForOpenSessions() {
        Logger.userAction("Check Open Sessions", "Checking for existing open sessions before creating new one");
        
        Call<List<Session>> call = apiService.getOpenSessions();
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
                            createNewSession();
                        } else {
                            // Show session selection dialog
                            serverLogger.session("Open Sessions Check", "Found " + openSessions.size() + " open sessions");
                            showSessionSelectionDialog(openSessions);
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
    
    private void showSessionSelectionDialog(List<Session> openSessions) {
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
            endCurrentSessionsAndCreateNew(openSessions);
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
    
    private void endCurrentSessionsAndCreateNew(List<Session> openSessions) {
        Logger.userAction("End All Sessions", "Ending " + openSessions.size() + " current sessions");
        
        // Close all open sessions first
        int sessionsToClose = openSessions.size();
        final int[] closedCount = {0};
        
        for (Session session : openSessions) {
            Call<Session> closeCall = apiService.closeSession(session.getSessionId());
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
                            createNewSession();
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
                            createNewSession();
                        }
                    });
                }
            });
        }
    }
    
    private void createNewSession() {
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
        
        Call<Session> call = apiService.createSession(request);
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
        
        // API key no longer required - removed authentication
        
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
        
        Call<Seminar> call = apiService.createSeminar(request);
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
    
    // Test method to debug presenter seminars API response
    private void testPresenterSeminarsApi() {
        String presenterId = preferencesManager.getUserId();
        
        Logger.i(Logger.TAG_UI, "Testing presenter seminars API for: " + presenterId);
        Toast.makeText(this, "Testing API for: " + presenterId, Toast.LENGTH_SHORT).show();
        
        apiService.getPresenterSeminars(presenterId).enqueue(new Callback<java.util.List<ApiService.PresenterSeminarDto>>() {
            @Override
            public void onResponse(Call<java.util.List<ApiService.PresenterSeminarDto>> call, Response<java.util.List<ApiService.PresenterSeminarDto>> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        java.util.List<ApiService.PresenterSeminarDto> list = response.body();
                        String result = "API Test Success!\nCount: " + list.size() + "\n";
                        for (int i = 0; i < list.size(); i++) {
                            ApiService.PresenterSeminarDto item = list.get(i);
                            result += "Item " + i + ": " + item.seminarName + "\n";
                        }
                        Toast.makeText(PresenterStartSessionActivity.this, result, Toast.LENGTH_LONG).show();
                        Logger.i(Logger.TAG_UI, "API Test Result: " + result);
                    } else {
                        String error = "API Test Failed!\nCode: " + response.code() + "\nMessage: " + response.message();
                        Toast.makeText(PresenterStartSessionActivity.this, error, Toast.LENGTH_LONG).show();
                        Logger.e(Logger.TAG_UI, "API Test Failed: " + error);
                    }
                });
            }
            
            @Override
            public void onFailure(Call<java.util.List<ApiService.PresenterSeminarDto>> call, Throwable t) {
                runOnUiThread(() -> {
                    String error = "API Test Network Error: " + t.getMessage();
                    Toast.makeText(PresenterStartSessionActivity.this, error, Toast.LENGTH_LONG).show();
                    Logger.e(Logger.TAG_UI, "API Test Network Error: " + error);
                });
            }
        });
    }
    
    // Test method to debug API response
    private void testApiResponse() {
        // API key no longer required - removed authentication
        
        Call<List<Seminar>> call = apiService.getSeminars();
        call.enqueue(new Callback<List<Seminar>>() {
            @Override
            public void onResponse(Call<List<Seminar>> call, Response<List<Seminar>> response) {
                runOnUiThread(() -> {
                    if (response.isSuccessful() && response.body() != null) {
                        // Removed debug toast - too cluttered
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_ADD_SEMINAR && resultCode == RESULT_OK) {
            loadPresenterSeminarsGrid();
        }
    }

    private static String formatCompactSlots(java.util.List<ApiService.PresenterSeminarSlotDto> slots) {
        if (slots == null || slots.isEmpty()) return "";
        String[] days = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
        java.util.Map<String, java.util.List<String>> dayToRanges = new java.util.LinkedHashMap<>();
        for (ApiService.PresenterSeminarSlotDto s : slots) {
            String day = (s.weekday >=0 && s.weekday < 7) ? days[s.weekday] : String.valueOf(s.weekday);
            String range = String.format("%02d–%02d", s.startHour, s.endHour);
            dayToRanges.computeIfAbsent(day, k -> new java.util.ArrayList<>()).add(range);
        }
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (java.util.Map.Entry<String, java.util.List<String>> e : dayToRanges.entrySet()) {
            if (!first) sb.append("; ");
            first = false;
            sb.append(e.getKey()).append(' ').append(String.join(", ", e.getValue()));
        }
        return sb.toString();
    }

    private static class PresenterSeminarsAdapter extends RecyclerView.Adapter<PresenterSeminarsAdapter.ViewHolder> {
        interface OnItemClick { void onClick(ApiService.PresenterSeminarDto item); }
        private java.util.List<ApiService.PresenterSeminarDto> items;
        private final OnItemClick onItemClick;
        PresenterSeminarsAdapter(java.util.List<ApiService.PresenterSeminarDto> items, OnItemClick onItemClick) {
            this.items = items != null ? items : new java.util.ArrayList<>();
            this.onItemClick = onItemClick;
            Logger.d("PresenterSeminarsAdapter", "Created with " + this.items.size() + " items");
        }
        void update(java.util.List<ApiService.PresenterSeminarDto> newItems) {
            this.items = newItems != null ? newItems : new java.util.ArrayList<>();
            Logger.d("PresenterSeminarsAdapter", "Updated with " + this.items.size() + " items");
            notifyDataSetChanged();
        }
        @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            // Original tile layout (commented out for debugging)
            // View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_presenter_seminar_tile, parent, false);
            
            // Simple list layout for easier debugging
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_seminar_simple, parent, false);
            Logger.d("PresenterSeminarsAdapter", "Creating ViewHolder for position " + viewType);
            return new ViewHolder(view);
        }
        @Override public void onBindViewHolder(ViewHolder h, int position) {
            ApiService.PresenterSeminarDto item = items.get(position);
            Logger.d("PresenterSeminarsAdapter", "Binding item " + position + ": " + item.seminarName);
            h.subjectName.setText(item.seminarName);
            h.compactSlots.setText(formatCompactSlots(item.slots));
            h.itemView.setOnClickListener(v -> {
                Logger.d("PresenterSeminarsAdapter", "Item clicked: " + item.seminarName);
                onItemClick.onClick(item);
            });
        }
        @Override public int getItemCount() { return items.size(); }
        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView subjectName;
            TextView compactSlots;
            ViewHolder(View itemView) {
                super(itemView);
                // Original tile layout IDs (commented out for debugging)
                // subjectName = itemView.findViewById(R.id.text_subject_name);
                // compactSlots = itemView.findViewById(R.id.text_compact_slots);
                
                // Simple list layout IDs for easier debugging
                subjectName = itemView.findViewById(R.id.text_seminar_name);
                compactSlots = itemView.findViewById(R.id.text_seminar_slots);
                Logger.d("PresenterSeminarsAdapter", "ViewHolder created with views: " + (subjectName != null) + ", " + (compactSlots != null));
            }
        }
    }
    
    
    // Custom adapter for seminar list with selection highlighting
    private static class SeminarListAdapter extends ArrayAdapter<ApiService.PresenterSeminarDto> {
        private final java.util.List<ApiService.PresenterSeminarDto> seminars;
        
        public SeminarListAdapter(android.content.Context context, java.util.List<ApiService.PresenterSeminarDto> seminars) {
            super(context, R.layout.item_list_seminar, seminars);
            this.seminars = seminars;
        }
        
        @Override
        public android.view.View getView(int position, android.view.View convertView, android.view.ViewGroup parent) {
            if (convertView == null) {
                convertView = android.view.LayoutInflater.from(getContext()).inflate(R.layout.item_list_seminar, parent, false);
            }
            
            ApiService.PresenterSeminarDto seminar = seminars.get(position);
            
            TextView nameView = convertView.findViewById(R.id.text_seminar_name);
            TextView slotsView = convertView.findViewById(R.id.text_seminar_slots);
            
            nameView.setText(seminar.seminarName);
            slotsView.setText(formatCompactSlots(seminar.slots));
            
            // Highlight selected item
            if (position == getSelectedPosition()) {
                convertView.setBackgroundColor(0xFFE3F2FD); // Light blue background
                nameView.setTextColor(0xFF1976D2); // Blue text
            } else {
                convertView.setBackgroundColor(0xFFFFFFFF); // White background
                nameView.setTextColor(0xFF333333); // Dark text
            }
            
            return convertView;
        }
        
        private int selectedPosition = -1;
        
        public void setSelectedPosition(int position) {
            selectedPosition = position;
            notifyDataSetChanged();
        }
        
        public int getSelectedPosition() {
            return selectedPosition;
        }
        
        private String formatCompactSlots(java.util.List<ApiService.PresenterSeminarSlotDto> slots) {
            if (slots == null || slots.isEmpty()) return "No time slots";
            
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < slots.size(); i++) {
                if (i > 0) sb.append(", ");
                ApiService.PresenterSeminarSlotDto slot = slots.get(i);
                String dayName = getDayName(slot.weekday);
                sb.append(dayName).append(" ").append(slot.startHour).append("-").append(slot.endHour);
            }
            return sb.toString();
        }
        
        private String getDayName(int weekday) {
            String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            return days[weekday];
        }
    }
}
