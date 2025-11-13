package org.example.semscan.data.api;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import org.example.semscan.constants.ApiConstants;

public class ApiClient {
    private static final String DEFAULT_BASE_URL = ApiConstants.SERVER_URL;
    private static ApiClient instance;
    private ApiService apiService;
    private String currentBaseUrl;
    private Context context;
    
    private ApiClient(Context context) {
        this.context = context.getApplicationContext();
        currentBaseUrl = normalizeBaseUrl(DEFAULT_BASE_URL);
        
        // Log the current API URL for debugging
        android.util.Log.d("ApiClient", "Current API Base URL: " + currentBaseUrl);
        
        createApiService();
    }
    
    private void createApiService() {
        // Standard HTTP logging for Android Logcat
        HttpLoggingInterceptor httpLogging = new HttpLoggingInterceptor();
        httpLogging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        // Custom interceptor for ServerLogger (app_logs) - logs request/response bodies
        ApiLoggingInterceptor apiLogging = new ApiLoggingInterceptor(context);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(httpLogging) // Android Logcat logging
                .addInterceptor(apiLogging)  // ServerLogger (app_logs) logging
                .connectTimeout(ApiConstants.CONNECTION_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(ApiConstants.READ_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(ApiConstants.WRITE_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        
        android.util.Log.d("ApiClient", "Creating Retrofit with base URL: " + currentBaseUrl);
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        apiService = retrofit.create(ApiService.class);
    }
    
    public static synchronized ApiClient getInstance(Context context) {
        String currentUrl = normalizeBaseUrl(DEFAULT_BASE_URL);
        
        // If instance is null or URL has changed, create new instance
        if (instance == null || !instance.currentBaseUrl.equals(currentUrl)) {
            android.util.Log.d("ApiClient", "Creating new instance - URL changed from " + 
                (instance != null ? instance.currentBaseUrl : "null") + " to " + currentUrl);
            instance = new ApiClient(context);
        }
        return instance;
    }
    
    public ApiService getApiService() {
        return apiService;
    }
    
    public String getCurrentBaseUrl() {
        return currentBaseUrl;
    }
    
    private static String normalizeBaseUrl(String url) {
        if (url == null || url.trim().isEmpty()) {
            return "http://localhost:8080/";
        }
        String trimmed = url.trim();
        if (!trimmed.endsWith("/")) {
            trimmed += "/";
        }
        return trimmed;
    }
}
