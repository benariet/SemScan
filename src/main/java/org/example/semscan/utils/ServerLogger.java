package org.example.semscan.utils;

import android.content.Context;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.content.Context;

import org.example.semscan.data.api.ApiClient;
import org.example.semscan.data.api.ApiService;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Enhanced Logger that sends logs to server
 * Provides both local Android logging and server-side logging
 */
public class ServerLogger {
    
    // Log levels
    public static final int VERBOSE = 0;
    public static final int DEBUG = 1;
    public static final int INFO = 2;
    public static final int WARN = 3;
    public static final int ERROR = 4;
    
    // Tags for different components
    public static final String TAG_UI = "SemScan-UI";
    public static final String TAG_API = "SemScan-API";
    public static final String TAG_QR = "SemScan-QR";
    public static final String TAG_SESSION = "SemScan-Session";
    public static final String TAG_ATTENDANCE = "SemScan-Attendance";
    public static final String TAG_SECURITY = "SemScan-Security";
    public static final String TAG_PERFORMANCE = "SemScan-Performance";
    
    private static ServerLogger instance;
    private Context context;
    private ApiService apiService;
    private ScheduledExecutorService executorService;
    private boolean serverLoggingEnabled = true;
    private String userId;
    private String userRole;
    private java.util.List<LogEntry> pendingLogs = new java.util.ArrayList<>();
    private static final int BATCH_SIZE = 10;
    private static final long BATCH_TIMEOUT_MS = 30000; // 30 seconds
    private static final int MAX_MESSAGE_LENGTH = 10000;
    private static final int MAX_RETRY_ATTEMPTS = 5;
    private static final long BASE_RETRY_DELAY_MS = 2000; // 2s
    private static final long MAX_RETRY_DELAY_MS = 60000; // 60s
    private int currentRetryAttempt = 0;
    private final Gson gson = new Gson();
    private long lastBatchTime = System.currentTimeMillis();
    
    private ServerLogger(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = ApiClient.getInstance(context).getApiService();
        this.executorService = Executors.newSingleThreadScheduledExecutor();
        
        // Get user info for logging context
        PreferencesManager prefs = PreferencesManager.getInstance(context);
        this.userId = prefs.getUserId();
        this.userRole = prefs.getUserRole();

        // Load any persisted pending logs from previous runs
        loadPendingLogs();

        // Try to send any restored logs shortly after startup
        executorService.schedule(this::flushLogs, 2, TimeUnit.SECONDS);
    }
    
    public static synchronized ServerLogger getInstance(Context context) {
        if (instance == null) {
            instance = new ServerLogger(context);
        }
        return instance;
    }
    
    /**
     * Enable or disable server logging
     */
    public void setServerLoggingEnabled(boolean enabled) {
        this.serverLoggingEnabled = enabled;
    }
    
    /**
     * Update user context for logging
     */
    public void updateUserContext(String userId, String userRole) {
        this.userId = userId;
        this.userRole = userRole;
    }
    
    /**
     * Log verbose message
     */
    public void v(String tag, String message) {
        log(VERBOSE, tag, message, null);
    }
    
    /**
     * Log debug message
     */
    public void d(String tag, String message) {
        log(DEBUG, tag, message, null);
    }
    
    /**
     * Log info message
     */
    public void i(String tag, String message) {
        log(INFO, tag, message, null);
    }
    
    /**
     * Log warning message
     */
    public void w(String tag, String message) {
        log(WARN, tag, message, null);
    }
    
    /**
     * Log error message
     */
    public void e(String tag, String message) {
        log(ERROR, tag, message, null);
    }
    
    /**
     * Log error with exception
     */
    public void e(String tag, String message, Throwable throwable) {
        log(ERROR, tag, message, throwable);
    }
    
    /**
     * Log API call
     */
    public void api(String method, String endpoint, String details) {
        String message = String.format("API %s %s - %s", method, endpoint, details);
        log(INFO, TAG_API, message, null);
    }
    
    /**
     * Log API response
     */
    public void apiResponse(String method, String endpoint, int statusCode, String details) {
        String message = String.format("API Response %s %s - Status: %d - %s", method, endpoint, statusCode, details);
        log(INFO, TAG_API, message, null);
    }
    
    /**
     * Log API error
     */
    public void apiError(String method, String endpoint, int statusCode, String error) {
        String message = String.format("API Error %s %s - Status: %d - Error: %s", method, endpoint, statusCode, error);
        log(ERROR, TAG_API, message, null);
    }
    
    /**
     * Log user action
     */
    public void userAction(String action, String details) {
        String message = String.format("User Action: %s - %s", action, details);
        log(INFO, TAG_UI, message, null);
    }
    
    /**
     * Log session event
     */
    public void session(String event, String details) {
        String message = String.format("Session Event: %s - %s", event, details);
        log(INFO, TAG_SESSION, message, null);
    }
    
    /**
     * Log QR code event
     */
    public void qr(String event, String details) {
        String message = String.format("QR Event: %s - %s", event, details);
        log(INFO, TAG_QR, message, null);
    }
    
    /**
     * Log attendance event
     */
    public void attendance(String event, String details) {
        String message = String.format("Attendance Event: %s - %s", event, details);
        log(INFO, TAG_ATTENDANCE, message, null);
    }
    
    /**
     * Log security event
     */
    public void security(String event, String details) {
        String message = String.format("Security Event: %s - %s", event, details);
        log(WARN, TAG_SECURITY, message, null);
    }
    
    /**
     * Log performance event
     */
    public void performance(String event, String details) {
        String message = String.format("Performance Event: %s - %s", event, details);
        log(INFO, TAG_PERFORMANCE, message, null);
    }
    
    /**
     * Log preferences change
     */
    public void prefs(String key, String value) {
        String message = String.format("Preference Changed: %s = %s", key, value);
        log(DEBUG, TAG_UI, message, null);
    }
    
    /**
     * Core logging method
     */
    private void log(int level, String tag, String message, Throwable throwable) {
        // Create log entry
        LogEntry logEntry = createLogEntry(level, tag, message, throwable);
        
        // Log to Android system
        logToAndroid(level, tag, message, throwable);
        
        // Send to server if enabled
        if (serverLoggingEnabled) {
            sendToServer(logEntry);
        }
    }
    
    /**
     * Create structured log entry
     */
    private LogEntry createLogEntry(int level, String tag, String message, Throwable throwable) {
        LogEntry entry = new LogEntry();
        entry.timestamp = System.currentTimeMillis();
        entry.level = getLevelString(level);
        entry.tag = tag;
        entry.message = truncate(message, MAX_MESSAGE_LENGTH);
        entry.userId = this.userId;
        entry.userRole = this.userRole;
        entry.deviceInfo = getDeviceInfo();
        entry.appVersion = getAppVersion();
        
        if (throwable != null) {
            entry.stackTrace = getStackTrace(throwable);
            entry.exceptionType = throwable.getClass().getSimpleName();
        }
        
        return entry;
    }
    
    /**
     * Log to Android system
     */
    private void logToAndroid(int level, String tag, String message, Throwable throwable) {
        switch (level) {
            case VERBOSE:
                Log.v(tag, message, throwable);
                break;
            case DEBUG:
                Log.d(tag, message, throwable);
                break;
            case INFO:
                Log.i(tag, message, throwable);
                break;
            case WARN:
                Log.w(tag, message, throwable);
                break;
            case ERROR:
                Log.e(tag, message, throwable);
                break;
        }
    }
    
    /**
     * Send log to server (batched)
     */
    private void sendToServer(LogEntry logEntry) {
        synchronized (pendingLogs) {
            pendingLogs.add(logEntry);
            persistPendingLogs();
            
            // Send immediately for errors or when batch is full
            boolean shouldSend = logEntry.level.equals("ERROR") || 
                               pendingLogs.size() >= BATCH_SIZE ||
                               (System.currentTimeMillis() - lastBatchTime) > BATCH_TIMEOUT_MS;
            
            if (shouldSend) {
                sendBatchedLogsToServer();
            }
        }
    }
    
    /**
     * Send batched logs to server
     */
    private void sendBatchedLogsToServer() {
        if (pendingLogs.isEmpty()) return;
        if (!isNetworkAvailable()) {
            scheduleRetry();
            return;
        }
        
        executorService.execute(() -> {
            try {
                // Create a copy of pending logs and clear the original
                java.util.List<LogEntry> logsToSend;
                synchronized (pendingLogs) {
                    logsToSend = new java.util.ArrayList<>(pendingLogs);
                    pendingLogs.clear();
                    lastBatchTime = System.currentTimeMillis();
                    persistPendingLogs();
                }
                
                // Debug logging
                Log.d(TAG_API, "Sending " + logsToSend.size() + " logs to server");
                
                // Create API request
                LogRequest request = new LogRequest(logsToSend);
                
                // Send to server
                String apiKey = PreferencesManager.getInstance(context).getPresenterApiKey();
                Log.d(TAG_API, "Using API key: " + (apiKey != null ? apiKey.substring(0, Math.min(10, apiKey.length())) + "..." : "null"));
                Call<LogResponse> call = apiService.sendLogs(apiKey, request);
                call.enqueue(new Callback<LogResponse>() {
                    @Override
                    public void onResponse(Call<LogResponse> call, Response<LogResponse> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG_API, "Logs sent successfully: " + logsToSend.size() + " entries");
                            currentRetryAttempt = 0; // reset backoff on success
                        } else {
                            Log.w(TAG_API, "Failed to send logs to server: " + response.code());
                            // Re-add logs to pending list for retry
                            synchronized (pendingLogs) {
                                pendingLogs.addAll(0, logsToSend);
                                persistPendingLogs();
                            }
                            scheduleRetry();
                        }
                    }
                    
                    @Override
                    public void onFailure(Call<LogResponse> call, Throwable t) {
                        Log.w(TAG_API, "Failed to send logs to server", t);
                        // Re-add logs to pending list for retry
                        synchronized (pendingLogs) {
                            pendingLogs.addAll(0, logsToSend);
                            persistPendingLogs();
                        }
                        scheduleRetry();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG_API, "Error sending logs to server", e);
                scheduleRetry();
            }
        });
    }
    
    /**
     * Force send all pending logs
     */
    public void flushLogs() {
        synchronized (pendingLogs) {
            if (!pendingLogs.isEmpty()) {
                sendBatchedLogsToServer();
            }
        }
    }

    /**
     * Exponential backoff scheduler for retries
     */
    private void scheduleRetry() {
        if (pendingLogs.isEmpty()) return;
        if (currentRetryAttempt >= MAX_RETRY_ATTEMPTS) {
            Log.w(TAG_API, "Max retry attempts reached. Dropping " + pendingLogs.size() + " pending logs.");
            synchronized (pendingLogs) {
                pendingLogs.clear();
                persistPendingLogs();
            }
            currentRetryAttempt = 0;
            return;
        }

        long delay = (long) Math.min(MAX_RETRY_DELAY_MS, BASE_RETRY_DELAY_MS * Math.pow(2, currentRetryAttempt));
        currentRetryAttempt++;
        Log.d(TAG_API, "Scheduling retry attempt " + currentRetryAttempt + " in " + delay + "ms");
        executorService.schedule(() -> {
            synchronized (pendingLogs) {
                if (!pendingLogs.isEmpty()) {
                    sendBatchedLogsToServer();
                }
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Persist pending logs to local storage for offline durability
     */
    private void persistPendingLogs() {
        try {
            String json = gson.toJson(pendingLogs);
            context.getSharedPreferences("semscan_logs", Context.MODE_PRIVATE)
                    .edit()
                    .putString("pending_logs", json)
                    .apply();
        } catch (Exception e) {
            Log.w(TAG_API, "Failed to persist pending logs", e);
        }
    }

    /**
     * Load pending logs from local storage
     */
    private void loadPendingLogs() {
        try {
            String json = context.getSharedPreferences("semscan_logs", Context.MODE_PRIVATE)
                    .getString("pending_logs", null);
            if (json != null && !json.isEmpty()) {
                java.lang.reflect.Type listType = new TypeToken<java.util.List<LogEntry>>() {}.getType();
                java.util.List<LogEntry> restored = gson.fromJson(json, listType);
                if (restored != null && !restored.isEmpty()) {
                    synchronized (pendingLogs) {
                        pendingLogs.addAll(restored);
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG_API, "Failed to load persisted pending logs", e);
        }
    }

    /**
     * Check if network is available for sending logs
     */
    private boolean isNetworkAvailable() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            NetworkCapabilities capabilities = cm.getNetworkCapabilities(cm.getActiveNetwork());
            return capabilities != null && (
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
            );
        } catch (Exception e) {
            return true; // fallback to allow attempts
        }
    }

    /**
     * Truncate oversized messages to server limit
     */
    private String truncate(String s, int max) {
        if ( s == null ) return null;
        if ( s.length() <= max ) return s;
        return s.substring(0, max);
    }
    
    /**
     * Get level string
     */
    private String getLevelString(int level) {
        switch (level) {
            case VERBOSE: return "VERBOSE";
            case DEBUG: return "DEBUG";
            case INFO: return "INFO";
            case WARN: return "WARN";
            case ERROR: return "ERROR";
            default: return "UNKNOWN";
        }
    }
    
    /**
     * Get device information
     */
    private String getDeviceInfo() {
        return String.format("Android %s - %s %s", 
            android.os.Build.VERSION.RELEASE,
            android.os.Build.MANUFACTURER,
            android.os.Build.MODEL);
    }
    
    /**
     * Get app version
     */
    private String getAppVersion() {
        try {
            return context.getPackageManager()
                .getPackageInfo(context.getPackageName(), 0)
                .versionName;
        } catch (Exception e) {
            return "Unknown";
        }
    }
    
    /**
     * Get stack trace as string
     */
    private String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
    
    /**
     * Log entry data class
     */
    public static class LogEntry {
        public Long timestamp;
        public String level;
        public String tag;
        public String message;
        public String userId;
        public String userRole;
        public String deviceInfo;
        public String appVersion;
        public String stackTrace;
        public String exceptionType;
        
        // Default constructor for JSON serialization
        public LogEntry() {}
        
        // Constructor for easy creation
        public LogEntry(Long timestamp, String level, String tag, String message, 
                       String userId, String userRole, String deviceInfo, String appVersion) {
            this.timestamp = timestamp;
            this.level = level;
            this.tag = tag;
            this.message = message;
            this.userId = userId;
            this.userRole = userRole;
            this.deviceInfo = deviceInfo;
            this.appVersion = appVersion;
        }
        
        // Getters and setters for JSON serialization
        public Long getTimestamp() { return timestamp; }
        public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
        
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
        
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        
        public String getUserRole() { return userRole; }
        public void setUserRole(String userRole) { this.userRole = userRole; }
        
        public String getDeviceInfo() { return deviceInfo; }
        public void setDeviceInfo(String deviceInfo) { this.deviceInfo = deviceInfo; }
        
        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
        
        public String getStackTrace() { return stackTrace; }
        public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
        
        public String getExceptionType() { return exceptionType; }
        public void setExceptionType(String exceptionType) { this.exceptionType = exceptionType; }
    }
    
    /**
     * Log request data class
     */
    public static class LogRequest {
        public java.util.List<LogEntry> logs;
        
        public LogRequest() {}
        
        public LogRequest(java.util.List<LogEntry> logs) {
            this.logs = logs;
        }
        
        public java.util.List<LogEntry> getLogs() { return logs; }
        public void setLogs(java.util.List<LogEntry> logs) { this.logs = logs; }
    }
    
    /**
     * Log response data class
     */
    public static class LogResponse {
        public boolean success;
        public String message;
        public int processedCount;
        
        public LogResponse() {}
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        
        public int getProcessedCount() { return processedCount; }
        public void setProcessedCount(int processedCount) { this.processedCount = processedCount; }
    }
}
