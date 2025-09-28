package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class Attendance {
    @SerializedName("attendance_id")
    private String attendanceId;
    
    @SerializedName("session_id")
    private String sessionId;
    
    @SerializedName("student_id")
    private String studentId;
    
    @SerializedName("attendance_time")
    private long attendanceTime;
    
    private String method; // "QR_SCAN", "MANUAL", "PROXY"
    
    @SerializedName("already_present")
    private boolean alreadyPresent;
    
    public Attendance() {}
    
    public Attendance(String attendanceId, String sessionId, String studentId, long attendanceTime, String method) {
        this.attendanceId = attendanceId;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.attendanceTime = attendanceTime;
        this.method = method;
    }
    
    // Getters and Setters
    public String getAttendanceId() {
        return attendanceId;
    }
    
    public void setAttendanceId(String attendanceId) {
        this.attendanceId = attendanceId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getStudentId() {
        return studentId;
    }
    
    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
    
    public long getAttendanceTime() {
        return attendanceTime;
    }
    
    public void setAttendanceTime(long attendanceTime) {
        this.attendanceTime = attendanceTime;
    }
    
    public String getMethod() {
        return method;
    }
    
    public void setMethod(String method) {
        this.method = method;
    }
    
    public boolean isAlreadyPresent() {
        return alreadyPresent;
    }
    
    public void setAlreadyPresent(boolean alreadyPresent) {
        this.alreadyPresent = alreadyPresent;
    }
    
    public boolean isQrScan() {
        return "QR_SCAN".equals(method);
    }
    
    public boolean isManual() {
        return "MANUAL".equals(method);
    }
    
    public boolean isProxy() {
        return "PROXY".equals(method);
    }
    
    @Override
    public String toString() {
        return "Attendance{" +
                "attendanceId='" + attendanceId + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", studentId='" + studentId + '\'' +
                ", attendanceTime=" + attendanceTime +
                ", method='" + method + '\'' +
                ", alreadyPresent=" + alreadyPresent +
                '}';
    }
}
