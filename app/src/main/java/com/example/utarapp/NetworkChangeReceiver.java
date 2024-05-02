package com.example.utarapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.room.Room;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private FirebaseFirestore db;
    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (isNetworkAvailable(context)) {
            // Network is available, synchronize local database with Firestore
            synchronizeLocalDatabaseWithFirestore(context);
        }
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    private void synchronizeLocalDatabaseWithFirestore(Context context) {
        // Since database operations should not be done on the main thread, use an AsyncTask, Thread, or similar.
        new Thread(() -> {
            AppDatabase localDb = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class, "attendance-database").allowMainThreadQueries().build();
            List<AttendanceRecordDatabase> records = localDb.attendanceRecordDao().getAll();

            FirebaseFirestore db = FirebaseFirestore.getInstance();

            for (AttendanceRecordDatabase record : records) {
                // Convert each AttendanceRecord to a Map or create a custom object to match your Firestore structure
                Map<String, Object> recordMap = new HashMap<>();
                recordMap.put("studId", record.studentId);
                recordMap.put("scanTime", record.scanTime);
                recordMap.put("latitude", record.latitude);
                recordMap.put("longitude", record.longitude);

                // Assume "classInfoId" is stored in each AttendanceRecord for identifying the specific document in "qr_class_info" collection
                String classInfoId = String.valueOf(record.id); // Ensure this property exists and is correctly set in your AttendanceRecord model

                // Firestore upload logic to append to the "attendanceRecords" array
                DocumentReference classInfoDocRef = db.collection("qr_class_info").document(classInfoId);
                classInfoDocRef.update("attendanceRecords", FieldValue.arrayUnion(recordMap))
                        .addOnSuccessListener(aVoid -> {
                            // Delete the record from local database after successful upload
                            localDb.attendanceRecordDao().delete(record);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("NetworkChangeReceiver", "Error appending record to Firestore", e);
                        });
            }
        }).start();
    }
}
