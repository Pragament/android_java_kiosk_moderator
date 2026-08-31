package com.example.teacherapp.model;

public class Classroom {
    private String className;
    private String classCode;
    private String createdBy;
    private String creatorId;
    private Long createdDate;
    private String sectionId;
    private String sectionName;

    public Classroom() {}

    public Classroom(String className, String classCode, String createdBy, String creatorId, Long createdDate) {
        this.className = className;
        this.classCode = classCode;
        this.createdBy = createdBy;
        this.creatorId = creatorId;
        this.createdDate = createdDate;
    }

    public Classroom(String className,
                     String classCode,
                     String createdBy,
                     String creatorId,
                     Long createdDate,
                     String sectionId,
                     String sectionName) {
        this(className, classCode, createdBy, creatorId, createdDate);
        this.sectionId = sectionId;
        this.sectionName = sectionName;
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
}
