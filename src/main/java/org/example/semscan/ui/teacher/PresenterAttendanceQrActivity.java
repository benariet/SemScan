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

    private static final long REFRESH_INTERVAL_MS = 5000L;

    private ImageView imageQr;
    private TextView textSlotTitle;
    private TextView textOpenedAt;
    private TextView textValidUntil;
    private ProgressBar progressRefresh;
    private com.google.android.material.button.MaterialButton btnEndSession;
    private com.google.android.material.button.MaterialButton btnCancelSession;

    private ApiService apiService;
    private Handler refreshHandler;
    private Runnable refreshRunnable;

    private Long slotId;
    private Long sessionId;
    private String username;
    private String lastPayload;
    
    // Slot details for export filename
    private String slotDate;
    private String slotTimeRange;
    private String presenterName;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_attendance_qr);

        apiService = ApiClient.getInstance(this).getApiService();
        refreshHandler = new Handler(Looper.getMainLooper());

        setupToolbar();
        initializeViews();
        readExtras();
        startRefreshLoop();
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

    private void startRefreshLoop() {
        if (slotId == null || TextUtils.isEmpty(username)) {
            return;
        }
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                fetchLatestQr();
                refreshHandler.postDelayed(this, REFRESH_INTERVAL_MS);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS);
    }

    private void fetchLatestQr() {
        progressRefresh.setVisibility(View.VISIBLE);
        apiService.getPresenterAttendanceQr(username.trim().toLowerCase(Locale.US), slotId)
                .enqueue(new Callback<ApiService.PresenterAttendanceOpenResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.PresenterAttendanceOpenResponse> call, Response<ApiService.PresenterAttendanceOpenResponse> response) {
                        progressRefresh.setVisibility(View.GONE);
                        if (!response.isSuccessful()) {
                            handleQrError(response.code(), response);
                            return;
                        }
                        if (response.body() == null) {
                            Logger.w(Logger.TAG_API, "QR refresh failed - null response body");
                            return;
                        }
                        ApiService.PresenterAttendanceOpenResponse body = response.body();
                        if (!TextUtils.isEmpty(body.openedAt)) {
                            textOpenedAt.setText(getString(R.string.presenter_attendance_qr_opened_at, body.openedAt));
                        }
                        if (!TextUtils.isEmpty(body.closesAt)) {
                            textValidUntil.setText(getString(R.string.presenter_attendance_qr_valid_until, body.closesAt));
                        }
                        if (body.sessionId != null) {
                            sessionId = body.sessionId;
                        }

                        // Use new qrContent structure if available, otherwise fallback to legacy fields
                        String qrUrl = body.getQrUrl();
                        String candidate = normalizeQrContent(qrUrl);
                        if (candidate != null && !candidate.equals(lastPayload)) {
                            lastPayload = candidate;
                            generateQr(candidate);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.PresenterAttendanceOpenResponse> call, Throwable t) {
                        progressRefresh.setVisibility(View.GONE);
                        Logger.e(Logger.TAG_API, "Failed to refresh attendance QR", t);
                        
                        // Handle connection errors gracefully
                        String errorMessage = getString(R.string.presenter_attendance_qr_error_generic);
                        if (t instanceof java.net.SocketException || t instanceof java.net.SocketTimeoutException) {
                            errorMessage = getString(R.string.presenter_attendance_qr_error_connection);
                        } else if (t instanceof java.net.ConnectException) {
                            errorMessage = getString(R.string.presenter_attendance_qr_error_server_unavailable);
                        }
                        
                        // Only show error toast if it's a critical connection issue
                        // Don't spam the user with errors during normal refresh loop
                        if (t instanceof java.net.ConnectException || 
                            (t instanceof java.net.SocketException && t.getMessage() != null && 
                             t.getMessage().contains("Connection refused"))) {
                            android.widget.Toast.makeText(PresenterAttendanceQrActivity.this, 
                                    errorMessage, android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }
                });
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

    private void handleQrError(int code, Response<ApiService.PresenterAttendanceOpenResponse> response) {
        String errorMessage = getString(R.string.presenter_attendance_qr_error_generic);
        
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                if (errorBody != null && !errorBody.trim().isEmpty()) {
                    try {
                        JsonObject jsonObject = new JsonParser().parse(errorBody).getAsJsonObject();
                        if (jsonObject.has("error") && jsonObject.get("error").isJsonPrimitive()) {
                            errorMessage = jsonObject.get("error").getAsString();
                        }
                    } catch (Exception e) {
                        // If JSON parsing fails, try manual extraction
                        int errorStart = errorBody.indexOf("\"error\":\"");
                        if (errorStart >= 0) {
                            errorStart += 9; // Length of "\"error\":\""
                            int errorEnd = errorBody.indexOf("\"", errorStart);
                            if (errorEnd > errorStart) {
                                errorMessage = errorBody.substring(errorStart, errorEnd);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.e(Logger.TAG_API, "Failed to read error body", e);
        }
        
        switch (code) {
            case 404:
                errorMessage = getString(R.string.presenter_attendance_qr_error_not_found);
                break;
            case 400:
                errorMessage = getString(R.string.presenter_attendance_qr_error_not_open);
                break;
            case 500:
                errorMessage = getString(R.string.presenter_attendance_qr_error_server);
                break;
        }
        
        Logger.w(Logger.TAG_API, "QR refresh failed code=" + code + ", message=" + errorMessage);
        
        // Show error to user
        android.widget.Toast.makeText(PresenterAttendanceQrActivity.this, errorMessage, android.widget.Toast.LENGTH_LONG).show();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (refreshHandler != null && refreshRunnable != null) {
            refreshHandler.removeCallbacks(refreshRunnable);
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
                .setPositiveButton(R.string.presenter_attendance_qr_cancel_session, (dialog, which) -> {
                    endSession();
                    finish();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
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


