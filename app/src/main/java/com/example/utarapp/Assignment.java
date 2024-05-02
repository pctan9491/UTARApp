package com.example.utarapp;

public class Assignment {
    private String title;
    private String dueDate;
    private String description;
    private String documentUrl;
    private String docId;
    private String course;
    private String courseTitle;

    public Assignment() {
        // Default constructor required for Firestore
    }

    public Assignment(String title, String dueDate, String description, String documentUrl) {
        this.title = title;
        this.dueDate = dueDate;
        this.description = description;
        this.documentUrl = documentUrl;
    }

    // Getters and setters
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDueDate() { return dueDate; }
    public void setDueDate(String dueDate) { this.dueDate = dueDate; }

    public String getDocId() {
        return docId;
    }

    public void setDocId(String docId) {
        this.docId = docId;
    }
    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }
    public String getDocumentUrl() {
        return documentUrl;
    }

    public void setDocumentUrl(String documentUrl) {
        this.documentUrl = documentUrl;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }
}
