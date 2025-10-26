package org.example.semscan.data.model;

public class ManualAttendanceRequest {
    private Long sessionId;
    private Long studentId;
    private String reason;
    private String deviceId;
    
    public ManualAttendanceRequest() {}
    
    public ManualAttendanceRequest(Long sessionId, Long studentId, String reason, String deviceId) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.reason = reason;
        this.deviceId = deviceId;
    }
    
    // Getters and Setters
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
