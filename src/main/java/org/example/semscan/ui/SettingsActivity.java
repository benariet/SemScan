package org.example.semscan.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.example.semscan.R;
import org.example.semscan.utils.PreferencesManager;

public class SettingsActivity extends AppCompatActivity {
    
    private EditText editUserId;
    private EditText editApiUrl;
    private EditText editApiKey;
    private Button btnSave;
    private Button btnClearData;
    
    private PreferencesManager preferencesManager;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        preferencesManager = PreferencesManager.getInstance(this);
        
        initializeViews();
        setupToolbar();
        setupClickListeners();
        loadCurrentSettings();
    }
    
    private void initializeViews() {
        editUserId = findViewById(R.id.edit_user_id);
        editApiUrl = findViewById(R.id.edit_api_url);
        editApiKey = findViewById(R.id.edit_api_key);
        btnSave = findViewById(R.id.btn_save);
        btnClearData = findViewById(R.id.btn_clear_data);
    }
    
    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }
    
    private void setupClickListeners() {
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveSettings();
            }
        });
        
        btnClearData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showClearDataDialog();
            }
        });
    }
    
    private void loadCurrentSettings() {
        editUserId.setText(preferencesManager.getUserId());
        editApiUrl.setText(preferencesManager.getApiBaseUrl());
        editApiKey.setText(preferencesManager.getPresenterApiKey());
    }
    
    private void saveSettings() {
        String userId = editUserId.getText().toString().trim();
        String apiUrl = editApiUrl.getText().toString().trim();
        String apiKey = editApiKey.getText().toString().trim();
        
        // Validate inputs
        if (userId.isEmpty()) {
            Toast.makeText(this, "User ID is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (apiUrl.isEmpty()) {
            Toast.makeText(this, "API URL is required", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save settings
        preferencesManager.setUserId(userId);
        preferencesManager.setApiBaseUrl(apiUrl);
        preferencesManager.setPresenterApiKey(apiKey);
        
        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show();
    }
    
    private void showClearDataDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Clear All Data")
                .setMessage("This will clear all user data and settings. Are you sure?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        clearAllData();
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }
    
    private void clearAllData() {
        preferencesManager.clearAll();
        Toast.makeText(this, "All data cleared", Toast.LENGTH_SHORT).show();
        
        // Navigate back to role picker
        finish();
    }
    
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
