package com.example.utarapp;

import java.util.Date;

public class Comment {
    private String userId;
    private String commentText;
    private Date timestamp;
    private String userName;
    private String userType;
    private boolean pinned; // New field to indicate whether the comment is pinned

    public Comment() {
        // Needed for Firestore deserialization
    }

    public Comment(String userId, String commentText, Date timestamp, String userName, String userType, boolean pinned) {
        this.userId = userId;
        this.commentText = commentText;
        this.timestamp = timestamp;
        this.userName = userName;
        this.userType = userType;
        this.pinned = pinned; // Initialize the pinned field
    }

    public String getUserId() {
        return userId;
    }

    public String getCommentText() {
        return commentText;
    }
    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public Date getTimestamp() {
        return timestamp;
    }
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public boolean getPin() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }
}
