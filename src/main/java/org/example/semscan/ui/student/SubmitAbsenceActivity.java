package org.example.semscan.ui.student;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.AbsenceRequest;
import org.example.semscan.data.model.Course;
import org.example.semscan.data.model.Session;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SubmitAbsenceActivity extends AppCompatActivity {
    
    private Spinner spinnerCourse;
    private Spinner spinnerSession;
    private Spinner spinnerReason;
    private EditText editNote;
    private Button btnSubmit;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    private List<Course> courses = new ArrayList<>();
    private List<Session> sessions = new ArrayList<>();
    private String selectedCourseId;
    private String selectedSessionId;
    
    private final String[] absenceReasons = {
        "Sick", "Family", "Exam", "Other"
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_absence);
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance().getApiService();
        
        initializeViews();
        setupToolbar();
        setupSpinners();
        setupClickListeners();
        
        loadCourses();
    }
    
    private void initializeViews() {
        spinnerCourse = findViewById(R.id.spinner_course);
        spinnerSession = findViewById(R.id.spinner_session);
        spinnerReason = findViewById(R.id.spinner_reason);
        editNote = findViewById(R.id.edit_note);
        btnSubmit = findViewById(R.id.btn_submit);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupSpinners() {
        // Setup reason spinner
        ArrayAdapter<String> reasonAdapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, absenceReasons);
        reasonAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReason.setAdapter(reasonAdapter);
        
        // Setup course spinner listener
        spinnerCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select Course" placeholder
                    selectedCourseId = courses.get(position - 1).getCourseId();
                    loadSessions(selectedCourseId);
                } else {
                    selectedCourseId = null;
                    sessions.clear();
                    updateSessionSpinner();
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCourseId = null;
            }
        });
        
        // Setup session spinner listener
        spinnerSession.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select Session" placeholder
                    selectedSessionId = sessions.get(position - 1).getSessionId();
                } else {
                    selectedSessionId = null;
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedSessionId = null;
            }
        });
    }
    
    private void setupClickListeners() {
        btnSubmit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitAbsenceRequest();
            }
        });
    }
    
    private void loadCourses() {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Call<List<Course>> call = apiService.getCourses(apiKey);
        call.enqueue(new Callback<List<Course>>() {
            @Override
            public void onResponse(Call<List<Course>> call, Response<List<Course>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    courses.clear();
                    courses.addAll(response.body());
                    updateCourseSpinner();
                } else {
                    Toast.makeText(SubmitAbsenceActivity.this, 
                            "Failed to load courses", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Toast.makeText(SubmitAbsenceActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadSessions(String courseId) {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null) {
            return;
        }
        
        // Load sessions for the last 30 days
        long from = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        long to = System.currentTimeMillis();
        
        Call<List<Session>> call = apiService.getSessions(apiKey, courseId, from, to);
        call.enqueue(new Callback<List<Session>>() {
            @Override
            public void onResponse(Call<List<Session>> call, Response<List<Session>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessions.clear();
                    sessions.addAll(response.body());
                    updateSessionSpinner();
                } else {
                    Toast.makeText(SubmitAbsenceActivity.this, 
                            "Failed to load sessions", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Session>> call, Throwable t) {
                Toast.makeText(SubmitAbsenceActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateCourseSpinner() {
        List<String> courseNames = new ArrayList<>();
        courseNames.add("Select Course");
        
        for (Course course : courses) {
            courseNames.add(course.getCourseName());
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, courseNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(adapter);
    }
    
    private void updateSessionSpinner() {
        List<String> sessionNames = new ArrayList<>();
        sessionNames.add("Select Session");
        
        for (Session session : sessions) {
            String sessionName = "Session " + session.getSessionId() + 
                    " (" + new java.text.SimpleDateFormat("MMM dd, HH:mm")
                    .format(new java.util.Date(session.getStartTime())) + ")";
            sessionNames.add(sessionName);
        }
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, sessionNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSession.setAdapter(adapter);
    }
    
    private void submitAbsenceRequest() {
        // Validate inputs
        if (selectedCourseId == null) {
            Toast.makeText(this, "Please select a course", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (selectedSessionId == null) {
            Toast.makeText(this, "Please select a session", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String userId = preferencesManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "User ID not found. Please check settings.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String reason = absenceReasons[spinnerReason.getSelectedItemPosition()];
        String note = editNote.getText().toString().trim();
        
        // Create request
        ApiService.SubmitAbsenceRequest request = new ApiService.SubmitAbsenceRequest(
                userId, selectedCourseId, selectedSessionId, reason, note);
        
        Call<AbsenceRequest> call = apiService.submitAbsenceRequest(request);
        call.enqueue(new Callback<AbsenceRequest>() {
            @Override
            public void onResponse(Call<AbsenceRequest> call, Response<AbsenceRequest> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SubmitAbsenceActivity.this, 
                            "Absence request submitted successfully", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(SubmitAbsenceActivity.this, 
                            "Failed to submit absence request", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<AbsenceRequest> call, Throwable t) {
                Toast.makeText(SubmitAbsenceActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
