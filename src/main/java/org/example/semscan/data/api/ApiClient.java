package org.example.semscan.data.api;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import org.example.semscan.constants.ApiConstants;
import org.example.semscan.utils.PreferencesManager;

public class ApiClient {
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/"; // Android emulator localhost
    private static ApiClient instance;
    private ApiService apiService;
    private String currentBaseUrl;
    
    private ApiClient(Context context) {
        // Force use of localhost for debugging
        currentBaseUrl = "http://localhost:8080/";
        
        // Log the current API URL for debugging
        android.util.Log.d("ApiClient", "Current API Base URL: " + currentBaseUrl);
        
        createApiService();
    }
    
    private void createApiService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
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
        // Force use of localhost for debugging - ignore preferences
        String currentUrl = "http://localhost:8080/";
        
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
    
    public void updateBaseUrl(Context context) {
        // Get the latest API URL from preferences
        PreferencesManager preferencesManager = PreferencesManager.getInstance(context);
        String newBaseUrl = preferencesManager.getApiBaseUrl();
        
        // Ensure URL ends with /
        if (!newBaseUrl.endsWith("/")) {
            newBaseUrl += "/";
        }
        
        // Only recreate if URL changed
        if (!newBaseUrl.equals(currentBaseUrl)) {
            currentBaseUrl = newBaseUrl;
            createApiService();
        }
    }
}
