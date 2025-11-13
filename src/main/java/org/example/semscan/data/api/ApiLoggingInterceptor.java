package org.example.semscan.data.api;

import android.content.Context;
import android.util.Log;

import org.example.semscan.utils.ServerLogger;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;

/**
 * Custom OkHttp interceptor that logs all API request and response bodies to ServerLogger (app_logs)
 */
public class ApiLoggingInterceptor implements Interceptor {
    private static final String TAG = "ApiLoggingInterceptor";
    private final Context context;
    private volatile ServerLogger serverLogger; // Lazy-loaded to avoid circular dependency
    
    public ApiLoggingInterceptor(Context context) {
        this.context = context.getApplicationContext();
        // Don't initialize ServerLogger here - lazy load it in intercept() to avoid circular dependency
        // ServerLogger -> ApiClient -> ApiLoggingInterceptor -> ServerLogger (circular!)
    }
    
    /**
     * Get ServerLogger instance lazily to avoid circular dependency during initialization
     */
    private ServerLogger getServerLogger() {
        if (serverLogger == null) {
            synchronized (this) {
                if (serverLogger == null) {
                    serverLogger = ServerLogger.getInstance(context);
                }
            }
        }
        return serverLogger;
    }
    
    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        String method = originalRequest.method();
        String url = originalRequest.url().toString();
        String path = originalRequest.url().encodedPath();
        
        // Check if this is the logs endpoint - skip ServerLogger to avoid recursion
        // but still log to Android Logcat for debugging
        boolean isLogsEndpoint = "/api/v1/logs".equals(path);
        
        // Log request body (read and recreate to avoid consuming)
        String requestBody = null;
        Request request = originalRequest;
        if (originalRequest.body() != null) {
            RequestBody originalRequestBody = originalRequest.body();
            Buffer buffer = new Buffer();
            originalRequestBody.writeTo(buffer);
            byte[] bodyBytes = buffer.readByteArray();
            
            // Read as string for logging
            Charset charset = StandardCharsets.UTF_8;
            MediaType contentType = originalRequestBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(StandardCharsets.UTF_8);
            }
            requestBody = new String(bodyBytes, charset);
            
            if (requestBody != null && !requestBody.isEmpty()) {
                // Always log to Android Logcat
                String requestLog = String.format("Request Body: %s", requestBody);
                Log.d(TAG, method + " " + url + " - " + requestLog);
                
                // Only log to ServerLogger if NOT the logs endpoint (to avoid recursion)
                if (!isLogsEndpoint) {
                    getServerLogger().d(ServerLogger.TAG_API, requestLog);
                }
            }
            
            // Recreate request with new body (since we consumed the original)
            RequestBody newRequestBody = RequestBody.create(contentType, bodyBytes);
            request = originalRequest.newBuilder()
                .method(method, newRequestBody)
                .build();
        }
        
        // Log request details
        String requestDetails = String.format("Request: %s %s", method, path);
        if (requestBody != null && !requestBody.isEmpty()) {
            requestDetails += " - Body: " + requestBody;
        }
        
        // Only log to ServerLogger if NOT the logs endpoint
        if (!isLogsEndpoint) {
            getServerLogger().api(method, path, requestDetails);
        } else {
            // Still log to Android Logcat for debugging
            Log.d(TAG, requestDetails);
        }
        
        // Execute request
        long startTime = System.currentTimeMillis();
        Response response = chain.proceed(request);
        long duration = System.currentTimeMillis() - startTime;
        
        // Log response
        ResponseBody responseBody = response.body();
        String responseBodyString = null;
        
        if (responseBody != null) {
            // Read response body (we need to create a new response body for the chain)
            BufferedSource source = responseBody.source();
            source.request(Long.MAX_VALUE); // Request all bytes
            Buffer buffer = source.buffer();
            
            Charset charset = StandardCharsets.UTF_8;
            MediaType contentType = responseBody.contentType();
            if (contentType != null) {
                charset = contentType.charset(StandardCharsets.UTF_8);
            }
            
            responseBodyString = buffer.clone().readString(charset);
            
            // Always log to Android Logcat
            if (responseBodyString != null && !responseBodyString.isEmpty()) {
                String responseLog = String.format("Response Body: %s", responseBodyString);
                Log.d(TAG, method + " " + url + " - " + responseLog);
                
                // Only log to ServerLogger if NOT the logs endpoint
                if (!isLogsEndpoint) {
                    getServerLogger().d(ServerLogger.TAG_API, responseLog);
                }
            }
            
            // Create new response body with the same content
            ResponseBody newResponseBody = ResponseBody.create(
                responseBody.contentType(),
                responseBodyString
            );
            
            // Build new response with the logged body
            response = response.newBuilder()
                .body(newResponseBody)
                .build();
        }
        
        // Log response details
        int statusCode = response.code();
        String responseDetails = String.format("Status: %d, Duration: %dms", statusCode, duration);
        if (responseBodyString != null && !responseBodyString.isEmpty()) {
            responseDetails += " - Body: " + responseBodyString;
        }
        
        // Only log to ServerLogger if NOT the logs endpoint
        if (!isLogsEndpoint) {
            if (response.isSuccessful()) {
                getServerLogger().apiResponse(method, path, statusCode, responseDetails);
            } else {
                String errorDetails = responseDetails;
                if (responseBodyString != null && !responseBodyString.isEmpty()) {
                    errorDetails = responseDetails;
                }
                getServerLogger().apiError(method, path, statusCode, errorDetails);
            }
        } else {
            // Still log to Android Logcat for debugging
            Log.d(TAG, responseDetails);
        }
        
        return response;
    }
    
}

