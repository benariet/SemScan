package org.example.semscan.ui.teacher;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.AbsenceRequest;
import org.example.semscan.data.model.Course;
import org.example.semscan.ui.adapters.AttendanceAdapter;
import org.example.semscan.ui.adapters.AbsenceRequestAdapter;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecordsDashboardActivity extends AppCompatActivity {
    
    private Spinner spinnerCourse;
    private Spinner spinnerViewType;
    private Button btnRefresh;
    private RecyclerView recyclerViewRecords;
    private TextView textStats;
    private TextView textEmptyState;
    
    private List<Course> courses;
    private List<Attendance> attendanceRecords;
    private List<AbsenceRequest> absenceRequests;
    private AttendanceAdapter attendanceAdapter;
    private AbsenceRequestAdapter absenceRequestAdapter;
    private RecyclerView.Adapter currentAdapter;
    private ArrayAdapter<Course> courseAdapter;
    private ApiService apiService;
    private PreferencesManager preferencesManager;
    
    private String selectedCourseId = null;
    private String selectedViewType = "attendance";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_records_dashboard);
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance().getApiService();
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        setupSpinners();
        loadCourses();
    }
    
    private void initializeViews() {
        spinnerCourse = findViewById(R.id.spinner_course);
        spinnerViewType = findViewById(R.id.spinner_view_type);
        btnRefresh = findViewById(R.id.btn_refresh);
        recyclerViewRecords = findViewById(R.id.listView_records);
        textStats = findViewById(R.id.text_stats);
        textEmptyState = findViewById(R.id.text_empty_state);
        
        courses = new ArrayList<>();
        attendanceRecords = new ArrayList<>();
        absenceRequests = new ArrayList<>();
        
        attendanceAdapter = new AttendanceAdapter(attendanceRecords);
        absenceRequestAdapter = new AbsenceRequestAdapter(absenceRequests, new AbsenceRequestAdapter.OnActionClickListener() {
            @Override
            public void onApprove(AbsenceRequest absenceRequest) {
                // TODO: Implement approve functionality
                Toast.makeText(RecordsDashboardActivity.this, "Approve functionality coming soon", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onReject(AbsenceRequest absenceRequest) {
                // TODO: Implement reject functionality
                Toast.makeText(RecordsDashboardActivity.this, "Reject functionality coming soon", Toast.LENGTH_SHORT).show();
            }
        });
        
        // Set up RecyclerView
        recyclerViewRecords.setLayoutManager(new LinearLayoutManager(this));
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Records Dashboard");
        }
    }
    
    private void setupClickListeners() {
        btnRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                loadRecords();
            }
        });
    }
    
    private void setupSpinners() {
        // Course spinner
        courseAdapter = new ArrayAdapter<Course>(this, android.R.layout.simple_spinner_item, courses) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view.findViewById(android.R.id.text1);
                if (position == 0) {
                    textView.setText("All Courses");
                } else {
                    Course course = courses.get(position - 1);
                    textView.setText(course.getCourseName() + " (" + course.getCourseCode() + ")");
                }
                return view;
            }
        };
        courseAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCourse.setAdapter(courseAdapter);
        
        spinnerCourse.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position == 0) {
                    selectedCourseId = null; // All courses
                } else {
                    selectedCourseId = courses.get(position - 1).getCourseId();
                }
                loadRecords();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedCourseId = null;
            }
        });
        
        // View type spinner
        String[] viewTypes = {"Attendance Records", "Absence Requests"};
        ArrayAdapter<String> viewTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, viewTypes);
        viewTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerViewType.setAdapter(viewTypeAdapter);
        
        spinnerViewType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedViewType = position == 0 ? "attendance" : "absence";
                updateListView();
                loadRecords();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedViewType = "attendance";
            }
        });
    }
    
    private void loadCourses() {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
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
                    courseAdapter.notifyDataSetChanged();
                    loadRecords();
                } else {
                    Toast.makeText(RecordsDashboardActivity.this, "Failed to load courses", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Toast.makeText(RecordsDashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadRecords() {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null || apiKey.isEmpty()) {
            return;
        }
        
        if ("attendance".equals(selectedViewType)) {
            loadAttendanceRecords(apiKey);
        } else {
            loadAbsenceRequests(apiKey);
        }
    }
    
    private void loadAttendanceRecords(String apiKey) {
        Call<List<Attendance>> call;
        if (selectedCourseId != null) {
            call = apiService.getAttendanceByCourse(apiKey, selectedCourseId);
        } else {
            call = apiService.getAllAttendance(apiKey);
        }
        
        call.enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    attendanceRecords.clear();
                    attendanceRecords.addAll(response.body());
                    attendanceAdapter.notifyDataSetChanged();
                    updateAttendanceStats();
                    updateListView();
                } else {
                    Toast.makeText(RecordsDashboardActivity.this, "Failed to load attendance records", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Toast.makeText(RecordsDashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void loadAbsenceRequests(String apiKey) {
        Call<List<AbsenceRequest>> call;
        if (selectedCourseId != null) {
            call = apiService.getAbsenceRequestsByCourse(apiKey, selectedCourseId);
        } else {
            call = apiService.getAllAbsenceRequests(apiKey);
        }
        
        call.enqueue(new Callback<List<AbsenceRequest>>() {
            @Override
            public void onResponse(Call<List<AbsenceRequest>> call, Response<List<AbsenceRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    absenceRequests.clear();
                    absenceRequests.addAll(response.body());
                    absenceRequestAdapter.notifyDataSetChanged();
                    updateAbsenceStats();
                    updateListView();
                } else {
                    Toast.makeText(RecordsDashboardActivity.this, "Failed to load absence requests", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<AbsenceRequest>> call, Throwable t) {
                Toast.makeText(RecordsDashboardActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateAttendanceStats() {
        int totalRecords = attendanceRecords.size();
        int presentCount = 0;
        int absentCount = 0;
        
        for (Attendance attendance : attendanceRecords) {
            if ("present".equals(attendance.getStatus())) {
                presentCount++;
            } else {
                absentCount++;
            }
        }
        
        String stats = String.format("Total Records: %d | Present: %d | Absent: %d", 
                totalRecords, presentCount, absentCount);
        textStats.setText(stats);
    }
    
    private void updateAbsenceStats() {
        int totalRequests = absenceRequests.size();
        int pendingCount = 0;
        int approvedCount = 0;
        int rejectedCount = 0;
        
        for (AbsenceRequest request : absenceRequests) {
            switch (request.getStatus()) {
                case "pending":
                    pendingCount++;
                    break;
                case "approved":
                    approvedCount++;
                    break;
                case "rejected":
                    rejectedCount++;
                    break;
            }
        }
        
        String stats = String.format("Total Requests: %d | Pending: %d | Approved: %d | Rejected: %d", 
                totalRequests, pendingCount, approvedCount, rejectedCount);
        textStats.setText(stats);
    }
    
    private void updateListView() {
        if ("attendance".equals(selectedViewType)) {
            currentAdapter = attendanceAdapter;
            recyclerViewRecords.setAdapter(attendanceAdapter);
            textEmptyState.setText("No attendance records found");
            textEmptyState.setVisibility(attendanceRecords.isEmpty() ? View.VISIBLE : View.GONE);
        } else {
            currentAdapter = absenceRequestAdapter;
            recyclerViewRecords.setAdapter(absenceRequestAdapter);
            textEmptyState.setText("No absence requests found");
            textEmptyState.setVisibility(absenceRequests.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_records_dashboard, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        
        if (id == R.id.action_export) {
            // TODO: Implement export functionality
            Toast.makeText(this, "Export feature coming soon", Toast.LENGTH_SHORT).show();
            return true;
        }
        
        if (id == R.id.action_refresh) {
            loadRecords();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}
