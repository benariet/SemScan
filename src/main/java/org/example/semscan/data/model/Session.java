package org.example.semscan.data.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;

import java.lang.reflect.Type;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Session {
    
    // Custom deserializer for timestamp fields that can be either long or ISO 8601 string
    public static class TimestampDeserializer implements JsonDeserializer<Long> {
        private static final SimpleDateFormat ISO_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
        
        @Override
        public Long deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json.isJsonPrimitive()) {
                if (json.getAsJsonPrimitive().isNumber()) {
                    // Already a number (timestamp)
                    long timestamp = json.getAsLong();
                    android.util.Log.d("Session", "Deserialized timestamp as number: " + timestamp);
                    return timestamp;
                } else if (json.getAsJsonPrimitive().isString()) {
                    // ISO 8601 string format
                    String dateString = json.getAsString();
                    android.util.Log.d("Session", "Deserializing ISO 8601 string: " + dateString);
                    try {
                        Date date = ISO_FORMAT.parse(dateString);
                        long timestamp = date.getTime();
                        android.util.Log.d("Session", "Converted to timestamp: " + timestamp);
                        return timestamp;
                    } catch (ParseException e) {
                        android.util.Log.e("Session", "Failed to parse date: " + dateString, e);
                        throw new JsonParseException("Unable to parse date: " + dateString, e);
                    }
                }
            }
            android.util.Log.w("Session", "Unexpected JSON element type for timestamp: " + json);
            return null;
        }
    }
    @SerializedName(value = "session_id", alternate = {"sessionId", "id"})
    private String sessionId;
    
    @SerializedName(value = "seminar_id", alternate = {"seminarId"})
    private String seminarId;
    
    @SerializedName(value = "start_time", alternate = {"startTime"})
    @JsonAdapter(TimestampDeserializer.class)
    private long startTime;
    
    @SerializedName(value = "end_time", alternate = {"endTime"})
    @JsonAdapter(TimestampDeserializer.class)
    private Long endTime;
    
    private String status; // "open" or "closed"
    
    public Session() {}
    
    public Session(String sessionId, String seminarId, long startTime, Long endTime, String status) {
        this.sessionId = sessionId;
        this.seminarId = seminarId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }
    
    // Getters and Setters
    public String getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
    
    public String getSeminarId() {
        return seminarId;
    }
    
    public void setSeminarId(String seminarId) {
        this.seminarId = seminarId;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }
    
    public Long getEndTime() {
        return endTime;
    }
    
    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public boolean isOpen() {
        return "OPEN".equals(status);
    }
    
    public boolean isClosed() {
        return "CLOSED".equals(status);
    }
    
    public boolean isWithinScanWindow() {
        long currentTime = System.currentTimeMillis();
        long windowEnd = startTime + (15 * 60 * 1000); // 15 minutes in milliseconds
        return currentTime <= windowEnd;
    }
    
    @Override
    public String toString() {
        return "Session{" +
                "sessionId='" + sessionId + '\'' +
                ", seminarId='" + seminarId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", status='" + status + '\'' +
                '}';
    }
}
