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

    private ApiService apiService;
    private Handler refreshHandler;
    private Runnable refreshRunnable;

    private Long slotId;
    private Long sessionId;
    private String username;
    private String lastPayload;

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
                        if (!response.isSuccessful() || response.body() == null) {
                            Logger.w(Logger.TAG_API, "QR refresh failed code=" + response.code());
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

                        String candidate = normalizeQrContent(!TextUtils.isEmpty(body.qrPayload) ? body.qrPayload : body.qrUrl);
                        if (candidate != null && !candidate.equals(lastPayload)) {
                            lastPayload = candidate;
                            generateQr(candidate);
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.PresenterAttendanceOpenResponse> call, Throwable t) {
                        progressRefresh.setVisibility(View.GONE);
                        Logger.e(Logger.TAG_API, "Failed to refresh attendance QR", t);
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
}


