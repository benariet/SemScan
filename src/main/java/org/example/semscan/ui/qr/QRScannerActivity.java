package org.example.semscan.ui.qr;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.Result;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.QRPayload;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.QRUtils;
import org.example.semscan.utils.PreferencesManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRScannerActivity extends AppCompatActivity {
    
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    
    private DecoratedBarcodeView barcodeView;
    private Button btnFlashlight;
    private Button btnCancel;
    private Button btnManualRequest;
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    private boolean isFlashlightOn = false;
    private String currentSessionId = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        
        Logger.i(Logger.TAG_QR, "QRScannerActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            Logger.i(Logger.TAG_QR, "Requesting camera permission");
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    CAMERA_PERMISSION_REQUEST);
        } else {
            Logger.i(Logger.TAG_QR, "Camera permission already granted, starting scanner");
            startScanning();
        }
    }
    
    private void initializeViews() {
        barcodeView = findViewById(R.id.barcode_scanner);
        btnFlashlight = findViewById(R.id.btn_flashlight);
        btnCancel = findViewById(R.id.btn_cancel);
        btnManualRequest = findViewById(R.id.btn_manual_request);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupClickListeners() {
        btnFlashlight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleFlashlight();
            }
        });
        
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        
        btnManualRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showManualRequestDialog();
            }
        });
        
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                handleQRResult(result.getText());
            }
            
            @Override
            public void possibleResultPoints(java.util.List<com.google.zxing.ResultPoint> resultPoints) {
                // Optional: Handle possible result points
            }
        });
    }
    
    private void startScanning() {
        Logger.qr("Scanner Started", "QR scanner started");
        barcodeView.resume();
    }
    
    private void stopScanning() {
        Logger.qr("Scanner Stopped", "QR scanner stopped");
        barcodeView.pause();
    }
    
    private void toggleFlashlight() {
        if (isFlashlightOn) {
            barcodeView.setTorchOff();
            btnFlashlight.setText("Flashlight");
            isFlashlightOn = false;
            Logger.qr("Flashlight", "Flashlight turned off");
        } else {
            barcodeView.setTorchOn();
            btnFlashlight.setText("Flashlight Off");
            isFlashlightOn = true;
            Logger.qr("Flashlight", "Flashlight turned on");
        }
    }
    
    private void handleQRResult(String qrContent) {
        Logger.qr("QR Code Scanned", "Content: " + qrContent);
        stopScanning();
        
        // Parse QR content
        QRPayload payload = QRUtils.parseQRContent(qrContent);
        if (payload == null || !QRUtils.isValidQRContent(qrContent)) {
            Logger.qr("Invalid QR Code", "Failed to parse QR content: " + qrContent);
            showError("Invalid session code");
            return;
        }
        
        Logger.qr("QR Code Parsed", "Session ID: " + payload.getSessionId());
        
        // Store current session ID and show manual request button
        currentSessionId = payload.getSessionId();
        btnManualRequest.setVisibility(View.VISIBLE);
        
        // Submit attendance
        submitAttendance(payload.getSessionId());
    }
    
    private void submitAttendance(String sessionId) {
        String studentId = preferencesManager.getUserId();
        if (studentId == null) {
            Logger.e(Logger.TAG_QR, "Cannot submit attendance - no student ID");
            showError("Student ID not found. Please check settings.");
            return;
        }
        
        long timestampMs = System.currentTimeMillis();
        
        Logger.attendance("Submitting Attendance", "Session ID: " + sessionId + ", Student ID: " + studentId);
        Logger.api("POST", "api/v1/attendance", "Session ID: " + sessionId + ", Student ID: " + studentId);
        
        ApiService.SubmitAttendanceRequest request = new ApiService.SubmitAttendanceRequest(
                sessionId, studentId, timestampMs);
        
        Call<Attendance> call = apiService.submitAttendance(request);
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful()) {
                    Attendance attendance = response.body();
                    if (attendance != null) {
                        if (attendance.isAlreadyPresent()) {
                            Logger.attendance("Attendance Already Present", "Student: " + studentId + ", Session: " + sessionId);
                            Logger.apiResponse("POST", "api/v1/attendance", response.code(), "Already present");
                            showSuccess("Already checked in");
                        } else {
                            Logger.attendance("Attendance Submitted", "Student: " + studentId + ", Session: " + sessionId);
                            Logger.apiResponse("POST", "api/v1/attendance", response.code(), "Attendance submitted successfully");
                            showSuccess("Checked in for this session");
                        }
                    } else {
                        Logger.w(Logger.TAG_QR, "Invalid attendance response from server");
                        showError("Invalid response from server");
                    }
                } else {
                    Logger.apiError("POST", "api/v1/attendance", response.code(), "Failed to submit attendance");
                    handleErrorResponse(response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                Logger.e(Logger.TAG_QR, "Attendance submission failed", t);
                showError("Network error: " + t.getMessage());
            }
        });
    }
    
    private void handleErrorResponse(int responseCode) {
        switch (responseCode) {
            case 409:
                showError("This session is not accepting new check-ins");
                break;
            case 404:
                showError("Invalid session code");
                break;
            case 400:
                showError("Invalid session code");
                break;
            default:
                showError("Invalid session code");
                break;
        }
    }
    
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Return to student home after a delay
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finish();
            }
        }, 2000);
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        // Resume scanning after error
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startScanning();
            }
        }, 2000);
    }
    
    private void showManualRequestDialog() {
        if (currentSessionId == null) {
            Toast.makeText(this, "Please scan a valid QR code first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_manual_request, null);
        
        EditText editReason = dialogView.findViewById(R.id.edit_reason);
        Button btnCancelRequest = dialogView.findViewById(R.id.btn_cancel_request);
        Button btnSubmitRequest = dialogView.findViewById(R.id.btn_submit_request);
        
        AlertDialog dialog = builder.setView(dialogView).create();
        
        btnCancelRequest.setOnClickListener(v -> dialog.dismiss());
        btnSubmitRequest.setOnClickListener(v -> {
            String reason = editReason.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "Please provide a reason", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            submitManualRequest(reason);
        });
        
        dialog.show();
    }
    
    private void submitManualRequest(String reason) {
        String studentId = preferencesManager.getUserId();
        if (studentId == null) {
            Logger.e(Logger.TAG_QR, "Cannot submit manual request - no student ID");
            showError("Student ID not found. Please check settings.");
            return;
        }
        
        String deviceId = android.provider.Settings.Secure.getString(
            getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        
        Logger.attendance("Submitting Manual Request", "Session ID: " + currentSessionId + 
            ", Student ID: " + studentId + ", Reason: " + reason);
        Logger.api("POST", "api/v1/attendance/manual-request", 
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
                        Logger.apiResponse("POST", "api/v1/attendance/manual-request", 
                            response.code(), "Manual request submitted successfully");
                        showSuccess("Manual attendance request submitted. Please wait for approval.");
                    } else {
                        Logger.w(Logger.TAG_QR, "Invalid manual request response from server");
                        showError("Invalid response from server");
                    }
                } else {
                    Logger.apiError("POST", "api/v1/attendance/manual-request", 
                        response.code(), "Failed to submit manual request");
                    handleManualRequestError(response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                Logger.e(Logger.TAG_QR, "Manual request submission failed", t);
                showError("Network error: " + t.getMessage());
            }
        });
    }
    
    private void handleManualRequestError(int responseCode) {
        switch (responseCode) {
            case 409:
                showError("You already have a pending request for this session");
                break;
            case 404:
                showError("Session not found or not accepting requests");
                break;
            case 400:
                showError("Invalid request. Please try again.");
                break;
            default:
                showError("Failed to submit request. Please try again.");
                break;
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning();
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", 
                        Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED) {
            startScanning();
        }
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        stopScanning();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
