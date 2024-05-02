package com.example.utarapp;

import com.google.firebase.firestore.DocumentSnapshot;

import java.util.Date;

public class TimetableEntryWithDate {
    private DocumentSnapshot documentSnapshot;
    private Date date;

    public TimetableEntryWithDate(DocumentSnapshot documentSnapshot, Date date) {
        this.documentSnapshot = documentSnapshot;
        this.date = date;
    }

    // Getters and setters
    public DocumentSnapshot getDocumentSnapshot() {
        return documentSnapshot;
    }

    public Date getDate() {
        return date;
    }
}
