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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.Build;
import android.text.TextUtils;

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
import org.example.semscan.utils.ServerLogger;
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
    private ServerLogger serverLogger;
    
    // State
    private boolean isScanning = true;
    private boolean isFlashOn = false;
    private Long currentSessionId = null;
    
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
        backButton.setOnClickListener(v -> {
            Logger.userAction("QR Back", "Student tapped back from QR scanner");
            if (serverLogger != null) {
                serverLogger.userAction("QR Back", "Student tapped back from QR scanner");
                serverLogger.flushLogs();
            }
            finish();
        });
        
        flashButton.setOnClickListener(v -> {
            toggleFlash();
            if (serverLogger != null) {
                serverLogger.userAction("Toggle Flash", "Student toggled flashlight");
            }
        });
        
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
        serverLogger = ServerLogger.getInstance(this);
        Long userId = preferencesManager.getUserId();
        String userRole = preferencesManager.getUserRole();
        serverLogger.updateUserContext(userId, userRole);
        serverLogger.userAction("Open QR Scanner", "ModernQRScannerActivity opened");
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
        if (serverLogger != null) {
            serverLogger.qr("QR Code Scanned", "Content: " + qrContent);
        }
        
        // Parse QR content
        QRPayload payload = QRUtils.parseQRContent(qrContent);
        if (payload == null || !QRUtils.isValidQRContent(qrContent)) {
            Logger.qr("Invalid QR Code", "Failed to parse QR content: " + qrContent);
            if (serverLogger != null) {
                serverLogger.qr("Invalid QR Code", "Failed to parse QR content: " + qrContent);
            }
            updateStatus("Invalid QR code format", R.color.error_red);
            showError("Invalid QR code format. Expected: {\"sessionId\":\"session-xxx\"}");
            resumeScanning();
            return;
        }
        
        Long sessionId = payload.getSessionId();
        if (sessionId == null || sessionId <= 0) {
            Logger.qr("Invalid QR Code", "Session ID is null or empty");
            if (serverLogger != null) {
                serverLogger.qr("Invalid QR Code", "Session ID is null or empty");
            }
            updateStatus("QR code missing session ID", R.color.error_red);
            showError("QR code missing session ID");
            resumeScanning();
            return;
        }
        
        Logger.qr("QR Code Parsed", "Session ID: " + sessionId);
        if (serverLogger != null) {
            serverLogger.qr("QR Code Parsed", "Session ID: " + sessionId);
        }
        currentSessionId = sessionId;
        
        updateStatus("Processing...", R.color.warning_orange);
        animateScanFrame();
        
        // Submit attendance
        submitAttendance(sessionId);
    }
    
    private void submitAttendance(Long sessionId) {
        String studentUsername = preferencesManager.getUserName();
        if (TextUtils.isEmpty(studentUsername)) {
            Logger.e(TAG, "Student username not found or invalid");
            if (serverLogger != null) {
                serverLogger.e(ServerLogger.TAG_QR, "Student username not found or invalid");
            }
            showError("Student username not found. Please log in again.");
            return;
        }

        Logger.d(TAG, "Submitting attendance - Session: " + sessionId + ", Student: " + studentUsername);
        if (serverLogger != null) {
            serverLogger.attendance("Submit Attendance", "Session: " + sessionId + ", Student: " + studentUsername);
        }
        
        // Debug: Log the API base URL being used
        String apiBaseUrl = ApiClient.getInstance(this).getCurrentBaseUrl();
        Logger.d(TAG, "API Base URL: " + apiBaseUrl);
        if (serverLogger != null) {
            serverLogger.api("POST", "api/v1/attendance", "Base URL: " + apiBaseUrl + ", Session: " + sessionId + ", Student: " + studentUsername);
        }
        
        ApiService.SubmitAttendanceRequest request = new ApiService.SubmitAttendanceRequest(
            sessionId, studentUsername, System.currentTimeMillis()
        );
        
        // API key no longer required - removed authentication
        
        Call<Attendance> call = apiService.submitAttendance(request);
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful()) {
                    Attendance result = response.body();
                    if (result != null) {
                        Logger.apiResponse("POST", "api/v1/attendance", response.code(), "Attendance submitted successfully");
                        if (serverLogger != null) {
                            serverLogger.apiResponse("POST", "api/v1/attendance", response.code(), "Attendance submitted successfully");
                            serverLogger.attendance("Attendance Success", "Session: " + sessionId + ", Username: " + studentUsername);
                            serverLogger.flushLogs();
                        }
                        vibrateSuccess();
                        updateStatus("Success!", R.color.success_green);
                        showSuccess("Attendance recorded successfully!");
                        Logger.attendance("Attendance Success", "Session: " + sessionId + ", Username: " + studentUsername);
                        
                        // Return to previous screen after delay
                        new android.os.Handler().postDelayed(() -> finish(), 2000);
                    } else {
                        updateStatus("Invalid response", R.color.error_red);
                        showError("Invalid response from server");
                        resumeScanning();
                    }
                } else {
                    Logger.apiError("POST", "api/v1/attendance", response.code(), "Failed to submit attendance");
                    if (serverLogger != null) {
                        serverLogger.apiError("POST", "api/v1/attendance", response.code(), "Failed to submit attendance for student " + studentUsername);
                    }
                    updateStatus("Request failed", R.color.error_red);
                    
                    // Parse error message from response body
                    String errorMessage = "Unknown error occurred";
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Logger.e(TAG, "Error response body: " + errorBody);
                            
                            // Try to extract the actual error message from the response
                            if (errorBody.contains("Student already attended this session")) {
                                errorMessage = "You have already attended this session";
                            } else if (errorBody.contains("Session not found")) {
                                errorMessage = "Session not found or not active";
                            } else if (errorBody.contains("Invalid session")) {
                                errorMessage = "Invalid session ID";
                            } else if (errorBody.contains("Server error")) {
                                errorMessage = "Server error - please try again";
                            } else if (errorBody.contains("Access denied")) {
                                errorMessage = "Access denied - insufficient permissions";
                            } else {
                                // Try to extract a more specific error message
                                if (errorBody.contains("message")) {
                                    // Look for JSON message field
                                    int messageStart = errorBody.indexOf("\"message\":\"") + 10;
                                    int messageEnd = errorBody.indexOf("\"", messageStart);
                                    if (messageStart > 9 && messageEnd > messageStart) {
                                        errorMessage = errorBody.substring(messageStart, messageEnd);
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        Logger.e(TAG, "Failed to read error response body", e);
                        // Fall back to generic error handling
                        handleAttendanceError(response.code());
                        return;
                    }
                    
                    // Show the specific error message in a dialog
                    showErrorDialog(errorMessage);
                    if (serverLogger != null) {
                        serverLogger.attendance("Attendance Failed", "Session: " + sessionId + ", Student: " + studentUsername + ", Reason: " + errorMessage);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                Logger.e(TAG, "Attendance submission failed", t);
                if (serverLogger != null) {
                    serverLogger.e(ServerLogger.TAG_QR, "Attendance submission failed", t);
                }
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
                showError("Network error - please check your connection");
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
    
    private void showErrorDialog(String message) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("⚠️ Error")
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> {
                    dialog.dismiss();
                    // Resume scanning after user acknowledges the error
                    resumeScanning();
                })
                .setCancelable(false) // User must press OK to dismiss
                .show();
    }

    private void vibrateSuccess() {
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            VibrationEffect effect = VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE);
            vibrator.vibrate(effect);
        } else {
            vibrator.vibrate(80);
        }
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


