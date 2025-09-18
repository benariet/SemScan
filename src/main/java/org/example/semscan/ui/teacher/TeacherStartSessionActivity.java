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
import org.example.semscan.data.model.Course;
import org.example.semscan.data.model.Session;
import org.example.semscan.ui.qr.QRDisplayActivity;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TeacherStartSessionActivity extends AppCompatActivity {
    
    private Spinner spinnerCourse;
    private Button btnStartSession;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    private List<Course> courses = new ArrayList<>();
    private String selectedCourseId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_teacher_start_session);
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance().getApiService();
        
        initializeViews();
        setupToolbar();
        setupSpinner();
        setupClickListeners();
        
        loadCourses();
    }
    
    private void initializeViews() {
        spinnerCourse = findViewById(R.id.spinner_course);
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
        spinnerCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) { // Skip "Select Course" placeholder
                    selectedCourseId = courses.get(position - 1).getCourseId();
                } else {
                    selectedCourseId = null;
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCourseId = null;
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
                    Toast.makeText(TeacherStartSessionActivity.this, 
                            "Failed to load courses", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Toast.makeText(TeacherStartSessionActivity.this, 
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
    
    private void startSession() {
        if (selectedCourseId == null) {
            Toast.makeText(this, "Please select a course", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        long startTime = System.currentTimeMillis();
        
        ApiService.CreateSessionRequest request = new ApiService.CreateSessionRequest(
                selectedCourseId, startTime);
        
        Call<Session> call = apiService.createSession(apiKey, request);
        call.enqueue(new Callback<Session>() {
            @Override
            public void onResponse(Call<Session> call, Response<Session> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Session session = response.body();
                    openQRDisplay(session);
                } else {
                    Toast.makeText(TeacherStartSessionActivity.this, 
                            "Failed to create session", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Session> call, Throwable t) {
                Toast.makeText(TeacherStartSessionActivity.this, 
                        "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void openQRDisplay(Session session) {
        Intent intent = new Intent(this, QRDisplayActivity.class);
        intent.putExtra("sessionId", session.getSessionId());
        intent.putExtra("courseId", session.getCourseId());
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
