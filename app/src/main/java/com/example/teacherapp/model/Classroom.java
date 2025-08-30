package com.example.teacherapp.model;

public class Classroom {
    private String className;
    private String classCode;
    private String createdBy;
    private String creatorId;
    private Long createdDate;

    public Classroom() {}

    public Classroom(String className, String classCode, String createdBy, String creatorId, Long createdDate) {
        this.className = className;
        this.classCode = classCode;
        this.createdBy = createdBy;
        this.creatorId = creatorId;
        this.createdDate = createdDate;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getClassCode() {
        return classCode;
    }

    public void setClassCode(String classCode) {
        this.classCode = classCode;
    }

    public String getCreatedBy () {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getCreatorId () {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public Long getCreatedDate () {
        return createdDate;
    }

    public void setCreatedDate(Long createdDate) {
        this.createdDate = createdDate;
    }
}
