package org.example.semscan.data.api;

import org.example.semscan.constants.ApiConstants;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.Seminar;
import org.example.semscan.data.model.Session;
import org.example.semscan.utils.Logger;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {
    
    // Sessions (Presenter endpoints require X-API-Key header)
    @POST("api/v1/sessions")
    Call<Session> createSession(
            @Header("x-api-key") String apiKey,
            @Body CreateSessionRequest request
    );
    
    @PATCH("api/v1/sessions/{sessionId}/close")
    Call<Session> closeSession(
            @Header("x-api-key") String apiKey,
            @Path("sessionId") String sessionId
    );
    
    @GET("api/v1/sessions/open")
    Call<List<Session>> getOpenSessions(@Header("x-api-key") String apiKey);
    
    // Removed getSessions - not needed for lean MVP
    
    // Attendance
    @POST("api/v1/attendance")
    Call<Attendance> submitAttendance(@Header("x-api-key") String apiKey, @Body SubmitAttendanceRequest request);
    
    @GET("api/v1/attendance")
    Call<List<Attendance>> getAttendance(
            @Header("x-api-key") String apiKey,
            @Query("sessionId") String sessionId
    );
    
    // Manual attendance requests
    @POST("api/v1/attendance/manual-request")
    Call<Attendance> createManualRequest(@Header("x-api-key") String apiKey, @Body CreateManualRequestRequest request);
    
    @GET("api/v1/attendance/pending-requests")
    Call<List<Attendance>> getPendingRequests(
            @Header("x-api-key") String apiKey,
            @Query("sessionId") String sessionId
    );
    
    @POST("api/v1/attendance/{attendanceId}/approve")
    Call<Attendance> approveManualRequest(
            @Header("x-api-key") String apiKey,
            @Path("attendanceId") String attendanceId
    );
    
    @POST("api/v1/attendance/{attendanceId}/reject")
    Call<Attendance> rejectManualRequest(
            @Header("x-api-key") String apiKey,
            @Path("attendanceId") String attendanceId
    );
    
    
    // Seminars
    @GET("api/v1/seminars")
    Call<List<Seminar>> getSeminars(@Header("x-api-key") String apiKey);
    
    @POST("api/v1/seminars")
    Call<Seminar> createSeminar(
            @Header("x-api-key") String apiKey,
            @Body CreateSeminarRequest request
    );
    
    
    // Export
    @GET("api/v1/export/xlsx")
    Call<okhttp3.ResponseBody> exportXlsx(
            @Header("x-api-key") String apiKey,
            @Query("sessionId") String sessionId
    );
    
    @GET("api/v1/export/csv")
    Call<okhttp3.ResponseBody> exportCsv(
            @Header("x-api-key") String apiKey,
            @Query("sessionId") String sessionId
    );
    
    // Request/Response DTOs
    class CreateSessionRequest {
        public String seminarId;    // Required: ID of the seminar to create session for
        public String startTime;    // Required: ISO 8601 formatted start time
        public String status;       // Required: Session status (e.g., "OPEN")
        // Note: sessionId is NOT included - server generates it automatically
        
        public CreateSessionRequest(String seminarId, String startTime, String status) {
            this.seminarId = seminarId;
            this.startTime = startTime;
            this.status = status;
        }
    }
    
    class SubmitAttendanceRequest {
        public String sessionId;
        public String studentId;
        public long timestampMs;
        
        public SubmitAttendanceRequest(String sessionId, String studentId, long timestampMs) {
            this.sessionId = sessionId;
            this.studentId = studentId;
            this.timestampMs = timestampMs;
        }
    }
    
    class CreateSeminarRequest {
        public String seminarName;
        public String seminarCode;
        public String description;
        public String presenterId;
        
        public CreateSeminarRequest(String seminarName, String seminarCode, String description, String presenterId) {
            this.seminarName = seminarName;
            this.seminarCode = seminarCode;
            this.description = description;
            this.presenterId = presenterId;
        }
    }
    
    class CreateManualRequestRequest {
        public String sessionId;
        public String studentId;
        public String reason;
        public String deviceId;
        
        public CreateManualRequestRequest(String sessionId, String studentId, String reason, String deviceId) {
            this.sessionId = sessionId;
            this.studentId = studentId;
            this.reason = reason;
            this.deviceId = deviceId;
        }
    }
    
}
