package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class Seminar {
    @SerializedName("seminar_id")
    private String seminarId;
    
    @SerializedName("seminar_name")
    private String seminarName;
    
    @SerializedName("seminar_code")
    private String seminarCode;
    
    private String description;
    
    @SerializedName("presenter_id")
    private String presenterId;
    
    public Seminar() {}
    
    public Seminar(String seminarId, String seminarName, String seminarCode, String description, String presenterId) {
        this.seminarId = seminarId;
        this.seminarName = seminarName;
        this.seminarCode = seminarCode;
        this.description = description;
        this.presenterId = presenterId;
    }
    
    // Getters and Setters
    public String getSeminarId() {
        return seminarId;
    }
    
    public void setSeminarId(String seminarId) {
        this.seminarId = seminarId;
    }
    
    public String getSeminarName() {
        return seminarName;
    }
    
    public void setSeminarName(String seminarName) {
        this.seminarName = seminarName;
    }
    
    public String getSeminarCode() {
        return seminarCode;
    }
    
    public void setSeminarCode(String seminarCode) {
        this.seminarCode = seminarCode;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPresenterId() {
        return presenterId;
    }
    
    public void setPresenterId(String presenterId) {
        this.presenterId = presenterId;
    }
    
    @Override
    public String toString() {
        return "Seminar{" +
                "seminarId='" + seminarId + '\'' +
                ", seminarName='" + seminarName + '\'' +
                ", seminarCode='" + seminarCode + '\'' +
                ", description='" + description + '\'' +
                ", presenterId='" + presenterId + '\'' +
                '}';
    }
}
