package org.example.semscan.ui.qr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.Session;
import org.example.semscan.ui.teacher.ExportActivity;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.utils.QRUtils;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class QRDisplayActivity extends AppCompatActivity {
    
    private TextView textSessionInfo;
    private ImageView imageQRCode;
    private TextView textStatus;
    private TextView textPresentCount;
    private Button btnEndSession;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    private Session currentSession;
    private Timer attendanceUpdateTimer;
    private int presentCount = 0;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_display);
        
        Logger.i(Logger.TAG_QR, "QRDisplayActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        
        // Get session from intent
        String sessionId = getIntent().getStringExtra("sessionId");
        String seminarId = getIntent().getStringExtra("seminarId");
        long startTime = getIntent().getLongExtra("startTime", 0);
        long endTime = getIntent().getLongExtra("endTime", 0);
        String status = getIntent().getStringExtra("status");
        
        Logger.qr("Session Data Received", "Session ID: " + sessionId + ", Seminar ID: " + seminarId + ", Status: " + status);
        
        if (sessionId == null || seminarId == null) {
            Logger.e(Logger.TAG_QR, "No session data provided in intent");
            Toast.makeText(this, "No session data", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        currentSession = new Session(sessionId, seminarId, startTime, endTime > 0 ? endTime : null, status);
        Logger.qr("Session Created", "Session object created for ID: " + sessionId);
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        generateAndDisplayQR();
        startAttendanceUpdates();
    }
    
    private void initializeViews() {
        textSessionInfo = findViewById(R.id.text_session_info);
        imageQRCode = findViewById(R.id.image_qr_code);
        textStatus = findViewById(R.id.text_status);
        textPresentCount = findViewById(R.id.text_present_count);
        btnEndSession = findViewById(R.id.btn_end_session);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupClickListeners() {
        btnEndSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEndSessionDialog();
            }
        });
    }
    
    private void generateAndDisplayQR() {
        // Generate QR content
        String qrContent = QRUtils.generateQRContent(currentSession.getSessionId());
        Logger.qr("QR Code Generated", "Content: " + qrContent);
        
        // Generate QR code bitmap
        try {
            Bitmap qrBitmap = generateQRCode(qrContent, 500, 500);
            imageQRCode.setImageBitmap(qrBitmap);
            
            // Update session info
            textSessionInfo.setText("Session: " + currentSession.getSessionId());
            
            Logger.qr("QR Display Updated", "QR code displayed for session: " + currentSession.getSessionId());
            
        } catch (Exception e) {
            Logger.e(Logger.TAG_QR, "Failed to generate QR code", e);
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    private void startAttendanceUpdates() {
        // Update attendance count every 5 seconds
        attendanceUpdateTimer = new Timer();
        attendanceUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                updateAttendanceCount();
            }
        }, 0, 5000); // Update every 5 seconds
    }
    
    private void updateAttendanceCount() {
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            Logger.w(Logger.TAG_QR, "Cannot update attendance count - no API key");
            return;
        }
        
        Logger.api("GET", "api/v1/attendance", "Session ID: " + currentSession.getSessionId());
        
        Call<List<Attendance>> call = apiService.getAttendance(apiKey, currentSession.getSessionId());
        call.enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    int newCount = response.body().size();
                    if (newCount != presentCount) {
                        Logger.attendance("Attendance Count Updated", "Session: " + currentSession.getSessionId() + ", Count: " + newCount);
                        presentCount = newCount;
                    }
                    
                    // Update UI on main thread
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            textPresentCount.setText(getString(R.string.present_count, presentCount));
                        }
                    });
                } else {
                    Logger.w(Logger.TAG_QR, "Failed to get attendance count - Response code: " + response.code());
                }
            }
            
            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Logger.e(Logger.TAG_QR, "Attendance count update failed", t);
            }
        });
    }
    
    private void openExport() {
        Intent intent = new Intent(this, ExportActivity.class);
        intent.putExtra("sessionId", currentSession.getSessionId());
        startActivity(intent);
        finish();
    }
    
    private void showEndSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("End Session")
                .setMessage("Are you sure you want to end this session? Students will no longer be able to scan the QR code.")
                .setPositiveButton("End Session", new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        endSession();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void endSession() {
        Logger.userAction("End Session", "Presenter clicked end session for: " + currentSession.getSessionId());
        
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            Logger.e(Logger.TAG_QR, "Cannot end session - no API key");
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Logger.api("PATCH", "api/v1/sessions/" + currentSession.getSessionId() + "/close", null);
        
        Call<Session> call = apiService.closeSession(apiKey, currentSession.getSessionId());
        call.enqueue(new Callback<Session>() {
            @Override
            public void onResponse(Call<Session> call, Response<Session> response) {
                if (response.isSuccessful()) {
                    Logger.session("Session Ended", "Session ID: " + currentSession.getSessionId());
                    Logger.apiResponse("PATCH", "api/v1/sessions/" + currentSession.getSessionId() + "/close", response.code(), "Session closed successfully");
                    
                    Toast.makeText(QRDisplayActivity.this, "Session ended successfully", Toast.LENGTH_SHORT).show();
                    // Navigate to export after session ends
                    openExport();
                } else {
                    Logger.apiError("PATCH", "api/v1/sessions/" + currentSession.getSessionId() + "/close", response.code(), "Failed to close session");
                    Toast.makeText(QRDisplayActivity.this, "Failed to end session", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Session> call, Throwable t) {
                Logger.e(Logger.TAG_QR, "Failed to end session", t);
                Toast.makeText(QRDisplayActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (attendanceUpdateTimer != null) {
            attendanceUpdateTimer.cancel();
        }
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        showEndSessionDialog();
        return true;
    }
    
    @Override
    public void onBackPressed() {
        showEndSessionDialog();
    }
    
    private Bitmap generateQRCode(String content, int width, int height) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height);
        
        int w = bitMatrix.getWidth();
        int h = bitMatrix.getHeight();
        int[] pixels = new int[w * h];
        
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF;
            }
        }
        
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }
}
