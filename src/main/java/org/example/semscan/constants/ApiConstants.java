package org.example.semscan.constants;

/**
 * Global API Constants for SemScan Application
 * Centralized configuration for all API endpoints and settings
 */
public class ApiConstants {
    
    // =============================================
    // BASE CONFIGURATION
    // =============================================
    public static final String SERVER_URL = "http://localhost:8080";
    public static final String API_BASE_URL = "http://localhost:8080/api/v1";
    public static final String API_VERSION = "v1";
    // =============================================
    // API ENDPOINTS
    // =============================================
    
    // Seminars
    public static final String SEMINARS_ENDPOINT = API_BASE_URL + "/seminars";
    public static final String CREATE_SEMINAR_ENDPOINT = API_BASE_URL + "/seminars";
    
    // Sessions
    public static final String SESSIONS_ENDPOINT = API_BASE_URL + "/sessions";
    public static final String OPEN_SESSIONS_ENDPOINT = API_BASE_URL + "/sessions/open";
    public static final String CREATE_SESSION_ENDPOINT = API_BASE_URL + "/sessions";
    public static final String CLOSE_SESSION_ENDPOINT = API_BASE_URL + "/sessions/{sessionId}/close";
    
    // Attendance
    public static final String ATTENDANCE_ENDPOINT = API_BASE_URL + "/attendance";
    public static final String ATTENDANCE_BY_SESSION_ENDPOINT = API_BASE_URL + "/attendance";
    public static final String CREATE_ATTENDANCE_ENDPOINT = API_BASE_URL + "/attendance";
    
    // =============================================
    // MANUAL ATTENDANCE ENDPOINTS
    // =============================================
    public static final String MANUAL_ATTENDANCE_ENDPOINT = API_BASE_URL + "/attendance/manual-request";
    public static final String PENDING_REQUESTS_ENDPOINT = API_BASE_URL + "/attendance/pending-requests";
    public static final String APPROVE_REQUEST_ENDPOINT = API_BASE_URL + "/attendance/{attendanceId}/approve";
    public static final String REJECT_REQUEST_ENDPOINT = API_BASE_URL + "/attendance/{attendanceId}/reject";
    
    // =============================================
    // EXPORT ENDPOINTS
    // =============================================
    public static final String EXPORT_CSV_ENDPOINT = API_BASE_URL + "/export/csv";
    public static final String EXPORT_XLSX_ENDPOINT = API_BASE_URL + "/export/xlsx";
    
    // =============================================
    // API INFO ENDPOINTS
    // =============================================
    public static final String API_ENDPOINTS_INFO = API_BASE_URL + "/info/endpoints";
    public static final String API_CONFIG_INFO = API_BASE_URL + "/info/config";
    
    // =============================================
    // MANUAL ATTENDANCE CONFIGURATION
    // =============================================
    public static final int MANUAL_ATTENDANCE_WINDOW_BEFORE_MINUTES = 10;
    public static final int MANUAL_ATTENDANCE_WINDOW_AFTER_MINUTES = 15;
    public static final int MANUAL_ATTENDANCE_AUTO_APPROVE_CAP_PERCENTAGE = 5;
    public static final int MANUAL_ATTENDANCE_AUTO_APPROVE_MIN_CAP = 5;
    
    // =============================================
    // EXPORT CONFIGURATION
    // =============================================
    public static final int MAX_EXPORT_FILE_SIZE_MB = 50;
    public static final String ALLOWED_EXPORT_FORMATS = "csv,xlsx";
    
    // =============================================
    // APPLICATION CONFIGURATION
    // =============================================
    public static final String ENVIRONMENT = "development";
    public static final String APPLICATION_NAME = "SemScan";
    public static final String APPLICATION_VERSION = "1.0.0";
    public static final String APPLICATION_DESCRIPTION = "SemScan Attendance System";
    
    // =============================================
    // HTTP CONFIGURATION
    // =============================================
    public static final int CONNECTION_TIMEOUT_SECONDS = 5;  // Reduced from 30 to 10 seconds
    public static final int READ_TIMEOUT_SECONDS = 5;        // Reduced from 30 to 10 seconds
    public static final int WRITE_TIMEOUT_SECONDS = 5;       // Reduced from 30 to 10 seconds
    
    // =============================================
    // ERROR CODES
    // =============================================
    public static final int HTTP_OK = 200;
    public static final int HTTP_CREATED = 201;
    public static final int HTTP_BAD_REQUEST = 400;
    public static final int HTTP_UNAUTHORIZED = 401;
    public static final int HTTP_NOT_FOUND = 404;
    public static final int HTTP_CONFLICT = 409;
    public static final int HTTP_INTERNAL_SERVER_ERROR = 500;
    
    // =============================================
    // REQUEST METHODS
    // =============================================
    public static final String METHOD_GET = "GET";
    public static final String METHOD_POST = "POST";
    public static final String METHOD_PUT = "PUT";
    public static final String METHOD_DELETE = "DELETE";
    public static final String METHOD_PATCH = "PATCH";
    
    // =============================================
    // CONTENT TYPES
    // =============================================
    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_FORM = "application/x-www-form-urlencoded";
    public static final String CONTENT_TYPE_MULTIPART = "multipart/form-data";
    
    // =============================================
    // UTILITY METHODS
    // =============================================
    
    /**
     * Build URL with session ID parameter
     */
    public static String buildSessionUrl(long sessionId) {
        return ATTENDANCE_BY_SESSION_ENDPOINT + "?sessionId=" + sessionId;
    }
    
    /**
     * Build URL for pending requests with session ID
     */
    public static String buildPendingRequestsUrl(long sessionId) {
        return PENDING_REQUESTS_ENDPOINT + "?sessionId=" + sessionId;
    }
    
    /**
     * Build URL for approve request with attendance ID
     */
    public static String buildApproveRequestUrl(long attendanceId) {
        return APPROVE_REQUEST_ENDPOINT.replace("{attendanceId}", String.valueOf(attendanceId));
    }
    
    /**
     * Build URL for reject request with attendance ID
     */
    public static String buildRejectRequestUrl(long attendanceId) {
        return REJECT_REQUEST_ENDPOINT.replace("{attendanceId}", String.valueOf(attendanceId));
    }
    
    /**
     * Build URL for close session with session ID
     */
    public static String buildCloseSessionUrl(long sessionId) {
        return CLOSE_SESSION_ENDPOINT.replace("{sessionId}", String.valueOf(sessionId));
    }
    
    /**
     * Build URL for export CSV with session ID
     */
    public static String buildExportCsvUrl(long sessionId) {
        return EXPORT_CSV_ENDPOINT + "?sessionId=" + sessionId;
    }
    
    /**
     * Build URL for export XLSX with session ID
     */
    public static String buildExportXlsxUrl(long sessionId) {
        return EXPORT_XLSX_ENDPOINT + "?sessionId=" + sessionId;
    }
    
    /**
     * Get full API URL with endpoint
     */
    public static String getFullApiUrl(String endpoint) {
        return API_BASE_URL + endpoint;
    }
    
    /**
     * Check if response code indicates success
     */
    public static boolean isSuccessResponse(int responseCode) {
        return responseCode >= 200 && responseCode < 300;
    }
    
    /**
     * Check if response code indicates client error
     */
    public static boolean isClientError(int responseCode) {
        return responseCode >= 400 && responseCode < 500;
    }
    
    /**
     * Check if response code indicates server error
     */
    public static boolean isServerError(int responseCode) {
        return responseCode >= 500 && responseCode < 600;
    }
    
    // =============================================
    // TOAST MESSAGE CONFIGURATION
    // =============================================
    public static final int TOAST_DURATION_ERROR = 10000;  // 10 seconds for errors
    public static final int TOAST_DURATION_SUCCESS = 5000;  // 5 seconds for success
    public static final int TOAST_DURATION_INFO = 6000;  // 6 seconds for info
    public static final int TOAST_DURATION_DEBUG = 7000;  // 7 seconds for debug
}
