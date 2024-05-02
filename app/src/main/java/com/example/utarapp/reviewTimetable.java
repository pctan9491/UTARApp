package com.example.utarapp;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

public class reviewTimetable extends BaseActivity implements reviewTimetableBtmBar.OnDaySelectedListener, reviewTimetableBtmBar.OnAllButtonClickListener{
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private String intakeTime;
    private Date startDate, endDate;
    private Spinner spinner;
    private String loginId;
    private RecyclerView recyclerView;
    private TimetableAdapter adapter;
    private List<TimetableEntry> entries = new ArrayList<>();
    private static final List<String> ORDERED_DAYS = Arrays.asList("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday");

    private List<TimetableEntry> allEntriesForTheWeek = new ArrayList<>();
    private long oneDayMillis = 24 * 60 * 60 * 1000;
    private SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_timetable);

        // For display Timetable used
        //set up the RecyclerView and provide the adapter.
        RecyclerView timetableRecyclerView = findViewById(R.id.timetable_recycler_view);
        adapter = new TimetableAdapter();
        timetableRecyclerView.setAdapter(adapter);
        timetableRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        fetchIntakeDetails();
        //Navigation bar btm
        FrameLayout navigationCont = findViewById(R.id.navigation_container);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.add(navigationCont.getId(), new reviewTimetableBtmBar());
        fragmentTransaction.commit();

        //Spinner for week
        spinner = (Spinner) findViewById(R.id.spinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this, R.array.weeks, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.activity_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Enable offline persistence for Cloud Firestore
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        Button convertButton = findViewById(R.id.convert_button);
        convertButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(reviewTimetable.this, TimetableTableActivity.class);
                intent.putParcelableArrayListExtra("timetableEntries", new ArrayList<>(allEntriesForTheWeek));
                startActivity(intent);
            }
        });

        Button setReminderButton = findViewById(R.id.set_reminder_button);
        setReminderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Get the selected timetable entry
                TimetableEntry selectedEntry = getSelectedTimetableEntry();
                if (selectedEntry != null) {
                    // Schedule the reminder notification
                    scheduleReminderNotification(selectedEntry);
                }
            }
        });
    }

    private TimetableEntry getSelectedTimetableEntry() {
        if (adapter != null) {
            return adapter.getSelectedEntry();
        }
        return null;
    }
    private void scheduleReminderNotification(TimetableEntry entry) {
        // Parse the class start time
        String startTime = entry.getSubjectTime().split("-")[0];
        SimpleDateFormat format = new SimpleDateFormat("HH:mm", Locale.US);
        try {
            Date classStartTime = format.parse(startTime);
            Calendar reminderCalendar = Calendar.getInstance();

            // Set the reminder date based on the class date
            reminderCalendar.setTime(entry.getDate());

            // Set the reminder time one hour before the class start time
            reminderCalendar.set(Calendar.HOUR_OF_DAY, classStartTime.getHours());
            reminderCalendar.set(Calendar.MINUTE, classStartTime.getMinutes());
            reminderCalendar.set(Calendar.SECOND, 0);
            reminderCalendar.add(Calendar.HOUR_OF_DAY, -1);

            // Create an intent for the reminder broadcast
            Intent reminderIntent = new Intent(this, ReminderService.class);
            reminderIntent.putExtra("subjectCode", entry.getSubjectCode());
            reminderIntent.putExtra("subjectTime", entry.getSubjectTime());
            reminderIntent.putExtra("subjectVenue", entry.getSubjectVenue());

            // Create a unique request code for the reminder
            int requestCode = entry.getTimetableId().hashCode();

            // Schedule the reminder using AlarmManager
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, requestCode, reminderIntent, PendingIntent.FLAG_IMMUTABLE);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, reminderCalendar.getTimeInMillis(), pendingIntent);

            Toast.makeText(this, "Reminder set for " + entry.getSubjectCode(), Toast.LENGTH_SHORT).show();
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
    public int getCurrentWeekNumber() {
        Spinner weekSpinner = findViewById(R.id.spinner);
        return weekSpinner.getSelectedItemPosition() + 1; // Assuming the weeks start from "Week 1"
    }

    private Date getDateForDayOfWeek(String day) {
        Calendar calendar = Calendar.getInstance();
        switch (day) {
            case "Monday":
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                break;
            case "Tuesday":
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY);
                break;
            case "Wednesday":
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY);
                break;
            case "Thursday":
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY);
                break;
            case "Friday":
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
                break;
            case "Saturday":
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY);
                break;
            case "Sunday":
                calendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
                break;
        }
        return calendar.getTime();
    }


    @Override
    public void onDaySelected(String day) {
        Log.d("TIMETABLE", "Day selected: " + day);
        fetchAndDisplayTimetableForDay(day);
        Date currentDayDate = getDateForDayOfWeek(day);
        Log.d("TIMETABLE", "Date derived for selected day: " + currentDayDate.toString());
        fetchAndIntegrateReplacementsForDate(currentDayDate, entries, () -> {
            // Once replacements are integrated, fetch the cancellations
            fetchAndIntegrateCancellationsForDate(currentDayDate, entries);
        });
    }


    @Override
    public void onAllButtonClick(int weekNumber) {
        // Call your function to fetch and display the timetable for the entire week
        fetchAndDisplayTimetableForWeek(weekNumber);
    }


    private void fetchAndDisplayTimetableForDay(String day) {
        Log.d("TIMETABLE", "Fetching timetable for: " + day);
        entries.clear();
        List<TimetableEntry> emptyList = new ArrayList<>();
        adapter.setTimetableEntries(emptyList);

        // Get today's date
        Date currentDate = new Date();

        List<TimetableEntry> entriesForTheDay = new ArrayList<>();
        Log.d("TIMETABLE", "Fetching data for: " + day);

        db.collection("timetable")
                .whereArrayContains("studRegister", loginId)
                .whereEqualTo("day", day)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Log.d("TIMETABLE", "Successfully fetched data for: " + day);

                    // Convert the fetched DocumentSnapshots into TimetableEntry objects and add them to the list.
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        TimetableEntry entry = new TimetableEntry();
                        entry.setTimetableId(doc.getId()); // Set the timetableId
                        entry.setSubjectCode(doc.getString("course"));
                        entry.setSubjectName(""); // You'll need to fetch this later or set it up differently.
                        entry.setSubjectClass(doc.getString("classType"));
                        entry.setSubjectDay(doc.getString("day"));
                        entry.setSubjectTime(doc.getString("startTime") + "-" + doc.getString("endTime"));
                        entry.setSubjectVenue(doc.getString("venue"));
                        entry.setDate(currentDate); // Use current date for simplicity
                        entriesForTheDay.add(entry);
                    }
                    TextView noEntriesTextView = findViewById(R.id.no_entries_text_view);
                    if (entriesForTheDay.isEmpty()) {
                        noEntriesTextView.setText("No entries found for " + day + ".");
                        noEntriesTextView.setVisibility(View.VISIBLE);
                        return;
                    }else {
                        noEntriesTextView.setVisibility(View.GONE);
                        adapter.setTimetableEntries(entriesForTheDay);
                    }

                    // Sort entries by start time
                    Collections.sort(entriesForTheDay, (entry1, entry2) -> {
                        String startTime1 = entry1.getSubjectTime().split("-")[0];
                        String startTime2 = entry2.getSubjectTime().split("-")[0];
                        return startTime1.compareTo(startTime2);
                    });

                    Log.d("TIMETABLE", "Updating adapter for: " + day);
                    adapter.setTimetableEntries(entriesForTheDay);
                    Date currentDayDate = getDateForDayOfWeek(day);
                    fetchAndIntegrateReplacementsForDate(currentDayDate, entriesForTheDay, () -> {
                        // Once replacements are integrated, fetch the cancellations
                        fetchAndIntegrateCancellationsForDate(currentDayDate, entriesForTheDay);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("TIMETABLE", "Error fetching timetable", e);
                });
    }


    private class CustomArrayAdapter extends ArrayAdapter<String> {
        public CustomArrayAdapter(Context context, List<String> items) {
            super(context, R.layout.spinner_item, items);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            View view = super.getDropDownView(position, convertView, parent);
            view.setBackgroundColor(getResources().getColor(R.color.white)); // Replace with your color.
            return view;
        }
    }

    private void fetchIntakeDetails() {
        // Get the loginId and userType
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        loginId = sharedPreferences.getString("LoginId", null);
        String userType = sharedPreferences.getString("UserType", null);

        if (userType != null) {
            if (userType.equals("student")) {
                // Fetch timetable for students
                db.collection("timetable")
                        .whereArrayContains("studRegister", loginId)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                intakeTime = queryDocumentSnapshots.getDocuments().get(0).getString("intakeTime");
                                fetchIntakeDetails(intakeTime);
                            }
                        });
            } else if (userType.equals("lecturer")) {
                // Fetch timetable for lecturers
                db.collection("timetable")
                        .whereEqualTo("lecturerTutor", loginId)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            if (!queryDocumentSnapshots.isEmpty()) {
                                intakeTime = queryDocumentSnapshots.getDocuments().get(0).getString("intakeTime");
                                fetchIntakeDetails(intakeTime);
                            }
                        });
            }
        }
    }

    private void fetchIntakeDetails(String intakeTime) {
        db.collection("intake")
                .whereEqualTo("intake", intakeTime)
                .get()
                .addOnSuccessListener(intakeSnapshots -> {
                    if (!intakeSnapshots.isEmpty()) {
                        DocumentSnapshot intakeDocument = intakeSnapshots.getDocuments().get(0);
                        startDate = intakeDocument.getDate("startDate");
                        endDate = intakeDocument.getDate("endDate");

                        // Populate the spinner
                        populateWeeksSpinner();

                        // Initialize the Spinner listener here after populating it
                        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                                Log.d("TIMETABLE", "Spinner item selected: " + position);
                                fetchAndDisplayTimetableForWeek(position + 1);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parentView) {
                                Log.d("TIMETABLE", "Nothing selected in the spinner");
                            }
                        });
                    }
                });
    }


    private void populateWeeksSpinner() {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        long diff = endDate.getTime() - startDate.getTime();
        int weeks = (int) (diff / (7 * 24 * 60 * 60 * 1000));

        List<String> weekList = new ArrayList<>();
        for (int i = 1; i <= weeks; i++) {
            weekList.add("Week " + i);
        }

        Spinner spinner = findViewById(R.id.spinner);
        CustomArrayAdapter adapter = new CustomArrayAdapter(this, weekList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        // Calculate the current week number
        Date currentDate = new Date();
        long diffInDays = (currentDate.getTime() - startDate.getTime()) / (24 * 60 * 60 * 1000);
        int currentWeekNumber = (int) (diffInDays / 7);

        // Set the spinner to the current week
        if (currentWeekNumber >= 0 && currentWeekNumber < weeks) {
            spinner.setSelection(currentWeekNumber);
        }
    }


    private void fetchAndDisplayTimetableForWeek(int weekNumber) {
        allEntriesForTheWeek.clear();
        TextView noEntriesTextView = findViewById(R.id.no_entries_text_view);
        RecyclerView timetableRecyclerView = findViewById(R.id.timetable_recycler_view);
        Log.d("TIMETABLE", "Fetching timetable for week: " + weekNumber);

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userType = sharedPreferences.getString("UserType", null);

        TextView dateRangeTextView = findViewById(R.id.date_range);
        Date weekStartDate = getWeekStartAndEndDates(weekNumber)[0];
        Date weekEndDate = getWeekStartAndEndDates(weekNumber)[1];

        String dateRangeText = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(weekStartDate)
                + " - "
                + new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(weekEndDate);
        dateRangeTextView.setText(dateRangeText);

        final CountDownLatch latch = new CountDownLatch(7);
        for (int i = 0; i < 7; i++) {
            Date currentDayDate = new Date(weekStartDate.getTime() + i * oneDayMillis);
            String currentDayName = dayFormat.format(currentDayDate);

            if (userType.equals("student")) {
            db.collection("timetable")
                    .whereArrayContains("studRegister", loginId)
                    .whereEqualTo("day", currentDayName)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        // Convert the fetched DocumentSnapshots into TimetableEntry objects and add them to the list.
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            TimetableEntry entry = new TimetableEntry();
                            entry.setTimetableId(doc.getId());
                            entry.setSubjectCode(doc.getString("course"));
                            entry.setSubjectName(""); // You'll need to fetch this later or set it up differently.
                            entry.setSubjectClass(doc.getString("classType"));
                            entry.setSubjectDay(doc.getString("day"));
                            entry.setSubjectTime(doc.getString("startTime") + "-" + doc.getString("endTime"));
                            entry.setSubjectVenue(doc.getString("venue"));
                            entry.setDate(currentDayDate);
                            allEntriesForTheWeek.add(entry);
                        }
                        // After fetching the regular timetable, fetch the replacement data for that day
                        fetchAndIntegrateReplacementsForDate(currentDayDate, allEntriesForTheWeek, () -> {
                            // Once replacements are integrated, fetch the cancellations
                            fetchAndIntegrateCancellationsForDate(currentDayDate, allEntriesForTheWeek);
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TIMETABLE", "Error fetching timetable", e);
                        latch.countDown();
                    });
        } else if (userType.equals("lecturer")) {
                db.collection("timetable")
                        .whereEqualTo("lecturerTutor", loginId)
                        .whereEqualTo("day", currentDayName)
                        .get()
                        .addOnSuccessListener(queryDocumentSnapshots -> {
                            // Convert the fetched DocumentSnapshots into TimetableEntry objects and add them to the list.
                            for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                                TimetableEntry entry = new TimetableEntry();
                                entry.setTimetableId(doc.getId());
                                entry.setSubjectCode(doc.getString("course"));
                                entry.setSubjectName(""); // You'll need to fetch this later or set it up differently.
                                entry.setSubjectClass(doc.getString("classType"));
                                entry.setSubjectDay(doc.getString("day"));
                                entry.setSubjectTime(doc.getString("startTime") + "-" + doc.getString("endTime"));
                                entry.setSubjectVenue(doc.getString("venue"));
                                entry.setDate(currentDayDate);
                                allEntriesForTheWeek.add(entry);
                            }
                            // After fetching the regular timetable, fetch the replacement data for that day
                            fetchAndIntegrateReplacementsForDate(currentDayDate, allEntriesForTheWeek, () -> {
                                // Once replacements are integrated, fetch the cancellations
                                fetchAndIntegrateCancellationsForDate(currentDayDate, allEntriesForTheWeek);
                            });
                        })
                        .addOnFailureListener(e -> {
                            Log.e("TIMETABLE", "Error fetching timetable", e);
                            latch.countDown();
                        });
            }
        }
        new Thread(() -> {
            try {
                latch.await();  // This will block until latch count reaches zero

                // After all async calls are finished, sort and update the RecyclerView
                // ... your sorting and RecyclerView update code ...

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        // After all days are processed, sort and update the RecyclerView
        Collections.sort(allEntriesForTheWeek, (entry1, entry2) -> {
            String day1 = entry1.getSubjectDay();
            String day2 = entry2.getSubjectDay();
            int dayComparison = Integer.compare(ORDERED_DAYS.indexOf(day1), ORDERED_DAYS.indexOf(day2));

            // If days are the same, compare by start time
            if (dayComparison == 0) {
                String startTime1 = entry1.getSubjectTime().split("-")[0];
                String startTime2 = entry2.getSubjectTime().split("-")[0];
                return startTime1.compareTo(startTime2);
            }

            return dayComparison;
        });

        adapter.setTimetableEntries(allEntriesForTheWeek);
    }

    private String dateToDayName(Date date) {
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.ENGLISH);
        return dayFormat.format(date);
    }

    private Date[] getWeekStartAndEndDates(int weekNumber) {
        long oneDayMillis = 24 * 60 * 60 * 1000;
        long oneWeekMillis = 7 * oneDayMillis;
        Date weekStartDate = new Date(startDate.getTime() + (weekNumber - 1) * oneWeekMillis);
        Date weekEndDate = new Date(weekStartDate.getTime() + 6 * oneDayMillis); // 6 days after the start date
        return new Date[]{weekStartDate, weekEndDate};
    }

    private void fetchAndIntegrateCancellationsForDate(Date currentDayDate, List<TimetableEntry> currentDayEntries) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        String formattedDate = dateFormat.format(currentDayDate);
        Log.d("TIMETABLE", "Fetching cancellations for date: " + formattedDate);

        db.collection("cancellation")
                .whereEqualTo("approved", true)
                .whereEqualTo("date", formattedDate)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot cancellationDoc : queryDocumentSnapshots.getDocuments()) {
                        String timetableId = cancellationDoc.getString("timetableId");

                        for (TimetableEntry entry : currentDayEntries) {
                            if (entry.getTimetableId() == null) {
                                continue; // Skip to the next iteration if timetableId is null
                            }

                            // Check if the timetableId matches and the entry date matches the cancellation date
                            if (entry.getTimetableId().equals(timetableId) && entry.getDate().equals(currentDayDate)) {
                                entry.setCancelled(true);
                                Log.d("TIMETABLE", "Inside fetchAndIntegrateCancellationsForDate");
                            }
                        }
                    }

                    // Update the RecyclerView's adapter with the updated list
                    adapter.setTimetableEntries(currentDayEntries);
                })
                .addOnFailureListener(e -> {
                    Log.e("TIMETABLE", "Error fetching cancellations for date: " + formattedDate, e);
                });
    }


    private void fetchAndIntegrateReplacementsForDate(Date currentDayDate, List<TimetableEntry> currentDayEntries, Runnable onComplete) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        String formattedDate = dateFormat.format(currentDayDate);
        Log.d("TIMETABLE", "Fetching replacements for date: " + formattedDate);
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userType = sharedPreferences.getString("UserType", null);

        if (userType.equals("student")) {
            db.collection("timetable")
                    .whereArrayContains("studRegister", loginId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot timetableDoc : queryDocumentSnapshots.getDocuments()) {
                            String timetableId = timetableDoc.getId();
                            String classType = timetableDoc.getString("classType");

                            // Fetch the replacement using the timetableId and the specific date
                            db.collection("replacement")
                                    .whereEqualTo("timetableId", timetableId)
                                    .whereEqualTo("date", formattedDate)
                                    .whereEqualTo("approved", true)
                                    .get()
                                    .addOnSuccessListener(replacementSnapshots -> {
                                        Log.d("TIMETABLE", "Successfully fetched replacements for date: " + formattedDate);
                                        for (DocumentSnapshot replacementDoc : replacementSnapshots.getDocuments()) {
                                            TimetableEntry replacementEntry = new TimetableEntry();
                                            replacementEntry.setTimetableId(timetableId); // Set the timetableId
                                            replacementEntry.setSubjectCode(replacementDoc.getString("courseCode"));
                                            replacementEntry.setSubjectName(replacementDoc.getString("courseName")); // Update as necessary
                                            replacementEntry.setSubjectClass(classType);
                                            replacementEntry.setSubjectDay(dateToDayName(currentDayDate));
                                            replacementEntry.setSubjectTime(replacementDoc.getString("startTime") + "-" + replacementDoc.getString("endTime"));
                                            replacementEntry.setSubjectVenue(replacementDoc.getString("venue"));
                                            replacementEntry.setDate(currentDayDate);
                                            replacementEntry.setIsReplacement(true);

                                            // Add the replacement entry to the list
                                            currentDayEntries.add(replacementEntry);
                                        }

                                    // Sort the list based on start time
                                    Collections.sort(currentDayEntries, (entry1, entry2) -> {
                                        int dateComparison = entry1.getDate().compareTo(entry2.getDate());

                                        // If dates are the same, compare by start time
                                        if (dateComparison == 0) {
                                            String startTime1 = entry1.getSubjectTime().split("-")[0];
                                            String startTime2 = entry2.getSubjectTime().split("-")[0];
                                            return startTime1.compareTo(startTime2);
                                        }

                                        return dateComparison;
                                    });

                                    TextView noEntriesTextView = findViewById(R.id.no_entries_text_view);
                                    if (currentDayEntries.isEmpty()) {
                                        noEntriesTextView.setText("No entries found for " + dateToDayName(currentDayDate) + ".");
                                        noEntriesTextView.setVisibility(View.VISIBLE);
                                    } else {
                                        noEntriesTextView.setVisibility(View.GONE);
                                        adapter.setTimetableEntries(currentDayEntries);
                                    }
                                    // Update the RecyclerView's adapter with the updated list
                                    adapter.setTimetableEntries(currentDayEntries);
                                    if (onComplete != null) {
                                        onComplete.run();
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("TIMETABLE", "Error fetching replacements for timetableId: " + timetableId, e);
                                    if (onComplete != null) {
                                        onComplete.run();
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TIMETABLE", "Error fetching timetable for replacements", e);
                });
    }else if (userType.equals("lecturer")) {
            db.collection("timetable")
                    .whereEqualTo("lecturerTutor", loginId)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot timetableDoc : queryDocumentSnapshots.getDocuments()) {
                            String timetableId = timetableDoc.getId();
                            String classType = timetableDoc.getString("classType");

                            // Fetch the replacement using the timetableId and the specific date
                            db.collection("replacement")
                                    .whereEqualTo("timetableId", timetableId)
                                    .whereEqualTo("date", formattedDate)
                                    .whereEqualTo("approved", true)
                                    .get()
                                    .addOnSuccessListener(replacementSnapshots -> {
                                        Log.d("TIMETABLE", "Successfully fetched replacements for date: " + formattedDate);
                                        for (DocumentSnapshot replacementDoc : replacementSnapshots.getDocuments()) {
                                            TimetableEntry replacementEntry = new TimetableEntry();
                                            replacementEntry.setSubjectCode(replacementDoc.getString("courseCode"));
                                            replacementEntry.setSubjectName(replacementDoc.getString("courseName")); // Update as necessary
                                            replacementEntry.setSubjectClass(classType);
                                            replacementEntry.setSubjectDay(dateToDayName(currentDayDate));
                                            replacementEntry.setSubjectTime(replacementDoc.getString("startTime") + "-" + replacementDoc.getString("endTime"));
                                            replacementEntry.setSubjectVenue(replacementDoc.getString("venue"));
                                            replacementEntry.setDate(currentDayDate);
                                            replacementEntry.setIsReplacement(true);

                                            // Add the replacement entry to the list
                                            currentDayEntries.add(replacementEntry);
                                        }

                                        // Sort the list based on start time
                                        Collections.sort(currentDayEntries, (entry1, entry2) -> {
                                            int dateComparison = entry1.getDate().compareTo(entry2.getDate());

                                            // If dates are the same, compare by start time
                                            if (dateComparison == 0) {
                                                String startTime1 = entry1.getSubjectTime().split("-")[0];
                                                String startTime2 = entry2.getSubjectTime().split("-")[0];
                                                return startTime1.compareTo(startTime2);
                                            }

                                            return dateComparison;
                                        });

                                        TextView noEntriesTextView = findViewById(R.id.no_entries_text_view);
                                        if (currentDayEntries.isEmpty()) {
                                            noEntriesTextView.setText("No entries found for " + dateToDayName(currentDayDate) + ".");
                                            noEntriesTextView.setVisibility(View.VISIBLE);
                                        } else {
                                            noEntriesTextView.setVisibility(View.GONE);
                                            adapter.setTimetableEntries(currentDayEntries);
                                        }
                                        // Update the RecyclerView's adapter with the updated list
                                        adapter.setTimetableEntries(currentDayEntries);
                                        if (onComplete != null) {
                                            onComplete.run();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("TIMETABLE", "Error fetching replacements for timetableId: " + timetableId, e);
                                        if (onComplete != null) {
                                            onComplete.run();
                                        }
                                    });
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TIMETABLE", "Error fetching timetable for replacements", e);
                    });
        }
    }


}