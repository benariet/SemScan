package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class Session {
    @SerializedName("session_id")
    private String sessionId;
    
    @SerializedName("seminar_id")
    private String seminarId;
    
    @SerializedName("start_time")
    private long startTime;
    
    @SerializedName("end_time")
    private Long endTime;
    
    private String status; // "open" or "closed"
    
    public Session() {}
    
    public Session(String sessionId, String seminarId, long startTime, Long endTime, String status) {
        this.sessionId = sessionId;
        this.seminarId = seminarId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getSeminarId() {
        return seminarId;
    }
    
    public void setSeminarId(String seminarId) {
        this.seminarId = seminarId;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public Long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isOpen() {
        return "OPEN".equals(status);
    }
    
    public boolean isClosed() {
        return "CLOSED".equals(status);
    }
    
    public boolean isWithinScanWindow() {
        long currentTime = System.currentTimeMillis();
        long windowEnd = startTime + (15 * 60 * 1000); // 15 minutes in milliseconds
        return currentTime <= windowEnd;
    }
    
    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", seminarId='" + seminarId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status='" + status + '\'' +
                '}';
    }
}
