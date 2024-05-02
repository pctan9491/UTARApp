package com.example.utarapp;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.List;

public class CourseRegistration extends AppCompatActivity implements CourseAdapter.CourseAdapterListener {

    private TextView tvFaculty;
    private ListView lvCourses;
    private Button btnFinalize;

    private List<CourseEntry> coursesList = new ArrayList<>();
    private CourseAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_course_registration);

        // Initialize UI elements
        tvFaculty = findViewById(R.id.tvFaculty);
        lvCourses = findViewById(R.id.lvTimetable);
        btnFinalize = findViewById(R.id.btnFinalize);
        fetchAndDisplayCourses();
        btnFinalize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finalizeRegistration();
            }
        });

    }

    private void fetchAndDisplayCourses() {
        // Assuming you have already set up Firebase
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String loginId = sharedPreferences.getString("LoginId", null);
        Log.d(TAG, "Student's id: " + loginId);

        db.collection("student")
                .whereEqualTo("studentID", loginId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot document = task.getResult().getDocuments().get(0);
                        String studentFaculty = document.getString("course"); // Retrieve student's faculty
                        Log.d(TAG, "Student's faculty: " + studentFaculty);
                        tvFaculty.setText("Faculty: " + studentFaculty);

                        // Now fetch the timetable entries
                        db.collection("timetable")
                                .whereEqualTo("approved", true)
                                .get()
                                .addOnCompleteListener(innerTask -> {
                                    if (innerTask.isSuccessful()) {
                                        for (QueryDocumentSnapshot timetableDocument : innerTask.getResult()) {
                                            String courseCodeFromTimetable = timetableDocument.getString("course");
                                            List<String> registeredStudents = (List<String>) timetableDocument.get("studRegister");

                                            db.collection("course")
                                                    .whereEqualTo("courseCode", courseCodeFromTimetable)
                                                    .get()
                                                    .addOnCompleteListener(courseTask -> {
                                                        if (courseTask.isSuccessful()) {
                                                            for (QueryDocumentSnapshot courseDocument : courseTask.getResult()) {
                                                                List<String> faculties = (List<String>) courseDocument.get("faculty");
                                                                if (faculties != null && faculties.contains(studentFaculty)) {
                                                                    CourseEntry entry = new CourseEntry();

                                                                    entry.setDocumentId(timetableDocument.getId());
                                                                    entry.setCourseTitle(courseDocument.getString("courseTitle"));
                                                                    entry.setCourseCode(timetableDocument.getString("course"));
                                                                    entry.setVenue(timetableDocument.getString("venue"));
                                                                    entry.setStartTime(timetableDocument.getString("startTime"));
                                                                    entry.setEndTime(timetableDocument.getString("endTime"));
                                                                    entry.setDay(timetableDocument.getString("day"));
                                                                    entry.setClassType(timetableDocument.getString("classType"));

                                                                    if (timetableDocument.getLong("maxPersons") != null) {
                                                                        entry.setMaxPersons(timetableDocument.getLong("maxPersons").intValue());
                                                                    }

                                                                    if (registeredStudents != null && registeredStudents.contains(loginId)) {
                                                                        entry.setAlreadyRegistered(true);
                                                                    } else {
                                                                        entry.setAlreadyRegistered(false);
                                                                    }

                                                                    coursesList.add(entry);
                                                                }
                                                            }

                                                            // Display courses in ListView
                                                            adapter = new CourseAdapter((Context) CourseRegistration.this, coursesList, (CourseAdapter.CourseAdapterListener) CourseRegistration.this);
                                                            lvCourses.setAdapter(adapter);
                                                        } else {
                                                            Log.w(TAG, "Error fetching courses.", courseTask.getException());
                                                        }
                                                    });
                                        }
                                    } else {
                                        Log.w(TAG, "Error fetching timetable entries.", innerTask.getException());
                                    }
                                });

                    } else {
                        Log.w(TAG, "No student data found or error fetching student data.", task.getException());
                        Toast.makeText(CourseRegistration.this, "No data found.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void finalizeRegistration() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String studentId = sharedPreferences.getString("LoginId", null);

        WriteBatch batch = db.batch();

        for (CourseEntry course : coursesList) {
            if (course.isSelected()) {
                DocumentReference docRef = db.collection("timetable").document(course.getDocumentId());  // Use the stored document ID
                batch.update(docRef, "studRegister", FieldValue.arrayUnion(studentId));
            }
        }

        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(CourseRegistration.this, "Registration finalized!", Toast.LENGTH_SHORT).show();
                // Refresh the activity
                Intent intent = getIntent();
                finish();
                startActivity(intent);
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error updating documents", e);
                Toast.makeText(CourseRegistration.this, "Failed to finalize registration. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    @Override
    public void deleteRegistration(String documentId) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("timetable").document(documentId);
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String studentId = sharedPreferences.getString("LoginId", null);

        docRef.update("studRegister", FieldValue.arrayRemove(studentId))
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(CourseRegistration.this, "Registration deleted!", Toast.LENGTH_SHORT).show();
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);  // Refresh the courses list after deletion
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(CourseRegistration.this, "Failed to delete registration. Please try again.", Toast.LENGTH_SHORT).show();
                });
    }


    private void commitBatch(WriteBatch batch) {
        batch.commit().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Toast.makeText(CourseRegistration.this, "Registration finalized!", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "Error updating documents", e);
                Toast.makeText(CourseRegistration.this, "Failed to finalize registration. Please try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }





}

