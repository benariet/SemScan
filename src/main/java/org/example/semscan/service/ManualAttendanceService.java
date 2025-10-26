package org.example.semscan.service;

import android.content.Context;
import org.example.semscan.constants.ApiConstants;
import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.utils.PreferencesManager;
import org.example.semscan.data.model.ManualAttendanceRequest;
import org.example.semscan.data.model.ManualAttendanceResponse;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManualAttendanceService {
    
    private ApiService apiService;
    private Context context;
    private PreferencesManager preferencesManager;
    
    public ManualAttendanceService(Context context) {
        this.context = context;
        this.preferencesManager = PreferencesManager.getInstance(context);
        ApiClient apiClient = ApiClient.getInstance(context);
        apiService = apiClient.getApiService();
    }
    
    public interface ManualAttendanceCallback {
        void onSuccess(Attendance response);
        void onError(String error);
    }
    
    public interface PendingRequestsCallback {
        void onSuccess(List<Attendance> requests);
        void onError(String error);
    }
    
    public interface PendingCountCallback {
        void onSuccess(long count);
        void onError(String error);
    }
    
    // Create manual attendance request
    public void createManualRequest(ManualAttendanceRequest request, ManualAttendanceCallback callback) {
        ApiService.CreateManualRequestRequest apiRequest = new ApiService.CreateManualRequestRequest(
            request.getSessionId(),
            request.getStudentId(),
            request.getReason(),
            request.getDeviceId()
        );
        // API key no longer required - removed authentication
        
        Call<Attendance> call = apiService.createManualRequest(apiRequest);
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to create manual request: " + response.message());
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // Get pending requests
    public void getPendingRequests(Long sessionId, PendingRequestsCallback callback) {
        // API key no longer required - removed authentication
        
        Call<List<Attendance>> call = apiService.getPendingRequests(sessionId);
        call.enqueue(new Callback<List<Attendance>>() {
            @Override
            public void onResponse(Call<List<Attendance>> call, Response<List<Attendance>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to get pending requests: " + response.message());
                }
            }
            
            @Override
            public void onFailure(Call<List<Attendance>> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // Approve request
    public void approveRequest(Long attendanceId, ManualAttendanceCallback callback) {
        // API key no longer required - removed authentication
        
        Call<Attendance> call = apiService.approveManualRequest(attendanceId);
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to approve request: " + response.message());
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
    
    // Reject request
    public void rejectRequest(Long attendanceId, ManualAttendanceCallback callback) {
        // API key no longer required - removed authentication
        
        Call<Attendance> call = apiService.rejectManualRequest(attendanceId);
        call.enqueue(new Callback<Attendance>() {
            @Override
            public void onResponse(Call<Attendance> call, Response<Attendance> response) {
                if (response.isSuccessful() && response.body() != null) {
                    callback.onSuccess(response.body());
                } else {
                    callback.onError("Failed to reject request: " + response.message());
                }
            }
            
            @Override
            public void onFailure(Call<Attendance> call, Throwable t) {
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }
}
