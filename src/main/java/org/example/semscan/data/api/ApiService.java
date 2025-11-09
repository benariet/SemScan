package org.example.semscan.data.api;

import com.google.gson.annotations.SerializedName;

import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.ManualAttendanceResponse;
import org.example.semscan.data.model.Session;
import org.example.semscan.data.model.User;
import org.example.semscan.utils.ServerLogger;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // =============================
    // Sessions
    // =============================

    @GET("api/v1/sessions/open")
    Call<List<Session>> getOpenSessions();

    @PATCH("api/v1/sessions/{sessionId}/close")
    Call<Session> closeSession(@Path("sessionId") Long sessionId);

    // =============================
    // Attendance
    // =============================

    @POST("api/v1/attendance")
    Call<Attendance> submitAttendance(@Body SubmitAttendanceRequest request);

    @GET("api/v1/attendance")
    Call<List<Attendance>> getAttendance(@Query("sessionId") Long sessionId);

    @GET("api/v1/attendance/has-attended")
    Call<Boolean> hasAttended(@Query("sessionId") Long sessionId,
                              @Query("studentUsername") String studentUsername);

    // =============================
    // Manual attendance workflow
    // =============================

    @POST("api/v1/attendance/manual")
    Call<ManualAttendanceResponse> createManualRequest(@Body CreateManualRequestRequest request);

    @GET("api/v1/attendance/manual/pending-requests")
    Call<List<ManualAttendanceResponse>> getPendingManualRequests(@Query("sessionId") Long sessionId);

    @POST("api/v1/attendance/manual/{attendanceId}/approve")
    Call<ManualAttendanceResponse> approveManualRequest(@Path("attendanceId") Long attendanceId,
                                                        @Query("approvedBy") String presenterUsername);

    @POST("api/v1/attendance/manual/{attendanceId}/reject")
    Call<ManualAttendanceResponse> rejectManualRequest(@Path("attendanceId") Long attendanceId,
                                                       @Query("approvedBy") String presenterUsername);

    // =============================
    // Presenter home + slot catalog
    // =============================

    @GET("api/v1/presenters/{username}/home")
    Call<PresenterHomeResponse> getPresenterHome(@Path("username") String username);

    @POST("api/v1/presenters/{username}/home/slots/{slotId}/register")
    Call<PresenterRegisterResponse> registerForSlot(
            @Path("username") String username,
            @Path("slotId") Long slotId,
            @Body PresenterRegisterRequest body
    );

    @DELETE("api/v1/presenters/{username}/home/slots/{slotId}/register")
    Call<Void> cancelSlotRegistration(
            @Path("username") String username,
            @Path("slotId") Long slotId
    );

    @POST("api/v1/presenters/{username}/home/slots/{slotId}/attendance/open")
    Call<PresenterAttendanceOpenResponse> openPresenterAttendance(
            @Path("username") String username,
            @Path("slotId") Long slotId
    );

    @GET("api/v1/presenters/{username}/home/slots/{slotId}/attendance/qr")
    Call<PresenterAttendanceOpenResponse> getPresenterAttendanceQr(
            @Path("username") String username,
            @Path("slotId") Long slotId
    );

    @GET("api/v1/slots")
    Call<List<SlotCard>> getPublicSlots();

    // =============================
    // Logging
    // =============================

    @POST("api/v1/logs")
    Call<ServerLogger.LogResponse> sendLogs(@Body ServerLogger.LogRequest request);

    // =============================
    // Authentication
    // =============================

    @POST("api/v1/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    // =============================
    // User profile
    // =============================

    @POST("api/v1/users/exists")
    Call<UserExistsResponse> checkUserExists(@Body UserExistsRequest request);

    @GET("api/v1/users/username/{bguUsername}")
    Call<UserProfileResponse> getUserProfile(@Path("bguUsername") String bguUsername);

    @POST("api/v1/users")
    Call<User> upsertUser(@Body UserProfileUpdateRequest request);

    // =============================
    // Export
    // =============================

    @GET("api/v1/export/xlsx")
    Call<ResponseBody> exportXlsx(@Query("sessionId") Long sessionId);

    @GET("api/v1/export/csv")
    Call<ResponseBody> exportCsv(@Query("sessionId") Long sessionId);

    // =============================
    // Request/Response DTOs
    // =============================

    class SubmitAttendanceRequest {
        public Long sessionId;
        public String studentUsername;
        public String method;   // QR_SCAN | MANUAL | MANUAL_REQUEST | PROXY
        public long timestampMs;

        public SubmitAttendanceRequest(Long sessionId, String studentUsername, long timestampMs) {
            this(sessionId, studentUsername, "QR_SCAN", timestampMs);
        }

        public SubmitAttendanceRequest(Long sessionId, String studentUsername, String method, long timestampMs) {
            this.sessionId = sessionId;
            this.studentUsername = studentUsername != null ? studentUsername.trim().toLowerCase() : null;
            this.method = method;
            this.timestampMs = timestampMs;
        }
    }

    class CreateManualRequestRequest {
        public Long sessionId;
        public String studentUsername;
        public String reason;
        public String deviceId;

        public CreateManualRequestRequest(Long sessionId, String studentUsername, String reason, String deviceId) {
            this.sessionId = sessionId;
            this.studentUsername = studentUsername != null ? studentUsername.trim().toLowerCase() : null;
            this.reason = reason;
            this.deviceId = deviceId;
        }
    }

    class PresenterHomeResponse {
        public PresenterSummary presenter;
        public MySlotSummary mySlot;
        public List<SlotCard> slotCatalog;
        public AttendancePanel attendance;
    }

    class PresenterSummary {
        public String bguUsername;
        public String name;
        public String degree;
        public boolean alreadyRegistered;
        public String currentCycleId;
    }

    class MySlotSummary {
        public Long slotId;
        public String semesterLabel;
        public String date;
        public String dayOfWeek;
        public String timeRange;
        public String room;
        public String building;
        public List<PresenterCoPresenter> coPresenters;
    }

    class PresenterCoPresenter {
        public String name;
        public String degree;
        public String topic;
    }

    class SlotCard {
        public Long slotId;
        public String semesterLabel;
        public String date;
        public String dayOfWeek;
        public String timeRange;
        public String room;
        public String building;
        public SlotState state;
        public int capacity;
        public int enrolledCount;
        public int availableCount;
        public boolean canRegister;
        public String disableReason;
        public boolean alreadyRegistered;
        public List<PresenterCoPresenter> registered;
        
        // Session status fields
        @SerializedName("attendanceOpenedAt")
        public String attendanceOpenedAt;
        
        @SerializedName("attendanceClosesAt")
        public String attendanceClosesAt;
        
        @SerializedName("hasClosedSession")
        public Boolean hasClosedSession; // True if slot has a closed attendance session
    }

    enum SlotState {
        FREE,
        SEMI,
        FULL
    }

    class AttendancePanel {
        public boolean canOpen;
        public String openQrUrl;
        public String status;
        public String warning;
        public String openedAt;
        public String closesAt;
        public boolean alreadyOpen;
        public Long sessionId;
        public String qrPayload;
    }

    class PresenterRegisterRequest {
        public String topic;
        public String supervisorName;
        public String supervisorEmail;

        public PresenterRegisterRequest() {}

        public PresenterRegisterRequest(String topic, String supervisorName, String supervisorEmail) {
            this.topic = topic;
            this.supervisorName = supervisorName;
            this.supervisorEmail = supervisorEmail;
        }
    }

    class PresenterRegisterResponse {
        public boolean ok;
        public String code;
        public String message;
    }

    class PresenterAttendanceOpenResponse {
        public boolean success;
        public String message;
        public String code;
        public String qrUrl; // Legacy field - kept for backward compatibility
        public String closesAt;
        public String openedAt;
        public Long sessionId;
        public String qrPayload; // Legacy field - kept for backward compatibility
        public QrContent qrContent; // New nested structure
        public ServerInfo serverInfo;
        public Metadata metadata;
        
        // Helper method to get the recommended QR URL
        public String getQrUrl() {
            if (qrContent != null && qrContent.recommended != null) {
                return qrContent.recommended;
            }
            if (qrContent != null && qrContent.fullUrl != null) {
                return qrContent.fullUrl;
            }
            // Fallback to legacy field
            return qrUrl != null ? qrUrl : qrPayload;
        }
    }
    
    class QrContent {
        public String fullUrl;
        public String relativePath;
        public String sessionIdOnly;
        public String recommended;
    }
    
    class ServerInfo {
        public String serverUrl;
        public String apiBaseUrl;
        public String environment;
    }
    
    class Metadata {
        public String generatedAt;
        public String version;
        public String format;
        public String description;
    }
    
    class ErrorResponse {
        public String error;
        public String code;
    }

    class LoginRequest {
        public String username;
        public String password;

        public LoginRequest(String username, String password) {
            this.username = username != null ? username.trim().toLowerCase() : null;
            this.password = password;
        }
    }

    class LoginResponse {
        public boolean ok;
        public String message;
        public String bguUsername;
        public String email;
        public boolean isFirstTime;
        public boolean isPresenter;
        public boolean isParticipant;
    }

    class UserProfileResponse {
        public String bguUsername;
        public String email;
        public String firstName;
        public String lastName;
        public String degree;
        public String participationPreference;
    }

    class UserExistsRequest {
        public String username;

        public UserExistsRequest(String username) {
            this.username = username != null ? username.trim() : null;
        }
    }

    class UserExistsResponse {
        public boolean exists;

        public UserExistsResponse(boolean exists) {
            this.exists = exists;
        }
    }

    class UserProfileUpdateRequest {
        public String bguUsername;
        public String email;
        public String firstName;
        public String lastName;
        public String degree;
        public String participationPreference;

        public UserProfileUpdateRequest(String bguUsername,
                                        String email,
                                        String firstName,
                                        String lastName,
                                        String degree,
                                        String participationPreference) {
            this.bguUsername = bguUsername == null ? null : bguUsername.trim().toLowerCase();
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.degree = degree;
            this.participationPreference = participationPreference;
        }
    }
}
