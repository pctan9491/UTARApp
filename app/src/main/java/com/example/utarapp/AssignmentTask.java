package com.example.utarapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AssignmentTask extends BaseActivity {

    private RecyclerView assignmentRecyclerView;
    private AssignmentAdapter assignmentAdapter;
    private List<Assignment> assignmentList;
    private static final String TAG = "AssignmentTaskActivity";
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_task);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        // Setup RecyclerView
        setupRecyclerView();

        // Fetch Assignments
        fetchAssignmentsFromFirestore();

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android 8.0 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("assignment_reminder", "Assignment Reminder", NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }
    }
    private void setupRecyclerView() {
        assignmentRecyclerView = findViewById(R.id.assignmentRecyclerView);
        assignmentList = new ArrayList<>();
        assignmentAdapter = new AssignmentAdapter(assignmentList, new AssignmentAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(String docId) {
                Intent intent = new Intent(AssignmentTask.this, AssignmentDetailActivity.class);
                intent.putExtra("DOC_ID", docId);
                startActivity(intent);
            }
        });

        assignmentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        assignmentRecyclerView.setAdapter(assignmentAdapter);

        // Sort assignments based on the course field
        assignmentList.sort((a1, a2) -> {
            String course1 = a1.getCourse() != null ? a1.getCourse() : "";
            String course2 = a2.getCourse() != null ? a2.getCourse() : "";
            return course1.compareToIgnoreCase(course2);
        });
    }

    private void fetchAssignmentsFromFirestore() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("LoginId", null);
        String userType = sharedPreferences.getString("UserType", null);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference assignmentsCollection = db.collection("assignments");

        assignmentsCollection.addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    Log.w("AssignmentTaskActivity", "Listen failed.", error);
                    return;
                }

                final Map<String, Assignment> uniqueAssignmentsMap = new HashMap<>();
                List<Task<DocumentSnapshot>> tasks = new ArrayList<>();

                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                String currentDateStr = sdf.format(new Date());

                for (QueryDocumentSnapshot assignmentSnapshot : value) {
                    String dueDateStr = assignmentSnapshot.getString("dueDate");
                    try {
                        Date dueDate = sdf.parse(dueDateStr);
                        Date currentDate = sdf.parse(currentDateStr);
                        if (dueDate != null && currentDate != null) {
                            long diffInMillies = dueDate.getTime() - currentDate.getTime();

                            if (diffInMillies > 0 && diffInMillies <= 24 * 60 * 60 * 1000) {
                                // Assignment due within 1 day
                                showReminderNotification(assignmentSnapshot);
                            } else if (dueDate.before(currentDate)) {
                                // Skip this assignment as it's due date is before current date
                                continue;
                            }
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Error parsing dates: ", e);
                        continue;
                    }

                    if (userType != null && userType.equals("lecturer")) {
                        // Check if the assignment's lecId matches the logged-in user's ID
                        String lecId = assignmentSnapshot.getString("lecId");
                        if (lecId != null && lecId.equals(userId)) {
                            Assignment assignment = assignmentSnapshot.toObject(Assignment.class);
                            if (assignment != null) {
                                assignment.setDocId(assignmentSnapshot.getId());
                                List<String> classIds = (List<String>) assignmentSnapshot.get("classes");
                                if (classIds != null && !classIds.isEmpty()) {
                                    String classId = classIds.get(0); // Assuming only one class per assignment for lecturers
                                    Task<DocumentSnapshot> task = db.collection("timetable").document(classId).get();
                                    tasks.add(task);

                                    task.addOnSuccessListener(documentSnapshot -> {
                                        String course = documentSnapshot.getString("course");
                                        assignment.setCourse(course);
                                        Log.d(TAG, "Course: " + course);

                                        // Retrieve the course title from the "course" collection based on the course code
                                        db.collection("course")
                                                .whereEqualTo("courseCode", course)
                                                .get()
                                                .addOnSuccessListener(courseQuerySnapshot -> {
                                                    if (!courseQuerySnapshot.isEmpty()) {
                                                        DocumentSnapshot courseDocument = courseQuerySnapshot.getDocuments().get(0);
                                                        String courseTitle = courseDocument.getString("courseTitle");
                                                        assignment.setCourseTitle(courseTitle);
                                                        Log.d(TAG, "can get in: " + assignment.getCourseTitle());
                                                    }
                                                });
                                        uniqueAssignmentsMap.put(assignmentSnapshot.getId(), assignment);
                                    }).addOnFailureListener(e -> Log.w(TAG, "Error checking class details: ", e));
                                }
                            }
                        }
                    } else {
                        // Handle student case (existing code)
                        List<String> classIds = (List<String>) assignmentSnapshot.get("classes");
                        if (classIds == null || classIds.isEmpty()) continue;

                        for (String classId : classIds) {
                            Task<DocumentSnapshot> task = db.collection("timetable").document(classId).get();
                            tasks.add(task);

                            task.addOnSuccessListener(documentSnapshot -> {
                                List<String> studRegister = (List<String>) documentSnapshot.get("studRegister");
                                if (studRegister != null && studRegister.contains(userId)) {
                                    Assignment assignment = assignmentSnapshot.toObject(Assignment.class);
                                    if (assignment != null) {
                                        assignment.setDocId(assignmentSnapshot.getId());
                                        String course = documentSnapshot.getString("course");
                                        assignment.setCourse(course);
                                        Log.d(TAG, "Course: " + course);

                                        // Retrieve the course title from the "course" collection based on the course code
                                        db.collection("course")
                                                .whereEqualTo("courseCode", course)
                                                .get()
                                                .addOnSuccessListener(courseQuerySnapshot -> {
                                                    if (!courseQuerySnapshot.isEmpty()) {
                                                        DocumentSnapshot courseDocument = courseQuerySnapshot.getDocuments().get(0);
                                                        String courseTitle = courseDocument.getString("courseTitle");
                                                        assignment.setCourseTitle(courseTitle);
                                                        Log.d(TAG, "can get in: " + assignment.getCourseTitle());
                                                    }
                                                });

                                        uniqueAssignmentsMap.put(assignmentSnapshot.getId(), assignment);
                                    }
                                }
                            }).addOnFailureListener(e -> Log.w(TAG, "Error checking class registration: ", e));
                        }
                    }
                }

                Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
                    assignmentList.clear();
                    assignmentList.addAll(uniqueAssignmentsMap.values());
                    assignmentAdapter.notifyDataSetChanged();
                });
            }
        });
    }
    private void showReminderNotification(QueryDocumentSnapshot assignmentSnapshot) {
        Assignment assignment = assignmentSnapshot.toObject(Assignment.class);
        if (assignment == null) return;

        Intent intent = new Intent(this, AssignmentDetailActivity.class);
        intent.putExtra("DOC_ID", assignmentSnapshot.getId());
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "assignment_reminder")
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Assignment Reminder")
                .setContentText(assignment.getTitle() + " is due within 1 day")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        Log.d(TAG, "Notification sent for assignment: " + assignment.getTitle());
    }
}