package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class PresenterApiKey {
    @SerializedName("api_key_id")
    private String apiKeyId;
    
    @SerializedName("presenter_id")
    private String presenterId;
    
    @SerializedName("api_key")
    private String apiKey;
    
    @SerializedName("created_at")
    private long createdAt;
    
    @SerializedName("is_active")
    private boolean isActive;
    
    public PresenterApiKey() {}
    
    public PresenterApiKey(String apiKeyId, String presenterId, String apiKey, long createdAt, boolean isActive) {
        this.apiKeyId = apiKeyId;
        this.presenterId = presenterId;
        this.apiKey = apiKey;
        this.createdAt = createdAt;
        this.isActive = isActive;
    }
    
    // Getters and Setters
    public String getApiKeyId() {
        return apiKeyId;
    }
    
    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }
    
    public String getPresenterId() {
        return presenterId;
    }
    
    public void setPresenterId(String presenterId) {
        this.presenterId = presenterId;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    @Override
    public String toString() {
        return "PresenterApiKey{" +
                "apiKeyId='" + apiKeyId + '\'' +
                ", presenterId='" + presenterId + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", createdAt=" + createdAt +
                ", isActive=" + isActive +
                '}';
    }
}
