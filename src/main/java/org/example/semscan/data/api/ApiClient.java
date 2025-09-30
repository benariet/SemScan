package org.example.semscan.data.api;

import android.content.Context;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import org.example.semscan.utils.PreferencesManager;

public class ApiClient {
    private static final String DEFAULT_BASE_URL = "http://10.0.2.2:8080/"; // Android emulator localhost
    private static ApiClient instance;
    private ApiService apiService;
    private String currentBaseUrl;
    
    private ApiClient(Context context) {
        // Get the saved API URL from preferences
        PreferencesManager preferencesManager = PreferencesManager.getInstance(context);
        currentBaseUrl = preferencesManager.getApiBaseUrl();
        
        // Ensure URL ends with /
        if (!currentBaseUrl.endsWith("/")) {
            currentBaseUrl += "/";
        }
        
        createApiService();
    }
    
    private void createApiService() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .build();
        
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(currentBaseUrl)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        
        apiService = retrofit.create(ApiService.class);
    }
    
    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context);
        }
        return instance;
    }
    
    public ApiService getApiService() {
        return apiService;
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
