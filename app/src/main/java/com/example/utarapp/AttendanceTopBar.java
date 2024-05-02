package com.example.utarapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

public class AttendanceTopBar extends Fragment implements NavigationView.OnNavigationItemSelectedListener {

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        Log.d("Tag", "This is item id " + itemId);
        if (itemId == R.id.btn_attendance_record) {
                Toast.makeText(getActivity(), "Choose Attendance Record", Toast.LENGTH_SHORT).show();
                Intent attRecordIntent = new Intent(getActivity(), AttendanceRecord.class);
                startActivity(attRecordIntent);
                return true;
            }
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_attendance_top_bar, container, false);

        Button attendanceRecordButton = rootView.findViewById(R.id.btn_attendance_record);

        attendanceRecordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getActivity(),"Attendance Record button clicked", Toast.LENGTH_SHORT).show();
                Log.d("TAG", "Attendance Record button clicked");
                Intent attRecordIntent = new Intent(getActivity(), AttendanceRecord.class);
                attRecordIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(attRecordIntent);
            }
        });
        return rootView;
    }
}