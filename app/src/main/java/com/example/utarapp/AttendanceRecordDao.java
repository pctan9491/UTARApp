package com.example.utarapp;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AttendanceRecordDao {
    @Insert
    void insert(AttendanceRecordDatabase record);

    @Query("SELECT * FROM attendance_records")
    List<AttendanceRecordDatabase> getAll();

    @Delete
    void delete(AttendanceRecordDatabase record);
}
