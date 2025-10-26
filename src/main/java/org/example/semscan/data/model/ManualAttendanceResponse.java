package org.example.semscan.data.model;

public class ManualAttendanceResponse {
    private Long attendanceId;
    private Long sessionId;
    private Long studentId;
    private String studentName;
    private String reason;
    private String requestStatus;
    private String requestedAt;
    private String autoFlags;
    private String message;
    
    public ManualAttendanceResponse() {}
    
    // Getters and Setters
    public Long getAttendanceId() { return attendanceId; }
    public void setAttendanceId(Long attendanceId) { this.attendanceId = attendanceId; }
    
    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }
    
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    
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
