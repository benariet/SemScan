package org.example.semscan.data.api;

import com.google.gson.annotations.SerializedName;

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
import android.text.TextUtils;

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
    @POST("api/v1/attendance/manual")
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

    @GET("api/v1/seminars/presenter/{presenterId}")
    Call<List<Seminar>> getSeminarsForPresenter(@Path("presenterId") Long presenterId);

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
        @DELETE("api/v1/presenters/{presenterId}/seminars/{presenterSeminarId}")
        Call<Void> deletePresenterSeminar(
                @Path("presenterId") Long presenterId,
                @Path("presenterSeminarId") Long presenterSeminarId
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
        public String username;
        public long timestampMs;

        public SubmitAttendanceRequest(Long sessionId, String username, long timestampMs) {
            this.sessionId = sessionId;
            this.username = username;
            this.timestampMs = timestampMs;
        }
    }
    
    class CreateSeminarRequest {
        public String seminarName;
        public String seminarCode;
        public String seminarDescription;
        public Long presenterId;

        public CreateSeminarRequest(String seminarName,
                                    String seminarCode,
                                    String seminarDescription,
                                    Long presenterId) {
            this.seminarName = seminarName;
            this.seminarCode = seminarCode;
            this.seminarDescription = seminarDescription;
            this.presenterId = presenterId;
        }
    }

    class CreateManualRequestRequest {
        public Long sessionId;
        public String username;
        public String reason;
        public String deviceId;

        public CreateManualRequestRequest(Long sessionId, String username, String reason, String deviceId) {
            this.sessionId = sessionId;
            this.username = username;
            this.reason = reason;
            this.deviceId = deviceId;
        }
    }

    // =============================
    // DTOs for Presenter Seminars
    // =============================


    class PresenterSeminarSlotDto {
        @SerializedName(value = "presenterSeminarSlotId", alternate = {"id", "slotId"})
        public Long presenterSeminarSlotId;

        public int weekday;     // 0=Sun .. 6=Sat
        public int startHour;   // 0..23
        public int endHour;     // 1..24, > startHour
    }

    class PresenterSeminarDto {
        @SerializedName(value = "presenterSeminarId", alternate = {"id", "presenter_seminar_id"})
        public Long presenterSeminarId;

        @SerializedName(value = "seminarId", alternate = {"seminar_id"})
        public Long seminarId;

        @SerializedName(value = "presenterId", alternate = {"presenter_id"})
        public Long presenterId;

        @SerializedName(value = "seminarName", alternate = {"seminar_name", "title"})
        public String seminarName;

        @SerializedName(value = "seminarDescription", alternate = {"seminar_description", "availabilityDescription"})
        public String seminarDescription;

        @SerializedName(value = "seminarDisplayName", alternate = {"seminar_display_name", "semName"})
        public String seminarDisplayName;

        @SerializedName(value = "instanceName", alternate = {"instance_name", "customTitle", "availabilityTitle"})
        public String instanceName;

        @SerializedName(value = "instanceDescription", alternate = {"instance_description", "tileDescription", "displayDescription"})
        public String instanceDescription;

        @SerializedName(value = "presenterDisplayName", alternate = {"presenter_display_name", "displayName"})
        public String presenterDisplayName;

        public java.util.List<PresenterSeminarSlotDto> slots;

        @SerializedName(value = "createdAt", alternate = {"created_at"})
        public String createdAt;

        @SerializedName(value = "updatedAt", alternate = {"updated_at"})
        public String updatedAt;

        @SerializedName(value = "createdAtEpoch", alternate = {"created_at_epoch"})
        public Long createdAtEpoch;

        public String getDisplayTitle() {
            if (!TextUtils.isEmpty(instanceName)) {
                return instanceName.trim();
            }
            if (!TextUtils.isEmpty(seminarName)) {
                return seminarName.trim();
            }
            if (!TextUtils.isEmpty(seminarDisplayName)) {
                return seminarDisplayName.trim();
            }
            return "";
        }

        public String getSeminarDisplayNameOrFallback() {
            if (!TextUtils.isEmpty(seminarDisplayName)) {
                return seminarDisplayName.trim();
            }
            if (!TextUtils.isEmpty(seminarName)) {
                return seminarName.trim();
            }
            return "";
        }

        public String getDescriptionLine() {
            if (!TextUtils.isEmpty(instanceDescription)) {
                return instanceDescription.trim();
            }
            if (!TextUtils.isEmpty(seminarDescription)) {
                return seminarDescription.trim();
            }
            return "";
        }

        public String getNormalizedSlots() {
            if (slots == null || slots.isEmpty()) {
                return "TBD";
            }
            String[] days = {"Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"};
            java.util.Map<String, java.util.List<String>> grouped = new java.util.LinkedHashMap<>();
            for (PresenterSeminarSlotDto slot : slots) {
                String dayLabel = (slot.weekday >= 0 && slot.weekday < days.length) ? days[slot.weekday] : String.valueOf(slot.weekday);
                String range = String.format("%02d:%02d–%02d:%02d",
                        slot.startHour, 0,
                        slot.endHour, 0);
                grouped.computeIfAbsent(dayLabel, k -> new java.util.ArrayList<>()).add(range);
            }
            StringBuilder builder = new StringBuilder();
            boolean firstDay = true;
            for (java.util.Map.Entry<String, java.util.List<String>> entry : grouped.entrySet()) {
                if (!firstDay) {
                    builder.append(", ");
                }
                firstDay = false;
                builder.append(entry.getKey()).append(" • ").append(TextUtils.join("; ", entry.getValue()));
            }
            return builder.toString();
        }
    }

    class CreatePresenterSeminarRequest {
        public Long seminarId;
        public String seminarName;
        public String seminarDescription;
        public String instanceName;
        public String instanceDescription;
        public java.util.List<PresenterSeminarSlotDto> slots;

        public CreatePresenterSeminarRequest() {}

        public CreatePresenterSeminarRequest(Long seminarId,
                                             String seminarName,
                                             String seminarDescription,
                                             String instanceName,
                                             String instanceDescription,
                                             java.util.List<PresenterSeminarSlotDto> slots) {
            this.seminarId = seminarId;
            this.seminarName = seminarName;
            this.seminarDescription = seminarDescription;
            this.instanceName = instanceName;
            this.instanceDescription = instanceDescription;
            this.slots = slots;
        }

        public CreatePresenterSeminarRequest(String seminarName, java.util.List<PresenterSeminarSlotDto> slots) {
            this(null, seminarName, null, null, null, slots);
        }
    }
    
    // =============================
    // Presenter Home + Slot Catalog
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
    Call<java.util.List<SlotCard>> getPublicSlots();

    class PresenterHomeResponse {
        public PresenterSummary presenter;
        public MySlotSummary mySlot;
        public java.util.List<SlotCard> slotCatalog = new java.util.ArrayList<>();
        public AttendancePanel attendance;
    }

    class PresenterSummary {
        public Long id;
        public String name;
        public String degree;
        public boolean alreadyRegistered;
        public String currentCycleId;
        public String bguUsername;
    }

    class MySlotSummary {
        public Long slotId;
        public String semesterLabel;
        public String date;
        public String dayOfWeek;
        public String timeRange;
        public String room;
        public String building;
        public java.util.List<PresenterCoPresenter> coPresenters = new java.util.ArrayList<>();
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
        public java.util.List<PresenterCoPresenter> registered = new java.util.ArrayList<>();
    }

    enum SlotState {
        FREE,
        SEMI,
        FULL
    }

    class AttendancePanel {
        public boolean canOpen;
        public boolean alreadyOpen;
        public String status;
        public String warning;
        public String openQrUrl;
        public String openedAt;
        public String closesAt;
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
        public boolean success;
        public String message;
        public String code;
    }

    class PresenterAttendanceOpenResponse {
        public boolean success;
        public String message;
        public String code;
        public String qrUrl;
        public String qrPayload;
        public String openedAt;
        public String closesAt;
        public Long sessionId;
    }

}
