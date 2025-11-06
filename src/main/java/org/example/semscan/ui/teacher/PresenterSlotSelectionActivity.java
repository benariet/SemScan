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

import java.util.Collections;
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
        List<ApiService.SlotCard> slots = response != null && response.slotCatalog != null
                ? response.slotCatalog : Collections.emptyList();
        slotAdapter.submitList(slots);
        emptyState.setVisibility(slots.isEmpty() ? View.VISIBLE : View.GONE);

        if (shouldScrollToMySlot && response != null && response.mySlot != null) {
            shouldScrollToMySlot = false;
            long targetSlot = response.mySlot.slotId != null ? response.mySlot.slotId : -1L;
            if (targetSlot > 0) {
                int position = findSlotPosition(slots, targetSlot);
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
