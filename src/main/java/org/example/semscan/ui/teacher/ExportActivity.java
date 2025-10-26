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
import org.example.semscan.data.model.Attendance;
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
            public void onApprove(Attendance request) {
                approveRequest(request);
            }
            
            @Override
            public void onReject(Attendance request) {
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
        
        Logger.api("GET", "api/v1/attendance/pending-requests", "Session ID: " + currentSessionId);
        
        Call<List<Attendance>> call = apiService.getPendingRequests(currentSessionId);
        call.enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Attendance> pendingRequests = response.body();
                    Logger.apiResponse("GET", "api/v1/attendance/pending-requests", 
                        response.code(), "Found " + pendingRequests.size() + " pending requests");
                    
                    // Debug logging for pending requests
                    Logger.d("ExportActivity", "=== PENDING REQUESTS DEBUG ===");
                    for (int i = 0; i < pendingRequests.size(); i++) {
                        Attendance req = pendingRequests.get(i);
                        Logger.d("ExportActivity", "Request " + i + ":");
                        Logger.d("ExportActivity", "  - Attendance ID: '" + req.getAttendanceId() + "'");
                        Logger.d("ExportActivity", "  - Session ID: '" + req.getSessionId() + "'");
                        Logger.d("ExportActivity", "  - Student ID: '" + req.getStudentId() + "'");
                        Logger.d("ExportActivity", "  - Request Status: '" + req.getRequestStatus() + "'");
                        Logger.d("ExportActivity", "  - Manual Reason: '" + req.getManualReason() + "'");
                        Logger.d("ExportActivity", "  - Full object: " + req.toString());
                    }
                    
                    if (pendingRequests.isEmpty()) {
                        // No pending requests, proceed with export
                        exportData();
                    } else {
                        // Show review modal
                        showReviewModal(pendingRequests);
                    }
                } else {
                    Logger.apiError("GET", "api/v1/attendance/pending-requests", 
                        response.code(), "Failed to get pending requests");
                    ToastUtils.showError(ExportActivity.this, "Failed to check pending requests");
                }
            }
            
            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to check pending requests", t);
                ToastUtils.showError(ExportActivity.this, "Network error: " + t.getMessage());
            }
        });
    }
    
    private void showReviewModal(List<Attendance> pendingRequests) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_review_requests, null);
        
        TextView textPendingCount = dialogView.findViewById(R.id.text_pending_count);
        RecyclerView recyclerRequests = dialogView.findViewById(R.id.recycler_requests);
        Button btnApproveAllSafe = dialogView.findViewById(R.id.btn_approve_all_safe);
        Button btnRejectAllDuplicates = dialogView.findViewById(R.id.btn_reject_all_duplicates);
        Button btnCancelReview = dialogView.findViewById(R.id.btn_cancel_review);
        Button btnContinueExport = dialogView.findViewById(R.id.btn_continue_export);
        
        // Set up recycler view
        recyclerRequests.setLayoutManager(new LinearLayoutManager(this));
        recyclerRequests.setAdapter(requestAdapter);
        requestAdapter.updateRequests(pendingRequests);
        
        // Update pending count
        textPendingCount.setText(pendingRequests.size() + " pending requests");
        
        AlertDialog dialog = builder.setView(dialogView).create();
        
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
        
        dialog.show();
    }
    
    private void approveRequest(Attendance request) {
        // API key no longer required - removed authentication
        
        // Debug logging to see what's in the request object
        Logger.d("ExportActivity", "=== ATTENDANCE REQUEST DEBUG ===");
        Logger.d("ExportActivity", "Attendance ID: '" + request.getAttendanceId() + "'");
        Logger.d("ExportActivity", "Session ID: '" + request.getSessionId() + "'");
        Logger.d("ExportActivity", "Student ID: '" + request.getStudentId() + "'");
        Logger.d("ExportActivity", "Request Status: '" + request.getRequestStatus() + "'");
        Logger.d("ExportActivity", "Manual Reason: '" + request.getManualReason() + "'");
        Logger.d("ExportActivity", "Attendance object: " + request.toString());
        
        // Check if attendanceId is null
        if (request.getAttendanceId() == null || request.getAttendanceId() <= 0) {
            Logger.e("ExportActivity", "Attendance ID is null or empty - cannot approve request");
            ToastUtils.showError(this, "Cannot approve request: Missing attendance ID");
            return;
        }
        
        Logger.userAction("Approve Request", "Approving manual request for student: " + request.getStudentId());
        Logger.api("POST", "api/v1/attendance/" + request.getAttendanceId() + "/approve", 
            "Attendance ID: " + request.getAttendanceId());
        
        Call<Attendance> call = apiService.approveManualRequest(request.getAttendanceId());
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful()) {
                    Logger.apiResponse("POST", "api/v1/attendance/" + request.getAttendanceId() + "/approve", 
                        response.code(), "Request approved successfully");
                    Toast.makeText(ExportActivity.this, "Request approved", Toast.LENGTH_SHORT).show();
                    // Refresh the list
                    checkPendingRequests();
                } else {
                    Logger.apiError("POST", "api/v1/attendance/" + request.getAttendanceId() + "/approve", 
                        response.code(), "Failed to approve request");
                    Toast.makeText(ExportActivity.this, "Failed to approve request", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to approve request", t);
                ToastUtils.showError(ExportActivity.this, "Network error: " + t.getMessage());
            }
        });
    }
    
    private void rejectRequest(Attendance request) {
        // API key no longer required - removed authentication
        
        Logger.userAction("Reject Request", "Rejecting manual request for student: " + request.getStudentId());
        Logger.api("POST", "api/v1/attendance/" + request.getAttendanceId() + "/reject", 
            "Attendance ID: " + request.getAttendanceId());
        
        Call<Attendance> call = apiService.rejectManualRequest(request.getAttendanceId());
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful()) {
                    Logger.apiResponse("POST", "api/v1/attendance/" + request.getAttendanceId() + "/reject", 
                        response.code(), "Request rejected successfully");
                    Toast.makeText(ExportActivity.this, "Request rejected", Toast.LENGTH_SHORT).show();
                    // Refresh the list
                    checkPendingRequests();
                } else {
                    Logger.apiError("POST", "api/v1/attendance/" + request.getAttendanceId() + "/reject", 
                        response.code(), "Failed to reject request");
                    Toast.makeText(ExportActivity.this, "Failed to reject request", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to reject request", t);
                ToastUtils.showError(ExportActivity.this, "Network error: " + t.getMessage());
            }
        });
    }
    
    private void approveAllSafe(List<Attendance> requests) {
        // TODO: Implement bulk approve logic based on auto_flags
        Toast.makeText(this, "Approve All Safe - Not implemented yet", Toast.LENGTH_SHORT).show();
    }
    
    private void rejectAllDuplicates(List<Attendance> requests) {
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
            filename = "attendance_" + sessionId + ".xlsx";
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            endpoint = "api/v1/export/xlsx";
        } else {
            call = apiService.exportCsv(sessionId);
            filename = "attendance_" + sessionId + ".csv";
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
                    Logger.apiError("GET", endpoint, response.code(), "Export request failed");
                    Toast.makeText(ExportActivity.this, "Export failed", Toast.LENGTH_SHORT).show();
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
        Logger.i(Logger.TAG_UI, "Sharing export file: " + file.getName() + " (" + mimeType + ")");
        
        try {
            // Use FileProvider for secure file sharing
            Uri fileUri = FileProvider.getUriForFile(this, 
                "org.example.semscan.fileprovider", file);
            
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Export");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "Attendance data export from SemScan");
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            startActivity(Intent.createChooser(shareIntent, "Share attendance data"));
            
        } catch (Exception e) {
            Logger.e(Logger.TAG_UI, "Failed to share file", e);
            Toast.makeText(this, "Failed to share file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}


