package com.example.utarapp;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.FrameLayout;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AttendanceRecord extends BaseActivity {
    private RecyclerView rvCoursesAttendance;
    private CourseAttendanceAdapter adapter;
    private List<Course> courses = new ArrayList<>();
    private SharedPreferences sharedPreferences;
    private String studId;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_record);

        sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        studId = sharedPreferences.getString("LoginId", null);

        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        setupNavigationBar();
        setupRecyclerView();
        fetchCoursesAndUpdateRecyclerView();
    }

    private void setupNavigationBar() {
        FrameLayout navigationCont = findViewById(R.id.navigation_container);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(navigationCont.getId(), new AttendanceTopBar());
        fragmentTransaction.commit();
    }

    private void setupRecyclerView() {
        rvCoursesAttendance = findViewById(R.id.rvCoursesAttendance);
        rvCoursesAttendance.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAttendanceAdapter(this, courses);
        rvCoursesAttendance.setAdapter(adapter);
    }

    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private List<Course> courseList = new ArrayList<>();
    private AtomicInteger pendingOperations = new AtomicInteger(0);

    private void fetchCoursesAndUpdateRecyclerView() {
        db.collection("timetable")
                .whereArrayContains("studRegister", studId)
                .whereEqualTo("approved", true)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<String> processedCourseCodes = new ArrayList<>();
                        for (QueryDocumentSnapshot timetableDoc : task.getResult()) {
                            String courseCode = timetableDoc.getString("course");
                            // Check if the course code has already been processed
                            if (!processedCourseCodes.contains(courseCode)) {
                                // Add the course code to the list of processed codes
                                processedCourseCodes.add(courseCode);

                                // Proceed to fetch details and attendance for this new course
                                fetchCourseTitleAndAttendance(timetableDoc);
                            }
                        }
                    } else {
                        Log.d("AttendanceRecord", "Error getting documents: ", task.getException());
                    }
                });
    }

    private void fetchCourseTitleAndAttendance(QueryDocumentSnapshot timetableDoc) {
        pendingOperations.incrementAndGet(); // Increment for each async operation
        String courseCode = timetableDoc.getString("course");
        db.collection("course")
                .whereEqualTo("courseCode", courseCode)
                .get()
                .addOnSuccessListener(courseQuerySnapshot -> {
                    if (courseQuerySnapshot.getDocuments().size() > 0) {
                        for (QueryDocumentSnapshot courseDoc : courseQuerySnapshot) {
                            Course course = new Course();
                            course.setCourseCode(courseCode);
                            course.setCourseTitle(courseDoc.getString("courseTitle"));
                            fetchAttendanceRecords(course, timetableDoc);
                        }
                    } else {
                        pendingOperations.decrementAndGet();
                    }
                }).addOnFailureListener(e -> pendingOperations.decrementAndGet());
    }

    private void fetchAttendanceRecords(Course course, QueryDocumentSnapshot timetableDoc) {
        pendingOperations.incrementAndGet();
        String classType = timetableDoc.getString("classType");
        String startTime = timetableDoc.getString("startTime");
        String venue = timetableDoc.getString("venue");

        List<AttendanceSession> attendanceSessions = new ArrayList<>();

        db.collection("qr_class_info")
                .whereEqualTo("subject", course.getCourseCode())
                .get()
                .addOnSuccessListener(qrClassInfoSnapshot -> {
                    Log.d("AttendanceRecord", "Fetched attendance sessions: " + qrClassInfoSnapshot.size());
                    for (QueryDocumentSnapshot qrDoc : qrClassInfoSnapshot) {
                        AttendanceSession session = new AttendanceSession();
                        session.date = qrDoc.getString("date");
                        Log.d("Course", "Created AttendanceSession with date: " + session.date);
                        session.venue = qrDoc.getString("venue");
                        session.classType = qrDoc.getString("classType");
                        session.attended = false; // Initialize as absent
                        attendanceSessions.add(session);
                    }

                    // Check attendance records and mark attended sessions
                    for (QueryDocumentSnapshot qrDoc : qrClassInfoSnapshot) {
                        List<Map<String, Object>> attendanceRecords = (List<Map<String, Object>>) qrDoc.get("attendanceRecords");
                        if (attendanceRecords != null && !attendanceRecords.isEmpty()) {
                            for (Map<String, Object> record : attendanceRecords) {
                                if (record.get("studId").equals(studId)) {
                                    String scanDate = (String) qrDoc.get("date");
                                    String scanClassType = (String) qrDoc.get("classType");
                                    for (AttendanceSession session : attendanceSessions) {
                                        if (session.date.equals(scanDate) && session.classType.equals(scanClassType)) {
                                            session.attended = true;
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Sort the attendance sessions by date in descending order
                    attendanceSessions.sort((s1, s2) -> {
                        SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd yyyy", Locale.getDefault());
                        try {
                            Date date1 = format.parse(s1.date);
                            Date date2 = format.parse(s2.date);
                            return date2.compareTo(date1);
                        } catch (ParseException e) {
                            Log.e("AttendanceRecord", "Error parsing date", e);
                            return 0;
                        }
                    });

                    Log.d("AttendanceRecord", "Sorted attendance sessions: " + attendanceSessions.size());
                    // Add the sorted attendance sessions to the course
                    course.setAttendanceSessions(attendanceSessions);
                    courseList.add(course);
                    Log.d("AttendanceRecord", "Updated courseList size: " + courseList.size());
                    updateRecyclerView();
                })
                .addOnFailureListener(e -> {
                    Log.e("AttendanceRecord", "Error fetching attendance records", e);
                    updateRecyclerView();
                });
    }

    private void updateRecyclerView() {
        Log.d("AttendanceRecord", "Attempting to update RecyclerView with course list of size: " + courseList.size());
        runOnUiThread(() -> {
            if (adapter == null) {
                Log.d("AttendanceRecord", "Initializing adapter for the first time.");
                adapter = new CourseAttendanceAdapter(AttendanceRecord.this, courseList);
                rvCoursesAttendance.setAdapter(adapter);
            } else {
                Log.d("AttendanceRecord", "Adapter already initialized, updating data.");
                adapter.setCourses(courseList);
            }
        });
    }

}
