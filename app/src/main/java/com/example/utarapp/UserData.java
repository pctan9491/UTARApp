package com.example.utarapp;

public class UserData {
    private static final UserData ourInstance = new UserData();

    public static UserData getInstance() {
        return ourInstance;
    }

    private String loginId;
    private String userType;

    private UserData() {
    }

    public String getLoginId() {
        return loginId;
    }
    public String getUserType() {
        return userType;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }
    public void setUserType(String userType) {
        this.userType = userType;
    }
    // Clears the user's session data
    public void clearSession() {
        loginId = null;
        userType = null;
        // Clear other fields as needed...
    }
}

