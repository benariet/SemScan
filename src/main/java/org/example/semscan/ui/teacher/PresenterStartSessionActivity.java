package org.example.semscan.ui.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.button.MaterialButton;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresenterStartSessionActivity extends AppCompatActivity {

    private TextView textSlotTitle;
    private TextView textSlotWindow;
    private TextView textSlotLocation;
    private TextView textSessionStatus;
    private TextView textEmptyState;
    private View cardSlotDetails;
    private ProgressBar progressBar;
    private MaterialButton btnStartSession;

    private PreferencesManager preferencesManager;
    private ApiService apiService;

    private ApiService.MySlotSummary currentSlot;
    private ApiService.AttendancePanel currentAttendance;
    private String normalizedUsername;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_start_session);

        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();

        setupToolbar();
        initializeViews();
        setupInteractions();

        loadPresenterSlot();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeViews() {
        textSlotTitle = findViewById(R.id.text_slot_title);
        textSlotWindow = findViewById(R.id.text_slot_window);
        textSlotLocation = findViewById(R.id.text_slot_location);
        textSessionStatus = findViewById(R.id.text_session_status);
        textEmptyState = findViewById(R.id.text_empty_state);
        cardSlotDetails = findViewById(R.id.card_slot_details);
        progressBar = findViewById(R.id.progress_bar);
        btnStartSession = findViewById(R.id.btn_start_session);
    }

    private void setupInteractions() {
        btnStartSession.setOnClickListener(v -> attemptOpenSession());
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnStartSession.setEnabled(!loading && currentSlot != null);
    }

    private void loadPresenterSlot() {
        final String username = preferencesManager.getUserName();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, R.string.presenter_start_session_error_no_user, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        normalizedUsername = username.trim().toLowerCase(Locale.US);

        setLoading(true);
        apiService.getPresenterHome(normalizedUsername).enqueue(new Callback<ApiService.PresenterHomeResponse>() {
            @Override
            public void onResponse(Call<ApiService.PresenterHomeResponse> call, Response<ApiService.PresenterHomeResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(PresenterStartSessionActivity.this, R.string.presenter_start_session_error_load, Toast.LENGTH_LONG).show();
                    showEmptyState();
                    return;
                }
                currentSlot = response.body().mySlot;
                currentAttendance = response.body().attendance;
                renderSlot();
            }

            @Override
            public void onFailure(Call<ApiService.PresenterHomeResponse> call, Throwable t) {
                setLoading(false);
                Logger.e(Logger.TAG_API, "Failed to load presenter home", t);
                Toast.makeText(PresenterStartSessionActivity.this, R.string.presenter_start_session_error_load, Toast.LENGTH_LONG).show();
                showEmptyState();
            }
        });
    }

    private void renderSlot() {
        if (currentSlot == null || currentSlot.slotId == null) {
            showEmptyState();
            return;
        }

        cardSlotDetails.setVisibility(View.VISIBLE);
        textEmptyState.setVisibility(View.GONE);

        textSlotTitle.setText(getString(R.string.presenter_home_slot_title_format,
                safe(currentSlot.dayOfWeek), safe(currentSlot.date)));
        textSlotWindow.setText(safe(currentSlot.timeRange));

        String location = buildLocation();
        textSlotLocation.setText(location);
        textSlotLocation.setVisibility(TextUtils.isEmpty(location) ? View.GONE : View.VISIBLE);

        if (currentAttendance != null && !TextUtils.isEmpty(currentAttendance.warning)) {
            textSessionStatus.setText(currentAttendance.warning);
        } else if (currentAttendance != null && currentAttendance.alreadyOpen) {
            textSessionStatus.setText(R.string.presenter_start_session_status_open);
        } else {
            textSessionStatus.setText("");
        }

        if (currentAttendance != null && currentAttendance.alreadyOpen) {
            btnStartSession.setText(R.string.presenter_start_session_button_resume);
        } else {
            btnStartSession.setText(R.string.presenter_start_session_button);
        }

        btnStartSession.setEnabled(true);
    }

    private void showEmptyState() {
        cardSlotDetails.setVisibility(View.GONE);
        textEmptyState.setVisibility(View.VISIBLE);
        btnStartSession.setEnabled(false);
    }

    private void attemptOpenSession() {
        if (currentSlot == null || currentSlot.slotId == null) {
            Toast.makeText(this, R.string.presenter_start_session_empty, Toast.LENGTH_LONG).show();
            return;
        }

        if (currentAttendance != null && currentAttendance.alreadyOpen && !TextUtils.isEmpty(currentAttendance.openQrUrl)) {
            openAttendanceQr(currentAttendance.openQrUrl, currentAttendance.qrPayload,
                    currentAttendance.openedAt, currentAttendance.closesAt, currentAttendance.sessionId);
            return;
        }

        setLoading(true);
        apiService.openPresenterAttendance(normalizedUsername, currentSlot.slotId)
                .enqueue(new Callback<ApiService.PresenterAttendanceOpenResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.PresenterAttendanceOpenResponse> call, Response<ApiService.PresenterAttendanceOpenResponse> response) {
                        setLoading(false);
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(PresenterStartSessionActivity.this, R.string.error, Toast.LENGTH_LONG).show();
                            return;
                        }

                        ApiService.PresenterAttendanceOpenResponse body = response.body();
                        String code = body.code != null ? body.code : "";
                        switch (code) {
                            case "OPENED":
                            case "ALREADY_OPEN":
                                openAttendanceQr(body.qrUrl, body.qrPayload, body.openedAt, body.closesAt, body.sessionId);
                                break;
                            case "TOO_EARLY":
                            case "IN_PROGRESS":
                                Toast.makeText(PresenterStartSessionActivity.this,
                                        body.message != null ? body.message : getString(R.string.presenter_start_session_error_load),
                                        Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(PresenterStartSessionActivity.this,
                                        body.message != null ? body.message : getString(R.string.error),
                                        Toast.LENGTH_LONG).show();
                                break;
                        }
                        loadPresenterSlot();
                    }

                    @Override
                    public void onFailure(Call<ApiService.PresenterAttendanceOpenResponse> call, Throwable t) {
                        setLoading(false);
                        Logger.e(Logger.TAG_API, "Failed to open attendance", t);
                        Toast.makeText(PresenterStartSessionActivity.this, R.string.error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void openAttendanceQr(@Nullable String qrUrl,
                                  @Nullable String qrPayload,
                                  @Nullable String openedAt,
                                  @Nullable String closesAt,
                                  @Nullable Long sessionId) {
        Intent intent = new Intent(this, PresenterAttendanceQrActivity.class);
        intent.putExtra(PresenterAttendanceQrActivity.EXTRA_QR_URL, qrUrl);
        intent.putExtra(PresenterAttendanceQrActivity.EXTRA_QR_PAYLOAD, qrPayload);
        intent.putExtra(PresenterAttendanceQrActivity.EXTRA_OPENED_AT, openedAt);
        intent.putExtra(PresenterAttendanceQrActivity.EXTRA_CLOSES_AT, closesAt);
        intent.putExtra(PresenterAttendanceQrActivity.EXTRA_SLOT_TITLE, textSlotTitle.getText().toString());
        intent.putExtra(PresenterAttendanceQrActivity.EXTRA_SESSION_ID, sessionId);
        intent.putExtra(PresenterAttendanceQrActivity.EXTRA_SLOT_ID, currentSlot != null ? currentSlot.slotId : null);
        intent.putExtra(PresenterAttendanceQrActivity.EXTRA_USERNAME, normalizedUsername);
        startActivity(intent);
    }

    private String buildLocation() {
        StringBuilder builder = new StringBuilder();
        if (!TextUtils.isEmpty(currentSlot.room)) {
            builder.append(getString(R.string.room_with_label, currentSlot.room));
        }
        if (!TextUtils.isEmpty(currentSlot.building)) {
            if (builder.length() > 0) builder.append(" • ");
            builder.append(getString(R.string.building_with_label, currentSlot.building));
        }
        return builder.toString();
    }

    private String safe(String value) {
        return value == null ? "" : value;
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
