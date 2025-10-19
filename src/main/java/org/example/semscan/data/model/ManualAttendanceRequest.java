package org.example.semscan.data.model;

public class ManualAttendanceRequest {
    private String sessionId;
    private String studentId;
    private String reason;
    private String deviceId;
    
    public ManualAttendanceRequest() {}
    
    public ManualAttendanceRequest(String sessionId, String studentId, String reason, String deviceId) {
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.reason = reason;
        this.deviceId = deviceId;
    }
    
    // Getters and Setters
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
