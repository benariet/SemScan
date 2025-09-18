package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class AbsenceRequest {
    private String id;
    
    @SerializedName("user_id")
    private String userId;
    
    @SerializedName("course_id")
    private String courseId;
    
    @SerializedName("session_id")
    private String sessionId;
    
    private String reason;
    private String note;
    private long timestamp;
    private String status; // "submitted", "approved", "rejected"
    
    public AbsenceRequest() {}
    
    public AbsenceRequest(String id, String userId, String courseId, String sessionId, 
                         String reason, String note, long timestamp, String status) {
        this.id = id;
        this.userId = userId;
        this.courseId = courseId;
        this.sessionId = sessionId;
        this.reason = reason;
        this.note = note;
        this.timestamp = timestamp;
        this.status = status;
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getNote() {
        return note;
    }
    
    public void setNote(String note) {
        this.note = note;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isSubmitted() {
        return "submitted".equals(status);
    }
    
    public boolean isApproved() {
        return "approved".equals(status);
    }
    
    public boolean isRejected() {
        return "rejected".equals(status);
    }
    
    @Override
    public String toString() {
        return "AbsenceRequest{" +
                "id='" + id + '\'' +
                ", userId='" + userId + '\'' +
                ", courseId='" + courseId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", reason='" + reason + '\'' +
                ", note='" + note + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                '}';
    }
}
