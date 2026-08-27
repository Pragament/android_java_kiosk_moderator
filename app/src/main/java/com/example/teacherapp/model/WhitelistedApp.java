package com.example.teacherapp.model;

public class WhitelistedApp {
    private String appName;
    private String packageName;
    private Long addedAt;

    public WhitelistedApp() {}

    public WhitelistedApp(String appName, String packageName, Long addedAt) {
        this.appName = appName;
        this.packageName = packageName;
        this.addedAt = addedAt;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public Long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Long addedAt) {
        this.addedAt = addedAt;
    }
}
