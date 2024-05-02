package com.example.utarapp;

public class AttendanceRecordClass {
    private String scanTime;
    private String venue;
    private String date;
    private String classType;

    public AttendanceRecordClass() {
        // Default constructor required for calls to DataSnapshot.getValue(AttendanceRecord.class)
    }

    public AttendanceRecordClass(String scanTime, String venue, String date, String classType) {
        this.scanTime = scanTime;
        this.venue = venue;
        this.date = date;
        this.classType = classType;
    }

    public String getScanTime() {
        return scanTime;
    }

    public void setScanTime(String scanTime) {
        this.scanTime = scanTime;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    public String getClassType() {
        return classType;
    }

    public void setClassType(String classType) {
        this.classType = classType;
    }
}

