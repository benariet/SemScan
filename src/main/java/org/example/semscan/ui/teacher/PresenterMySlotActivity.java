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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresenterMySlotActivity extends AppCompatActivity {

    private View layoutSlotDetails;
    private View layoutEmpty;
    private ProgressBar progressBar;
    private TextView textSlotTitle;
    private TextView textSlotSchedule;
    private TextView textSlotLocation;
    private TextView textSlotPresenters;
    private MaterialButton btnCancel;
    private MaterialButton btnChangeSlot;
    private MaterialButton btnGoToSlots;

    private PreferencesManager preferencesManager;
    private ApiService apiService;

    private ApiService.MySlotSummary currentSlot;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_my_slot);

        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();

        setupToolbar();
        initializeViews();
        setupInteractions();

        loadSlot();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeViews() {
        layoutSlotDetails = findViewById(R.id.container_slot_details);
        layoutEmpty = findViewById(R.id.container_empty_state);
        progressBar = findViewById(R.id.progress_bar);
        textSlotTitle = findViewById(R.id.text_slot_title);
        textSlotSchedule = findViewById(R.id.text_slot_schedule);
        textSlotLocation = findViewById(R.id.text_slot_location);
        textSlotPresenters = findViewById(R.id.text_slot_presenters);
        btnCancel = findViewById(R.id.btn_cancel_slot);
        btnChangeSlot = findViewById(R.id.btn_change_slot);
        btnGoToSlots = findViewById(R.id.btn_go_to_slots);
    }

    private void setupInteractions() {
        btnCancel.setOnClickListener(v -> cancelRegistration());
        btnChangeSlot.setOnClickListener(v -> openSlotSelection());
        btnGoToSlots.setOnClickListener(v -> openSlotSelection());
    }

    private void openSlotSelection() {
        Intent intent = new Intent(this, PresenterSlotSelectionActivity.class);
        intent.putExtra(PresenterSlotSelectionActivity.EXTRA_SCROLL_TO_MY_SLOT, true);
        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private void loadSlot() {
        final String username = preferencesManager.getUserName();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, R.string.presenter_my_slot_no_user_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setLoading(true);
        apiService.getPresenterHome(username.trim().toLowerCase(Locale.US)).enqueue(new Callback<ApiService.PresenterHomeResponse>() {
            @Override
            public void onResponse(Call<ApiService.PresenterHomeResponse> call, Response<ApiService.PresenterHomeResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(PresenterMySlotActivity.this, R.string.error, Toast.LENGTH_LONG).show();
                    return;
                }
                currentSlot = response.body().mySlot;
                renderSlot();
            }

            @Override
            public void onFailure(Call<ApiService.PresenterHomeResponse> call, Throwable t) {
                setLoading(false);
                Logger.e(Logger.TAG_API, "Failed to load my slot", t);
                Toast.makeText(PresenterMySlotActivity.this, R.string.error, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void renderSlot() {
        if (currentSlot == null || currentSlot.slotId == null) {
            layoutSlotDetails.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            return;
        }

        layoutSlotDetails.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);

        textSlotTitle.setText(formatTitle(currentSlot));
        textSlotSchedule.setText(formatSchedule(currentSlot));

        String location = buildLocation(currentSlot);
        textSlotLocation.setText(location);
        textSlotLocation.setVisibility(TextUtils.isEmpty(location) ? View.GONE : View.VISIBLE);

        String presenters = formatPresenters(currentSlot.coPresenters);
        if (TextUtils.isEmpty(presenters)) {
            textSlotPresenters.setVisibility(View.GONE);
        } else {
            textSlotPresenters.setVisibility(View.VISIBLE);
            textSlotPresenters.setText(presenters);
        }
    }

    private String formatTitle(ApiService.MySlotSummary summary) {
        String day = summary.dayOfWeek != null ? summary.dayOfWeek : "";
        String date = summary.date != null ? summary.date : "";
        return getString(R.string.presenter_home_slot_title_format, day, date);
    }

    private String formatSchedule(ApiService.MySlotSummary summary) {
        return summary.timeRange != null ? summary.timeRange : "";
    }

    private String buildLocation(ApiService.MySlotSummary summary) {
        List<String> parts = new ArrayList<>();
        if (!TextUtils.isEmpty(summary.room)) {
            parts.add(getString(R.string.room_with_label, summary.room));
        }
        if (!TextUtils.isEmpty(summary.building)) {
            parts.add(getString(R.string.building_with_label, summary.building));
        }
        return TextUtils.join(" • ", parts);
    }

    private String formatPresenters(List<ApiService.PresenterCoPresenter> presenters) {
        if (presenters == null || presenters.isEmpty()) {
            return null;
        }
        List<String> lines = new ArrayList<>();
        for (ApiService.PresenterCoPresenter presenter : presenters) {
            StringBuilder builder = new StringBuilder();
            if (!TextUtils.isEmpty(presenter.name)) {
                builder.append(presenter.name);
            }
            if (!TextUtils.isEmpty(presenter.topic)) {
                if (builder.length() > 0) {
                    builder.append(" — ");
                }
                builder.append(presenter.topic);
            }
            if (builder.length() > 0) {
                lines.add(builder.toString());
            }
        }
        return lines.isEmpty() ? null : TextUtils.join("\n", lines);
    }

    private void cancelRegistration() {
        if (currentSlot == null || currentSlot.slotId == null) {
            Toast.makeText(this, R.string.presenter_my_slot_no_slot_error, Toast.LENGTH_LONG).show();
            return;
        }
        final String username = preferencesManager.getUserName();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, R.string.presenter_my_slot_no_user_error, Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);
        apiService.cancelSlotRegistration(username.trim().toLowerCase(Locale.US), currentSlot.slotId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        setLoading(false);
                        if (response.isSuccessful()) {
                            Toast.makeText(PresenterMySlotActivity.this, R.string.presenter_my_slot_cancel_success, Toast.LENGTH_LONG).show();
                            loadSlot();
                        } else {
                            Toast.makeText(PresenterMySlotActivity.this, R.string.presenter_my_slot_cancel_error, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        setLoading(false);
                        Logger.e(Logger.TAG_API, "Failed to cancel slot", t);
                        Toast.makeText(PresenterMySlotActivity.this, R.string.presenter_my_slot_cancel_error, Toast.LENGTH_LONG).show();
                    }
                });
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
