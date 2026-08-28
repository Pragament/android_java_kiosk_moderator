package com.example.teacherapp.model;

public class WebUsageLog {
    private String host;
    private String studentLog;
    private String studentName;
    private long timestamp;
    private String title;
    private String url;

    public WebUsageLog() {}

    public WebUsageLog(String host, String studentLog, String studentName, long timestamp, String title, String url) {
        this.host = host;
        this.studentLog = studentLog;
        this.studentName = studentName;
        this.timestamp = timestamp;
        this.title = title;
        this.url = url;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getStudentLog() {
        return studentLog;
    }

    public void setStudentLog(String studentLog) {
        this.studentLog = studentLog;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
