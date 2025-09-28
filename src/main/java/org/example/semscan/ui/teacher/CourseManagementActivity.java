package org.example.semscan.ui.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Course;
import org.example.semscan.ui.adapters.CourseAdapter;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CourseManagementActivity extends AppCompatActivity {
    
    private ListView listViewCourses;
    private Button btnAddCourse;
    private CourseAdapter courseAdapter;
    private List<Course> courses;
    private ApiService apiService;
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_management);
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance().getApiService();
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        loadCourses();
    }
    
    private void initializeViews() {
        listViewCourses = findViewById(R.id.listView_courses);
        btnAddCourse = findViewById(R.id.btn_add_course);
        courses = new ArrayList<>();
        courseAdapter = new CourseAdapter(this, courses);
        listViewCourses.setAdapter(courseAdapter);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Manage Courses");
        }
    }
    
    private void setupClickListeners() {
        btnAddCourse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAddCourseDialog();
            }
        });
        
        listViewCourses.setOnItemClickListener((parent, view, position, id) -> {
            Course course = courses.get(position);
            showEditCourseDialog(course);
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
                } else {
                    Toast.makeText(CourseManagementActivity.this, "Failed to load courses", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Course>> call, Throwable t) {
                Toast.makeText(CourseManagementActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showAddCourseDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);
        
        EditText editCourseName = dialogView.findViewById(R.id.edit_course_name);
        EditText editCourseCode = dialogView.findViewById(R.id.edit_course_code);
        EditText editCourseDescription = dialogView.findViewById(R.id.edit_course_description);
        
        builder.setView(dialogView)
                .setTitle("Add New Course")
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = editCourseName.getText().toString().trim();
                    String code = editCourseCode.getText().toString().trim();
                    String description = editCourseDescription.getText().toString().trim();
                    
                    if (name.isEmpty() || code.isEmpty()) {
                        Toast.makeText(this, "Course name and code are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    addCourse(name, code, description);
                })
                .setNegativeButton("Cancel", null);
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showEditCourseDialog(Course course) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_course, null);
        
        EditText editCourseName = dialogView.findViewById(R.id.edit_course_name);
        EditText editCourseCode = dialogView.findViewById(R.id.edit_course_code);
        EditText editCourseDescription = dialogView.findViewById(R.id.edit_course_description);
        
        // Pre-fill with existing data
        editCourseName.setText(course.getCourseName());
        editCourseCode.setText(course.getCourseCode());
        editCourseDescription.setText(course.getDescription());
        
        builder.setView(dialogView)
                .setTitle("Edit Course")
                .setPositiveButton("Update", (dialog, which) -> {
                    String name = editCourseName.getText().toString().trim();
                    String code = editCourseCode.getText().toString().trim();
                    String description = editCourseDescription.getText().toString().trim();
                    
                    if (name.isEmpty() || code.isEmpty()) {
                        Toast.makeText(this, "Course name and code are required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    updateCourse(course.getCourseId(), name, code, description);
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Delete", (dialog, which) -> {
                    showDeleteConfirmDialog(course);
                });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void showDeleteConfirmDialog(Course course) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to delete " + course.getCourseName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse(course.getCourseId()))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void addCourse(String name, String code, String description) {
        String apiKey = preferencesManager.getTeacherApiKey();
        Course newCourse = new Course(null, name, code, description, System.currentTimeMillis());
        
        Call<Course> call = apiService.createCourse(apiKey, newCourse);
        call.enqueue(new Callback<Course>() {
            @Override
            public void onResponse(Call<Course> call, Response<Course> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CourseManagementActivity.this, "Course added successfully", Toast.LENGTH_SHORT).show();
                    loadCourses(); // Refresh the list
                } else {
                    Toast.makeText(CourseManagementActivity.this, "Failed to add course", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Course> call, Throwable t) {
                Toast.makeText(CourseManagementActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateCourse(String courseId, String name, String code, String description) {
        String apiKey = preferencesManager.getTeacherApiKey();
        Course updatedCourse = new Course(courseId, name, code, description, System.currentTimeMillis());
        
        Call<Course> call = apiService.updateCourse(apiKey, courseId, updatedCourse);
        call.enqueue(new Callback<Course>() {
            @Override
            public void onResponse(Call<Course> call, Response<Course> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CourseManagementActivity.this, "Course updated successfully", Toast.LENGTH_SHORT).show();
                    loadCourses(); // Refresh the list
                } else {
                    Toast.makeText(CourseManagementActivity.this, "Failed to update course", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Course> call, Throwable t) {
                Toast.makeText(CourseManagementActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void deleteCourse(String courseId) {
        String apiKey = preferencesManager.getTeacherApiKey();
        
        Call<Void> call = apiService.deleteCourse(apiKey, courseId);
        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(CourseManagementActivity.this, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                    loadCourses(); // Refresh the list
                } else {
                    Toast.makeText(CourseManagementActivity.this, "Failed to delete course", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(CourseManagementActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_course_management, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        
        if (id == R.id.action_refresh) {
            loadCourses();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
}


