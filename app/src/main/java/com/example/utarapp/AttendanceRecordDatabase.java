package com.example.utarapp;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "attendance_records")
public class AttendanceRecordDatabase {
    @PrimaryKey(autoGenerate = true)
    public int id;

    @NonNull
    public String studentId;
    public String scanTime;
    public double latitude;
    public double longitude;
}

