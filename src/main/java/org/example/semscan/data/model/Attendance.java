package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class Attendance {
    @SerializedName("attendanceId")
    private Long attendanceId;
    
    @SerializedName("sessionId")
    private Long sessionId;
    
    @SerializedName("studentId")
    private Long studentId;
    
    @SerializedName("attendanceTime")
    private String attendanceTime;
    
    private String method; // "QR_SCAN", "MANUAL", "MANUAL_REQUEST", "PROXY"
    
    @SerializedName("already_present")
    private boolean alreadyPresent;
    
    // Manual request fields
    @SerializedName("requestStatus")
    private String requestStatus; // "confirmed", "pending_approval", "rejected"
    
    @SerializedName("reason")
    private String manualReason;
    
    @SerializedName("requested_at")
    private Long requestedAt;
    
    @SerializedName("approved_by")
    private Long approvedBy;
    
    @SerializedName("approved_at")
    private Long approvedAt;
    
    @SerializedName("device_id")
    private String deviceId;
    
    @SerializedName("auto_flags")
    private String autoFlags; // JSON string for inWindow, duplicate, capExceeded flags
    
    public Attendance() {}
    
    public Attendance(Long attendanceId, Long sessionId, Long studentId, String attendanceTime, String method) {
        this.attendanceId = attendanceId;
        this.sessionId = sessionId;
        this.studentId = studentId;
        this.attendanceTime = attendanceTime;
        this.method = method;
    }
    
    // Getters and Setters
    public Long getAttendanceId() {
        return attendanceId;
    }
    
    public void setAttendanceId(Long attendanceId) {
        this.attendanceId = attendanceId;
    }
    
    public Long getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }
    
    public Long getStudentId() {
        return studentId;
    }
    
    public void setStudentId(Long studentId) {
        this.studentId = studentId;
    }
    
    public String getAttendanceTime() {
        return attendanceTime;
    }
    
    public void setAttendanceTime(String attendanceTime) {
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
    
    public boolean isManualRequest() {
        return "MANUAL_REQUEST".equals(method);
    }
    
    // Manual request getters and setters
    public String getRequestStatus() {
        return requestStatus;
    }
    
    public void setRequestStatus(String requestStatus) {
        this.requestStatus = requestStatus;
    }
    
    public String getManualReason() {
        return manualReason;
    }
    
    public void setManualReason(String manualReason) {
        this.manualReason = manualReason;
    }
    
    public Long getRequestedAt() {
        return requestedAt;
    }
    
    public void setRequestedAt(Long requestedAt) {
        this.requestedAt = requestedAt;
    }
    
    public Long getApprovedBy() {
        return approvedBy;
    }
    
    public void setApprovedBy(Long approvedBy) {
        this.approvedBy = approvedBy;
    }
    
    public Long getApprovedAt() {
        return approvedAt;
    }
    
    public void setApprovedAt(Long approvedAt) {
        this.approvedAt = approvedAt;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getAutoFlags() {
        return autoFlags;
    }
    
    public void setAutoFlags(String autoFlags) {
        this.autoFlags = autoFlags;
    }
    
    // Utility methods for manual requests
    public boolean isPendingApproval() {
        return "pending_approval".equals(requestStatus);
    }
    
    public boolean isConfirmed() {
        return "confirmed".equals(requestStatus);
    }
    
    public boolean isRejected() {
        return "rejected".equals(requestStatus);
    }
    
    @Override
    public String toString() {
        return "Attendance{" +
                "attendanceId=" + attendanceId +
                ", sessionId=" + sessionId +
                ", studentId=" + studentId +
                ", attendanceTime=" + attendanceTime +
                ", method='" + method + '\'' +
                ", alreadyPresent=" + alreadyPresent +
                ", requestStatus='" + requestStatus + '\'' +
                ", manualReason='" + manualReason + '\'' +
                '}';
    }
}
