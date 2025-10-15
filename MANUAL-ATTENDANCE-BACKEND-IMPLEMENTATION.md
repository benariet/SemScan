# 🎯 Manual Attendance Backend Implementation Guide

## 📋 **Complete Implementation Context for Backend Team**

This document provides everything needed to implement the manual attendance feature on the backend side, based on the Android app implementation completed above.

---

## 🗄️ **Database Schema Changes**

### **Enhanced Attendance Table**
```sql
-- Add new columns to existing attendance table
ALTER TABLE attendance 
ADD COLUMN request_status ENUM('confirmed', 'pending_approval', 'rejected') DEFAULT 'confirmed',
ADD COLUMN manual_reason VARCHAR(255) NULL,
ADD COLUMN requested_at TIMESTAMP NULL,
ADD COLUMN approved_by VARCHAR(36) NULL,
ADD COLUMN approved_at TIMESTAMP NULL,
ADD COLUMN device_id VARCHAR(255) NULL,
ADD COLUMN auto_flags JSON NULL,
ADD FOREIGN KEY (approved_by) REFERENCES users(user_id) ON DELETE SET NULL;

-- Update method enum to include manual_request
ALTER TABLE attendance 
MODIFY COLUMN method ENUM('qr_scan', 'manual', 'manual_request', 'proxy') DEFAULT 'qr_scan';
```

### **Data Flow Explanation**
- **QR Scan**: `method='qr_scan'`, `request_status='confirmed'`
- **Manual Request**: `method='manual_request'`, `request_status='pending_approval'`
- **Approved Manual**: `method='manual'`, `request_status='confirmed'`
- **Rejected Manual**: `method='manual_request'`, `request_status='rejected'`

---

## 🔌 **API Endpoints to Implement**

### **1. Create Manual Request**
```http
POST /api/v1/attendance/manual-request
Content-Type: application/json
Body: {
  "sessionId": "session-123",
  "studentId": "student-456", 
  "reason": "Camera broken",
  "deviceId": "device-789"
}
Response: Attendance (with request_status='pending_approval')
```

**Business Logic:**
- Validate session exists and is within window (-10 min to +15 min of start)
- Check if student already has attendance record for this session
- If exists and is QR scan: reject as duplicate
- If exists and is pending: update existing request
- If not exists: create new manual request
- Set auto_flags JSON: `{"inWindow": true/false, "duplicate": true/false, "capExceeded": false}`

### **2. Get Pending Requests**
```http
GET /api/v1/attendance/pending-requests?sessionId=session-123
Headers: X-API-Key: presenter-api-key
Response: List<Attendance> (where request_status='pending_approval')
```

**Business Logic:**
- Only return requests for sessions owned by the presenter
- Include student names (join with users table)
- Sort by requested_at timestamp

### **3. Approve Manual Request**
```http
POST /api/v1/attendance/{attendanceId}/approve
Headers: X-API-Key: presenter-api-key
Response: Attendance (updated with method='manual', request_status='confirmed')
```

**Business Logic:**
- Validate presenter owns the session
- Update: `method='manual'`, `request_status='confirmed'`, `approved_by=presenterId`, `approved_at=now()`
- Log the approval action

### **4. Reject Manual Request**
```http
POST /api/v1/attendance/{attendanceId}/reject
Headers: X-API-Key: presenter-api-key
Response: Attendance (updated with request_status='rejected')
```

**Business Logic:**
- Validate presenter owns the session
- Update: `request_status='rejected'`, `approved_by=presenterId`, `approved_at=now()`
- Log the rejection action

---

## 🧠 **Business Logic Implementation**

### **Session Window Validation**
```java
public boolean isWithinRequestWindow(Session session) {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime sessionStart = session.getStartTime();
    LocalDateTime windowStart = sessionStart.minusMinutes(10);
    LocalDateTime windowEnd = sessionStart.plusMinutes(15);
    
    return now.isAfter(windowStart) && now.isBefore(windowEnd);
}
```

### **Auto-Flags Generation**
```java
public String generateAutoFlags(String sessionId, String studentId) {
    Session session = sessionRepository.findById(sessionId);
    Attendance existingAttendance = attendanceRepository.findBySessionIdAndStudentId(sessionId, studentId);
    
    boolean inWindow = isWithinRequestWindow(session);
    boolean duplicate = existingAttendance != null && 
                       !"manual_request".equals(existingAttendance.getMethod());
    
    // Calculate cap (max(5, 5% of roster))
    int rosterSize = getRosterSize(session.getCourseId());
    int cap = Math.max(5, (int) Math.ceil(rosterSize * 0.05));
    int approvedCount = getApprovedManualCount(sessionId);
    boolean capExceeded = approvedCount >= cap;
    
    return String.format("{\"inWindow\":%b,\"duplicate\":%b,\"capExceeded\":%b}", 
                        inWindow, duplicate, capExceeded);
}
```

### **Duplicate Prevention**
```java
public Attendance createOrUpdateManualRequest(CreateManualRequestRequest request) {
    Attendance existing = attendanceRepository.findBySessionIdAndStudentId(
        request.getSessionId(), request.getStudentId());
    
    if (existing != null) {
        if ("qr_scan".equals(existing.getMethod())) {
            throw new ConflictException("Student already marked present via QR scan");
        }
        if ("manual_request".equals(existing.getMethod()) && 
            "pending_approval".equals(existing.getRequestStatus())) {
            // Update existing pending request
            existing.setManualReason(request.getReason());
            existing.setRequestedAt(LocalDateTime.now());
            existing.setAutoFlags(generateAutoFlags(request.getSessionId(), request.getStudentId()));
            return attendanceRepository.save(existing);
        }
    }
    
    // Create new request
    Attendance newRequest = new Attendance();
    newRequest.setAttendanceId(UUID.randomUUID().toString());
    newRequest.setSessionId(request.getSessionId());
    newRequest.setStudentId(request.getStudentId());
    newRequest.setMethod("manual_request");
    newRequest.setRequestStatus("pending_approval");
    newRequest.setManualReason(request.getReason());
    newRequest.setRequestedAt(LocalDateTime.now());
    newRequest.setDeviceId(request.getDeviceId());
    newRequest.setAutoFlags(generateAutoFlags(request.getSessionId(), request.getStudentId()));
    
    return attendanceRepository.save(newRequest);
}
```

---

## 📊 **Export Logic Updates**

### **Export Validation**
```java
public void validateExportReady(String sessionId) {
    List<Attendance> pendingRequests = attendanceRepository
        .findBySessionIdAndRequestStatus(sessionId, "pending_approval");
    
    if (!pendingRequests.isEmpty()) {
        throw new BusinessException("Cannot export while " + pendingRequests.size() + 
                                  " manual requests are pending approval");
    }
}
```

### **Export Data Enhancement**
```java
public List<ExportRow> getExportData(String sessionId) {
    List<Attendance> attendanceRecords = attendanceRepository.findBySessionId(sessionId);
    
    return attendanceRecords.stream()
        .filter(attendance -> "confirmed".equals(attendance.getRequestStatus()))
        .map(this::mapToExportRow)
        .collect(Collectors.toList());
}

private ExportRow mapToExportRow(Attendance attendance) {
    ExportRow row = new ExportRow();
    row.setStudentId(attendance.getStudentId());
    row.setAttendanceTime(attendance.getAttendanceTime());
    row.setSource(attendance.getMethod()); // "qr_scan" or "manual"
    
    if ("manual".equals(attendance.getMethod())) {
        row.setManualReason(attendance.getManualReason());
        row.setManualDecidedBy(attendance.getApprovedBy());
        row.setManualDecidedAt(attendance.getApprovedAt());
    }
    
    return row;
}
```

---

## 🔒 **Security & Validation**

### **Presenter Authorization**
```java
@PreAuthorize("hasRole('PRESENTER') and @sessionService.isOwner(#sessionId, authentication.name)")
public List<Attendance> getPendingRequests(String sessionId) {
    // Implementation
}
```

### **Session Window Enforcement**
```java
public void validateRequestWindow(String sessionId) {
    Session session = sessionRepository.findById(sessionId);
    if (!isWithinRequestWindow(session)) {
        throw new BusinessException("Manual requests only allowed 10 minutes before to 15 minutes after session start");
    }
}
```

### **Rate Limiting**
```java
@RateLimiter(name = "manual-requests", fallbackMethod = "fallbackManualRequest")
public Attendance createManualRequest(CreateManualRequestRequest request) {
    // Implementation
}
```

---

## 📝 **Error Handling**

### **HTTP Status Codes**
- **200**: Success
- **400**: Invalid request (outside window, invalid session)
- **409**: Conflict (duplicate, already present)
- **404**: Session not found
- **403**: Unauthorized (not session owner)

### **Error Response Format**
```json
{
  "error": {
    "code": "WINDOW_CLOSED",
    "message": "Manual requests only allowed 10 minutes before to 15 minutes after session start",
    "details": {
      "sessionStart": "2024-01-15T10:00:00",
      "currentTime": "2024-01-15T10:30:00",
      "windowStart": "2024-01-15T09:50:00",
      "windowEnd": "2024-01-15T10:15:00"
    }
  }
}
```

---

## 🧪 **Testing Scenarios**

### **Unit Tests**
1. **Window Validation**: Test -10 to +15 minute window
2. **Duplicate Prevention**: Test QR scan vs manual request conflicts
3. **Cap Calculation**: Test max(5, 5% of roster) logic
4. **Auto-Flags**: Test JSON generation for all scenarios

### **Integration Tests**
1. **End-to-End Flow**: Student request → Presenter approve → Export
2. **Bulk Operations**: Approve all safe, reject all duplicates
3. **Export Validation**: Block export with pending requests
4. **Error Scenarios**: Invalid sessions, unauthorized access

### **Performance Tests**
1. **Concurrent Requests**: Multiple students requesting simultaneously
2. **Large Rosters**: Test with 300+ student classes
3. **Export Performance**: Large attendance datasets

---

## 🚀 **Deployment Checklist**

### **Database Migration**
- [ ] Run ALTER TABLE statements
- [ ] Update existing records with default values
- [ ] Create indexes on new columns
- [ ] Test migration rollback

### **API Deployment**
- [ ] Deploy new endpoints
- [ ] Update API documentation
- [ ] Configure rate limiting
- [ ] Test with Android app

### **Monitoring**
- [ ] Add metrics for manual requests
- [ ] Set up alerts for high rejection rates
- [ ] Monitor export performance
- [ ] Track approval/rejection patterns

---

## 📱 **Android App Integration**

The Android app is already implemented and expects these exact endpoints:

### **Request Flow**
1. Student scans QR → Gets session ID
2. Student taps "Request Manual Attendance" → Shows dialog
3. Student enters reason → Calls `POST /api/v1/attendance/manual-request`
4. Presenter exports → Calls `GET /api/v1/attendance/pending-requests`
5. Presenter reviews → Calls approve/reject endpoints
6. Export proceeds → Calls existing export endpoints

### **Expected Response Formats**
The Android app expects the `Attendance` model with these fields:
- `attendanceId`, `sessionId`, `studentId`
- `method` (qr_scan, manual, manual_request)
- `requestStatus` (confirmed, pending_approval, rejected)
- `manualReason`, `requestedAt`, `approvedBy`, `approvedAt`
- `deviceId`, `autoFlags` (JSON string)

---

## 🎯 **Success Criteria**

### **Functional Requirements**
- ✅ Students can request manual attendance during session window
- ✅ Presenters can review and approve/reject requests
- ✅ Export is blocked until all requests are resolved
- ✅ Duplicate prevention works correctly
- ✅ Window validation is enforced

### **Non-Functional Requirements**
- ✅ Response time < 500ms for all endpoints
- ✅ Support 100+ concurrent manual requests
- ✅ 99.9% uptime for attendance system
- ✅ Complete audit trail for all decisions

---

## 🔧 **Implementation Priority**

### **Phase 1 (Critical)**
1. Database schema changes
2. Basic CRUD endpoints
3. Window validation
4. Duplicate prevention

### **Phase 2 (Important)**
1. Auto-flags generation
2. Export validation
3. Bulk operations
4. Error handling

### **Phase 3 (Nice to Have)**
1. Advanced analytics
2. Rate limiting
3. Performance optimizations
4. Advanced bulk operations

---

This implementation guide provides everything needed to build the backend for the manual attendance feature. The Android app is ready and waiting for these endpoints! 🚀
