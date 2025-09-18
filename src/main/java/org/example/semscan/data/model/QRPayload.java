package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class QRPayload {
    @SerializedName("sessionId")
    private String sessionId;
    
    public QRPayload() {}
    
    public QRPayload(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    @Override
    public String toString() {
        return "QRPayload{" +
                "sessionId='" + sessionId + '\'' +
                '}';
    }
}
