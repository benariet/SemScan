package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class Session {
    @SerializedName("session_id")
    private String sessionId;
    
    @SerializedName("course_id")
    private String courseId;
    
    @SerializedName("start_time")
    private long startTime;
    
    @SerializedName("end_time")
    private Long endTime;
    
    private String status; // "open" or "closed"
    
    public Session() {}
    
    public Session(String sessionId, String courseId, long startTime, Long endTime, String status) {
        this.sessionId = sessionId;
        this.courseId = courseId;
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
    
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
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
        return "open".equals(status);
    }
    
    public boolean isClosed() {
        return "closed".equals(status);
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
                ", courseId='" + courseId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status='" + status + '\'' +
                '}';
    }
}
