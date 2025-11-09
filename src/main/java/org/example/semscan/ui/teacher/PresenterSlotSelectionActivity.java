package org.example.semscan.ui.teacher;

import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresenterSlotSelectionActivity extends AppCompatActivity implements PresenterSlotsAdapter.SlotActionListener {

    public static final String EXTRA_SCROLL_TO_MY_SLOT = "presenter_slot_selection.extra_scroll_to_my_slot";

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView recyclerSlots;
    private View emptyState;
    private ProgressBar progressBar;
    private MaterialButton btnReload;

    private PresenterSlotsAdapter slotAdapter;
    private PreferencesManager preferencesManager;
    private ApiService apiService;

    private boolean shouldScrollToMySlot;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_slot_selection);

        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();

        if (!preferencesManager.isPresenter()) {
            finish();
            return;
        }

        shouldScrollToMySlot = getIntent().getBooleanExtra(EXTRA_SCROLL_TO_MY_SLOT, false);

        setupToolbar();
        initializeViews();
        setupRecycler();
        setupInteractions();

        loadSlots();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void initializeViews() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh);
        recyclerSlots = findViewById(R.id.recycler_slots);
        emptyState = findViewById(R.id.layout_empty_state);
        progressBar = findViewById(R.id.progress_bar);
        btnReload = findViewById(R.id.btn_reload);
    }

    private void setupRecycler() {
        slotAdapter = new PresenterSlotsAdapter(this);
        recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        recyclerSlots.setAdapter(slotAdapter);
    }

    private void setupInteractions() {
        swipeRefreshLayout.setOnRefreshListener(this::loadSlots);
        btnReload.setOnClickListener(v -> loadSlots());
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (!loading) {
            swipeRefreshLayout.setRefreshing(false);
        }
    }

    private void renderSlots(ApiService.PresenterHomeResponse response) {
        List<ApiService.SlotCard> allSlots = response != null && response.slotCatalog != null
                ? response.slotCatalog : Collections.emptyList();
        
        // Filter out past slots
        List<ApiService.SlotCard> futureSlots = filterPastSlots(allSlots);
        
        slotAdapter.submitList(futureSlots);
        emptyState.setVisibility(futureSlots.isEmpty() ? View.VISIBLE : View.GONE);

        if (shouldScrollToMySlot && response != null && response.mySlot != null) {
            shouldScrollToMySlot = false;
            long targetSlot = response.mySlot.slotId != null ? response.mySlot.slotId : -1L;
            if (targetSlot > 0) {
                int position = findSlotPosition(futureSlots, targetSlot);
                if (position >= 0) {
                    recyclerSlots.post(() -> recyclerSlots.smoothScrollToPosition(position));
                }
            }
        }
    }

    private int findSlotPosition(List<ApiService.SlotCard> slots, long slotId) {
        for (int i = 0; i < slots.size(); i++) {
            ApiService.SlotCard slot = slots.get(i);
            if (slot.slotId != null && slot.slotId == slotId) {
                return i;
            }
        }
        return -1;
    }

    private List<ApiService.SlotCard> filterPastSlots(List<ApiService.SlotCard> slots) {
        if (slots == null || slots.isEmpty()) {
            return Collections.emptyList();
        }

        List<ApiService.SlotCard> futureSlots = new ArrayList<>();
        Date now = new Date();
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US);

        for (ApiService.SlotCard slot : slots) {
            // Filter out slots with closed sessions
            // If a slot has a closed attendance session, it should not be available for registration
            if (slot.hasClosedSession != null && slot.hasClosedSession) {
                Logger.d(Logger.TAG_UI, "Filtering out slot " + slot.slotId + " - has closed session");
                continue; // Skip this slot
            }
            
            // Also check if attendance was closed by checking attendanceClosesAt
            // If attendanceClosesAt exists and is in the past, the session is closed
            if (slot.attendanceClosesAt != null && !slot.attendanceClosesAt.isEmpty()) {
                try {
                    // Parse the closesAt timestamp (format: "2025-11-09 13:10:49")
                    SimpleDateFormat closesAtFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
                    Date closesAt = closesAtFormat.parse(slot.attendanceClosesAt);
                    if (closesAt != null && closesAt.before(now)) {
                        // Session closed in the past, filter out this slot
                        Logger.d(Logger.TAG_UI, "Filtering out slot " + slot.slotId + " - attendance closed at " + slot.attendanceClosesAt);
                        continue; // Skip this slot
                    }
                } catch (ParseException e) {
                    // If parsing fails, don't filter it out (better to show than hide)
                    Logger.w(Logger.TAG_UI, "Failed to parse attendanceClosesAt: " + slot.attendanceClosesAt + " - " + e.getMessage());
                }
            }
            if (slot.date == null || slot.timeRange == null) {
                // If we can't parse the date/time, include it (better to show than hide)
                futureSlots.add(slot);
                continue;
            }

            try {
                // Parse date and time range
                // Format: date is "yyyy-MM-dd", timeRange is "HH:mm-HH:mm"
                String[] timeParts = slot.timeRange.split("-");
                if (timeParts.length != 2) {
                    // Can't parse time range, include it
                    futureSlots.add(slot);
                    continue;
                }

                String startTimeStr = slot.date + " " + timeParts[0].trim();
                Date slotStartTime = dateTimeFormat.parse(startTimeStr);

                if (slotStartTime != null) {
                    // Include slots that are today or in the future
                    // Compare dates (not times) to include all slots for today
                    Calendar slotCal = Calendar.getInstance();
                    slotCal.setTime(slotStartTime);
                    slotCal.set(Calendar.HOUR_OF_DAY, 0);
                    slotCal.set(Calendar.MINUTE, 0);
                    slotCal.set(Calendar.SECOND, 0);
                    slotCal.set(Calendar.MILLISECOND, 0);
                    
                    Calendar nowCal = Calendar.getInstance();
                    nowCal.setTime(now);
                    nowCal.set(Calendar.HOUR_OF_DAY, 0);
                    nowCal.set(Calendar.MINUTE, 0);
                    nowCal.set(Calendar.SECOND, 0);
                    nowCal.set(Calendar.MILLISECOND, 0);
                    
                    // Include if slot date is today or in the future
                    if (!slotCal.getTime().before(nowCal.getTime())) {
                        futureSlots.add(slot);
                    }
                }
                // If slot is in the past (before today), skip it
            } catch (ParseException e) {
                // If parsing fails, include the slot (better to show than hide)
                Logger.w(Logger.TAG_UI, "Failed to parse slot date/time: " + slot.date + " " + slot.timeRange + " - " + e.getMessage());
                futureSlots.add(slot);
            }
        }

        return futureSlots;
    }

    private void loadSlots() {
        final String username = preferencesManager.getUserName();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, R.string.presenter_start_session_error_no_user, Toast.LENGTH_LONG).show();
            swipeRefreshLayout.setRefreshing(false);
            return;
        }

        final String normalizedUsername = username.trim().toLowerCase(Locale.US);
        setLoading(true);
        apiService.getPresenterHome(normalizedUsername).enqueue(new Callback<ApiService.PresenterHomeResponse>() {
            @Override
            public void onResponse(Call<ApiService.PresenterHomeResponse> call, Response<ApiService.PresenterHomeResponse> response) {
                setLoading(false);
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(PresenterSlotSelectionActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                    Logger.apiError("GET", "api/v1/presenters/{username}/home", response.code(), response.message());
                    return;
                }
                renderSlots(response.body());
            }

            @Override
            public void onFailure(Call<ApiService.PresenterHomeResponse> call, Throwable t) {
                setLoading(false);
                Toast.makeText(PresenterSlotSelectionActivity.this, R.string.error, Toast.LENGTH_SHORT).show();
                Logger.e(Logger.TAG_API, "Failed to load presenter home", t);
            }
        });
    }

    @Override
    public void onRegisterClicked(ApiService.SlotCard slot) {
        View dialogView = getLayoutInflater().inflate(R.layout.view_register_slot_dialog, null);
        TextInputLayout layoutTopic = dialogView.findViewById(R.id.input_layout_topic);
        TextInputEditText inputTopic = dialogView.findViewById(R.id.input_topic);
        TextInputEditText inputSupervisorName = dialogView.findViewById(R.id.input_supervisor_name);
        TextInputEditText inputSupervisorEmail = dialogView.findViewById(R.id.input_supervisor_email);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.presenter_slot_register_button, null)
                .setNegativeButton(R.string.cancel, (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            final Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            button.setOnClickListener(v -> {
                String topic = inputTopic.getText() != null ? inputTopic.getText().toString().trim() : null;
                String supervisorName = inputSupervisorName.getText() != null ? inputSupervisorName.getText().toString().trim() : null;
                String supervisorEmail = inputSupervisorEmail.getText() != null ? inputSupervisorEmail.getText().toString().trim() : null;

                layoutTopic.setError(null);
                inputSupervisorEmail.setError(null);

                if (!TextUtils.isEmpty(supervisorEmail) && !Patterns.EMAIL_ADDRESS.matcher(supervisorEmail).matches()) {
                    inputSupervisorEmail.setError(getString(R.string.presenter_home_supervisor_email_invalid));
                    return;
                }

                performRegistration(slot,
                        TextUtils.isEmpty(topic) ? null : topic,
                        TextUtils.isEmpty(supervisorName) ? null : supervisorName,
                        TextUtils.isEmpty(supervisorEmail) ? null : supervisorEmail,
                        dialog);
            });
        });

        dialog.show();
    }

    private void performRegistration(ApiService.SlotCard slot,
                                     @Nullable String topic,
                                     @Nullable String supervisorName,
                                     @Nullable String supervisorEmail,
                                     AlertDialog dialog) {
        final String username = preferencesManager.getUserName();
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, R.string.presenter_start_session_error_no_user, Toast.LENGTH_LONG).show();
            return;
        }

        final String normalizedUsername = username.trim().toLowerCase(Locale.US);
        ApiService.PresenterRegisterRequest request = new ApiService.PresenterRegisterRequest(topic, supervisorName, supervisorEmail);

        apiService.registerForSlot(normalizedUsername, slot.slotId, request)
                .enqueue(new Callback<ApiService.PresenterRegisterResponse>() {
                    @Override
                    public void onResponse(Call<ApiService.PresenterRegisterResponse> call, Response<ApiService.PresenterRegisterResponse> response) {
                        if (!response.isSuccessful() || response.body() == null) {
                            Toast.makeText(PresenterSlotSelectionActivity.this, R.string.error, Toast.LENGTH_LONG).show();
                            return;
                        }

                        ApiService.PresenterRegisterResponse body = response.body();
                        String code = body.code != null ? body.code : "";
                        switch (code) {
                            case "REGISTERED":
                                Toast.makeText(PresenterSlotSelectionActivity.this, R.string.presenter_home_register_success, Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                loadSlots();
                                break;
                            case "ALREADY_IN_SLOT":
                            case "ALREADY_REGISTERED":
                                Toast.makeText(PresenterSlotSelectionActivity.this, R.string.presenter_home_register_already, Toast.LENGTH_LONG).show();
                                dialog.dismiss();
                                loadSlots();
                                break;
                            case "SLOT_FULL":
                                Toast.makeText(PresenterSlotSelectionActivity.this, R.string.presenter_home_register_full, Toast.LENGTH_LONG).show();
                                break;
                            default:
                                Toast.makeText(PresenterSlotSelectionActivity.this,
                                        body.message != null ? body.message : getString(R.string.error),
                                        Toast.LENGTH_LONG).show();
                                break;
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiService.PresenterRegisterResponse> call, Throwable t) {
                        Toast.makeText(PresenterSlotSelectionActivity.this, R.string.error, Toast.LENGTH_LONG).show();
                        Logger.e(Logger.TAG_API, "Slot registration failed", t);
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
