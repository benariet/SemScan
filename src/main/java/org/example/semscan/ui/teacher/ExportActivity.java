package org.example.semscan.ui.teacher;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Session;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExportActivity extends AppCompatActivity {
    
    private RadioGroup radioGroupFormat;
    private Button btnExport;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    
    private String currentSessionId;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_export);
        
        Logger.i(Logger.TAG_UI, "ExportActivity created");
        
        preferencesManager = PreferencesManager.getInstance(this);
        apiService = ApiClient.getInstance(this).getApiService();
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
    }
    
    private void initializeViews() {
        radioGroupFormat = findViewById(R.id.radio_group_format);
        btnExport = findViewById(R.id.btn_export);
        
        // Get current session ID from intent (passed from QR display)
        currentSessionId = getIntent().getStringExtra("sessionId");
        
        Logger.d(Logger.TAG_UI, "Export activity initialized with session ID: " + currentSessionId);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupClickListeners() {
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                exportData();
            }
        });
    }
    
    // Simplified MVP: No session loading needed - export current session only
    
    
    private void exportData() {
        Logger.userAction("Export Data", "User clicked export button");
        
        String apiKey = preferencesManager.getPresenterApiKey();
        if (apiKey == null) {
            Logger.e(Logger.TAG_UI, "Export failed - no API key configured");
            Toast.makeText(this, "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentSessionId == null) {
            Logger.e(Logger.TAG_UI, "Export failed - no session ID available");
            Toast.makeText(this, "No session data available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        boolean isExcel = radioGroupFormat.getCheckedRadioButtonId() == R.id.radio_excel;
        String format = isExcel ? "Excel (.xlsx)" : "CSV (.csv)";
        
        Logger.i(Logger.TAG_UI, "Starting export - Session ID: " + currentSessionId + ", Format: " + format);
        exportSessionData(apiKey, currentSessionId, isExcel);
    }
    
    private void exportSessionData(String apiKey, String sessionId, boolean isExcel) {
        Call<ResponseBody> call;
        String filename;
        String mimeType;
        String endpoint;
        
        if (isExcel) {
            call = apiService.exportXlsx(apiKey, sessionId);
            filename = "attendance_" + sessionId + ".xlsx";
            mimeType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            endpoint = "api/v1/export/xlsx";
        } else {
            call = apiService.exportCsv(apiKey, sessionId);
            filename = "attendance_" + sessionId + ".csv";
            mimeType = "text/csv";
            endpoint = "api/v1/export/csv";
        }
        
        Logger.api("GET", endpoint, "Session ID: " + sessionId);
        
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Logger.apiResponse("GET", endpoint, response.code(), "Export data received successfully");
                        
                        // Save file to external storage
                        File file = new File(getExternalFilesDir(null), filename);
                        FileOutputStream fos = new FileOutputStream(file);
                        fos.write(response.body().bytes());
                        fos.close();
                        
                        Logger.i(Logger.TAG_UI, "Export file saved: " + filename + " (" + file.length() + " bytes)");
                        
                        // Share the file
                        shareFile(file, mimeType);
                        
                        Logger.i(Logger.TAG_UI, "Export completed successfully - Session: " + sessionId);
                        Toast.makeText(ExportActivity.this, "Export successful", Toast.LENGTH_SHORT).show();
                    } catch (IOException e) {
                        Logger.e(Logger.TAG_UI, "Failed to save export file", e);
                        Toast.makeText(ExportActivity.this, "Failed to save file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Logger.apiError("GET", endpoint, response.code(), "Export request failed");
                    Toast.makeText(ExportActivity.this, "Export failed", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Export network failure", t);
                Toast.makeText(ExportActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void shareFile(File file, String mimeType) {
        Logger.i(Logger.TAG_UI, "Sharing export file: " + file.getName() + " (" + mimeType + ")");
        
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


