package org.example.semscan.data.api;

import org.example.semscan.data.model.AbsenceRequest;
import org.example.semscan.data.model.Attendance;
import org.example.semscan.data.model.Course;
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
    
    // Sessions (Teacher endpoints require X-API-Key header)
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
    
    @GET("api/v1/sessions")
    Call<List<Session>> getSessions(
            @Header("X-API-Key") String apiKey,
            @Query("courseId") String courseId,
            @Query("from") Long from,
            @Query("to") Long to
    );
    
    // Attendance
    @POST("api/v1/attendance")
    Call<Attendance> submitAttendance(@Body SubmitAttendanceRequest request);
    
    @GET("api/v1/attendance")
    Call<List<Attendance>> getAttendance(
            @Header("X-API-Key") String apiKey,
            @Query("sessionId") String sessionId
    );
    
    // Absence Requests
    @POST("api/v1/absence-requests")
    Call<AbsenceRequest> submitAbsenceRequest(@Body SubmitAbsenceRequest request);
    
    @PATCH("api/v1/absence-requests/{id}")
    Call<AbsenceRequest> updateAbsenceRequest(
            @Header("X-API-Key") String apiKey,
            @Path("id") String id,
            @Body UpdateAbsenceRequest request
    );
    
    @GET("api/v1/absence-requests")
    Call<List<AbsenceRequest>> getAbsenceRequests(
            @Header("X-API-Key") String apiKey,
            @Query("courseId") String courseId,
            @Query("status") String status
    );
    
    // Courses
    @GET("api/v1/courses")
    Call<List<Course>> getCourses(@Header("X-API-Key") String apiKey);
    
    @POST("api/v1/courses")
    Call<Course> createCourse(@Header("X-API-Key") String apiKey, @Body Course course);
    
    @PUT("api/v1/courses/{courseId}")
    Call<Course> updateCourse(@Header("X-API-Key") String apiKey, @Path("courseId") String courseId, @Body Course course);
    
    @DELETE("api/v1/courses/{courseId}")
    Call<Void> deleteCourse(@Header("X-API-Key") String apiKey, @Path("courseId") String courseId);
    
    // Additional Attendance endpoints
    @GET("api/v1/attendance/all")
    Call<List<Attendance>> getAllAttendance(@Header("X-API-Key") String apiKey);
    
    @GET("api/v1/attendance/course/{courseId}")
    Call<List<Attendance>> getAttendanceByCourse(@Header("X-API-Key") String apiKey, @Path("courseId") String courseId);
    
    // Additional Absence Request endpoints
    @GET("api/v1/absence-requests/all")
    Call<List<AbsenceRequest>> getAllAbsenceRequests(@Header("X-API-Key") String apiKey);
    
    @GET("api/v1/absence-requests/course/{courseId}")
    Call<List<AbsenceRequest>> getAbsenceRequestsByCourse(@Header("X-API-Key") String apiKey, @Path("courseId") String courseId);
    
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
        public String courseId;
        public long startTime;
        
        public CreateSessionRequest(String courseId, long startTime) {
            this.courseId = courseId;
            this.startTime = startTime;
        }
    }
    
    class SubmitAttendanceRequest {
        public String sessionId;
        public String userId;
        public long timestamp;
        
        public SubmitAttendanceRequest(String sessionId, String userId, long timestamp) {
            this.sessionId = sessionId;
            this.userId = userId;
            this.timestamp = timestamp;
        }
    }
    
    class SubmitAbsenceRequest {
        public String userId;
        public String courseId;
        public String sessionId;
        public String reason;
        public String note;
        
        public SubmitAbsenceRequest(String userId, String courseId, String sessionId, 
                                   String reason, String note) {
            this.userId = userId;
            this.courseId = courseId;
            this.sessionId = sessionId;
            this.reason = reason;
            this.note = note;
        }
    }
    
    class UpdateAbsenceRequest {
        public String status;
        
        public UpdateAbsenceRequest(String status) {
            this.status = status;
        }
    }
}
