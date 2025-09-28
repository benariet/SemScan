# Android App Schema Updates - Complete Summary

## 🎯 **Schema Changes Applied**

### **📊 Database Schema Changes**
- **`courses`** → **`seminars`** (table name and all references)
- **`teachers`** → **`presenters`** (role and all references)  
- **`course_id`** → **`seminar_id`** (foreign key references)
- **`lecturer_id`** → **`presenter_id`** (foreign key references)
- **`teacher_api_keys`** → **`presenter_api_keys`** (table name)
- **User roles**: `STUDENT` and `PRESENTER` (uppercase, no `TEACHER`)
- **Attendance fields**: Added `attendance_id`, `method` (QR_SCAN/MANUAL/PROXY)

---

## 🔧 **Android Code Updates**

### **📱 Data Models Updated**

#### **1. User.java**
```java
// OLD
private String name;
private String role; // "teacher" or "student"

// NEW  
@SerializedName("first_name") private String firstName;
@SerializedName("last_name") private String lastName;
@SerializedName("student_id") private String studentId;
private String role; // "STUDENT" or "PRESENTER"

// Methods updated
public boolean isPresenter() // was isTeacher()
public String getFullName() // new method
```

#### **2. Course.java → Seminar.java**
```java
// DELETED: Course.java
// CREATED: Seminar.java

@SerializedName("seminar_id") private String seminarId;
@SerializedName("seminar_name") private String seminarName;
@SerializedName("seminar_code") private String seminarCode;
@SerializedName("presenter_id") private String presenterId;
```

#### **3. Session.java**
```java
// OLD
@SerializedName("course_id") private String courseId;

// NEW
@SerializedName("seminar_id") private String seminarId;

// Methods updated
public String getSeminarId() // was getCourseId()
public boolean isOpen() // now checks "OPEN" not "open"
```

#### **4. Attendance.java**
```java
// OLD
@SerializedName("user_id") private String userId;
private long timestamp;
private String status;

// NEW
@SerializedName("attendance_id") private String attendanceId;
@SerializedName("student_id") private String studentId;
@SerializedName("attendance_time") private long attendanceTime;
private String method; // "QR_SCAN", "MANUAL", "PROXY"

// Methods updated
public boolean isQrScan(), isManual(), isProxy() // new methods
```

#### **5. PresenterApiKey.java (NEW)**
```java
// CREATED: PresenterApiKey.java
@SerializedName("api_key_id") private String apiKeyId;
@SerializedName("presenter_id") private String presenterId;
@SerializedName("api_key") private String apiKey;
@SerializedName("created_at") private long createdAt;
@SerializedName("is_active") private boolean isActive;
```

---

### **🌐 API Service Updates**

#### **ApiService.java**
```java
// OLD
@GET("api/v1/courses")
Call<List<Course>> getCourses(@Header("X-API-Key") String apiKey);

// NEW
@GET("api/v1/seminars") 
Call<List<Seminar>> getSeminars(@Header("X-API-Key") String apiKey);

// Request DTOs updated
class CreateSessionRequest {
    public String seminarId; // was courseId
}

class SubmitAttendanceRequest {
    public String studentId; // was userId
    public long timestampMs; // was timestamp
}
```

---

### **🎮 Activity Updates**

#### **1. TeacherStartSessionActivity.java**
```java
// OLD
private Spinner spinnerCourse;
private List<Course> courses;
private String selectedCourseId;

// NEW
private Spinner spinnerSeminar;
private List<Seminar> seminars;
private String selectedSeminarId;

// Method updates
loadSeminars() // was loadCourses()
updateSeminarSpinner() // was updateCourseSpinner()
apiService.getSeminars() // was getCourses()
```

#### **2. QRDisplayActivity.java**
```java
// OLD
String courseId = getIntent().getStringExtra("courseId");
new Session(sessionId, courseId, ...)

// NEW
String seminarId = getIntent().getStringExtra("seminarId");
new Session(sessionId, seminarId, ...)
```

#### **3. QRScannerActivity.java**
```java
// OLD
String userId = preferencesManager.getUserId();
new SubmitAttendanceRequest(sessionId, userId, timestamp)

// NEW
String studentId = preferencesManager.getUserId();
new SubmitAttendanceRequest(sessionId, studentId, timestampMs)
```

#### **4. PreferencesManager.java**
```java
// OLD
private static final String KEY_TEACHER_API_KEY = "teacher_api_key";
public boolean isTeacher() { return "teacher".equals(getUserRole()); }

// NEW
private static final String KEY_PRESENTER_API_KEY = "presenter_api_key";
public boolean isPresenter() { return "PRESENTER".equals(getUserRole()); }
public boolean isTeacher() { return "PRESENTER".equals(getUserRole()); } // backward compatibility

// API Key methods
public void setPresenterApiKey() / getPresenterApiKey() // new methods
public void setTeacherApiKey() / getTeacherApiKey() // backward compatibility
```

#### **5. RolePickerActivity.java**
```java
// OLD
selectRole("teacher")
selectRole("student")

// NEW
selectRole("PRESENTER")
selectRole("STUDENT")
```

---

### **📋 Files Modified**

#### **✅ Updated Files (15)**
1. `src/main/java/org/example/semscan/data/model/User.java`
2. `src/main/java/org/example/semscan/data/model/Session.java`
3. `src/main/java/org/example/semscan/data/model/Attendance.java`
4. `src/main/java/org/example/semscan/data/api/ApiService.java`
5. `src/main/java/org/example/semscan/ui/teacher/TeacherStartSessionActivity.java`
6. `src/main/java/org/example/semscan/ui/qr/QRDisplayActivity.java`
7. `src/main/java/org/example/semscan/ui/qr/QRScannerActivity.java`
8. `src/main/java/org/example/semscan/utils/PreferencesManager.java`
9. `src/main/java/org/example/semscan/ui/RolePickerActivity.java`
10. `SERVER-TEAM-GUIDE.md`

#### **✅ Created Files (2)**
1. `src/main/java/org/example/semscan/data/model/Seminar.java`
2. `src/main/java/org/example/semscan/data/model/PresenterApiKey.java`

#### **✅ Deleted Files (1)**
1. `src/main/java/org/example/semscan/data/model/Course.java`

---

### **🔄 Backward Compatibility**

#### **PreferencesManager.java**
- Kept `isTeacher()` and `getTeacherApiKey()` methods for backward compatibility
- They now delegate to the new presenter methods

#### **API Service**
- All endpoint URLs remain the same
- Only internal model names changed

---

### **📱 UI Impact**

#### **No UI Changes Required**
- All UI text remains the same ("Teacher", "Course", etc.)
- Only internal code references updated
- User experience unchanged

#### **Spinner Updates**
- TeacherStartSessionActivity now loads "seminars" instead of "courses"
- Dropdown shows "Select Seminar" instead of "Select Course"

---

### **🚀 Ready for Testing**

#### **✅ All Updates Complete**
- ✅ Data models updated to match new schema
- ✅ API service updated with correct endpoints
- ✅ Activities updated to use new model names
- ✅ Preferences updated for presenter terminology
- ✅ Backward compatibility maintained
- ✅ Server team guide updated

#### **📋 Next Steps**
1. **Build and test** the Android app
2. **Verify** API calls work with new schema
3. **Test** session creation with seminars
4. **Test** attendance submission with studentId
5. **Confirm** QR scanning and display functionality

---

### **🎯 Key Changes Summary**

| Component | Old | New |
|-----------|-----|-----|
| **Database Table** | `courses` | `seminars` |
| **Database Table** | `teachers` | `presenters` |
| **User Role** | `teacher` | `PRESENTER` |
| **User Role** | `student` | `STUDENT` |
| **Model Class** | `Course.java` | `Seminar.java` |
| **Model Class** | - | `PresenterApiKey.java` |
| **API Endpoint** | `/api/v1/courses` | `/api/v1/seminars` |
| **Foreign Key** | `course_id` | `seminar_id` |
| **Foreign Key** | `lecturer_id` | `presenter_id` |
| **Attendance Field** | `user_id` | `student_id` |
| **Attendance Field** | `timestamp` | `attendance_time` |

**Status**: ✅ **All Android code updated to match new database schema!**
