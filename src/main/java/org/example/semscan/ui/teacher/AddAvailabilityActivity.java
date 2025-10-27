package org.example.semscan.ui.teacher;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Seminar;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddAvailabilityActivity extends AppCompatActivity {

    private EditText etTileName;
    private EditText etTileDescription;
    private Spinner spinnerSeminars;
    private RecyclerView recyclerSlots;
    private final List<Seminar> seminarCatalog = new ArrayList<>();
    private ArrayAdapter<String> seminarAdapter;
    private Button btnAddSlot;
    private Button btnSave;
    private Button btnCancel;
    private SlotsAdapter slotsAdapter;
    private ApiService apiService;
    private PreferencesManager preferencesManager;
    private Long selectedSeminarId;
    private String seminarNameFromSeminarsTable;
    private String seminarDescriptionFromSeminarsTable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_availability);

        apiService = ApiClient.getInstance(this).getApiService();
        preferencesManager = PreferencesManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Availability");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etTileName = findViewById(R.id.et_tile_name);
        etTileDescription = findViewById(R.id.et_tile_description);
        spinnerSeminars = findViewById(R.id.spinner_seminars);
        recyclerSlots = findViewById(R.id.recycler_slots);
        recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        slotsAdapter = new SlotsAdapter();
        recyclerSlots.setAdapter(slotsAdapter);
        btnAddSlot = findViewById(R.id.btn_add_slot);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        seminarAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, new ArrayList<>());
        spinnerSeminars.setAdapter(seminarAdapter);
        spinnerSeminars.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < seminarCatalog.size()) {
                    Seminar seminar = seminarCatalog.get(position);
                    selectedSeminarId = seminar.getSeminarId();
                    seminarNameFromSeminarsTable = seminar.getSeminarName();
                    seminarDescriptionFromSeminarsTable = seminar.getDescription();
                } else {
                    selectedSeminarId = null;
                    seminarNameFromSeminarsTable = null;
                    seminarDescriptionFromSeminarsTable = null;
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                selectedSeminarId = null;
                seminarNameFromSeminarsTable = null;
                seminarDescriptionFromSeminarsTable = null;
            }
        });

        btnAddSlot.setOnClickListener(v -> slotsAdapter.addEmpty());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());

        selectedSeminarId = (Long) getIntent().getSerializableExtra("seminarId");
        seminarNameFromSeminarsTable = getIntent().getStringExtra("seminarName");
        seminarDescriptionFromSeminarsTable = getIntent().getStringExtra("seminarDescription");
        loadSeminarCatalog();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void save() {
        String tileName = etTileName.getText().toString().trim();
        String tileDescription = etTileDescription.getText().toString().trim();

        if (TextUtils.isEmpty(tileName)) {
            etTileName.setError("Availability title is required");
            return;
        }

        if (selectedSeminarId == null || selectedSeminarId <= 0) {
            Toast.makeText(this, "Please choose a seminar", Toast.LENGTH_LONG).show();
            return;
        }

        List<ApiService.PresenterSeminarSlotDto> slots = slotsAdapter.getSlots();
        if (slots.isEmpty()) {
            Toast.makeText(this, "Add at least one time slot", Toast.LENGTH_LONG).show();
            return;
        }
        for (ApiService.PresenterSeminarSlotDto s : slots) {
            if (s.weekday < 0 || s.weekday > 6 || s.startHour < 0 || s.startHour >= 24 || s.endHour <= 0 || s.endHour > 24 || s.endHour <= s.startHour) {
                Toast.makeText(this, "Invalid slot values", Toast.LENGTH_LONG).show();
                return;
            }
        }

        Long presenterId = preferencesManager.getUserId();
        if (presenterId == null || presenterId <= 0) {
            Logger.e(Logger.TAG_UI, "Cannot create availability - missing presenter ID");
            Toast.makeText(this, "Presenter ID not found. Please check settings.", Toast.LENGTH_LONG).show();
            return;
        }

        ApiService.CreatePresenterSeminarRequest body = new ApiService.CreatePresenterSeminarRequest();
        body.seminarId = selectedSeminarId;
        body.seminarName = seminarNameFromSeminarsTable; // official seminar name from catalog
        body.seminarDisplayName = seminarNameFromSeminarsTable;
        body.seminarInstanceName = tileName; // presenter-provided instance name

        String resolvedDescription = !TextUtils.isEmpty(tileDescription)
                ? tileDescription
                : seminarDescriptionFromSeminarsTable;
        body.seminarDescription = TextUtils.isEmpty(resolvedDescription) ? null : resolvedDescription; // instance description (fallback to catalog)
        body.tileDescription = TextUtils.isEmpty(tileDescription) ? null : tileDescription; // keep legacy description for older payloads
        body.slots = slots;

        Logger.userAction("Create Availability", "name=" + tileName + ", slots=" + slots.size());
        Logger.api("POST", "api/v1/presenters/" + presenterId + "/seminars", "creating presenter availability");

        apiService.createPresenterSeminar(presenterId, body).enqueue(new Callback<ApiService.PresenterSeminarDto>() {
            @Override
            public void onResponse(Call<ApiService.PresenterSeminarDto> call, Response<ApiService.PresenterSeminarDto> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddAvailabilityActivity.this, "Availability saved", Toast.LENGTH_SHORT).show();
                    Logger.apiResponse("POST", "api/v1/presenters/" + presenterId + "/seminars", response.code(), "Availability created");
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Logger.apiError("POST", "api/v1/presenters/" + presenterId + "/seminars", response.code(), "Failed to create availability");
                    Toast.makeText(AddAvailabilityActivity.this, "Failed to save availability", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.PresenterSeminarDto> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Availability creation failed", t);
                Toast.makeText(AddAvailabilityActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void loadSeminarCatalog() {
        apiService.getSeminars().enqueue(new Callback<List<Seminar>>() {
            @Override
            public void onResponse(Call<List<Seminar>> call, Response<List<Seminar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    seminarCatalog.clear();
                    seminarCatalog.addAll(response.body());
                    List<String> items = new ArrayList<>();
                    int selectedIndex = -1;
                    for (int i = 0; i < seminarCatalog.size(); i++) {
                        Seminar seminar = seminarCatalog.get(i);
                        String name = TextUtils.isEmpty(seminar.getSeminarName()) ? "Unnamed" : seminar.getSeminarName();
                        items.add(String.format("#%d • %s", seminar.getSeminarId(), name));
                        if (selectedSeminarId != null && selectedSeminarId.equals(seminar.getSeminarId())) {
                            selectedIndex = i;
                        }
                    }
                    seminarAdapter.clear();
                    seminarAdapter.addAll(items);
                    if (selectedIndex >= 0) {
                        spinnerSeminars.setSelection(selectedIndex);
                    } else if (!seminarCatalog.isEmpty()) {
                        selectedSeminarId = seminarCatalog.get(0).getSeminarId();
                        seminarNameFromSeminarsTable = seminarCatalog.get(0).getSeminarName();
                        seminarDescriptionFromSeminarsTable = seminarCatalog.get(0).getDescription();
                        spinnerSeminars.setSelection(0);
                    } else {
                        spinnerSeminars.setSelection(-1);
                    }
                } else {
                    Toast.makeText(AddAvailabilityActivity.this, "Failed to load seminars", Toast.LENGTH_LONG).show();
                    selectedSeminarId = null;
                    seminarNameFromSeminarsTable = null;
                    seminarDescriptionFromSeminarsTable = null;
                    seminarAdapter.clear();
                }
            }

            @Override
            public void onFailure(Call<List<Seminar>> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Failed to load seminars", t);
                Toast.makeText(AddAvailabilityActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                selectedSeminarId = null;
                seminarNameFromSeminarsTable = null;
                seminarDescriptionFromSeminarsTable = null;
                seminarAdapter.clear();
            }
        });
    }

    private static class SlotsAdapter extends RecyclerView.Adapter<SlotsAdapter.VH> {
        private final List<ApiService.PresenterSeminarSlotDto> items = new ArrayList<>();

        void addEmpty() {
            ApiService.PresenterSeminarSlotDto s = new ApiService.PresenterSeminarSlotDto();
            s.weekday = 0;
            s.startHour = 10;
            s.endHour = 12;
            items.add(s);
            notifyItemInserted(items.size() - 1);
        }

        List<ApiService.PresenterSeminarSlotDto> getSlots() {
            return new ArrayList<>(items);
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slot_row, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            ApiService.PresenterSeminarSlotDto s = items.get(pos);
            h.spinnerDay.setSelection(s.weekday);
            h.spinnerStart.setSelection(s.startHour);
            h.spinnerEnd.setSelection(Math.max(0, s.endHour - 1));

            h.spinnerDay.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    s.weekday = position;
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            h.spinnerStart.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    s.startHour = position;
                    if (s.endHour <= s.startHour) {
                        s.endHour = Math.min(24, s.startHour + 1);
                    }
                    updateEndHourOptions(h.spinnerEnd, s.startHour, s.endHour);
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            updateEndHourOptions(h.spinnerEnd, s.startHour, s.endHour);
            h.spinnerEnd.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                    Object item = h.spinnerEnd.getAdapter().getItem(position);
                    if (item instanceof Integer) {
                        s.endHour = ((Integer) item);
                    } else {
                        s.endHour = position + s.startHour + 1;
                    }
                }

                @Override
                public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });

            h.btnRemove.setOnClickListener(v -> {
                int p = h.getAdapterPosition();
                if (p >= 0) {
                    items.remove(p);
                    notifyItemRemoved(p);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            Spinner spinnerDay, spinnerStart, spinnerEnd;
            Button btnRemove;

            VH(View v) {
                super(v);
                spinnerDay = v.findViewById(R.id.spinner_day);
                spinnerStart = v.findViewById(R.id.spinner_start);
                spinnerEnd = v.findViewById(R.id.spinner_end);
                btnRemove = v.findViewById(R.id.btn_remove);

                String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
                spinnerDay.setAdapter(new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, days));
                Integer[] hours = new Integer[24];
                for (int i = 0; i < 24; i++) hours[i] = i;
                spinnerStart.setAdapter(new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, hours));
            }
        }

        private void updateEndHourOptions(Spinner spinnerEnd, int startHour, int currentEndHour) {
            int optionsCount = Math.max(1, 24 - startHour - 1);
            Integer[] endOptions = new Integer[optionsCount];
            for (int i = 0; i < optionsCount; i++) {
                endOptions[i] = startHour + i + 1;
            }
            ArrayAdapter<Integer> adapter = new ArrayAdapter<>(spinnerEnd.getContext(), android.R.layout.simple_spinner_dropdown_item, endOptions);
            spinnerEnd.setAdapter(adapter);

            int selectionIndex = 0;
            for (int i = 0; i < endOptions.length; i++) {
                if (endOptions[i] == currentEndHour) {
                    selectionIndex = i;
                    break;
                }
            }
            spinnerEnd.setSelection(selectionIndex);
        }
    }
}
