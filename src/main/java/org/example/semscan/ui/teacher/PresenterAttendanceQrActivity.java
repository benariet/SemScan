package org.example.semscan.ui.teacher;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.QRUtils;
import org.example.semscan.utils.ServerLogger;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresenterAttendanceQrActivity extends AppCompatActivity {

    public static final String EXTRA_QR_URL = "presenter_attendance_qr_url";
    public static final String EXTRA_QR_PAYLOAD = "presenter_attendance_qr_payload";
    public static final String EXTRA_OPENED_AT = "presenter_attendance_opened_at";
    public static final String EXTRA_CLOSES_AT = "presenter_attendance_closes_at";
    public static final String EXTRA_SLOT_TITLE = "presenter_attendance_slot_title";
    public static final String EXTRA_SLOT_ID = "presenter_attendance_slot_id";
    public static final String EXTRA_USERNAME = "presenter_attendance_username";
    public static final String EXTRA_SESSION_ID = "presenter_attendance_session_id";

    private static final long AUTO_CLOSE_CHECK_INTERVAL_MS = 30000L; // Check every 30 seconds
    private static final long AUTO_CLOSE_DURATION_MS = 15 * 60 * 1000L; // 15 minutes in milliseconds

    private ImageView imageQr;
    private TextView textSlotTitle;
    private TextView textOpenedAt;
    private TextView textValidUntil;
    private ProgressBar progressRefresh;
    private com.google.android.material.button.MaterialButton btnEndSession;
    private com.google.android.material.button.MaterialButton btnCancelSession;

    private ApiService apiService;
    private ServerLogger serverLogger;
    private Handler autoCloseHandler;
    private Runnable autoCloseRunnable;

    private Long slotId;
    private Long sessionId;
    private String username;
    private String lastPayload;
    
    // Slot details for export filename
    private String slotDate;
    private String slotTimeRange;
    private String presenterName;
    
    // Auto-close tracking
    private long sessionOpenedAtMs; // Timestamp when session was opened (in milliseconds)
    private boolean isAutoClosing = false; // Flag to prevent multiple auto-close attempts

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_attendance_qr);

        apiService = ApiClient.getInstance(this).getApiService();
        serverLogger = ServerLogger.getInstance(this);
        autoCloseHandler = new Handler(Looper.getMainLooper());

        setupToolbar();
        initializeViews();
        readExtras();
        startAutoCloseTimer();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeViews() {
        imageQr = findViewById(R.id.image_qr_code);
        textSlotTitle = findViewById(R.id.text_slot_title);
        textOpenedAt = findViewById(R.id.text_opened_at);
        textValidUntil = findViewById(R.id.text_valid_until);
        progressRefresh = findViewById(R.id.progress_refresh);
        btnEndSession = findViewById(R.id.btn_end_session);
        btnCancelSession = findViewById(R.id.btn_cancel_session);
        
        btnEndSession.setOnClickListener(v -> showEndSessionDialog());
        btnCancelSession.setOnClickListener(v -> showCancelSessionDialog());
    }

    private void readExtras() {
        Intent intent = getIntent();
        String qrUrl = intent.getStringExtra(EXTRA_QR_URL);
        String qrPayload = intent.getStringExtra(EXTRA_QR_PAYLOAD);
        String openedAt = intent.getStringExtra(EXTRA_OPENED_AT);
        String closesAt = intent.getStringExtra(EXTRA_CLOSES_AT);
        String slotTitle = intent.getStringExtra(EXTRA_SLOT_TITLE);
        slotId = (Long) intent.getSerializableExtra(EXTRA_SLOT_ID);
        sessionId = (Long) intent.getSerializableExtra(EXTRA_SESSION_ID);
        username = intent.getStringExtra(EXTRA_USERNAME);

        if (!TextUtils.isEmpty(slotTitle)) {
            textSlotTitle.setText(slotTitle);
            textSlotTitle.setVisibility(View.VISIBLE);
        }

        if (!TextUtils.isEmpty(openedAt)) {
            textOpenedAt.setText(getString(R.string.presenter_attendance_qr_opened_at, openedAt));
            // Parse openedAt timestamp to calculate auto-close time
            sessionOpenedAtMs = parseTimestamp(openedAt);
            if (sessionOpenedAtMs <= 0) {
                // If parsing fails, use current time as fallback
                sessionOpenedAtMs = System.currentTimeMillis();
            }
        } else {
            // If no openedAt provided, use current time
            sessionOpenedAtMs = System.currentTimeMillis();
        }
        if (!TextUtils.isEmpty(closesAt)) {
            textValidUntil.setText(getString(R.string.presenter_attendance_qr_valid_until, closesAt));
        }

        String normalizedContent = normalizeQrContent(!TextUtils.isEmpty(qrPayload) ? qrPayload : qrUrl);
        if (normalizedContent != null) {
            lastPayload = normalizedContent;
            generateQr(normalizedContent);
        }
    }
    
    /**
     * Parse timestamp string to milliseconds.
     * Supports formats like "2025-11-09 14:30:00" or ISO 8601 format.
     */
    private long parseTimestamp(String timestamp) {
        if (TextUtils.isEmpty(timestamp)) {
            return 0;
        }
        try {
            // Try ISO 8601 format first (e.g., "2025-11-09T14:30:00Z" or "2025-11-09T14:30:00+00:00")
            java.text.SimpleDateFormat isoFormat = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US);
            try {
                return isoFormat.parse(timestamp.replace("Z", "").replaceAll("\\+\\d{2}:\\d{2}$", "")).getTime();
            } catch (Exception e) {
                // Try without timezone
            }
            
            // Try standard format (e.g., "2025-11-09 14:30:00")
            java.text.SimpleDateFormat standardFormat = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US);
            return standardFormat.parse(timestamp).getTime();
        } catch (Exception e) {
            Logger.w(Logger.TAG_UI, "Failed to parse timestamp: " + timestamp + " - " + e.getMessage());
            return 0;
        }
    }


    private String normalizeQrContent(@Nullable String rawContent) {
        if (TextUtils.isEmpty(rawContent)) {
            return null;
        }
        if (QRUtils.isValidQRContent(rawContent)) {
            return rawContent;
        }
        try {
            Uri uri = Uri.parse(rawContent);
            String sessionQuery = uri.getQueryParameter("sessionId");
            Long parsed = parseSessionId(sessionQuery);
            if (parsed == null) {
                String lastSegment = uri.getLastPathSegment();
                parsed = parseSessionId(lastSegment);
            }
            if (parsed != null) {
                sessionId = parsed;
                return QRUtils.generateQRContent(parsed);
            }
        } catch (Exception ignored) {
        }
        if (sessionId != null) {
            return QRUtils.generateQRContent(sessionId);
        }
        return null;
    }

    private Long parseSessionId(@Nullable String value) {
        if (TextUtils.isEmpty(value)) {
            return null;
        }
        try {
            return Long.parseLong(value.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void generateQr(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            Bitmap bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.RGB_565);
            for (int x = 0; x < 512; x++) {
                for (int y = 0; y < 512; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            imageQr.setImageBitmap(bitmap);
        } catch (WriterException e) {
            Logger.e(Logger.TAG_UI, "Failed to generate session QR", e);
            Toast.makeText(this, R.string.presenter_start_session_error_load, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Start the auto-close timer that checks if 15 minutes have passed
     */
    private void startAutoCloseTimer() {
        if (sessionOpenedAtMs <= 0) {
            // If we don't have a valid open time, use current time
            sessionOpenedAtMs = System.currentTimeMillis();
        }
        
        autoCloseRunnable = new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                long elapsedTime = currentTime - sessionOpenedAtMs;
                
                if (elapsedTime >= AUTO_CLOSE_DURATION_MS && !isAutoClosing) {
                    // 15 minutes have passed - auto-close the session
                    Logger.i(Logger.TAG_UI, "Auto-closing session after 15 minutes. Elapsed: " + (elapsedTime / 1000) + " seconds");
                    isAutoClosing = true;
                    
                    // Show a toast notification
                    runOnUiThread(() -> {
                        Toast.makeText(PresenterAttendanceQrActivity.this, 
                            getString(R.string.presenter_attendance_qr_auto_close_message), 
                            Toast.LENGTH_LONG).show();
                    });
                    
                    // Auto-close the session
                    endSession();
                } else if (elapsedTime < AUTO_CLOSE_DURATION_MS) {
                    // Schedule next check
                    autoCloseHandler.postDelayed(this, AUTO_CLOSE_CHECK_INTERVAL_MS);
                }
            }
        };
        
        // Start checking after a short delay
        autoCloseHandler.postDelayed(autoCloseRunnable, AUTO_CLOSE_CHECK_INTERVAL_MS);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoCloseHandler != null && autoCloseRunnable != null) {
            autoCloseHandler.removeCallbacks(autoCloseRunnable);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showEndSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.presenter_attendance_qr_end_confirm_title)
                .setMessage(R.string.presenter_attendance_qr_end_confirm_message)
                .setPositiveButton(R.string.presenter_attendance_qr_end_session, (dialog, which) -> endSession())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showCancelSessionDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.presenter_attendance_qr_cancel_confirm_title)
                .setMessage(R.string.presenter_attendance_qr_cancel_confirm_message)
                .setPositiveButton(R.string.presenter_attendance_qr_cancel_session, (dialog, which) -> cancelSession())
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
    
    /**
     * Cancel session - closes the session and returns to home (does not navigate to export)
     * This is different from endSession() which navigates to export page
     */
    private void cancelSession() {
        if (sessionId == null) {
            Toast.makeText(this, R.string.presenter_attendance_qr_end_error, Toast.LENGTH_SHORT).show();
            finish(); // Still finish even if no session ID
            return;
        }

        btnEndSession.setEnabled(false);
        btnCancelSession.setEnabled(false);
        progressRefresh.setVisibility(View.VISIBLE);

        apiService.closeSession(sessionId).enqueue(new Callback<org.example.semscan.data.model.Session>() {
            @Override
            public void onResponse(Call<org.example.semscan.data.model.Session> call, Response<org.example.semscan.data.model.Session> response) {
                progressRefresh.setVisibility(View.GONE);
                btnEndSession.setEnabled(true);
                btnCancelSession.setEnabled(true);
                
                if (response.isSuccessful()) {
                    Toast.makeText(PresenterAttendanceQrActivity.this, R.string.presenter_attendance_qr_end_success, Toast.LENGTH_SHORT).show();
                    Logger.userAction("Cancel Session", "Session " + sessionId + " cancelled successfully");
                    // For cancel, just go back to home - don't navigate to export
                    finish();
                } else {
                    Toast.makeText(PresenterAttendanceQrActivity.this, R.string.presenter_attendance_qr_end_error, Toast.LENGTH_SHORT).show();
                    Logger.apiError("PATCH", "/api/v1/sessions/" + sessionId + "/close", response.code(), "Failed to cancel session");
                    // Even on error, allow user to go back
                    finish();
                }
            }

            @Override
            public void onFailure(Call<org.example.semscan.data.model.Session> call, Throwable t) {
                progressRefresh.setVisibility(View.GONE);
                btnEndSession.setEnabled(true);
                btnCancelSession.setEnabled(true);
                Toast.makeText(PresenterAttendanceQrActivity.this, R.string.presenter_attendance_qr_end_error, Toast.LENGTH_SHORT).show();
                Logger.e(Logger.TAG_API, "Failed to cancel session", t);
                // Even on failure, allow user to go back
                finish();
            }
        });
    }

    private void endSession() {
        if (sessionId == null) {
            Toast.makeText(this, R.string.presenter_attendance_qr_end_error, Toast.LENGTH_SHORT).show();
            return;
        }

        btnEndSession.setEnabled(false);
        btnCancelSession.setEnabled(false);
        progressRefresh.setVisibility(View.VISIBLE);

        apiService.closeSession(sessionId).enqueue(new Callback<org.example.semscan.data.model.Session>() {
            @Override
            public void onResponse(Call<org.example.semscan.data.model.Session> call, Response<org.example.semscan.data.model.Session> response) {
                progressRefresh.setVisibility(View.GONE);
                btnEndSession.setEnabled(true);
                btnCancelSession.setEnabled(true);
                
                if (response.isSuccessful()) {
                    Toast.makeText(PresenterAttendanceQrActivity.this, R.string.presenter_attendance_qr_end_success, Toast.LENGTH_SHORT).show();
                    Logger.userAction("End Session", "Session " + sessionId + " ended successfully");
                    
                    // Fetch slot details for export filename before navigating
                    fetchSlotDetailsForExport();
                } else {
                    Toast.makeText(PresenterAttendanceQrActivity.this, R.string.presenter_attendance_qr_end_error, Toast.LENGTH_SHORT).show();
                    Logger.apiError("PATCH", "/api/v1/sessions/" + sessionId + "/close", response.code(), "Failed to end session");
                }
            }

            @Override
            public void onFailure(Call<org.example.semscan.data.model.Session> call, Throwable t) {
                progressRefresh.setVisibility(View.GONE);
                btnEndSession.setEnabled(true);
                btnCancelSession.setEnabled(true);
                Toast.makeText(PresenterAttendanceQrActivity.this, R.string.presenter_attendance_qr_end_error, Toast.LENGTH_SHORT).show();
                Logger.e(Logger.TAG_API, "Failed to end session", t);
            }
        });
    }
    
    private void fetchSlotDetailsForExport() {
        if (slotId == null || TextUtils.isEmpty(username)) {
            // If we don't have slot details, navigate without them
            navigateToExport();
            return;
        }
        
        // Fetch presenter home to get slot details
        apiService.getPresenterHome(username.trim().toLowerCase(Locale.US))
                .enqueue(new Callback<ApiService.PresenterHomeResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.PresenterHomeResponse> call, Response<ApiService.PresenterHomeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiService.PresenterHomeResponse body = response.body();
                            
                            // Get slot details from mySlot
                            if (body.mySlot != null) {
                                slotDate = body.mySlot.date;
                                slotTimeRange = body.mySlot.timeRange;
                                
                                // Get presenter name from presenter summary
                                if (body.presenter != null) {
                                    presenterName = body.presenter.name;
                                }
                            }
                            
                            // Also check slot catalog for this slot
                            if (body.slotCatalog != null) {
                                for (ApiService.SlotCard slot : body.slotCatalog) {
                                    if (slot.slotId != null && slot.slotId.equals(slotId)) {
                                        if (slotDate == null) slotDate = slot.date;
                                        if (slotTimeRange == null) slotTimeRange = slot.timeRange;
                                        
                                        // Get presenter name from registered presenters
                                        if (slot.registered != null && !slot.registered.isEmpty()) {
                                            // Use the first registered presenter's name
                                            ApiService.PresenterCoPresenter firstPresenter = slot.registered.get(0);
                                            if (firstPresenter != null && firstPresenter.name != null) {
                                                presenterName = firstPresenter.name;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        
                        // Navigate to export with slot details
                        navigateToExport();
                    }
                    
                    @Override
                    public void onFailure(Call<ApiService.PresenterHomeResponse> call, Throwable t) {
                        Logger.e(Logger.TAG_API, "Failed to fetch slot details for export", t);
                        // Navigate anyway without slot details
                        navigateToExport();
                    }
                });
    }
    
    private void navigateToExport() {
        Intent intent = new Intent(PresenterAttendanceQrActivity.this, org.example.semscan.ui.teacher.ExportActivity.class);
        intent.putExtra("sessionId", sessionId);
        
        // Pass slot details for filename generation
        if (slotDate != null) {
            intent.putExtra("sessionDate", slotDate);
        }
        if (slotTimeRange != null && slotDate != null) {
            intent.putExtra("sessionTimeSlot", slotDate + " " + slotTimeRange); // Format: "2025-11-09 14:00-15:00"
        }
        if (presenterName != null) {
            intent.putExtra("sessionPresenter", presenterName);
        }
        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}


