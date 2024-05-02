package com.example.utarapp;

import static android.content.Context.MODE_PRIVATE;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomePage extends Fragment {

    //slider
    private ViewPager2 viewPager;
    private SliderAdapter sliderAdapter;
    private Handler handler;
    private Runnable runnable;
    private int delay = 2000;
    private int currentPage = 0;

    private LinearLayout btnDigitalStudId;

    private LinearLayout btnReviewTimetable;
    private TextView studentName, studentId, studentCourse, studentExpiryDate;
    private ImageView studentPhoto;
    private FirebaseFirestore db;
    private List<Object> sliderItems = new ArrayList<>();

    public void homePageFrame() {
        // require a empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.home_page, container, false);
        FirebaseApp.initializeApp(requireActivity());
        // adv at btm
        viewPager = view.findViewById(R.id.viewPager);
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userType = sharedPreferences.getString("UserType", null);

        List<Integer> slideImages = new ArrayList<>();
        slideImages.add(R.drawable.home_page_pic1);
        slideImages.add(R.drawable.home_page_pic2);
        slideImages.add(R.drawable.home_page_pic3);

        List<String> imageUrls = new ArrayList<>();
        imageUrls.add("https://www.example.com/image1");
        imageUrls.add("https://www.example.com/image2");
        imageUrls.add("https://www.example.com/image3");

        sliderAdapter = new SliderAdapter(slideImages, imageUrls);


        //sliderAdapter = new SliderAdapter(slideImages);
        viewPager.setAdapter(sliderAdapter);

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = dateFormat.format(new Date());
        TextView textViewDate = view.findViewById(R.id.timetable_date);
        textViewDate.setText(currentDate);

        // Start auto-scrolling
        startAutoScroll();

        // Pause auto-scrolling when the user interacts with the ViewPager2
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageScrollStateChanged(int state) {
                super.onPageScrollStateChanged(state);
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    stopAutoScroll();
                } else if (state == ViewPager2.SCROLL_STATE_IDLE) {
                    startAutoScroll();
                }
            }
        });

        //Choose Digital student Id
        btnDigitalStudId = view.findViewById(R.id.student_id_view);

        btnDigitalStudId.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent digitalStudIdIntent = new Intent(getActivity(), digitalStudentId.class);
                startActivity(digitalStudIdIntent);
            }
        });

        //Choose Review Timetable
        btnReviewTimetable = view.findViewById(R.id.layout_timetable);
        btnReviewTimetable.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentReTt = new Intent(getActivity(), reviewTimetable.class);
                startActivity(intentReTt);
            }
        });
        ViewPager2 viewPager2 = view.findViewById(R.id.viewPager2);
        Button buttonPrevious = view.findViewById(R.id.button_previous);
        Button buttonNext = view.findViewById(R.id.button_next);


        buttonPrevious.setOnClickListener(v -> {
            int currentItem = viewPager2.getCurrentItem();
            if (currentItem > 0) {
                viewPager2.setCurrentItem(currentItem - 1);
            }
        });

        buttonNext.setOnClickListener(v -> {
            int currentItem = viewPager2.getCurrentItem();
            if (currentItem < viewPager2.getAdapter().getItemCount() - 1) {
                viewPager2.setCurrentItem(currentItem + 1);
            }
        });
        //Retrieve data to student ID
        db = FirebaseFirestore.getInstance();
        studentName = view.findViewById(R.id.studentName);
        studentId = view.findViewById(R.id.studentId);
        studentCourse = view.findViewById(R.id.studentCourse);
        studentExpiryDate = view.findViewById(R.id.studentExpiryDate);
        studentPhoto = view.findViewById(R.id.student_photo);
        retrieveUserData();

        //Notification Setting Channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.channel_name);
            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(getString(R.string.assignment_channel), name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getActivity().getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
        TextView titleId = view.findViewById(R.id.idTitle); // Assuming you add an id to the TextView that says "STUDENT ID"
        TableRow expiryDateRow = view.findViewById(R.id.expiryDateRow); // Assuming you add an id to the TableRow for the expiry date
        TextView txtId = view.findViewById(R.id.txtStudentId);

        if (userType.equals("lecturer")) {
            // Change "STUDENT ID" to "LECTURER ID"
            titleId.setText("LECTURER ID");

            // Hide the expiry date row
            expiryDateRow.setVisibility(View.GONE);

            txtId.setText("LECTURER ID: ");
            // Additional logic for setting lecturer-specific information can go here
        }

        return view;
    }
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        // Now that the view is fully created, it's safe to retrieve timetable info
        retrieveTimetableInfo();
    }

    private void retrieveUserData() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String loginId = sharedPreferences.getString("LoginId", null);
        String userType = sharedPreferences.getString("UserType", null);

        if (userType.equals("student")) {
            db.collection("student")
                    .whereEqualTo("studentID", loginId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (!task.isSuccessful()) {
                            Toast.makeText(getActivity(), "Failed to load data. Please try again later.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                            if (task.getResult().isEmpty()) {
                                Toast.makeText(getActivity(), "No data found.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                                DocumentSnapshot document = task.getResult().getDocuments().get(0);

                                String name = document.getString("name");
                                String id = document.getString("studentID");
                                String course = document.getString("course");

                                // Retrieve the expiryDate field as a Timestamp
                                Timestamp expiryTimestamp = document.getTimestamp("expiryDate");
                                if (expiryTimestamp == null) {
                                    Toast.makeText(getActivity(), "No expiry date found.", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                    // Convert timestamp to a formatted date string
                                    Date expiryDate = expiryTimestamp.toDate();
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    String formattedExpiryDate = dateFormat.format(expiryDate);

                                    studentName.setText(name);
                                    studentId.setText(id);
                                    studentCourse.setText(course);
                                    studentExpiryDate.setText(formattedExpiryDate);

                                    // Retrieve and load the student photo using the image URL
                                    String imageUrl = document.getString("imageURL");
                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        Glide.with(getContext())
                                                .load(imageUrl)
                                                .apply(RequestOptions.circleCropTransform())  // Apply circular transformation
                                                .listener(new RequestListener<Drawable>() {
                                                    @Override
                                                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                                        // Handle load failure
                                                        return false;
                                                    }

                                                    @Override
                                                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                                        // Image loaded successfully
                                                        return false;
                                                    }
                                                })
                                                .into(studentPhoto);
                                    }
                    });
        } else if (userType.equals("lecturer")) {
            db.collection("lecturer")
                    .whereEqualTo("lectutId", loginId)
                    .get()
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && !task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String name = document.getString("name");
                            String id = document.getString("lectutId");
                            String faculty = document.getString("faculty");

                            // Assuming you have a layout to display lecturer info
                            studentName.setText(name);
                            studentId.setText(id);
                            studentCourse.setText(faculty);

                            // For image loading, adjust as necessary
                            String imageUrl = document.getString("imageURL");
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(getContext())
                                        .load(imageUrl)
                                        .apply(RequestOptions.circleCropTransform())
                                        .into(studentPhoto);
                            } else {
                                // Handle the case where there is no image URL
                            }
                        } else {
                            Toast.makeText(getActivity(), "No lecturer data found.", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void updateViewPagerAdapter(List<TimetableEntry> entries, ViewPager2 viewPager2) {
        if (entries.isEmpty()) {
            Log.d("ReplacementData", "No entries to display.");
            // Handle the case when there are no entries, e.g., show a message
        } else {
            Log.d("ReplacementData", "Updating adapter with entries. Size: " + entries.size());
            ViewPager2Adapter adapter = new ViewPager2Adapter(entries);
            viewPager2.setAdapter(adapter);

            // Update button state after setting the adapter
            Button buttonPrevious = getView().findViewById(R.id.button_previous);
            Button buttonNext = getView().findViewById(R.id.button_next);
            updateButtonState(viewPager2, buttonPrevious, buttonNext);
        }
    }
    private void retrieveTimetableInfo() {
        SharedPreferences sharedPreferences = getActivity().getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String loginId = sharedPreferences.getString("LoginId", null);
        String userType = sharedPreferences.getString("UserType", null);
        ViewPager2 viewPager2 = getView().findViewById(R.id.viewPager2);
        List<TimetableEntry> entries = new ArrayList<>();

        SimpleDateFormat dateFormatDay = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        String todayFormattedDate = dateFormatDay.format(new Date());
        // Get today's day of the week
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("EEEE", Locale.US);
        String todayDayOfWeek = simpleDateFormat.format(new Date());

        if (userType != null) {
            if (userType.equals("student")) {
                // Query for students
                db.collection("timetable")
                        .whereArrayContains("studRegister", loginId)
                        .whereEqualTo("day", todayDayOfWeek)
                        .get()
                        .addOnCompleteListener(timetableTask -> {
                            if (timetableTask.isSuccessful()) {
                                for (DocumentSnapshot document : timetableTask.getResult()) {
                                    String course = document.getString("course");
                                    String startTime = document.getString("startTime");
                                    String endTime = document.getString("endTime");
                                    String venue = document.getString("venue");

                                    TimetableEntry entry = new TimetableEntry(course, "name", "L", todayDayOfWeek, startTime + " - " + endTime, venue, null);
                                    Log.d("ReplacementData", "Fetched entry: " + entry.toString());
                                    entries.add(entry);
                                }
                                processRemainingData(entries, viewPager2, todayFormattedDate, todayDayOfWeek, userType, loginId);
                            }
                        });
            } else if (userType.equals("lecturer")) {
                // Query for lecturers
                db.collection("timetable")
                        .whereEqualTo("lecturerTutor", loginId)
                        .whereEqualTo("day", todayDayOfWeek)
                        .get()
                        .addOnCompleteListener(timetableTask -> {
                            if (timetableTask.isSuccessful()) {
                                for (DocumentSnapshot document : timetableTask.getResult()) {
                                    String course = document.getString("course");
                                    String startTime = document.getString("startTime");
                                    String endTime = document.getString("endTime");
                                    String venue = document.getString("venue");

                                    TimetableEntry entry = new TimetableEntry(course, "name", "L", todayDayOfWeek, startTime + " - " + endTime, venue, null);
                                    Log.d("ReplacementData", "Fetched entry: " + entry.toString());
                                    entries.add(entry);
                                }
                                processRemainingData(entries, viewPager2, todayFormattedDate, todayDayOfWeek, userType, loginId);
                            }
                        });
            }
        }
    }
    private void processRemainingData(List<TimetableEntry> entries, ViewPager2 viewPager2,
                                      String todayFormattedDate, String todayDayOfWeek, String userType, String loginId) {
        // New fetch operation for cancellations
        db.collection("cancellation")
                .whereEqualTo("date", todayFormattedDate)
                .whereEqualTo("approved", true)
                .get()
                .addOnCompleteListener(cancellationTask -> {
                    if (cancellationTask.isSuccessful() && !cancellationTask.getResult().isEmpty()) {
                        List<String> cancelledIds = new ArrayList<>();
                        for (DocumentSnapshot document : cancellationTask.getResult()) {
                            cancelledIds.add(document.getString("timetableId"));
                        }

                        // Remove cancelled entries from 'entries'
                        entries.removeIf(entry -> cancelledIds.contains(entry.getTimetableId()));
                    }
                    processReplacements(entries, viewPager2, todayFormattedDate, todayDayOfWeek, userType, loginId);
                });
    }

    private void processReplacements(List<TimetableEntry> entries, ViewPager2 viewPager2,
                                     String todayFormattedDate, String todayDayOfWeek, String userType, String loginId) {
        // Query for replacements
        db.collection("replacement")
                .whereEqualTo("approved", true)
                .get()
                .addOnCompleteListener(replacementTask -> {
                    if (replacementTask.isSuccessful() && !replacementTask.getResult().isEmpty()) {
                        for (DocumentSnapshot document : replacementTask.getResult()) {
                            String timetableId = document.getString("timetableId");
                            String dateString = document.getString("date");

                            // Convert the date string to the day of the week format
                            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                            try {
                                Date date = dateFormat.parse(dateString);
                                String dayOfWeek = new SimpleDateFormat("EEEE", Locale.US).format(date);

                                if (dateString != null && dateString.equals(todayFormattedDate)) {
                                    Log.d("ReplacementData", "Today: " + todayDayOfWeek + ", Replacement: " + dayOfWeek);
                                    // Fetch studRegister/lecturerTutor from "timetable" collection
                                    db.collection("timetable")
                                            .document(timetableId)
                                            .get()
                                            .addOnSuccessListener(timetableDoc -> {
                                                if (timetableDoc.exists()) {
                                                    if (userType.equals("student")) {
                                                        List<String> studRegister = (List<String>) timetableDoc.get("studRegister");
                                                        if (studRegister != null && studRegister.contains(loginId)) {
                                                            addReplacementEntry(document, dayOfWeek, entries);
                                                        }
                                                    } else if (userType.equals("lecturer")) {
                                                        String lecturerTutor = timetableDoc.getString("lecturerTutor");
                                                        if (lecturerTutor != null && lecturerTutor.equals(loginId)) {
                                                            addReplacementEntry(document, dayOfWeek, entries);
                                                        }
                                                    }
                                                }
                                                updateViewPagerAdapter(entries, viewPager2);
                                            });
                                } else {
                                    updateViewPagerAdapter(entries, viewPager2);
                                }
                            } catch (ParseException e) {
                                e.printStackTrace();
                                updateViewPagerAdapter(entries, viewPager2);
                            }
                        }
                    } else {
                        updateViewPagerAdapter(entries, viewPager2);
                    }
                });
    }

    private void addReplacementEntry(DocumentSnapshot document, String dayOfWeek, List<TimetableEntry> entries) {
        String courseCode = document.getString("courseCode");
        String startTime = document.getString("startTime");
        String endTime = document.getString("endTime");
        String venue = document.getString("venue");

        TimetableEntry entry = new TimetableEntry(courseCode + " (Replacement)", "name", "R", dayOfWeek, startTime + " - " + endTime, venue, null);
        Log.d("ReplacementData", "Fetched entry: " + entry.toString());
        entries.add(entry);
    }

    private void updateButtonState(ViewPager2 viewPager2, Button buttonPrevious, Button buttonNext) {
        if (viewPager2.getAdapter() != null) {
            int itemCount = viewPager2.getAdapter().getItemCount();
            int currentItem = viewPager2.getCurrentItem();
            buttonPrevious.setEnabled(currentItem >= 0); // Enable the previous button if not on the first item
            buttonNext.setEnabled(currentItem < itemCount - 1); // Enable the next button if not on the last item
        } else {
            buttonPrevious.setEnabled(false);
            buttonNext.setEnabled(false);
        }
    }

    private void startAutoScroll() {
        handler = new Handler();
        runnable = new Runnable() {
            public void run() {
                if (currentPage == sliderAdapter.getItemCount()) {
                    currentPage = 0;
                }
                viewPager.setCurrentItem(currentPage++, true);
            }
        };
        handler.postDelayed(runnable, delay);
    }

    private void stopAutoScroll() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop auto-scrolling when the activity is destroyed
        stopAutoScroll();
    }

    public class SliderAdapter extends RecyclerView.Adapter<SliderAdapter.SliderViewHolder> {

        private List<Integer> slideImages;

        public SliderAdapter(List<Integer> slideImages, List<String> imageUrls) {
            this.slideImages = slideImages;
            for (int i = 0; i < slideImages.size(); i++) {
                sliderItems.add(slideImages.get(i));
                sliderItems.add(imageUrls.get(i));
            }
        }


        @NonNull
        @Override
        public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.activity_slide_item, parent, false);
            return new SliderViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
            int imageIndex = position / 2;
            Object item = sliderItems.get(position);

            if (item instanceof Integer) {
                holder.imageView.setImageResource((Integer) item);
            } else if (item instanceof String) {
                String url = (String) item;
                Glide.with(holder.itemView.getContext())
                        .load(url)
                        .into(holder.imageView);

                holder.imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    v.getContext().startActivity(intent);
                });
            }
        }

        @Override
        public int getItemCount() {
            return slideImages.size();
        }

        public class SliderViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;

            public SliderViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageSlide);
            }
        }
    }

    public void onResume() {
        super.onResume();
        retrieveUserData();
    }
}
