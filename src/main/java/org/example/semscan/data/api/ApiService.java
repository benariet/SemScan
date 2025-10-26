package org.example.semscan.data.api;

import org.example.semscan.constants.ApiConstants;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.Seminar;
import org.example.semscan.data.model.Session;
import org.example.semscan.data.model.StudentSessionResponse;
import org.example.semscan.data.model.User;
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
    
    // Sessions (No authentication required)
    @POST("api/v1/sessions")
    Call<Session> createSession(@Body CreateSessionRequest request);
    
    @PATCH("api/v1/sessions/{sessionId}/close")
    Call<Session> closeSession(@Path("sessionId") Long sessionId);
    
    @GET("api/v1/sessions/open")
    Call<List<Session>> getOpenSessions();
    
    // Student-specific endpoint (no API key required)
    @GET("api/v1/student/sessions/open")
    Call<StudentSessionResponse> getOpenSessionsForStudent();
    
    @GET("api/v1/student/user/{userId}")
    Call<User> getStudentUserById(@Path("userId") Long userId);
    
    // Removed getSessions - not needed for lean MVP
    
    // Attendance (No authentication required)
    @POST("api/v1/attendance")
    Call<Attendance> submitAttendance(@Body SubmitAttendanceRequest request);
    
    @GET("api/v1/attendance")
    Call<List<Attendance>> getAttendance(@Query("sessionId") Long sessionId);
    
    // Manual attendance requests
    @POST("api/v1/attendance/manual-request")
    Call<Attendance> createManualRequest(@Body CreateManualRequestRequest request);
    
    @GET("api/v1/attendance/pending-requests")
    Call<List<Attendance>> getPendingRequests(@Query("sessionId") Long sessionId);
    
    @POST("api/v1/attendance/{attendanceId}/approve")
    Call<Attendance> approveManualRequest(@Path("attendanceId") Long attendanceId);
    
    @POST("api/v1/attendance/{attendanceId}/reject")
    Call<Attendance> rejectManualRequest(@Path("attendanceId") Long attendanceId);
    
    
    // Seminars (No authentication required)
    @GET("api/v1/seminars")
    Call<List<Seminar>> getSeminars();
    
    @POST("api/v1/seminars")
    Call<Seminar> createSeminar(@Body CreateSeminarRequest request);
    
    // Users
    @GET("api/v1/users/{userId}")
    Call<User> getUserById(@Path("userId") Long userId);
    
    
        // Export (No authentication required)
        @GET("api/v1/export/xlsx")
        Call<okhttp3.ResponseBody> exportXlsx(@Query("sessionId") Long sessionId);

        @GET("api/v1/export/csv")
        Call<okhttp3.ResponseBody> exportCsv(@Query("sessionId") Long sessionId);

        // Logging (No authentication required)
        @POST("api/v1/logs")
        Call<org.example.semscan.utils.ServerLogger.LogResponse> sendLogs(
                @Body org.example.semscan.utils.ServerLogger.LogRequest request
        );

        // =============================
        // Presenter Seminars (MVP)
        // =============================


        // List presenter seminars (tiles) - No authentication required
        @GET("api/v1/presenters/{presenterId}/seminars")
        Call<java.util.List<PresenterSeminarDto>> getPresenterSeminars(@Path("presenterId") Long presenterId);

        // Create presenter seminar - No authentication required
        @POST("api/v1/presenters/{presenterId}/seminars")
        Call<PresenterSeminarDto> createPresenterSeminar(
                @Path("presenterId") Long presenterId,
                @Body CreatePresenterSeminarRequest body
        );

        // Optional: delete presenter seminar - No authentication required
        @DELETE("api/v1/presenters/{presenterId}/seminars/{seminarId}")
        Call<Void> deletePresenterSeminar(
                @Path("presenterId") Long presenterId,
                @Path("seminarId") Long seminarId
        );
    
    // Request/Response DTOs
    class CreateSessionRequest {
        public Long seminarId;    // Required: ID of the seminar to create session for
        public String startTime;    // Required: ISO 8601 formatted start time
        public String status;       // Required: Session status (e.g., "OPEN")
        // Note: sessionId is NOT included - server generates it automatically
        
        public CreateSessionRequest(Long seminarId, String startTime, String status) {
            this.seminarId = seminarId;
            this.startTime = startTime;
            this.status = status;
        }
    }
    
    class SubmitAttendanceRequest {
        public Long sessionId;
        public Long studentId;
        public long timestampMs;
        
        public SubmitAttendanceRequest(Long sessionId, Long studentId, long timestampMs) {
            this.sessionId = sessionId;
            this.studentId = studentId;
            this.timestampMs = timestampMs;
        }
    }
    
    class CreateSeminarRequest {
        public String seminarName;
        public String seminarCode;
        public String description;
        public Long presenterId;
        
        public CreateSeminarRequest(String seminarName, String seminarCode, String description, Long presenterId) {
            this.seminarName = seminarName;
            this.seminarCode = seminarCode;
            this.description = description;
            this.presenterId = presenterId;
        }
    }
    
    class CreateManualRequestRequest {
        public Long sessionId;
        public Long studentId;
        public String reason;
        public String deviceId;
        
        public CreateManualRequestRequest(Long sessionId, Long studentId, String reason, String deviceId) {
            this.sessionId = sessionId;
            this.studentId = studentId;
            this.reason = reason;
            this.deviceId = deviceId;
        }
    }

    // =============================
    // DTOs for Presenter Seminars
    // =============================


    class PresenterSeminarSlotDto {
        public int weekday;     // 0=Sun .. 6=Sat
        public int startHour;   // 0..23
        public int endHour;     // 1..24, > startHour
    }

    class PresenterSeminarDto {
        public Long id;
        public Long presenterId;
        public String seminarName;
        public java.util.List<PresenterSeminarSlotDto> slots;
        public String createdAt;
    }

    class CreatePresenterSeminarRequest {
        public String seminarName;
        public java.util.List<PresenterSeminarSlotDto> slots;

        public CreatePresenterSeminarRequest(String seminarName, java.util.List<PresenterSeminarSlotDto> slots) {
            this.seminarName = seminarName;
            this.slots = slots;
        }
    }
    
}
