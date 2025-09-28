# SemScan MVP - Server Team Integration Guide

## 🎯 **Critical Changes for Backend Implementation**

### **📅 DATABASE SCHEMA (URGENT)**

#### **✅ Use Standard MySQL TIMESTAMP**
```sql
-- CORRECT: Standard MySQL TIMESTAMP
sessions.start_time TIMESTAMP NOT NULL
sessions.end_time TIMESTAMP NULL
attendance.attendance_time TIMESTAMP NOT NULL

-- WRONG: Don't use Unix milliseconds
-- sessions.start_time_ms BIGINT  ❌
-- attendance.timestamp_ms BIGINT ❌
```

#### **🗄️ Schema Files to Use**
- **Primary**: `mysql-setup/database-schema-mvp.sql` (lean MVP version)
- **Reference**: `mysql-setup/database-schema.sql` (updated main version)

#### **🗑️ Removed Tables (Don't Implement)**
```sql
-- These tables are NOT needed for MVP:
absence_requests  ❌
audit_log        ❌
complex_system_settings ❌
```

#### **✅ Required Tables Only**
```sql
users (STUDENT/PRESENTER roles only)
seminars
sessions (with start_time, end_time TIMESTAMP)
attendance (with attendance_time TIMESTAMP)
presenter_api_keys
```

---

### **🔌 API ENDPOINTS (MVP ONLY)**

#### **✅ Required Endpoints (5 total)**
```http
# Session Management
POST   /api/v1/sessions
GET    /api/v1/seminars

# Attendance  
POST   /api/v1/attendance
GET    /api/v1/attendance?sessionId={id}

# Export
GET    /api/v1/export/xlsx?sessionId={id}
GET    /api/v1/export/csv?sessionId={id}
```

#### **❌ Don't Implement These**
```http
# Absence Requests (removed from MVP)
POST   /api/v1/absence-requests     ❌
PATCH  /api/v1/absence-requests/{id} ❌
GET    /api/v1/absence-requests     ❌

# Seminar Management (removed from MVP)  
POST   /api/v1/seminars            ❌
PUT    /api/v1/seminars/{id}       ❌
DELETE /api/v1/seminars/{id}       ❌

# Complex Attendance (removed from MVP)
GET    /api/v1/attendance/all      ❌
GET    /api/v1/attendance/course/{id} ❌
```

---

### **📱 Mobile App Integration**

#### **QR Code Payload**
```json
{
  "sessionId": "uuid-string"
}
```

#### **Attendance Request**
```json
POST /api/v1/attendance
{
  "sessionId": "uuid-string",
  "studentId": "uuid-string", 
  "timestampMs": 1640995200000  // Mobile app sends Unix milliseconds
}
```

#### **Database Storage**
```sql
-- Convert mobile timestamp to MySQL TIMESTAMP
INSERT INTO attendance (attendance_time, ...) 
VALUES (FROM_UNIXTIME(1640995200000/1000), ...)
```

#### **Required Error Messages**
```json
// Success Responses
"Checked in for this session"
"Already checked in"

// Error Responses  
"Invalid session code"           // 404, 400
"This session is not accepting new check-ins"  // 409
```

---

### **🔧 Technical Requirements**

#### **Authentication**
```http
# Presenter endpoints require API key
GET /api/v1/sessions
X-API-Key: test-api-key-12345

# Student endpoints (no auth for MVP)
POST /api/v1/attendance
```

#### **Timestamp Handling**
```javascript
// Mobile app sends: 1640995200000 (Unix milliseconds)
// Server converts to: '2024-09-18 10:00:00' (MySQL TIMESTAMP)
// Database stores: TIMESTAMP format

// Conversion example:
const timestamp = new Date(1640995200000).toISOString().slice(0, 19).replace('T', ' ');
// Result: "2024-09-18 10:00:00"
```

#### **Export Requirements**
- **Format**: CSV and Excel
- **Scope**: Session-based only (not date range)
- **Data**: Student info, attendance_time, status
- **Example**: `GET /api/v1/export/csv?sessionId=session-001`

---

### **🎯 MVP Scope Clarification**

#### **✅ What to Build**
1. **Session Creation**: Presenter creates session with start time
2. **QR Display**: Generate QR code with sessionId
3. **Attendance Recording**: Student scans QR, submits attendance
4. **Session Export**: Export attendance list for specific session (accessed after session ends)
5. **Basic Seminar List**: Presenters can see their seminars

#### **❌ What NOT to Build**
1. **Absence Requests**: No submission or approval workflow
2. **Seminar Management**: No create/edit/delete seminars
3. **Complex Analytics**: No detailed attendance statistics
4. **User Management**: No user registration/management
5. **Date Range Exports**: Only session-based exports

---

### **📋 Implementation Checklist**

#### **Database Setup**
- [ ] Create database using `database-schema-mvp.sql`
- [ ] Insert sample data from the schema file
- [ ] Verify all tables use TIMESTAMP (not BIGINT)
- [ ] Test teacher_api_keys table

#### **API Development**
- [ ] Implement 6 required endpoints only
- [ ] Add API key authentication for teacher endpoints
- [ ] Implement timestamp conversion (Unix ms → MySQL TIMESTAMP)
- [ ] Return exact error messages as specified
- [ ] Test QR code payload format

#### **Testing**
- [ ] Test session creation and QR generation
- [ ] Test attendance submission with mobile timestamps
- [ ] Test export functionality (CSV/Excel)
- [ ] Verify error handling and messages

---

### **📁 Reference Files**

1. **`mysql-setup/database-schema-mvp.sql`** - Complete MVP schema
2. **`mysql-setup/MVP-SCHEMA-CHANGES.md`** - Detailed schema documentation  
3. **`docs/project-context/PROJECT-CONTEXT.md`** - Complete project context
4. **`mysql-setup/database-schema.sql`** - Updated main schema

---

### **🚨 Critical Notes**

1. **TIMESTAMP NOT BIGINT**: Use standard MySQL TIMESTAMP, convert mobile Unix milliseconds
2. **Only 6 Endpoints**: Don't build the removed endpoints
3. **Exact Error Messages**: Mobile app expects specific text
4. **Session-Based Export**: No date range exports
5. **No Absence Requests**: This feature was removed from MVP

---

**Questions?** Reference the complete project context in `docs/project-context/PROJECT-CONTEXT.md`

**Status**: ✅ Lean MVP ready for backend integration
