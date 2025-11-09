package org.example.semscan.ui.teacher;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.core.content.FileProvider;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.constants.ApiConstants;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.ManualAttendanceResponse;
import org.example.semscan.data.model.Session;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.ToastUtils;

import java.util.List;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExportActivity extends AppCompatActivity {
    
    private RadioGroup radioGroupFormat;
    private Button btnExport;
    private TextView textSessionId;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    private Long currentSessionId;
    private ManualRequestAdapter requestAdapter;
    
    // Session details for filename generation
    private String sessionDate;
    private String sessionTimeSlot;
    private String sessionPresenter;
    private String sessionTopic;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        
        Logger.i(Logger.TAG_UI, "ExportActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        setupRequestAdapter();
    }
    
    private void initializeViews() {
        radioGroupFormat = findViewById(R.id.radio_group_format);
        btnExport = findViewById(R.id.btn_export);
        textSessionId = findViewById(R.id.text_session_id);
        
        // Get current session ID from intent (passed from QR display)
        currentSessionId = getIntent().getLongExtra("sessionId", -1L);
        
        // Get session details for filename generation
        sessionDate = getIntent().getStringExtra("sessionDate");
        sessionTimeSlot = getIntent().getStringExtra("sessionTimeSlot");
        sessionPresenter = getIntent().getStringExtra("sessionPresenter");
        sessionTopic = getIntent().getStringExtra("sessionTopic");
        
        // Display the session ID
        if (currentSessionId != null && currentSessionId > 0) {
            textSessionId.setText(String.valueOf(currentSessionId));
            Logger.d(Logger.TAG_UI, "Export activity initialized with session ID: " + currentSessionId);
        } else {
            textSessionId.setText("No session data available");
            Logger.e(Logger.TAG_UI, "No session ID provided in intent");
        }
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupClickListeners() {
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkPendingRequests();
            }
        });
    }
    
    private void setupRequestAdapter() {
        requestAdapter = new ManualRequestAdapter(new ManualRequestAdapter.OnRequestActionListener() {
            @Override
            public void onApprove(ManualAttendanceResponse request) {
                approveRequest(request);
            }
            
            @Override
            public void onReject(ManualAttendanceResponse request) {
                rejectRequest(request);
            }
        });
    }
    
    private void checkPendingRequests() {
        Logger.userAction("Check Pending Requests", "Checking for pending manual requests before export");
        
        // API key no longer required - removed authentication
        
        if (currentSessionId == null || currentSessionId <= 0) {
            Logger.e(Logger.TAG_UI, "Export failed - no session ID available");
            ToastUtils.showError(this, "No session data available");
            return;
        }
        
        Logger.api("GET", "api/v1/attendance/manual/pending-requests", "Session ID: " + currentSessionId);
        
        Call<List<ManualAttendanceResponse>> call = apiService.getPendingManualRequests(currentSessionId);
        call.enqueue(new Callback<List<ManualAttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<ManualAttendanceResponse>> call, Response<List<ManualAttendanceResponse>> response) {
                Logger.d("ExportActivity", "=== API RESPONSE DEBUG ===");
                Logger.d("ExportActivity", "Response successful: " + response.isSuccessful());
                Logger.d("ExportActivity", "Response code: " + response.code());
                Logger.d("ExportActivity", "Response body null: " + (response.body() == null));
                
                if (response.isSuccessful() && response.body() != null) {
                    List<ManualAttendanceResponse> pendingRequests = response.body();
                    Logger.apiResponse("GET", "api/v1/attendance/manual/pending-requests", 
                        response.code(), "Found " + pendingRequests.size() + " pending requests");
                    
                    // Debug logging for pending requests
                    Logger.d("ExportActivity", "=== PENDING REQUESTS DEBUG ===");
                    Logger.d("ExportActivity", "Total pending requests: " + pendingRequests.size());
                    for (int i = 0; i < pendingRequests.size(); i++) {
                        ManualAttendanceResponse req = pendingRequests.get(i);
                        Logger.d("ExportActivity", "Request " + i + ":");
                        Logger.d("ExportActivity", "  - Attendance ID: '" + req.getAttendanceId() + "'");
                        Logger.d("ExportActivity", "  - Session ID: '" + req.getSessionId() + "'");
                        Logger.d("ExportActivity", "  - Student Username: '" + req.getStudentUsername() + "'");
                        Logger.d("ExportActivity", "  - Request Status: '" + req.getRequestStatus() + "'");
                        Logger.d("ExportActivity", "  - Manual Reason: '" + req.getReason() + "'");
                        Logger.d("ExportActivity", "  - Full object: " + req.toString());
                    }
                    
                    if (pendingRequests.isEmpty()) {
                        // No pending requests, proceed with export
                        Logger.d("ExportActivity", "No pending requests found, proceeding with export");
                        exportData();
                    } else {
                        // Show review modal
                        Logger.d("ExportActivity", "Found " + pendingRequests.size() + " pending requests, showing review modal");
                        showReviewModal(pendingRequests);
                    }
                } else {
                    // Log detailed error information
                    String errorBody = null;
                    if (response.errorBody() != null) {
                        try {
                            errorBody = response.errorBody().string();
                        } catch (Exception e) {
                            Logger.e(Logger.TAG_UI, "Error reading pending requests response body", e);
                        }
                    }
                    
                    Logger.apiError("GET", "api/v1/attendance/manual/pending-requests", 
                        response.code(), errorBody != null ? errorBody : "Failed to get pending requests");
                    Logger.d("ExportActivity", "API Error - Code: " + response.code() + ", Body: " + errorBody);
                    ToastUtils.showError(ExportActivity.this, "Failed to check pending requests");
                }
            }
            
            @Override
            public void onFailure(Call<List<ManualAttendanceResponse>> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to check pending requests", t);
                ToastUtils.showError(ExportActivity.this, "Network error: " + t.getMessage());
            }
        });
    }
    
    /**
     * Refresh pending requests list without triggering export
     * This is used when approving/rejecting requests - we just want to update the UI,
     * not automatically proceed with export
     */
    private void refreshPendingRequestsOnly() {
        if (currentSessionId == null || currentSessionId <= 0) {
            return;
        }
        
        Logger.api("GET", "api/v1/attendance/manual/pending-requests", "Session ID: " + currentSessionId + " (refresh only)");
        
        Call<List<ManualAttendanceResponse>> call = apiService.getPendingManualRequests(currentSessionId);
        call.enqueue(new Callback<List<ManualAttendanceResponse>>() {
            @Override
            public void onResponse(Call<List<ManualAttendanceResponse>> call, Response<List<ManualAttendanceResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<ManualAttendanceResponse> pendingRequests = response.body();
                    Logger.d("ExportActivity", "Refreshed pending requests: " + pendingRequests.size() + " remaining");
                    
                    // Update the adapter with the new list
                    requestAdapter.updateRequests(pendingRequests);
                    
                    // If there are no more pending requests, we could optionally show a message
                    // but we should NOT automatically trigger export
                    if (pendingRequests.isEmpty()) {
                        Logger.d("ExportActivity", "All pending requests have been resolved");
                        // Don't auto-export - user must click Export button explicitly
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<ManualAttendanceResponse>> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to refresh pending requests", t);
                // Don't show error toast for refresh - it's just a background update
            }
        });
    }
    
    private void showReviewModal(List<ManualAttendanceResponse> pendingRequests) {
        Logger.d("ExportActivity", "=== SHOW REVIEW MODAL DEBUG ===");
        Logger.d("ExportActivity", "Creating review modal for " + pendingRequests.size() + " requests");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_review_requests, null);
        
        TextView textPendingCount = dialogView.findViewById(R.id.text_pending_count);
        RecyclerView recyclerRequests = dialogView.findViewById(R.id.recycler_requests);
        Button btnApproveAllSafe = dialogView.findViewById(R.id.btn_approve_all_safe);
        Button btnRejectAllDuplicates = dialogView.findViewById(R.id.btn_reject_all_duplicates);
        Button btnCancelReview = dialogView.findViewById(R.id.btn_cancel_review);
        Button btnContinueExport = dialogView.findViewById(R.id.btn_continue_export);
        
        Logger.d("ExportActivity", "Dialog view components found:");
        Logger.d("ExportActivity", "  - textPendingCount: " + (textPendingCount != null));
        Logger.d("ExportActivity", "  - recyclerRequests: " + (recyclerRequests != null));
        Logger.d("ExportActivity", "  - btnApproveAllSafe: " + (btnApproveAllSafe != null));
        Logger.d("ExportActivity", "  - btnRejectAllDuplicates: " + (btnRejectAllDuplicates != null));
        Logger.d("ExportActivity", "  - btnCancelReview: " + (btnCancelReview != null));
        Logger.d("ExportActivity", "  - btnContinueExport: " + (btnContinueExport != null));
        
        // Set up recycler view
        recyclerRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerRequests.setAdapter(requestAdapter);
        requestAdapter.updateRequests(pendingRequests);
        
        // Update pending count
        textPendingCount.setText(pendingRequests.size() + " pending requests");
        
        AlertDialog dialog = builder.setView(dialogView).create();
        Logger.d("ExportActivity", "Dialog created successfully");
        
        // Set up button listeners
        btnApproveAllSafe.setOnClickListener(v -> {
            approveAllSafe(pendingRequests);
            dialog.dismiss();
        });
        
        btnRejectAllDuplicates.setOnClickListener(v -> {
            rejectAllDuplicates(pendingRequests);
            dialog.dismiss();
        });
        
        btnCancelReview.setOnClickListener(v -> dialog.dismiss());
        
        btnContinueExport.setOnClickListener(v -> {
            dialog.dismiss();
            exportData();
        });
        
        Logger.d("ExportActivity", "About to show dialog");
        try {
            dialog.show();
            Logger.d("ExportActivity", "Dialog show() called successfully");
        } catch (Exception e) {
            Logger.e("ExportActivity", "Failed to show dialog", e);
            // Fallback: handle pending requests directly
            handlePendingRequestsDirectly(pendingRequests);
        }
    }
    
    private void approveRequest(ManualAttendanceResponse request) {
        // API key no longer required - removed authentication
        
        // Debug logging to see what's in the request object
        Logger.d("ExportActivity", "=== ATTENDANCE REQUEST DEBUG ===");
        Logger.d("ExportActivity", "Attendance ID: '" + request.getAttendanceId() + "'");
        Logger.d("ExportActivity", "Session ID: '" + request.getSessionId() + "'");
        Logger.d("ExportActivity", "Student Username: '" + request.getStudentUsername() + "'");
        Logger.d("ExportActivity", "Request Status: '" + request.getRequestStatus() + "'");
        Logger.d("ExportActivity", "Manual Reason: '" + request.getReason() + "'");
        Logger.d("ExportActivity", "Attendance object: " + request.toString());
        
        // Check if attendanceId is null
        if (request.getAttendanceId() == null || request.getAttendanceId() <= 0) {
            Logger.e("ExportActivity", "Attendance ID is null or empty - cannot approve request");
            ToastUtils.showError(this, "Cannot approve request: Missing attendance ID");
            return;
        }
        
        Logger.userAction("Approve Request", "Approving manual request for student: " + request.getStudentUsername());
        Logger.api("POST", "api/v1/attendance/" + request.getAttendanceId() + "/approve",
                "Attendance ID: " + request.getAttendanceId());

        Call<ManualAttendanceResponse> call = apiService.approveManualRequest(
                request.getAttendanceId(), preferencesManager.getUserName());
        call.enqueue(new Callback<ManualAttendanceResponse>() {
            @Override
            public void onResponse(Call<ManualAttendanceResponse> call, Response<ManualAttendanceResponse> response) {
                if (response.isSuccessful()) {
                    Logger.apiResponse("POST", "api/v1/attendance/" + request.getAttendanceId() + "/approve", 
                        response.code(), "Request approved successfully");
                    Toast.makeText(ExportActivity.this, "Request approved", Toast.LENGTH_SHORT).show();
                    // Refresh the list WITHOUT triggering export
                    // Export should only happen when user clicks the Export button, not when approving requests
                    refreshPendingRequestsOnly();
                } else {
                    Logger.apiError("POST", "api/v1/attendance/" + request.getAttendanceId() + "/approve", 
                        response.code(), "Failed to approve request");
                    Toast.makeText(ExportActivity.this, "Failed to approve request", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ManualAttendanceResponse> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to approve request", t);
                ToastUtils.showError(ExportActivity.this, "Network error: " + t.getMessage());
            }
        });
    }
    
    private void rejectRequest(ManualAttendanceResponse request) {
        // API key no longer required - removed authentication
        
        Logger.userAction("Reject Request", "Rejecting manual request for student: " + request.getStudentUsername());
        Logger.api("POST", "api/v1/attendance/" + request.getAttendanceId() + "/reject",
                "Attendance ID: " + request.getAttendanceId());

        Call<ManualAttendanceResponse> call = apiService.rejectManualRequest(
                request.getAttendanceId(), preferencesManager.getUserName());
        call.enqueue(new Callback<ManualAttendanceResponse>() {
            @Override
            public void onResponse(Call<ManualAttendanceResponse> call, Response<ManualAttendanceResponse> response) {
                if (response.isSuccessful()) {
                    Logger.apiResponse("POST", "api/v1/attendance/" + request.getAttendanceId() + "/reject", 
                        response.code(), "Request rejected successfully");
                    Toast.makeText(ExportActivity.this, "Request rejected", Toast.LENGTH_SHORT).show();
                    // Refresh the list WITHOUT triggering export
                    // Export should only happen when user clicks the Export button, not when rejecting requests
                    refreshPendingRequestsOnly();
                } else {
                    Logger.apiError("POST", "api/v1/attendance/" + request.getAttendanceId() + "/reject", 
                        response.code(), "Failed to reject request");
                    Toast.makeText(ExportActivity.this, "Failed to reject request", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ManualAttendanceResponse> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to reject request", t);
                ToastUtils.showError(ExportActivity.this, "Network error: " + t.getMessage());
            }
        });
    }
    
    private void approveAllSafe(List<ManualAttendanceResponse> requests) {
        // TODO: Implement bulk approve logic based on auto_flags
        Toast.makeText(this, "Approve All Safe - Not implemented yet", Toast.LENGTH_SHORT).show();
    }
    
    private void rejectAllDuplicates(List<ManualAttendanceResponse> requests) {
        // TODO: Implement bulk reject duplicates logic
        Toast.makeText(this, "Reject All Duplicates - Not implemented yet", Toast.LENGTH_SHORT).show();
    }
    
    private void exportData() {
        Logger.userAction("Export Data", "User clicked export button");
        
        // API key no longer required - removed authentication
        
        if (currentSessionId == null) {
            Logger.e(Logger.TAG_UI, "Export failed - no session ID available");
            ToastUtils.showError(this, "No session data available");
            return;
        }
        
        boolean isExcel = radioGroupFormat.getCheckedRadioButtonId() == R.id.radio_excel;
        String format = isExcel ? "Excel (.xlsx)" : "CSV (.csv)";
        
        Logger.i(Logger.TAG_UI, "Starting export - Session ID: " + currentSessionId + ", Format: " + format);
        exportSessionData(currentSessionId, isExcel);
    }
    
    private void exportSessionData(Long sessionId, boolean isExcel) {
        Call<ResponseBody> call;
        String filename;
        String mimeType;
        String endpoint;
        
        if (isExcel) {
            call = apiService.exportXlsx(sessionId);
            filename = generateExportFilename(sessionId, ".xlsx");
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            endpoint = "api/v1/export/xlsx";
        } else {
            call = apiService.exportCsv(sessionId);
            filename = generateExportFilename(sessionId, ".csv");
            mimeType = "text/csv";
            endpoint = "api/v1/export/csv";
        }
        
        Logger.api("GET", endpoint, "Session ID: " + sessionId);
        
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Logger.apiResponse("GET", endpoint, response.code(), "Export data received successfully");
                        
                        // Save file to external storage
                        File file = new File(getExternalFilesDir(null), filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(response.body().bytes());
                        fos.close();
                        
                        Logger.i(Logger.TAG_UI, "Export file saved: " + filename + " (" + file.length() + " bytes)");
                        
                        // Share the file
                        shareFile(file, mimeType);
                        
                        Logger.i(Logger.TAG_UI, "Export completed successfully - Session: " + sessionId);
                        Toast.makeText(ExportActivity.this, "Export successful", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Logger.e(Logger.TAG_UI, "Failed to save export file", e);
                        Toast.makeText(ExportActivity.this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Parse error response body for detailed error message
                    String errorBody = null;
                    if (response.errorBody() != null) {
                        try {
                            errorBody = response.errorBody().string();
                        } catch (Exception e) {
                            Logger.e(Logger.TAG_UI, "Error reading export response body", e);
                        }
                    }
                    
                    Logger.apiError("GET", endpoint, response.code(), errorBody != null ? errorBody : "Export request failed");
                    
                    // Show specific error message based on response code
                    String errorMessage = getExportErrorMessage(response.code(), errorBody);
                    ToastUtils.showError(ExportActivity.this, errorMessage);
                }
            }
            
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Export network failure", t);
                ToastUtils.showError(ExportActivity.this, "Network error: " + t.getMessage());
            }
        });
    }
    
    private void shareFile(File file, String mimeType) {
        Logger.i(Logger.TAG_UI, "Sending export file via email: " + file.getName() + " (" + mimeType + ")");
        
        try {
            // Use FileProvider for secure file sharing
            Uri fileUri = FileProvider.getUriForFile(this, 
                "org.example.semscan.fileprovider", file);
            
            // Create email intent with specific recipients
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("message/rfc822"); // Email MIME type
            
            // Parse multiple email recipients (comma-separated)
            String[] recipients = ApiConstants.EXPORT_EMAIL_RECIPIENTS.split(",");
            // Trim whitespace from each email address
            for (int i = 0; i < recipients.length; i++) {
                recipients[i] = recipients[i].trim();
            }
            emailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SemScan Attendance Export - Session " + currentSessionId);
            
            // Build email body with session details
            StringBuilder emailBody = new StringBuilder();
            emailBody.append("Attendance data export from SemScan\n\n");
            emailBody.append("Session ID: ").append(currentSessionId).append("\n");
            if (sessionDate != null) {
                emailBody.append("Date: ").append(sessionDate).append("\n");
            }
            if (sessionTimeSlot != null) {
                emailBody.append("Time Slot: ").append(sessionTimeSlot).append("\n");
            }
            if (sessionPresenter != null) {
                emailBody.append("Presenter: ").append(sessionPresenter).append("\n");
            }
            emailBody.append("\nPlease find the attendance export file attached.");
            
            emailIntent.putExtra(Intent.EXTRA_TEXT, emailBody.toString());
            emailIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            // Only show email apps, not all sharing apps
            try {
                startActivity(Intent.createChooser(emailIntent, "Send attendance export via email"));
                Logger.i(Logger.TAG_UI, "Email intent launched successfully");
            } catch (android.content.ActivityNotFoundException e) {
                // No email app found
                Logger.e(Logger.TAG_UI, "No email app found on device", e);
                Toast.makeText(this, "No email app found. Please install an email app to send the export.", Toast.LENGTH_LONG).show();
            }
            
        } catch (Exception e) {
            Logger.e(Logger.TAG_UI, "Failed to send file via email", e);
            Toast.makeText(this, "Failed to send email: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Provide a simple way to handle pending requests without dialog
     * This can be called as a fallback if the dialog fails
     */
    private void handlePendingRequestsDirectly(List<ManualAttendanceResponse> pendingRequests) {
        Logger.d("ExportActivity", "Handling pending requests directly - count: " + pendingRequests.size());
        
        // For now, just show a message and allow user to continue
        // In a real implementation, you might want to show a simpler dialog or list
        String message = "Found " + pendingRequests.size() + " pending manual attendance requests. " +
                        "You can either:\n" +
                        "1. Approve/reject them individually, or\n" +
                        "2. Continue with export (requests will remain pending)";
        
        ToastUtils.showError(this, message);
        
        // For debugging, let's also log the details
        for (ManualAttendanceResponse req : pendingRequests) {
            Logger.d("ExportActivity", "Pending request - ID: " + req.getAttendanceId() + 
                      ", Student: " + req.getStudentUsername() + ", Reason: " + req.getReason());
        }
    }
    
    /**
     * Get user-friendly error message for export failures
     */
    private String getExportErrorMessage(int responseCode, String errorBody) {
        switch (responseCode) {
            case 409:
                // Parse the specific error message from the server
                if (errorBody != null && errorBody.contains("manual attendance requests are pending approval")) {
                    return "Cannot export while manual attendance requests are pending approval. Please review and resolve all pending requests before exporting.";
                }
                return "Export conflict: " + (errorBody != null ? errorBody : "Please resolve pending requests");
            case 404:
                return "Session not found or no data available for export";
            case 400:
                return "Invalid export request: " + (errorBody != null ? errorBody : "Please check session data");
            case 500:
                return "Server error during export: " + (errorBody != null ? errorBody : "Please try again later");
            default:
                return "Export failed (Code: " + responseCode + ")" + 
                       (errorBody != null ? " - " + errorBody : "");
        }
    }
    
    /**
     * Generate a descriptive filename for the export file
     * Format: day_month_year_presenter_time.ext
     * Example: 9_11_2025_john_doe_13-15.csv
     *         9_11_2025_13-15.csv (if presenter not available)
     */
    private String generateExportFilename(Long sessionId, String extension) {
        StringBuilder filename = new StringBuilder();
        
        // Parse date if available (format: "2025-11-09" or "09/11/2025")
        String datePart = "";
        if (sessionDate != null && !sessionDate.isEmpty()) {
            try {
                // Try to parse different date formats
                if (sessionDate.contains("-")) {
                    // Format: "2025-11-09" -> "9_11_2025"
                    String[] parts = sessionDate.split("-");
                    if (parts.length == 3) {
                        int day = Integer.parseInt(parts[2]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[0]);
                        datePart = String.format("%d_%d_%d", day, month, year);
                    }
                } else if (sessionDate.contains("/")) {
                    // Format: "09/11/2025" -> "9_11_2025"
                    String[] parts = sessionDate.split("/");
                    if (parts.length == 3) {
                        int day = Integer.parseInt(parts[0]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[2]);
                        datePart = String.format("%d_%d_%d", day, month, year);
                    }
                }
            } catch (Exception e) {
                Logger.w(Logger.TAG_UI, "Failed to parse session date: " + sessionDate + " - " + e.getMessage());
            }
        }
        
        // If date parsing failed, try to extract from timeSlot
        if (datePart.isEmpty() && sessionTimeSlot != null && sessionTimeSlot.contains(" ")) {
            try {
                String dateStr = sessionTimeSlot.split(" ")[0]; // Get date part
                if (dateStr.contains("-")) {
                    String[] parts = dateStr.split("-");
                    if (parts.length == 3) {
                        int day = Integer.parseInt(parts[2]);
                        int month = Integer.parseInt(parts[1]);
                        int year = Integer.parseInt(parts[0]);
                        datePart = String.format("%d_%d_%d", day, month, year);
                    }
                }
            } catch (Exception e) {
                Logger.w(Logger.TAG_UI, "Failed to extract date from timeSlot: " + sessionTimeSlot + " - " + e.getMessage());
            }
        }
        
        // If still no date, use current date
        if (datePart.isEmpty()) {
            java.util.Calendar cal = java.util.Calendar.getInstance();
            int day = cal.get(java.util.Calendar.DAY_OF_MONTH);
            int month = cal.get(java.util.Calendar.MONTH) + 1; // Calendar.MONTH is 0-based
            int year = cal.get(java.util.Calendar.YEAR);
            datePart = String.format("%d_%d_%d", day, month, year);
        }
        
        filename.append(datePart);
        
        // Extract time from timeSlot (format: "2025-11-09 14:00-15:00" -> "13-15" or "14-15")
        String timePart = "";
        if (sessionTimeSlot != null && !sessionTimeSlot.isEmpty()) {
            try {
                // Extract time range (e.g., "14:00-15:00" -> "14-15")
                if (sessionTimeSlot.contains(" ")) {
                    String timeRange = sessionTimeSlot.split(" ")[1]; // Get time part
                    if (timeRange.contains("-")) {
                        String[] times = timeRange.split("-");
                        if (times.length == 2) {
                            String startTime = times[0].trim(); // "14:00"
                            String endTime = times[1].trim();   // "15:00"
                            
                            // Extract hour from "14:00" -> "14"
                            if (startTime.contains(":")) {
                                String startHour = startTime.split(":")[0];
                                String endHour = endTime.contains(":") ? endTime.split(":")[0] : endTime;
                                timePart = startHour + "-" + endHour;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Logger.w(Logger.TAG_UI, "Failed to parse timeSlot: " + sessionTimeSlot + " - " + e.getMessage());
            }
        }
        
        // Add presenter name
        String presenterPart = "";
        
        if (sessionPresenter != null && !sessionPresenter.trim().isEmpty()) {
            // Use presenter name, sanitize for filename
            presenterPart = sanitizeForFilename(sessionPresenter.trim());
        }
        
        // Build filename: date_presenter_time.ext or date_time.ext
        if (!presenterPart.isEmpty()) {
            filename.append("_").append(presenterPart);
        }
        
        if (!timePart.isEmpty()) {
            filename.append("_").append(timePart);
        }
        
        // Fallback to sessionId if we don't have enough info
        if (datePart.isEmpty() && presenterPart.isEmpty() && timePart.isEmpty()) {
            filename = new StringBuilder("attendance_").append(sessionId);
        }
        
        filename.append(extension);
        
        Logger.d(Logger.TAG_UI, "Generated export filename: " + filename.toString());
        return filename.toString();
    }
    
    /**
     * Sanitize a string for use in a filename
     * Removes special characters, replaces spaces with underscores, limits length
     */
    private String sanitizeForFilename(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        
        // Replace spaces and special characters with underscores
        String sanitized = input.replaceAll("[^a-zA-Z0-9_-]", "_")
                                .replaceAll("_+", "_")  // Replace multiple underscores with single
                                .replaceAll("^_|_$", ""); // Remove leading/trailing underscores
        
        // Limit length to 50 characters
        if (sanitized.length() > 50) {
            sanitized = sanitized.substring(0, 50);
        }
        
        return sanitized.toLowerCase();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}


