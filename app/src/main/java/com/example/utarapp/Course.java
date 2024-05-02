package com.example.utarapp;

import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class Course {
    private String courseTitle;
    private String courseCode;
    private List<AttendanceRecordClass> attendanceRecords;
    private boolean isExpanded = false;
    private double attendancePercentage;
    private List<AttendanceSession> attendanceSessions = new ArrayList<>();

    public Course() {
        // Default constructor
        attendanceRecords = new ArrayList<>();
    }

    // Getters and setters
    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public boolean isExpanded() {
        return isExpanded;
    }

    public void setExpanded(boolean expanded) {
        isExpanded = expanded;
    }
    public double getAttendancePercentage() {
        return attendancePercentage;
    }

    public void setAttendancePercentage(double attendancePercentage) {
        this.attendancePercentage = attendancePercentage;
    }

    public List<AttendanceRecordClass> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(List<AttendanceRecordClass> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    // Add an attendance record
    public void addAttendanceRecord(AttendanceRecordClass record) {
        this.attendanceRecords.add(record);
    }
    public void setAttendanceSessions(List<AttendanceSession> attendanceSessions) {
        this.attendanceSessions = attendanceSessions;
        Log.d("Course", "Before sorting: " + attendanceSessions);

        // Sort the attendance sessions by class date in descending order
        this.attendanceSessions.sort((s1, s2) -> s1.getDate().compareTo(s2.getDate()));

        Log.d("Course", "After sorting: " + attendanceSessions);
    }

    public List<AttendanceSession> getAttendanceSessions() {
        return attendanceSessions;
    }
}

