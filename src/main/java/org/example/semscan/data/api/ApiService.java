package org.example.semscan.data.api;

import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.Seminar;
import org.example.semscan.data.model.Session;

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
            @Header("X-API-Key") String apiKey,
            @Body CreateSessionRequest request
    );
    
    @PATCH("api/v1/sessions/{sessionId}/close")
    Call<Session> closeSession(
            @Header("X-API-Key") String apiKey,
            @Path("sessionId") String sessionId
    );
    
    // Removed getSessions - not needed for lean MVP
    
    // Attendance
    @POST("api/v1/attendance")
    Call<Attendance> submitAttendance(@Body SubmitAttendanceRequest request);
    
    @GET("api/v1/attendance")
    Call<List<Attendance>> getAttendance(
            @Header("X-API-Key") String apiKey,
            @Query("sessionId") String sessionId
    );
    
    
    // Seminars
    @GET("api/v1/seminars")
    Call<List<Seminar>> getSeminars(@Header("X-API-Key") String apiKey);
    
    
    // Export
    @GET("api/v1/export/xlsx")
    Call<okhttp3.ResponseBody> exportXlsx(
            @Header("X-API-Key") String apiKey,
            @Query("sessionId") String sessionId
    );
    
    @GET("api/v1/export/csv")
    Call<okhttp3.ResponseBody> exportCsv(
            @Header("X-API-Key") String apiKey,
            @Query("sessionId") String sessionId
    );
    
    // Request/Response DTOs
    class CreateSessionRequest {
        public String seminarId;
        public long startTime;
        
        public CreateSessionRequest(String seminarId, long startTime) {
            this.seminarId = seminarId;
            this.startTime = startTime;
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
    
}
