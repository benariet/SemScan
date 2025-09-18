package org.example.semscan.ui.qr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    private boolean isFlashlightOn = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance().getApiService();
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        
        // Check camera permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.CAMERA}, 
                    CAMERA_PERMISSION_REQUEST);
        } else {
            startScanning();
        }
    }
    
    private void initializeViews() {
        barcodeView = findViewById(R.id.barcode_scanner);
        btnFlashlight = findViewById(R.id.btn_flashlight);
        btnCancel = findViewById(R.id.btn_cancel);
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
        barcodeView.resume();
    }
    
    private void stopScanning() {
        barcodeView.pause();
    }
    
    private void toggleFlashlight() {
        if (isFlashlightOn) {
            barcodeView.setTorchOff();
            btnFlashlight.setText("Flashlight");
            isFlashlightOn = false;
        } else {
            barcodeView.setTorchOn();
            btnFlashlight.setText("Flashlight Off");
            isFlashlightOn = true;
        }
    }
    
    private void handleQRResult(String qrContent) {
        stopScanning();
        
        // Parse QR content
        QRPayload payload = QRUtils.parseQRContent(qrContent);
        if (payload == null || !QRUtils.isValidQRContent(qrContent)) {
            showError("Invalid QR code format");
            return;
        }
        
        // Submit attendance
        submitAttendance(payload.getSessionId());
    }
    
    private void submitAttendance(String sessionId) {
        String userId = preferencesManager.getUserId();
        if (userId == null) {
            showError("User ID not found. Please check settings.");
            return;
        }
        
        long timestamp = System.currentTimeMillis();
        
        ApiService.SubmitAttendanceRequest request = new ApiService.SubmitAttendanceRequest(
                sessionId, userId, timestamp);
        
        Call<Attendance> call = apiService.submitAttendance(request);
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful()) {
                    Attendance attendance = response.body();
                    if (attendance != null) {
                        if (attendance.isAlreadyPresent()) {
                            showSuccess("You are already marked present for this session");
                        } else {
                            showSuccess("Attendance confirmed successfully!");
                        }
                    } else {
                        showError("Invalid response from server");
                    }
                } else {
                    handleErrorResponse(response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                showError("Network error: " + t.getMessage());
            }
        });
    }
    
    private void handleErrorResponse(int responseCode) {
        switch (responseCode) {
            case 409:
                showError("Attendance window is closed or session is closed");
                break;
            case 404:
                showError("Session not found");
                break;
            case 400:
                showError("Invalid request");
                break;
            default:
                showError("Server error: " + responseCode);
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
