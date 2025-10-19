package org.example.semscan.data.model;

public class ManualAttendanceResponse {
    private String attendanceId;
    private String sessionId;
    private String studentId;
    private String studentName;
    private String reason;
    private String requestStatus;
    private String requestedAt;
    private String autoFlags;
    private String message;
    
    public ManualAttendanceResponse() {}
    
    // Getters and Setters
    public String getAttendanceId() { return attendanceId; }
    public void setAttendanceId(String attendanceId) { this.attendanceId = attendanceId; }
    
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    
    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    
    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }
    
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    
    public String getRequestStatus() { return requestStatus; }
    public void setRequestStatus(String requestStatus) { this.requestStatus = requestStatus; }
    
    public String getRequestedAt() { return requestedAt; }
    public void setRequestedAt(String requestedAt) { this.requestedAt = requestedAt; }
    
    public String getAutoFlags() { return autoFlags; }
    public void setAutoFlags(String autoFlags) { this.autoFlags = autoFlags; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
