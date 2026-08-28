package com.example.teacherapp.model;

public class WhitelistedWebsite {
    private String host;
    private String title;
    private String url;
    private Long addedAt;

    public WhitelistedWebsite() {}

    public WhitelistedWebsite(String host, String title, String url, Long addedAt) {
        this.host = host;
        this.title = title;
        this.url = url;
        this.addedAt = addedAt;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
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

    public Long getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Long addedAt) {
        this.addedAt = addedAt;
    }
}
