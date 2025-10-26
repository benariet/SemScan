package org.example.semscan.ui.student;

import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.constants.ApiConstants;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.Session;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.ServerLogger;
import org.example.semscan.utils.ToastUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManualAttendanceRequestActivity extends AppCompatActivity {

    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private ServerLogger serverLogger;
    private Long currentSessionId;

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
        Long userId = preferencesManager.getUserId();
        String userRole = preferencesManager.getUserRole();
        serverLogger.updateUserContext(userId, userRole);
        
        // Test logging to verify student context
        serverLogger.i(ServerLogger.TAG_UI, "ManualAttendanceRequestActivity created - User: " + userId + ", Role: " + userRole);

        // Check if user is actually a student
        if (!preferencesManager.isStudent()) {
            Logger.w(Logger.TAG_UI, "User is not a student, finishing activity");
            finish();
            return;
        }

        setupToolbar();
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
                    
                    if (openSessions.isEmpty()) {
                        showNoActiveSessionsDialog();
                    } else if (openSessions.size() == 1) {
                        // Only one session, use it directly
                        currentSessionId = openSessions.get(0).getSessionId();
                        showManualRequestDialog();
                    } else {
                        // Multiple sessions, show selection dialog
                        showSessionSelectionDialog(openSessions);
                    }
                } else {
                    Logger.apiError("GET", "api/v1/sessions/open", response.code(), "Failed to get open sessions");
                    showError("Failed to get active sessions");
                }
            }
            
            @Override
            public void onFailure(Call<List<Session>> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to check active sessions", t);
                showError("Network error: " + t.getMessage());
            }
        });
    }

    private void showNoActiveSessionsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("No Active Sessions")
               .setMessage("There are currently no active sessions available for manual attendance requests.")
               .setPositiveButton("OK", (dialog, which) -> finish())
               .setCancelable(false)
               .show();
    }

    private void showSessionSelectionDialog(List<Session> sessions) {
        String[] sessionNames = new String[sessions.size()];
        for (int i = 0; i < sessions.size(); i++) {
            Session session = sessions.get(i);
            // Show session ID and start time for better identification
            String startTime = session.getStartTime() > 0 ? 
                new java.util.Date(session.getStartTime()).toString().substring(0, 16) : "Unknown";
            sessionNames[i] = session.getSessionId() + " (" + startTime + ")";
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Session")
               .setItems(sessionNames, (dialog, which) -> {
                   currentSessionId = sessions.get(which).getSessionId();
                   showManualRequestDialog();
               })
               .setNegativeButton("Cancel", (dialog, which) -> finish())
               .show();
    }

    private void showManualRequestDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_manual_request, null);
        
        EditText editReason = dialogView.findViewById(R.id.edit_reason);
        Button btnCancelRequest = dialogView.findViewById(R.id.btn_cancel_request);
        Button btnSubmitRequest = dialogView.findViewById(R.id.btn_submit_request);
        
        AlertDialog dialog = builder.setView(dialogView).create();
        
        btnCancelRequest.setOnClickListener(v -> {
            dialog.dismiss();
            finish();
        });
        
        btnSubmitRequest.setOnClickListener(v -> {
            String reason = editReason.getText().toString().trim();
            if (reason.isEmpty()) {
                ToastUtils.showError(this, "Please provide a reason");
                return;
            }
            dialog.dismiss();
            submitManualRequest(reason);
        });
        
        dialog.show();
    }

    private void submitManualRequest(String reason) {
        Long studentId = preferencesManager.getUserId();
        if (studentId == null || studentId <= 0) {
            Logger.e(Logger.TAG_UI, "Cannot submit manual request - no student ID");
            showError("Student ID not found. Please check settings.");
            return;
        }
        
        String deviceId = android.provider.Settings.Secure.getString(
            getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        
        Logger.attendance("Submitting Manual Request", "Session ID: " + currentSessionId + 
            ", Student ID: " + studentId + ", Reason: " + reason);
        serverLogger.attendance("Submitting Manual Request", "Session ID: " + currentSessionId + 
            ", Student ID: " + studentId + ", Reason: " + reason);
        Logger.api("POST", "api/v1/attendance/manual-request", 
            "Session ID: " + currentSessionId + ", Student ID: " + studentId);
        serverLogger.api("POST", "api/v1/attendance/manual-request", 
            "Session ID: " + currentSessionId + ", Student ID: " + studentId);
        
        ApiService.CreateManualRequestRequest request = new ApiService.CreateManualRequestRequest(
            currentSessionId, studentId, reason, deviceId);
        
        Call<Attendance> call = apiService.createManualRequest(request);
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful()) {
                    Attendance attendance = response.body();
                    if (attendance != null) {
                        Logger.attendance("Manual Request Submitted", "Student: " + studentId + 
                            ", Session: " + currentSessionId);
                        serverLogger.attendance("Manual Request Submitted", "Student: " + studentId + 
                            ", Session: " + currentSessionId);
                        Logger.apiResponse("POST", "api/v1/attendance/manual-request", 
                            response.code(), "Manual request submitted successfully");
                        serverLogger.apiResponse("POST", "api/v1/attendance/manual-request", 
                            response.code(), "Manual request submitted successfully");
                        serverLogger.flushLogs(); // Force send logs after successful submission
                        showSuccess("Manual attendance request submitted. Please wait for approval.");
                    } else {
                        Logger.w(Logger.TAG_UI, "Invalid manual request response from server");
                        showError("Invalid response from server");
                    }
                } else {
                    Logger.apiError("POST", "api/v1/attendance/manual-request", 
                        response.code(), "Failed to submit manual request");
                    // Send server-side error as well
                    serverLogger.apiError("POST", "api/v1/attendance/manual-request", response.code(), "Failed to submit manual request");
                    handleManualRequestError(response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
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
}
