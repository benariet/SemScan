package org.example.semscan.ui.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.example.semscan.R;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.Session;
import org.example.semscan.ui.adapters.AttendanceAdapter;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PresentAttendanceFragment extends Fragment {
    
    private Spinner spinnerSession;
    private RecyclerView recyclerAttendance;
    private LinearLayout layoutEmpty;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private AttendanceAdapter attendanceAdapter;
    
    private List<Session> sessions = new ArrayList<>();
    private List<Attendance> attendanceList = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_present_attendance, container, false);
        
        preferencesManager = PreferencesManager.getInstance(requireContext());
        apiService = ApiClient.getInstance().getApiService();
        
        initializeViews(view);
        setupRecyclerView();
        setupSpinner();
        
        loadSessions();
        
        return view;
    }
    
    private void initializeViews(View view) {
        spinnerSession = view.findViewById(R.id.spinner_session);
        recyclerAttendance = view.findViewById(R.id.recycler_attendance);
        layoutEmpty = view.findViewById(R.id.layout_empty);
    }
    
    private void setupRecyclerView() {
        attendanceAdapter = new AttendanceAdapter(attendanceList);
        recyclerAttendance.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAttendance.setAdapter(attendanceAdapter);
    }
    
    private void setupSpinner() {
        spinnerSession.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0) { // Skip "Select Session" placeholder
                    Session selectedSession = sessions.get(position - 1);
                    loadAttendance(selectedSession.getSessionId());
                } else {
                    attendanceList.clear();
                    attendanceAdapter.notifyDataSetChanged();
                    showEmptyState();
                }
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void loadSessions() {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null) {
            Toast.makeText(requireContext(), "API key not configured", Toast.LENGTH_SHORT).show();
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
                    Toast.makeText(requireContext(), "Failed to load sessions", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Session>> call, Throwable t) {
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
        
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), 
                android.R.layout.simple_spinner_item, sessionNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSession.setAdapter(adapter);
    }
    
    private void loadAttendance(String sessionId) {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null) {
            return;
        }
        
        Call<List<Attendance>> call = apiService.getAttendance(apiKey, sessionId);
        call.enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    attendanceList.clear();
                    attendanceList.addAll(response.body());
                    attendanceAdapter.notifyDataSetChanged();
                    
                    if (attendanceList.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load attendance", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerAttendance.setVisibility(View.GONE);
    }
    
    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        recyclerAttendance.setVisibility(View.VISIBLE);
    }
}
