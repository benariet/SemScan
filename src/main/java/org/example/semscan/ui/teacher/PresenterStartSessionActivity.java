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
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ListView;

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
import org.example.semscan.ui.teacher.AddSeminarActivity;
import org.example.semscan.ui.teacher.AddAvailabilityActivity;

public class PresenterStartSessionActivity extends AppCompatActivity {
    
    // Removed spinner - using grid only
    private ListView listSeminars;
    private Button btnStartSession;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private ServerLogger serverLogger;
    
    private final List<ApiService.PresenterSeminarDto> seminarTiles = new ArrayList<>();
    private ApiService.PresenterSeminarDto selectedSeminar;
    
    private static final int REQ_ADD_SEMINAR = 2001;
    private static final int REQ_ADD_AVAILABILITY = 2002;
    
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
        loadPresenterSeminars();
    }
    
    private void initializeViews() {
        // Initialize ListView for seminars
        listSeminars = findViewById(R.id.list_seminars);

        btnStartSession = findViewById(R.id.btn_start_session);
        
        // Pull-to-refresh functionality removed - using ListView instead of RecyclerView
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
    }
    
    private boolean checkSettings() {
        Long userId = preferencesManager.getUserId();
        String baseUrl = preferencesManager.getApiBaseUrl();
        
        if (userId == null || userId <= 0) {
            Toast.makeText(this, "Please configure your User ID in Settings first.", Toast.LENGTH_LONG).show();
            return false;
        }
        
        if (baseUrl == null || baseUrl.trim().isEmpty()) {
            Toast.makeText(this, "Please configure your API Base URL in Settings first.", Toast.LENGTH_LONG).show();
            return false;
        }
        
        return true;
    }
    
    private void loadPresenterSeminars() {
        try {
            Long presenterId = preferencesManager.getUserId();
            if (presenterId == null || presenterId <= 0) {
                Logger.e(Logger.TAG_UI, "Cannot load presenter seminars - missing presenter ID");
                Toast.makeText(PresenterStartSessionActivity.this, "Presenter ID not found. Please check settings.", Toast.LENGTH_LONG).show();
                return;
            }
            
            Logger.i(Logger.TAG_UI, "Loading presenter seminars for: " + presenterId);
            
            apiService.getPresenterSeminars(presenterId).enqueue(new Callback<java.util.List<ApiService.PresenterSeminarDto>>() {
                @Override
                public void onResponse(Call<java.util.List<ApiService.PresenterSeminarDto>> call, Response<java.util.List<ApiService.PresenterSeminarDto>> response) {
                    // Stop refresh indicator (temporarily removed)
                    
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
                    if (list == null) list = new ArrayList<>();

                    Logger.i(Logger.TAG_UI, "Loaded " + list.size() + " presenter seminar tiles");
                    seminarTiles.clear();
                    seminarTiles.addAll(sortSeminarsByNewest(list));

                    for (int i = 0; i < seminarTiles.size(); i++) {
                        ApiService.PresenterSeminarDto seminar = seminarTiles.get(i);
                        Logger.d(Logger.TAG_UI, "Seminar tile " + i + ": presenterSeminarId=" + seminar.presenterSeminarId + ", seminarId=" + seminar.seminarId + ", title=" + seminar.getDisplayTitle() + ", slots=" + (seminar.slots != null ? seminar.slots.size() : 0));
                    }

                    if (seminarTiles.isEmpty()) {
                        Logger.w(Logger.TAG_UI, "⚠️ WARNING: Presenter seminars list is EMPTY!");
                        Toast.makeText(PresenterStartSessionActivity.this, "No seminars found for this presenter", Toast.LENGTH_LONG).show();
                        listSeminars.setAdapter(null);
                        selectedSeminar = null;
                        return;
                    }
                    
                    SeminarListAdapter adapter = new SeminarListAdapter(PresenterStartSessionActivity.this, seminarTiles);
                    listSeminars.setAdapter(adapter);
                    
                    // Enable single choice mode for visual selection
                    listSeminars.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                    
                    // Add click listener to ListView (using filtered list)
                    listSeminars.setOnItemClickListener((parent, view, position, id) -> {
                        selectedSeminar = seminarTiles.get(position);
                        
                        if (selectedSeminar == null) {
                            Logger.w(Logger.TAG_UI, "Selected seminar tile is null at position " + position);
                            Toast.makeText(PresenterStartSessionActivity.this, "Failed to select seminar", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        btnStartSession.setEnabled(true);

                        Logger.d("ListView", "Using seminarId=" + selectedSeminar.seminarId + ", presenterSeminarId=" + selectedSeminar.presenterSeminarId);
                        Logger.d("ListView", "Selected seminar name: " + selectedSeminar.seminarName);
                        
                        // Update adapter selection
                        adapter.setSelectedPosition(position);
                        
                        Logger.d("ListView", "Item clicked: " + selectedSeminar.seminarName);
                        Logger.userAction("Select Seminar", selectedSeminar.seminarName);
                    });

                    restoreSelectionIfPossible();
                }

                @Override
                public void onFailure(Call<java.util.List<ApiService.PresenterSeminarDto>> call, Throwable t) {
                    // Stop refresh indicator (temporarily removed)
                    Logger.e(Logger.TAG_UI, "Failed to load presenter seminars", t);
                    Toast.makeText(PresenterStartSessionActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            // SwipeRefreshLayout removed - using ListView instead
            Logger.e(Logger.TAG_UI, "Error loading presenter seminars", e);
            Toast.makeText(this, "Error loading seminars", Toast.LENGTH_SHORT).show();
        }
    }
    
    // Removed spinner method - using grid only
    
    private void startSession() {
        Logger.userAction("Start Session", "User clicked start session button");
        serverLogger.userAction("Start Session", "User clicked start session button");
        
        if (selectedSeminar == null) {
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
        if (selectedSeminar == null) {
            Logger.e(Logger.TAG_UI, "createNewSession invoked with null selectedSeminar");
            Toast.makeText(this, "Please select a seminar", Toast.LENGTH_SHORT).show();
            return;
        }

        Long seminarId = selectedSeminar.seminarId;
        if (seminarId == null || seminarId <= 0) {
            Logger.e(Logger.TAG_UI, "Selected seminar missing seminarId. presenterSeminarId=" + selectedSeminar.presenterSeminarId);
            serverLogger.e(ServerLogger.TAG_UI, "Selected seminar missing seminarId. presenterSeminarId=" + selectedSeminar.presenterSeminarId);
            Toast.makeText(this, "Selected seminar lacks a valid ID", Toast.LENGTH_LONG).show();
            return;
        }

        Logger.userAction("Create New Session", "seminarId=" + seminarId + ", presenterSeminarId=" + selectedSeminar.presenterSeminarId);

        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault());
        String startTime = sdf.format(new java.util.Date());

        ApiService.CreateSessionRequest request = new ApiService.CreateSessionRequest(
                seminarId, startTime, "OPEN");

        Logger.session("Creating Session", "Seminar ID=" + seminarId + ", presenterSeminarId=" + selectedSeminar.presenterSeminarId + ", Start Time=" + startTime + " (server generates session ID)");
        Logger.api("POST", "api/v1/sessions", "Seminar ID=" + seminarId + ", presenterSeminarId=" + selectedSeminar.presenterSeminarId + ", Start Time=" + startTime + ", Status=OPEN");
        serverLogger.api("POST", "api/v1/sessions", "Seminar ID=" + seminarId + ", presenterSeminarId=" + selectedSeminar.presenterSeminarId + ", Start Time=" + startTime + ", Status=OPEN");

        Toast.makeText(this, "Creating session for seminar ID: " + seminarId, Toast.LENGTH_SHORT).show();

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
                        if (session.getSessionId() != null) {
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
        if (session == null || session.getSessionId() == null) {
            Logger.w(Logger.TAG_UI, "openQRDisplay called with invalid session");
            return;
        }

        Intent intent = new Intent(PresenterStartSessionActivity.this, QRDisplayActivity.class);
        intent.putExtra("sessionId", session.getSessionId());
        intent.putExtra("seminarId", session.getSeminarId());
        intent.putExtra("seminarName", selectedSeminar != null ? selectedSeminar.seminarName : null);
        intent.putExtra("presenterSeminarId", selectedSeminar != null ? selectedSeminar.presenterSeminarId : null);
        startActivity(intent);
    }
    
    // Removed legacy code for inline seminar creation and API debug calls
    
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
        if ((requestCode == REQ_ADD_SEMINAR || requestCode == REQ_ADD_AVAILABILITY) && resultCode == RESULT_OK) {
            loadPresenterSeminars();
        }
    }

    private void restoreSelectionIfPossible() {
        if (selectedSeminar == null) {
            return;
        }
        if (seminarTiles.isEmpty()) {
            selectedSeminar = null;
            btnStartSession.setEnabled(false);
            return;
        }
        for (int i = 0; i < seminarTiles.size(); i++) {
            ApiService.PresenterSeminarDto tile = seminarTiles.get(i);
            if (tile == null) continue;
            if (tile.presenterSeminarId != null && selectedSeminar.presenterSeminarId != null &&
                    tile.presenterSeminarId.equals(selectedSeminar.presenterSeminarId)) {
                selectedSeminar = tile;
                listSeminars.setItemChecked(i, true);
                btnStartSession.setEnabled(true);
                return;
            }
        }
        selectedSeminar = null;
        btnStartSession.setEnabled(false);
    }

    private java.util.List<ApiService.PresenterSeminarDto> sortSeminarsByNewest(
        java.util.List<ApiService.PresenterSeminarDto> list) {
        list.sort((a, b) -> {
            long tsA = a.createdAtEpoch != null ? a.createdAtEpoch : 0L;
            long tsB = b.createdAtEpoch != null ? b.createdAtEpoch : 0L;
            if (tsA == tsB) {
                long idA = a.presenterSeminarId != null ? a.presenterSeminarId : 0L;
                long idB = b.presenterSeminarId != null ? b.presenterSeminarId : 0L;
                return Long.compare(idB, idA);
            }
            return Long.compare(tsB, tsA);
        });
        return list;
    }

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

            TextView headerView = convertView.findViewById(R.id.text_seminar_header);
            TextView subtitleView = convertView.findViewById(R.id.text_seminar_subtitle);
            TextView metaView = convertView.findViewById(R.id.text_seminar_meta);
            TextView descriptionView = convertView.findViewById(R.id.text_seminar_description);

            String header = safeTrim(seminar.seminarInstanceName);
            if (TextUtils.isEmpty(header)) {
                header = safeTrim(seminar.seminarDescription);
            }
            if (TextUtils.isEmpty(header)) {
                header = safeTrim(seminar.seminarDisplayName);
            }
            if (TextUtils.isEmpty(header)) {
                header = safeTrim(seminar.seminarName);
            }
            headerView.setText(header);

            subtitleView.setVisibility(View.GONE);

            metaView.setText(seminar.getNormalizedSlots());

            String description = safeTrim(seminar.tileDescription);
            if (TextUtils.isEmpty(description)) {
                description = safeTrim(seminar.seminarDescription);
            }

            if (!TextUtils.isEmpty(description)) {
                descriptionView.setVisibility(View.VISIBLE);
                descriptionView.setText(description);
            } else {
                descriptionView.setVisibility(View.GONE);
            }

            if (position == getSelectedPosition()) {
                convertView.setBackgroundColor(0xFFE3F2FD);
                headerView.setTextColor(0xFF1976D2);
            } else {
                convertView.setBackgroundColor(0xFFFFFFFF);
                headerView.setTextColor(0xFF333333);
            }

            return convertView;
        }

        private int selectedPosition = -1;

        private String safeTrim(String value) {
            return TextUtils.isEmpty(value) ? "" : value.trim();
        }

        public void setSelectedPosition(int position) {
            selectedPosition = position;
            notifyDataSetChanged();
        }

        public int getSelectedPosition() {
            return selectedPosition;
        }
    }
}
