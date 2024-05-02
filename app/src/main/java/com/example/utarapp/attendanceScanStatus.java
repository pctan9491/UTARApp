package com.example.utarapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;
import java.util.Map;

public class attendanceScanStatus extends AppCompatActivity {

    private TextView textSubject;
    private TextView textClassType;
    private TextView textStartTime;
    private TextView textVenue;
    private TextView textDate;
    private TextView textStatus;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_scan_status);
        textSubject = findViewById(R.id.text_subject);
        textClassType = findViewById(R.id.text_class_type);
        textStartTime = findViewById(R.id.text_start_time);
        textVenue = findViewById(R.id.text_venue);
        textDate = findViewById(R.id.text_date);
        textStatus = findViewById(R.id.text_status);

        Bundle attendanceStatusExtras = getIntent().getExtras();
        if (attendanceStatusExtras != null) {
            QRCodeData qrData = attendanceStatusExtras.getParcelable("QR_CODE_DATA");
            if (qrData != null) {
                // Use the QR code data as needed
                String subject = qrData.getSubject();
                String classType = qrData.getClassType();
                String startTime = qrData.getStartTime();
                String venue = qrData.getVenue();
                String date = qrData.getDate();
                SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
                String studId = sharedPreferences.getString("LoginId", null);

                textSubject.setText("Subject: "+subject);
                textClassType.setText("Class Type: " + classType);
                textStartTime.setText("Time: " + startTime);
                textVenue.setText("Venue: " + venue);
                textDate.setText("Date: "+date);
                // check attendance status
                checkAttendanceStatus(subject, classType, startTime, venue, date, studId);
            }
        }
    }

    private void checkAttendanceStatus(String subject, String classType, String startTime, String venue, String date, String studentId) {
        // Assuming you have a Firebase Firestore instance
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Query Firestore for qr_class_info document based on QR code data
        db.collection("qr_class_info")
                .whereEqualTo("subject", subject)
                .whereEqualTo("classType", classType)
                .whereEqualTo("startTime", startTime)
                .whereEqualTo("venue", venue)
                .whereEqualTo("date", date)
                .get()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful() || task.getResult().isEmpty()) {
                        textStatus.setText("Status: Error checking status");
                        return;
                    }
                        boolean attendanceFound = false;
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            // Assuming attendanceRecords is an array of Maps with a studId key
                            List<Map<String, Object>> attendanceRecords = (List<Map<String, Object>>) document.get("attendanceRecords");
                            if (attendanceRecords != null) {
                                for (Map<String, Object> record : attendanceRecords) {
                                    String recordedStudId = (String) record.get("studId");
                                    if (studentId.equals(recordedStudId)) {
                                        attendanceFound = true;
                                        break; // Stop checking once a match is found
                                    }
                                }
                            }
                            if (attendanceFound) {
                                textStatus.setText("Status: Successful");
                                break; // Stop looping through documents once attendance is confirmed
                            } else {
                                textStatus.setText("Status: Unsuccessful");
                            }
                        }
                });
    }
}