package org.example.semscan.ui.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Seminar;
import org.example.semscan.data.model.Session;
import org.example.semscan.ui.qr.QRDisplayActivity;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresenterStartSessionActivity extends AppCompatActivity {
    
    private Spinner spinnerSeminar;
    private Button btnStartSession;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    private List<Seminar> seminars = new ArrayList<>();
    private String selectedSeminarId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_presenter_start_session);
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance().getApiService();
        
        initializeViews();
        setupToolbar();
        setupSpinner();
        setupClickListeners();
        
        loadSeminars();
    }
    
    private void initializeViews() {
        spinnerSeminar = findViewById(R.id.spinner_course);
        btnStartSession = findViewById(R.id.btn_start_session);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupSpinner() {
        spinnerSeminar.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select Seminar" placeholder
                    selectedSeminarId = seminars.get(position - 1).getSeminarId();
                } else {
                    selectedSeminarId = null;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSeminarId = null;
            }
        });
    }
    
    private void setupClickListeners() {
        btnStartSession.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSession();
            }
        });
    }
    
    private void loadSeminars() {
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Call<List<Seminar>> call = apiService.getSeminars(apiKey);
        call.enqueue(new Callback<List<Seminar>>() {
            @Override
            public void onResponse(Call<List<Seminar>> call, Response<List<Seminar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    seminars.clear();
                    seminars.addAll(response.body());
                    updateSeminarSpinner();
                } else {
                    Toast.makeText(PresenterStartSessionActivity.this, 
                            "Failed to load seminars", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Seminar>> call, Throwable t) {
                Toast.makeText(PresenterStartSessionActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateSeminarSpinner() {
        List<String> seminarNames = new ArrayList<>();
        seminarNames.add("Select Seminar");
        
        for (Seminar seminar : seminars) {
            seminarNames.add(seminar.getSeminarName());
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, seminarNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSeminar.setAdapter(adapter);
    }
    
    private void startSession() {
        if (selectedSeminarId == null) {
            Toast.makeText(this, "Please select a seminar", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        ApiService.CreateSessionRequest request = new ApiService.CreateSessionRequest(
                selectedSeminarId, startTime);
        
        Call<Session> call = apiService.createSession(apiKey, request);
        call.enqueue(new Callback<Session>() {
            @Override
            public void onResponse(Call<Session> call, Response<Session> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Session session = response.body();
                    openQRDisplay(session);
                } else {
                    Toast.makeText(PresenterStartSessionActivity.this, 
                            "Failed to create session", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Session> call, Throwable t) {
                Toast.makeText(PresenterStartSessionActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void openQRDisplay(Session session) {
        Intent intent = new Intent(this, QRDisplayActivity.class);
        intent.putExtra("sessionId", session.getSessionId());
        intent.putExtra("seminarId", session.getSeminarId());
        intent.putExtra("startTime", session.getStartTime());
        intent.putExtra("endTime", session.getEndTime() != null ? session.getEndTime() : 0L);
        intent.putExtra("status", session.getStatus());
        startActivity(intent);
        finish();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
