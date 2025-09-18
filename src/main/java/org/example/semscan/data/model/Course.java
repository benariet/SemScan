package org.example.semscan.data.model;

import com.google.gson.annotations.SerializedName;

public class Course {
    @SerializedName("course_id")
    private String courseId;
    
    @SerializedName("course_name")
    private String courseName;
    
    @SerializedName("course_code")
    private String courseCode;
    
    private String description;
    
    @SerializedName("lecturer_id")
    private String lecturerId;
    
    @SerializedName("created_at")
    private long createdAt;
    
    public Course() {}
    
    public Course(String courseId, String courseName, String courseCode, String description, long createdAt) {
        this.courseId = courseId;
        this.courseName = courseName;
        this.courseCode = courseCode;
        this.description = description;
        this.createdAt = createdAt;
    }
    
    // Getters and Setters
    public String getCourseId() {
        return courseId;
    }
    
    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }
    
    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }
    
    public String getCourseCode() {
        return courseCode;
    }
    
    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getLecturerId() {
        return lecturerId;
    }
    
    public void setLecturerId(String lecturerId) {
        this.lecturerId = lecturerId;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    @Override
    public String toString() {
        return "Course{" +
                "courseId='" + courseId + '\'' +
                ", courseName='" + courseName + '\'' +
                ", courseCode='" + courseCode + '\'' +
                ", description='" + description + '\'' +
                ", lecturerId='" + lecturerId + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
