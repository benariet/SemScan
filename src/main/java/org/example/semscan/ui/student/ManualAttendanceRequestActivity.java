package org.example.semscan.ui.student;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.text.TextUtils;
import android.text.format.DateFormat;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.function.Consumer;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.constants.ApiConstants;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Session;
import org.example.semscan.data.model.ManualAttendanceResponse;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.ServerLogger;
import org.example.semscan.utils.ToastUtils;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import android.view.View;

public class ManualAttendanceRequestActivity extends AppCompatActivity {

    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private ServerLogger serverLogger;
    private Long currentSessionId;

    private EditText editReason;
    private Button btnSubmitRequest;
    private ProgressBar progressLoading;
    private TextView textStatus;
    private View formContainer;
    private final List<Session> availableSessions = new ArrayList<>();
    private RecyclerView recyclerSessions;
    private SessionsAdapter sessionsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_attendance_request);

        Logger.i(Logger.TAG_UI, "ManualAttendanceRequestActivity created");

        preferencesManager = PreferencesManager.getInstance(this);
        ApiClient apiClient = ApiClient.getInstance(this);
        apiService = apiClient.getApiService();
        serverLogger = ServerLogger.getInstance(this);
        
        // Update user context for student logging
        String username = preferencesManager.getUserName();
        String userRole = preferencesManager.getUserRole();
        serverLogger.updateUserContext(username, userRole);

        // Test logging to verify student context
        serverLogger.i(ServerLogger.TAG_UI, "ManualAttendanceRequestActivity created - User: " + username + ", Role: " + userRole);

        // Check if user is actually a student
        if (!preferencesManager.isStudent()) {
            Logger.w(Logger.TAG_UI, "User is not a student, finishing activity");
            finish();
            return;
        }

        setupToolbar();
        initializeViews();
        checkForActiveSessions();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manual Attendance Request");
        }
    }

    private void initializeViews() {
        editReason = findViewById(R.id.edit_reason);
        btnSubmitRequest = findViewById(R.id.btn_submit_request);
        progressLoading = findViewById(R.id.progress_loading);
        textStatus = findViewById(R.id.text_status);
        formContainer = findViewById(R.id.container_form);
        recyclerSessions = findViewById(R.id.recycler_sessions);
        recyclerSessions.setLayoutManager(new LinearLayoutManager(this));
        sessionsAdapter = new SessionsAdapter(session -> currentSessionId = session.getSessionId());
        recyclerSessions.setAdapter(sessionsAdapter);

        btnSubmitRequest.setOnClickListener(v -> {
            if (validateInput()) {
                submitManualRequest(editReason.getText().toString().trim());
            }
        });
    }

    private void checkForActiveSessions() {
        Logger.userAction("Check Active Sessions", "Checking for active sessions for manual attendance");
        
        // No authentication required
        Logger.d("ManualAttendance", "Checking for active sessions (no authentication required)");
        
        Call<List<Session>> call = apiService.getOpenSessions();
        call.enqueue(new Callback<List<Session>>() {
            @Override
            public void onResponse(Call<List<Session>> call, Response<List<Session>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Session> openSessions = response.body();
                    
                    Logger.session("Sessions Retrieved", "Found " + openSessions.size() + " open sessions");
                    
                    // Filter to show only recent sessions (last 10) to avoid overwhelming the user
                    if (openSessions.size() > 10) {
                        openSessions = openSessions.subList(0, 10);
                        Logger.i(Logger.TAG_UI, "Filtered sessions to show only first 10 of " + response.body().size() + " total");
                    }
                    
                    handleOpenSessions(openSessions);
                } else {
                    Logger.apiError("GET", "api/v1/sessions/open", response.code(), "Failed to get open sessions");
                    showError("Failed to get active sessions");
                    progressLoading.setVisibility(View.GONE);
                    textStatus.setVisibility(View.VISIBLE);
                    textStatus.setText("Could not load sessions. Please try again later.");
                }
            }
            
            @Override
            public void onFailure(Call<List<Session>> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to check active sessions", t);
                showError("Network error: " + t.getMessage());
                progressLoading.setVisibility(View.GONE);
                textStatus.setVisibility(View.VISIBLE);
                textStatus.setText("Network error. Please try again later.");
            }
        });
    }

    private void handleOpenSessions(List<Session> sessions) {
        availableSessions.clear();
        availableSessions.addAll(sessions);

        if (sessions.isEmpty()) {
            textStatus.setVisibility(View.VISIBLE);
            textStatus.setText("No active sessions available right now.");
            btnSubmitRequest.setEnabled(false);
            progressLoading.setVisibility(View.GONE);
            formContainer.setVisibility(View.GONE);
            currentSessionId = null; // Clear session ID when no sessions available
        } else {
            sessionsAdapter.setSessions(sessions);
            // currentSessionId is automatically set via the selection listener (line 109)
            // No need to set it here as it will be set when the adapter notifies selection
            progressLoading.setVisibility(View.GONE);
            textStatus.setVisibility(View.GONE);
            formContainer.setVisibility(View.VISIBLE);
            btnSubmitRequest.setEnabled(true);
        }
    }

    private boolean validateInput() {
        String reason = editReason.getText().toString().trim();
        if (reason.isEmpty()) {
            ToastUtils.showError(this, "Please provide a reason");
            editReason.requestFocus();
            return false;
        }

        return true;
    }

    private void submitManualRequest(String reason) {
        String studentUsername = preferencesManager.getUserName();
        if (TextUtils.isEmpty(studentUsername)) {
            Logger.e(Logger.TAG_UI, "Cannot submit manual request - no student username");
            showError("Student username not found. Please check settings.");
            return;
        }
        
        // Get and validate selected session FIRST before any logging or processing
        Session selectedSession = sessionsAdapter.getSelectedSession();
        if (selectedSession == null) {
            Logger.w(Logger.TAG_UI, "No session selected when submitting manual request");
            ToastUtils.showError(this, "Please choose a session");
            return;
        }
        
        // Validate that the selected session is still in the available sessions list
        // This prevents using a stale session that might have been closed
        boolean sessionStillValid = false;
        for (Session session : availableSessions) {
            if (session.getSessionId() != null && session.getSessionId().equals(selectedSession.getSessionId())) {
                // Verify the session is still open
                if ("OPEN".equals(session.getStatus()) || "open".equalsIgnoreCase(session.getStatus())) {
                    sessionStillValid = true;
                    break;
                }
            }
        }
        
        if (!sessionStillValid) {
            Logger.w(Logger.TAG_UI, "Selected session is no longer valid - session may have been closed");
            ToastUtils.showError(this, "The selected session is no longer available. Please refresh and select a different session.");
            // Optionally refresh the session list
            checkForActiveSessions();
            return;
        }
        
        // Set currentSessionId from the validated selected session
        currentSessionId = selectedSession.getSessionId();
        
        String deviceId = android.provider.Settings.Secure.getString(
            getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        
        // Log AFTER validation and setting currentSessionId
        Logger.attendance("Submitting Manual Request", "Session ID: " + currentSessionId + 
            ", Student username: " + studentUsername + ", Reason: " + reason);
        serverLogger.attendance("Submitting Manual Request", "Session ID: " + currentSessionId + 
            ", Student username: " + studentUsername + ", Reason: " + reason);
        Logger.api("POST", "api/v1/attendance/manual", 
            "Session ID: " + currentSessionId + ", Student username: " + studentUsername);
        serverLogger.api("POST", "api/v1/attendance/manual", 
            "Session ID: " + currentSessionId + ", Student username: " + studentUsername);
        ApiService.CreateManualRequestRequest request = new ApiService.CreateManualRequestRequest(
            currentSessionId, studentUsername, reason, deviceId);
        
        Call<ManualAttendanceResponse> call = apiService.createManualRequest(request);
        call.enqueue(new Callback<ManualAttendanceResponse>() {
            @Override
            public void onResponse(Call<ManualAttendanceResponse> call, Response<ManualAttendanceResponse> response) {
                if (response.isSuccessful()) {
                    ManualAttendanceResponse attendance = response.body();
                    if (attendance != null) {
                        Logger.attendance("Manual Request Submitted", "Student: " + studentUsername + 
                            ", Session: " + currentSessionId);
                        serverLogger.attendance("Manual Request Submitted", "Student: " + studentUsername + 
                            ", Session: " + currentSessionId);
                        Logger.apiResponse("POST", "api/v1/attendance/manual", 
                            response.code(), "Manual request submitted successfully");
                        serverLogger.apiResponse("POST", "api/v1/attendance/manual", 
                            response.code(), "Manual request submitted successfully");
                        serverLogger.flushLogs(); // Force send logs after successful submission
                        showSuccess("Manual attendance request submitted. Please wait for approval.");
                    } else {
                        Logger.w(Logger.TAG_UI, "Invalid manual request response from server");
                        showError("Invalid response from server");
                    }
                } else {
                    Logger.apiError("POST", "api/v1/attendance/manual", 
                        response.code(), "Failed to submit manual request");
                    // Send server-side error as well
                    serverLogger.apiError("POST", "api/v1/attendance/manual", response.code(), "Failed to submit manual request");
                    handleManualRequestError(response.code());
                }
            }
            
            @Override
            public void onFailure(Call<ManualAttendanceResponse> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Manual request submission failed", t);
                // Forward to server error logger
                serverLogger.e(Logger.TAG_UI, "Manual request submission failed", t);
                serverLogger.flushLogs();
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void handleManualRequestError(int responseCode) {
        switch (responseCode) {
            case 400:
                showError("Invalid request. Please check your information.");
                break;
            case 401:
                showError("Server error. Please try again.");
                break;
            case 409:
                showError("You have already submitted a request for this session.");
                break;
            case 404:
                showError("Session not found or not accepting requests.");
                break;
            default:
                showError("Failed to submit manual request. Please try again.");
                break;
        }
    }

    private void showSuccess(String message) {
        ToastUtils.showSuccess(this, message);
        // Finish activity after showing success message
        finish();
    }

    private void showError(String message) {
        ToastUtils.showError(this, message);
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
    
    @Override
    protected void onDestroy() {
        if (serverLogger != null) {
            serverLogger.flushLogs();
        }
        super.onDestroy();
    }

    private static class SessionsAdapter extends RecyclerView.Adapter<SessionsAdapter.SessionViewHolder> {
        private final java.util.List<Session> sessions = new java.util.ArrayList<>();
        private int selectedPosition = 0;
        private final Consumer<Session> selectionListener;

        SessionsAdapter(Consumer<Session> selectionListener) {
            this.selectionListener = selectionListener;
        }

        void setSessions(List<Session> newSessions) {
            sessions.clear();
            sessions.addAll(newSessions);
            if (selectedPosition >= sessions.size()) {
                selectedPosition = 0;
            }
            notifyDataSetChanged();
            notifySelection();
        }

        Session getSelectedSession() {
            if (sessions.isEmpty() || selectedPosition < 0 || selectedPosition >= sessions.size()) {
                return null;
            }
            return sessions.get(selectedPosition);
        }

        Long getSelectedSessionId() {
            Session selected = getSelectedSession();
            return selected != null ? selected.getSessionId() : null;
        }

        @NonNull
        @Override
        public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_manual_session, parent, false);
            return new SessionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
            Session session = sessions.get(position);
            holder.bind(session, position, position == selectedPosition, v -> select(position));
        }

        @Override
        public int getItemCount() {
            return sessions.size();
        }

        private void select(int position) {
            if (position < 0 || position >= sessions.size()) return;
            selectedPosition = position;
            notifyDataSetChanged();
            notifySelection();
        }

        private void notifySelection() {
            if (selectionListener != null) {
                Session selected = getSelectedSession();
                if (selected != null) {
                    selectionListener.accept(selected);
                }
            }
        }

        static class SessionViewHolder extends RecyclerView.ViewHolder {
            private final TextView textTitle;
            private final TextView textDate;
            private final TextView textDescription;
            private final TextView badgeStatus;

            SessionViewHolder(@NonNull View itemView) {
                super(itemView);
                textTitle = itemView.findViewById(R.id.text_session_title);
                textDate = itemView.findViewById(R.id.text_session_date);
                textDescription = itemView.findViewById(R.id.text_session_description);
                badgeStatus = itemView.findViewById(R.id.badge_session_status);
            }

            void bind(Session session, int position, boolean selected, View.OnClickListener clickListener) {
                // Format date and time nicely
                String dateTimeStr = "Unknown date";
                if (session.getStartTime() > 0) {
                    java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMM dd, yyyy • HH:mm", java.util.Locale.getDefault());
                    dateTimeStr = dateFormat.format(new java.util.Date(session.getStartTime()));
                }

                // Set title - use seminar name if available, otherwise use a descriptive title
                String title = "Seminar Session";
                if (session.getSeminarId() != null) {
                    title = "Seminar Session #" + session.getSeminarId();
                }
                textTitle.setText(title);

                // Set date/time
                textDate.setText(dateTimeStr);

                // Set description - show seminar ID and status if available
                StringBuilder descriptionBuilder = new StringBuilder();
                if (session.getSeminarId() != null) {
                    descriptionBuilder.append("Seminar ID: ").append(session.getSeminarId());
                }
                if (session.getEndTime() != null && session.getEndTime() > 0) {
                    if (descriptionBuilder.length() > 0) {
                        descriptionBuilder.append(" • ");
                    }
                    java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                    String endTime = timeFormat.format(new java.util.Date(session.getEndTime()));
                    descriptionBuilder.append("Until ").append(endTime);
                }
                
                if (descriptionBuilder.length() > 0) {
                    textDescription.setText(descriptionBuilder.toString());
                    textDescription.setVisibility(View.VISIBLE);
                } else {
                    textDescription.setVisibility(View.GONE);
                }

                // Set status badge
                String status = session.getStatus() != null ? session.getStatus() : "OPEN";
                badgeStatus.setText(status);
                
                // Set badge color based on selection
                if (selected) {
                    badgeStatus.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_slot_status));
                    badgeStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                } else {
                    badgeStatus.setBackground(ContextCompat.getDrawable(itemView.getContext(), R.drawable.bg_slot_status));
                    badgeStatus.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.white));
                }

                // Set card selection state
                itemView.setOnClickListener(clickListener);
                itemView.setSelected(selected);
                
                // Change card elevation/background based on selection
                if (itemView instanceof com.google.android.material.card.MaterialCardView) {
                    com.google.android.material.card.MaterialCardView card = (com.google.android.material.card.MaterialCardView) itemView;
                    if (selected) {
                        card.setCardElevation(8f);
                        card.setStrokeWidth(2);
                        card.setStrokeColor(ContextCompat.getColor(itemView.getContext(), R.color.primary_blue));
                    } else {
                        card.setCardElevation(4f);
                        card.setStrokeWidth(0);
                    }
                }
            }
        }
    }
}
