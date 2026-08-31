package com.example.teacherapp.model;

public class ClassSection {
    private String sectionId;
    private String sectionName;
    private String teacherId;
    private Long createdAt;
    private Long studentCount;

    public ClassSection() {}

    public ClassSection(String sectionId, String sectionName, String teacherId, Long createdAt, Long studentCount) {
        this.sectionId = sectionId;
        this.sectionName = sectionName;
        this.teacherId = teacherId;
        this.createdAt = createdAt;
        this.studentCount = studentCount;
    }

    public String getSectionId() {
        return sectionId;
    }

    public void setSectionId(String sectionId) {
        this.sectionId = sectionId;
    }

    public String getSectionName() {
        return sectionName;
    }

    public void setSectionName(String sectionName) {
        this.sectionName = sectionName;
    }

    public String getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(String teacherId) {
        this.teacherId = teacherId;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getStudentCount() {
        return studentCount;
    }

    public void setStudentCount(Long studentCount) {
        this.studentCount = studentCount;
    }
}
