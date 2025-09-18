package org.example.semscan.ui.teacher;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Session;
import org.example.semscan.utils.PreferencesManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExportActivity extends AppCompatActivity {
    
    private RadioGroup radioGroupExportType;
    private RadioGroup radioGroupFormat;
    private View layoutSessionSelection;
    private View layoutDateRange;
    private Spinner spinnerSession;
    private Button btnFromDate;
    private Button btnToDate;
    private Button btnExport;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    private List<Session> sessions = new ArrayList<>();
    private Date fromDate;
    private Date toDate;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance().getApiService();
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        
        loadSessions();
    }
    
    private void initializeViews() {
        radioGroupExportType = findViewById(R.id.radio_group_export_type);
        radioGroupFormat = findViewById(R.id.radio_group_format);
        layoutSessionSelection = findViewById(R.id.layout_session_selection);
        layoutDateRange = findViewById(R.id.layout_date_range);
        spinnerSession = findViewById(R.id.spinner_session);
        btnFromDate = findViewById(R.id.btn_from_date);
        btnToDate = findViewById(R.id.btn_to_date);
        btnExport = findViewById(R.id.btn_export);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupClickListeners() {
        radioGroupExportType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radio_session) {
                    layoutSessionSelection.setVisibility(View.VISIBLE);
                    layoutDateRange.setVisibility(View.GONE);
                } else if (checkedId == R.id.radio_date_range) {
                    layoutSessionSelection.setVisibility(View.GONE);
                    layoutDateRange.setVisibility(View.VISIBLE);
                }
            }
        });
        
        btnFromDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(true);
            }
        });
        
        btnToDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDatePicker(false);
            }
        });
        
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportData();
            }
        });
    }
    
    private void loadSessions() {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Load sessions for the last 30 days
        long from = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        long to = System.currentTimeMillis();
        
        Call<List<Session>> call = apiService.getSessions(apiKey, null, from, to);
        call.enqueue(new Callback<List<Session>>() {
            @Override
            public void onResponse(Call<List<Session>> call, Response<List<Session>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    sessions.clear();
                    sessions.addAll(response.body());
                    updateSessionSpinner();
                } else {
                    Toast.makeText(ExportActivity.this, "Failed to load sessions", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Session>> call, Throwable t) {
                Toast.makeText(ExportActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateSessionSpinner() {
        List<String> sessionNames = new ArrayList<>();
        sessionNames.add("Select Session");
        
        for (Session session : sessions) {
            String sessionName = "Session " + session.getSessionId() + 
                    " (" + new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                    .format(new Date(session.getStartTime())) + ")";
            sessionNames.add(sessionName);
        }
        
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, 
                android.R.layout.simple_spinner_item, sessionNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSession.setAdapter(adapter);
    }
    
    private void showDatePicker(boolean isFromDate) {
        Calendar calendar = Calendar.getInstance();
        if (isFromDate && fromDate != null) {
            calendar.setTime(fromDate);
        } else if (!isFromDate && toDate != null) {
            calendar.setTime(toDate);
        }
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(year, month, dayOfMonth);
                    Date selectedDate = selectedCalendar.getTime();
                    
                    if (isFromDate) {
                        fromDate = selectedDate;
                        btnFromDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate));
                    } else {
                        toDate = selectedDate;
                        btnToDate.setText(new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(selectedDate));
                    }
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        
        datePickerDialog.show();
    }
    
    private void exportData() {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null) {
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean isExcel = radioGroupFormat.getCheckedRadioButtonId() == R.id.radio_excel;
        boolean isSessionExport = radioGroupExportType.getCheckedRadioButtonId() == R.id.radio_session;
        
        if (isSessionExport) {
            if (spinnerSession.getSelectedItemPosition() == 0) {
                Toast.makeText(this, "Please select a session", Toast.LENGTH_SHORT).show();
                return;
            }
            
            Session selectedSession = sessions.get(spinnerSession.getSelectedItemPosition() - 1);
            exportSessionData(apiKey, selectedSession.getSessionId(), isExcel);
        } else {
            if (fromDate == null || toDate == null) {
                Toast.makeText(this, "Please select date range", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (fromDate.after(toDate)) {
                Toast.makeText(this, "From date must be before to date", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // For date range export, we'll export all sessions in the range
            // This is a simplified implementation - in a real app, you might want to aggregate data
            Toast.makeText(this, "Date range export not implemented yet", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void exportSessionData(String apiKey, String sessionId, boolean isExcel) {
        Call<ResponseBody> call;
        String filename;
        String mimeType;
        
        if (isExcel) {
            call = apiService.exportXlsx(apiKey, sessionId);
            filename = "attendance_" + sessionId + ".xlsx";
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        } else {
            call = apiService.exportCsv(apiKey, sessionId);
            filename = "attendance_" + sessionId + ".csv";
            mimeType = "text/csv";
        }
        
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        // Save file to external storage
                        File file = new File(getExternalFilesDir(null), filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(response.body().bytes());
                        fos.close();
                        
                        // Share the file
                        shareFile(file, mimeType);
                        
                        Toast.makeText(ExportActivity.this, "Export successful", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Toast.makeText(ExportActivity.this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ExportActivity.this, "Export failed", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(ExportActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void shareFile(File file, String mimeType) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(mimeType);
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Attendance Export");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Attendance data export from SemScan");
        
        startActivity(Intent.createChooser(shareIntent, "Share attendance data"));
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}

