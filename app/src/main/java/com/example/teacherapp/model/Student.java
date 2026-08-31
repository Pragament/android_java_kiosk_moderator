package com.example.teacherapp.model;

public class Student {
    private String admissionNo;
    private String name;
    private String phone;

    public Student() {}

    public Student(String admissionNo, String name, String phone) {
        this.admissionNo = admissionNo;
        this.name = name;
        this.phone = phone;
    }

    public String getAdmissionNo() {
        return admissionNo;
    }

    public void setAdmissionNo(String admissionNo) {
        this.admissionNo = admissionNo;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }
}
