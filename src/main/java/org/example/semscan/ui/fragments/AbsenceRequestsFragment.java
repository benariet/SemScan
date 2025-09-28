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
import org.example.semscan.data.model.AbsenceRequest;
import org.example.semscan.data.model.Session;
import org.example.semscan.ui.adapters.AbsenceRequestAdapter;
import org.example.semscan.utils.PreferencesManager;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AbsenceRequestsFragment extends Fragment {
    
    private Spinner spinnerSession;
    private RecyclerView recyclerAbsenceRequests;
    private LinearLayout layoutEmpty;
    
    private PreferencesManager preferencesManager;
    private ApiService apiService;
    private AbsenceRequestAdapter absenceRequestAdapter;
    
    private List<Session> sessions = new ArrayList<>();
    private List<AbsenceRequest> absenceRequests = new ArrayList<>();
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_absence_requests, container, false);
        
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
        recyclerAbsenceRequests = view.findViewById(R.id.recycler_absence_requests);
        layoutEmpty = view.findViewById(R.id.layout_empty);
    }
    
    private void setupRecyclerView() {
        absenceRequestAdapter = new AbsenceRequestAdapter(absenceRequests, new AbsenceRequestAdapter.OnActionClickListener() {
            @Override
            public void onApprove(AbsenceRequest absenceRequest) {
                updateAbsenceRequest(absenceRequest.getId(), "approved");
            }
            
            @Override
            public void onReject(AbsenceRequest absenceRequest) {
                updateAbsenceRequest(absenceRequest.getId(), "rejected");
            }
        });
        recyclerAbsenceRequests.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerAbsenceRequests.setAdapter(absenceRequestAdapter);
    }
    
    private void setupSpinner() {
        spinnerSession.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                if (position > 0) { // Skip "Select Session" placeholder
                    Session selectedSession = sessions.get(position - 1);
                    loadAbsenceRequests(selectedSession.getSessionId());
                } else {
                    absenceRequests.clear();
                    absenceRequestAdapter.notifyDataSetChanged();
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
    
    private void loadAbsenceRequests(String sessionId) {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null) {
            return;
        }
        
        Call<List<AbsenceRequest>> call = apiService.getAbsenceRequests(apiKey, null, "submitted");
        call.enqueue(new Callback<List<AbsenceRequest>>() {
            @Override
            public void onResponse(Call<List<AbsenceRequest>> call, Response<List<AbsenceRequest>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    absenceRequests.clear();
                    // Filter by session ID
                    for (AbsenceRequest request : response.body()) {
                        if (sessionId.equals(request.getSessionId())) {
                            absenceRequests.add(request);
                        }
                    }
                    absenceRequestAdapter.notifyDataSetChanged();
                    
                    if (absenceRequests.isEmpty()) {
                        showEmptyState();
                    } else {
                        hideEmptyState();
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to load absence requests", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<List<AbsenceRequest>> call, Throwable t) {
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void updateAbsenceRequest(String requestId, String status) {
        String apiKey = preferencesManager.getTeacherApiKey();
        if (apiKey == null) {
            Toast.makeText(requireContext(), "API key not configured", Toast.LENGTH_SHORT).show();
            return;
        }
        
        ApiService.UpdateAbsenceRequest request = new ApiService.UpdateAbsenceRequest(status);
        
        Call<AbsenceRequest> call = apiService.updateAbsenceRequest(apiKey, requestId, request);
        call.enqueue(new Callback<AbsenceRequest>() {
            @Override
            public void onResponse(Call<AbsenceRequest> call, Response<AbsenceRequest> response) {
                if (response.isSuccessful()) {
                    String message = status.equals("approved") ? "Absence request approved" : "Absence request rejected";
                    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
                    
                    // Refresh the list
                    if (spinnerSession.getSelectedItemPosition() > 0) {
                        Session selectedSession = sessions.get(spinnerSession.getSelectedItemPosition() - 1);
                        loadAbsenceRequests(selectedSession.getSessionId());
                    }
                } else {
                    Toast.makeText(requireContext(), "Failed to update absence request", Toast.LENGTH_SHORT).show();
                }
            }
            
            @Override
            public void onFailure(Call<AbsenceRequest> call, Throwable t) {
                Toast.makeText(requireContext(), "Network error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void showEmptyState() {
        layoutEmpty.setVisibility(View.VISIBLE);
        recyclerAbsenceRequests.setVisibility(View.GONE);
    }
    
    private void hideEmptyState() {
        layoutEmpty.setVisibility(View.GONE);
        recyclerAbsenceRequests.setVisibility(View.VISIBLE);
    }
}


