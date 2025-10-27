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
import org.example.semscan.data.model.Seminar;
import org.example.semscan.utils.Logger;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AddSeminarActivity extends AppCompatActivity {

    private EditText etSeminarName;
    private EditText etSeminarCode;
    private EditText etSeminarDescription;
    private Button btnSave;
    private Button btnCancel;
    private ApiService apiService;
    private PreferencesManager preferencesManager;
    private Long editingSeminarId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_seminar);

        apiService = ApiClient.getInstance(this).getApiService();
        preferencesManager = PreferencesManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Create Seminar");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        etSeminarName = findViewById(R.id.et_seminar_name);
        etSeminarCode = findViewById(R.id.et_seminar_code);
        etSeminarDescription = findViewById(R.id.et_seminar_description);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        btnCancel.setOnClickListener(v -> finish());
        btnSave.setOnClickListener(v -> save());
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void save() {
        String seminarName = etSeminarName.getText().toString().trim();
        String seminarCode = etSeminarCode.getText().toString().trim();
        String seminarDescription = etSeminarDescription.getText().toString().trim();

        if (TextUtils.isEmpty(seminarName)) {
            etSeminarName.setError("Seminar name is required");
            return;
        }

        Long presenterId = preferencesManager.getUserId();
        if (presenterId == null || presenterId <= 0) {
            Logger.e(Logger.TAG_UI, "Cannot create seminar - missing presenter ID");
            Toast.makeText(this, "Presenter ID not found. Please check settings.", Toast.LENGTH_LONG).show();
            return;
        }

        ApiService.CreateSeminarRequest body = new ApiService.CreateSeminarRequest(
                seminarName,
                seminarCode.isEmpty() ? null : seminarCode,
                seminarDescription.isEmpty() ? null : seminarDescription,
                presenterId
        );

        Logger.userAction("Create Seminar", "name=" + seminarName + ", code=" + seminarCode);
        Logger.api("POST", "api/v1/seminars", "creating base seminar record");

        apiService.createSeminar(body).enqueue(new Callback<Seminar>() {
            @Override
            public void onResponse(Call<Seminar> call, Response<Seminar> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AddSeminarActivity.this, "Seminar saved", Toast.LENGTH_SHORT).show();
                    Logger.apiResponse("POST", "api/v1/seminars", response.code(), "Seminar created");
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Logger.apiError("POST", "api/v1/seminars", response.code(), "Failed to create seminar");
                    Toast.makeText(AddSeminarActivity.this, "Failed to save seminar", Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Seminar> call, Throwable t) {
                Logger.e(Logger.TAG_UI, "Seminar creation failed", t);
                Toast.makeText(AddSeminarActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}


