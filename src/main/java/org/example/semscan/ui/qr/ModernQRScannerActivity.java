package org.example.semscan.ui.qr;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.example.semscan.R;
import org.example.semscan.constants.ApiConstants;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.QRPayload;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.QRUtils;
import org.example.semscan.utils.ToastUtils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ModernQRScannerActivity extends AppCompatActivity {
    
    private static final int CAMERA_PERMISSION_REQUEST = 1001;
    private static final String TAG = "ModernQRScanner";
    
    // UI Components
    private PreviewView previewView;
    private View overlayView;
    private ImageView scanFrame;
    private TextView instructionText;
    private TextView statusText;
    private ImageView backButton;
    private ImageView flashButton;
    
    // Camera and ML Kit
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private BarcodeScanner barcodeScanner;
    private ExecutorService cameraExecutor;
    
    // App Components
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    // State
    private boolean isScanning = true;
    private boolean isFlashOn = false;
    private String currentSessionId = null;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modern_qr_scanner);
        
        initializeViews();
        setupClickListeners();
        initializeComponents();
        
        if (checkCameraPermission()) {
            startCamera();
        } else {
            requestCameraPermission();
        }
    }
    
    private void initializeViews() {
        previewView = findViewById(R.id.preview_view);
        overlayView = findViewById(R.id.overlay_view);
        scanFrame = findViewById(R.id.scan_frame);
        instructionText = findViewById(R.id.instruction_text);
        statusText = findViewById(R.id.status_text);
        backButton = findViewById(R.id.back_button);
        flashButton = findViewById(R.id.flash_button);
        
        // Set initial status
        statusText.setText("Ready to scan");
        statusText.setTextColor(ContextCompat.getColor(this, R.color.success_green));
    }
    
    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        flashButton.setOnClickListener(v -> toggleFlash());
        
        // Add smooth animations
        backButton.setOnTouchListener((v, event) -> {
            animateButtonPress(v);
            return false;
        });
        
        flashButton.setOnTouchListener((v, event) -> {
            animateButtonPress(v);
            return false;
        });
    }
    
    private void initializeComponents() {
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        cameraExecutor = Executors.newSingleThreadExecutor();
        
        // Initialize ML Kit Barcode Scanner
        BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build();
        barcodeScanner = BarcodeScanning.getClient(options);
    }
    
    private boolean checkCameraPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) 
                == PackageManager.PERMISSION_GRANTED;
    }
    
    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, 
                new String[]{Manifest.permission.CAMERA}, 
                CAMERA_PERMISSION_REQUEST);
    }
    
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = 
                ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                Logger.e(TAG, "Camera initialization failed", e);
                showError("Camera initialization failed");
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void bindCameraUseCases() {
        if (cameraProvider == null) return;
        
        // Preview use case
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());
        
        // Image analysis use case for QR code detection
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        
        imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);
        
        // Camera selector
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll();
            
            // Bind use cases to camera
            camera = cameraProvider.bindToLifecycle(
                    (LifecycleOwner) this,
                    cameraSelector,
                    preview,
                    imageAnalysis
            );
            
            Logger.i(TAG, "Camera started successfully");
            updateStatus("Camera ready", R.color.success_green);
            
        } catch (Exception e) {
            Logger.e(TAG, "Camera binding failed", e);
            showError("Camera binding failed");
        }
    }
    
    private void analyzeImage(ImageProxy image) {
        if (!isScanning) {
            image.close();
            return;
        }
        
        InputImage inputImage = InputImage.fromMediaImage(
                image.getImage(), 
                image.getImageInfo().getRotationDegrees()
        );
        
        barcodeScanner.process(inputImage)
                .addOnSuccessListener(barcodes -> {
                    if (!barcodes.isEmpty() && isScanning) {
                        Barcode barcode = barcodes.get(0);
                        String qrContent = barcode.getRawValue();
                        
                        if (qrContent != null) {
                            runOnUiThread(() -> handleQRResult(qrContent));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Logger.e(TAG, "Barcode scanning failed", e);
                })
                .addOnCompleteListener(task -> image.close());
    }
    
    private void handleQRResult(String qrContent) {
        if (!isScanning) return;
        
        isScanning = false;
        Logger.qr("QR Code Scanned", "Content: " + qrContent);
        
        // Parse QR content
        QRPayload payload = QRUtils.parseQRContent(qrContent);
        if (payload == null || !QRUtils.isValidQRContent(qrContent)) {
            Logger.qr("Invalid QR Code", "Failed to parse QR content: " + qrContent);
            updateStatus("Invalid QR code format", R.color.error_red);
            showError("Invalid QR code format. Expected: {\"sessionId\":\"session-xxx\"}");
            resumeScanning();
            return;
        }
        
        String sessionId = payload.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            Logger.qr("Invalid QR Code", "Session ID is null or empty");
            updateStatus("QR code missing session ID", R.color.error_red);
            showError("QR code missing session ID");
            resumeScanning();
            return;
        }
        
        Logger.qr("QR Code Parsed", "Session ID: " + sessionId);
        currentSessionId = sessionId;
        
        updateStatus("Processing...", R.color.warning_orange);
        animateScanFrame();
        
        // Submit attendance
        submitAttendance(sessionId);
    }
    
    private void submitAttendance(String sessionId) {
        String studentId = preferencesManager.getUserId();
        if (studentId == null) {
            studentId = "student-001";
            Logger.w(TAG, "No student ID found, using default: " + studentId);
        }
        
        final String finalStudentId = studentId;
        Logger.d(TAG, "Submitting attendance - Session: " + sessionId + ", Student: " + finalStudentId);
        
        // Debug: Log the API base URL being used
        String apiBaseUrl = ApiClient.getInstance(this).getCurrentBaseUrl();
        Logger.d(TAG, "API Base URL: " + apiBaseUrl);
        
        ApiService.SubmitAttendanceRequest request = new ApiService.SubmitAttendanceRequest(
            sessionId, finalStudentId, System.currentTimeMillis()
        );
        
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            apiKey = ApiConstants.PRESENTER_API_KEY;
            Logger.w(TAG, "No API key found, using default");
        }
        
        Call<Attendance> call = apiService.submitAttendance(apiKey, request);
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful()) {
                    Attendance result = response.body();
                    if (result != null) {
                        Logger.apiResponse("POST", "api/v1/attendance", response.code(), "Attendance submitted successfully");
                        updateStatus("Success!", R.color.success_green);
                        showSuccess("Attendance recorded successfully!");
                        Logger.attendance("Attendance Success", "Session: " + sessionId + ", Student: " + finalStudentId);
                        
                        // Return to previous screen after delay
                        new android.os.Handler().postDelayed(() -> finish(), 2000);
                    } else {
                        updateStatus("Invalid response", R.color.error_red);
                        showError("Invalid response from server");
                        resumeScanning();
                    }
                } else {
                    Logger.apiError("POST", "api/v1/attendance", response.code(), "Failed to submit attendance");
                    updateStatus("Request failed", R.color.error_red);
                    handleAttendanceError(response.code());
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                Logger.e(TAG, "Attendance submission failed", t);
                updateStatus("Network error", R.color.error_red);
                showError("Network error: " + t.getMessage());
                resumeScanning();
            }
        });
    }
    
    private void handleAttendanceError(int responseCode) {
        switch (responseCode) {
            case 409:
                showError("You have already marked attendance for this session");
                break;
            case 404:
                showError("Session not found or not active");
                break;
            case 400:
                showError("Invalid request - check session and student ID");
                break;
            case 401:
                showError("Authentication failed - check API key");
                break;
            case 403:
                showError("Access denied - insufficient permissions");
                break;
            default:
                showError("Server error (Code: " + responseCode + ")");
                break;
        }
        resumeScanning();
    }
    
    private void resumeScanning() {
        new android.os.Handler().postDelayed(() -> {
            isScanning = true;
            updateStatus("Ready to scan", R.color.success_green);
        }, 2000);
    }
    
    private void toggleFlash() {
        if (camera == null) return;
        
        if (isFlashOn) {
            camera.getCameraControl().enableTorch(false);
            isFlashOn = false;
            flashButton.setImageResource(R.drawable.ic_flash_off);
            Logger.qr("Flashlight", "Flashlight turned off");
        } else {
            camera.getCameraControl().enableTorch(true);
            isFlashOn = true;
            flashButton.setImageResource(R.drawable.ic_flash_on);
            Logger.qr("Flashlight", "Flashlight turned on");
        }
        
        animateButtonPress(flashButton);
    }
    
    private void updateStatus(String message, int colorRes) {
        statusText.setText(message);
        statusText.setTextColor(ContextCompat.getColor(this, colorRes));
        
        // Add fade animation
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(300);
        statusText.startAnimation(fadeIn);
    }
    
    private void animateScanFrame() {
        AlphaAnimation pulse = new AlphaAnimation(1.0f, 0.3f);
        pulse.setDuration(500);
        pulse.setRepeatCount(Animation.INFINITE);
        pulse.setRepeatMode(Animation.REVERSE);
        scanFrame.startAnimation(pulse);
    }
    
    private void animateButtonPress(View view) {
        view.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> {
                    view.animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(100)
                            .start();
                })
                .start();
    }
    
    private void showSuccess(String message) {
        ToastUtils.showSuccess(this, message);
    }
    
    private void showError(String message) {
        ToastUtils.showError(this, message);
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, 
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                showError("Camera permission is required to scan QR codes");
                finish();
            }
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}


