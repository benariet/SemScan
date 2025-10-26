package org.example.semscan.ui.teacher;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddSeminarActivity extends AppCompatActivity {

    private EditText etSeminarName;
    private RecyclerView recyclerSlots;
    private Button btnAddSlot;
    private Button btnSave;
    private Button btnCancel;
    private SlotsAdapter slotsAdapter;
    private ApiService apiService;
    private PreferencesManager preferencesManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_seminar);

        apiService = ApiClient.getInstance(this).getApiService();
        preferencesManager = PreferencesManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        etSeminarName = findViewById(R.id.et_seminar_name);
        recyclerSlots = findViewById(R.id.recycler_slots);
        recyclerSlots.setLayoutManager(new LinearLayoutManager(this));
        slotsAdapter = new SlotsAdapter();
        recyclerSlots.setAdapter(slotsAdapter);
        btnAddSlot = findViewById(R.id.btn_add_slot);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        btnAddSlot.setOnClickListener(v -> slotsAdapter.addEmpty());
        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());

        slotsAdapter.addEmpty();
    }

    private void save() {
        String seminarName = etSeminarName.getText().toString().trim();
        if (TextUtils.isEmpty(seminarName)) {
            etSeminarName.setError("Seminar name is required");
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
        
        // Check for duplicate slots
        for (int i = 0; i < slots.size(); i++) {
            for (int j = i + 1; j < slots.size(); j++) {
                ApiService.PresenterSeminarSlotDto slot1 = slots.get(i);
                ApiService.PresenterSeminarSlotDto slot2 = slots.get(j);
                if (slot1.weekday == slot2.weekday && slot1.startHour == slot2.startHour && slot1.endHour == slot2.endHour) {
                    Toast.makeText(this, "Duplicate time slots are not allowed", Toast.LENGTH_LONG).show();
                    return;
                }
            }
        }

        Long presenterId = preferencesManager.getUserId();
        if (presenterId == null || presenterId <= 0) {
            Logger.e(Logger.TAG_UI, "Cannot create seminar - missing presenter ID");
            Toast.makeText(this, "Presenter ID not found. Please check settings.", Toast.LENGTH_LONG).show();
            return;
        }

        ApiService.CreatePresenterSeminarRequest body = new ApiService.CreatePresenterSeminarRequest(seminarName, slots);
        apiService.createPresenterSeminar(presenterId, body).enqueue(new Callback<ApiService.PresenterSeminarDto>() {
            @Override
            public void onResponse(Call<ApiService.PresenterSeminarDto> call, Response<ApiService.PresenterSeminarDto> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddSeminarActivity.this, "Seminar created", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(AddSeminarActivity.this, "Failed to create seminar", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.PresenterSeminarDto> call, Throwable t) {
                Toast.makeText(AddSeminarActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // Simple slots adapter (weekday/start/end integer pickers via Spinners)
    private static class SlotsAdapter extends RecyclerView.Adapter<SlotsAdapter.VH> {
        private final List<ApiService.PresenterSeminarSlotDto> items = new ArrayList<>();
        void addEmpty() { ApiService.PresenterSeminarSlotDto s = new ApiService.PresenterSeminarSlotDto(); s.weekday=0; s.startHour=10; s.endHour=12; items.add(s); notifyItemInserted(items.size()-1);}    
        List<ApiService.PresenterSeminarSlotDto> getSlots() { return new ArrayList<>(items); }
        @Override public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext()).inflate(R.layout.item_slot_row, parent, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(VH h, int pos) {
            ApiService.PresenterSeminarSlotDto s = items.get(pos);
            h.spinnerDay.setSelection(s.weekday);
            h.spinnerStart.setSelection(s.startHour);
            h.spinnerEnd.setSelection(Math.max(0, s.endHour-1));
            
            // Update slot data when spinners change
            h.spinnerDay.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    s.weekday = position;
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            h.spinnerStart.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    s.startHour = position;
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            h.spinnerEnd.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
                @Override public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                    s.endHour = position + 1; // End hour is 1-24, spinner is 0-23
                }
                @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
            });
            
            h.btnRemove.setOnClickListener(v -> { int p=h.getAdapterPosition(); if(p>=0){ items.remove(p); notifyItemRemoved(p);} });
        }
        @Override public int getItemCount() { return items.size(); }
        static class VH extends RecyclerView.ViewHolder {
            Spinner spinnerDay, spinnerStart, spinnerEnd; Button btnRemove;
            VH(View v) { super(v);
                spinnerDay = v.findViewById(R.id.spinner_day);
                spinnerStart = v.findViewById(R.id.spinner_start);
                spinnerEnd = v.findViewById(R.id.spinner_end);
                btnRemove = v.findViewById(R.id.btn_remove);
                String[] days = {"Sun","Mon","Tue","Wed","Thu","Fri","Sat"};
                spinnerDay.setAdapter(new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, days));
                Integer[] hours = new Integer[24]; for(int i=0;i<24;i++) hours[i]=i;
                spinnerStart.setAdapter(new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, hours));
                spinnerEnd.setAdapter(new ArrayAdapter<>(v.getContext(), android.R.layout.simple_spinner_dropdown_item, hours));
            }
        }
    }
}


