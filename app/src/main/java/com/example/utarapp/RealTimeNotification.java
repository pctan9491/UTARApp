package com.example.utarapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class RealTimeNotification extends Fragment {
    private TableRow notificationConBtn;
    private Button sortByTitleAscBtn, sortByTitleDescBtn, sortByDateAscBtn, sortByDateDescBtn;

    public void notificationFrame() {
        // require an empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = inflater.inflate(R.layout.activity_real_time_notification, container, false);

        // Click notification
        notificationConBtn = view.findViewById(R.id.notification);
        notificationConBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentNotifCont = new Intent(getActivity(), NotificationContent.class);
                startActivity(intentNotifCont);
            }
        });

        // Sort buttons
        sortByTitleAscBtn = view.findViewById(R.id.sortByTitleAscBtn);
        sortByTitleDescBtn = view.findViewById(R.id.sortByTitleDescBtn);
        sortByDateAscBtn = view.findViewById(R.id.sortByDateAscBtn);
        sortByDateDescBtn = view.findViewById(R.id.sortByDateDescBtn);

        sortByTitleAscBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sortAndDisplayNotifications("title", Query.Direction.ASCENDING);
            }
        });

        sortByTitleDescBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sortAndDisplayNotifications("title", Query.Direction.DESCENDING);
            }
        });

        sortByDateAscBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sortAndDisplayNotifications("date", Query.Direction.ASCENDING);
            }
        });

        sortByDateDescBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sortAndDisplayNotifications("date", Query.Direction.DESCENDING);
            }
        });

        // Initially display notifications sorted by date in descending order
        sortAndDisplayNotifications("date", Query.Direction.DESCENDING);

        return view;
    }

    private void sortAndDisplayNotifications(String fieldName, Query.Direction sortOrder) {
        FirebaseApp.initializeApp(getActivity());
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("notification")
                .orderBy(fieldName, sortOrder)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            TableLayout tableLayout = getView().findViewById(R.id.dynamicTableLayout);
                            tableLayout.removeAllViews(); // Clear the existing TableRows

                            for (QueryDocumentSnapshot document : task.getResult()) {
                                String docId = document.getId();
                                String title = document.getString("title");
                                String date = document.getString("date");

                                // Create a TableRow dynamically
                                createTableRow(title, "Admin", date, docId);
                            }
                        } else {
                            Log.w(TAG, "Error getting documents.", task.getException());
                        }
                    }
                });
    }

    private void createTableRow(String title, String postedBy, String date, String docId) {
        TableLayout tableLayout = getView().findViewById(R.id.dynamicTableLayout);

        TableRow tr = new TableRow(getActivity());

        TextView tvTitle = new TextView(getActivity());
        TextView tvPostedBy = new TextView(getActivity());
        TextView tvDate = new TextView(getActivity());

        // Set fixed width for each TextView
        int titleWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 150, getResources().getDisplayMetrics());
        int postedByWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
        int dateWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());

        tvTitle.setWidth(titleWidth);
        tvPostedBy.setWidth(postedByWidth);
        tvDate.setWidth(dateWidth);

        // Enable text ellipsis for long text
        tvTitle.setEllipsize(TextUtils.TruncateAt.END);
        tvTitle.setSingleLine(true);

        tvTitle.setText(title);
        tvPostedBy.setText(postedBy);
        tvDate.setText(date);

        tvTitle.setPadding(18, 28, 8, 28);
        tvPostedBy.setPadding(27, 28, 8, 28);
        tvDate.setPadding(8, 28, 8, 28);
        tvTitle.setTextColor(Color.BLACK);
        tvPostedBy.setTextColor(Color.BLACK);
        tvDate.setTextColor(Color.BLACK);

        tr.addView(tvTitle);
        tr.addView(tvPostedBy);
        tr.addView(tvDate);

        tr.setBackgroundResource(R.drawable.notification_btn);
        // Set margins for the TableRow
        TableLayout.LayoutParams layoutParams = new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT);
        int marginTopInPixels = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 10, getResources().getDisplayMetrics()); // converting 8dp to pixels
        layoutParams.setMargins(0, marginTopInPixels, 0, 0); // Setting only the top margin
        tr.setLayoutParams(layoutParams);
        tr.setTag(docId);

        tr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Retrieve the document ID
                String docId = (String) view.getTag();

                // Start a new Activity or display a dialog
                Intent intent = new Intent(getActivity(), NotificationContent.class);
                intent.putExtra("docId", docId);
                startActivity(intent);
            }
        });

        // Add the TableRow to your TableLayout
        tableLayout.addView(tr);
    }
}