package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class Attendance {
    @SerializedName("session_id")
    private String sessionId;
    
    @SerializedName("user_id")
    private String userId;
    
    private long timestamp;
    private String status; // "present"
    
    @SerializedName("already_present")
    private boolean alreadyPresent;
    
    public Attendance() {}
    
    public Attendance(String sessionId, String userId, long timestamp, String status) {
        this.sessionId = sessionId;
        this.userId = userId;
        this.timestamp = timestamp;
        this.status = status;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
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
    
    public boolean isAlreadyPresent() {
        return alreadyPresent;
    }
    
    public void setAlreadyPresent(boolean alreadyPresent) {
        this.alreadyPresent = alreadyPresent;
    }
    
    public boolean isPresent() {
        return "present".equals(status);
    }
    
    @Override
    public String toString() {
        return "Attendance{" +
                "sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", timestamp=" + timestamp +
                ", status='" + status + '\'' +
                ", alreadyPresent=" + alreadyPresent +
                '}';
    }
}
