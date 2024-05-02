package com.example.utarapp;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TimetableTableActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timetable_table);

        // Get the timetable entries from the intent
        List<TimetableEntry> timetableEntries = getIntent().getParcelableArrayListExtra("timetableEntries");

        // Populate the table with the timetable entries
        populateTable(timetableEntries);
    }

    private void populateTable(List<TimetableEntry> timetableEntries) {
        HorizontalScrollView scrollView = findViewById(R.id.timetable_scroll_view);
        TableLayout tableLayout = scrollView.findViewById(R.id.timetable_table);
        tableLayout.setPadding(0, 0, 0, 0); // Remove padding from the TableLayout

        // Add table header row
        TableRow headerRow = new TableRow(this);
        String[] daysOfWeek = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        headerRow.addView(createTableCell("Time", true));
        for (String day : daysOfWeek) {
            headerRow.addView(createTableCell(day, true));
        }
        tableLayout.addView(headerRow);

        // Group the entries by time slot
        Map<String, Map<String, TimetableEntry>> timetableMap = new TreeMap<>();
        for (TimetableEntry entry : timetableEntries) {
            String timeSlot = entry.getSubjectTime();
            String day = entry.getSubjectDay();
            timetableMap.putIfAbsent(timeSlot, new HashMap<>());
            timetableMap.get(timeSlot).put(day, entry);
        }

        // Add search functionality
        EditText searchEditText = findViewById(R.id.search_edit_text);
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String searchQuery = s.toString().toLowerCase();
                tableLayout.removeAllViews();
                tableLayout.addView(headerRow);

                for (Map.Entry<String, Map<String, TimetableEntry>> entry : timetableMap.entrySet()) {
                    String timeSlot = entry.getKey();
                    Map<String, TimetableEntry> rowEntries = entry.getValue();

                    TableRow tableRow = new TableRow(TimetableTableActivity.this);

                    // Add time slot cell
                    tableRow.addView(createTimeCell(timeSlot));

                    // Add cells for each day of the week
                    for (String day : daysOfWeek) {
                        TimetableEntry timetableEntry = rowEntries.get(day);

                        String cellText = "";
                        if (timetableEntry != null) {
                            String subjectCode = timetableEntry.getSubjectCode().toLowerCase();
                            String subjectVenue = timetableEntry.getSubjectVenue().toLowerCase();

                            if (subjectCode.contains(searchQuery) || subjectVenue.contains(searchQuery)) {
                                cellText = timetableEntry.getSubjectCode() + "\n" + timetableEntry.getSubjectVenue();
                            }
                        }
                        tableRow.addView(createTableCell(cellText, false));
                    }

                    tableLayout.addView(tableRow);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        // Add rows to the table
        for (Map.Entry<String, Map<String, TimetableEntry>> entry : timetableMap.entrySet()) {
            String timeSlot = entry.getKey();
            Map<String, TimetableEntry> rowEntries = entry.getValue();

            TableRow tableRow = new TableRow(this);

            // Add time slot cell
            tableRow.addView(createTimeCell(timeSlot));

            // Add cells for each day of the week
            for (String day : daysOfWeek) {
                TimetableEntry timetableEntry = rowEntries.get(day);

                String cellText = "";
                if (timetableEntry != null) {
                    cellText = timetableEntry.getSubjectCode() + "\n" + timetableEntry.getSubjectVenue();
                }
                tableRow.addView(createTableCell(cellText, false));
            }

            tableLayout.addView(tableRow);
        }
    }

    private TextView createTimeCell(String text) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setPadding(8, 8, 8, 8);
        textView.setBackgroundColor(Color.parseColor("#FFFFFF"));
        textView.setTextColor(Color.BLACK);
        return textView;
    }

    private TableRow createTableCell(String text, boolean isHeader) {
        TableRow tableRow = new TableRow(this);

        if (isHeader) {
            TextView textView = new TextView(this);
            textView.setText(text);
            textView.setTypeface(null, Typeface.BOLD); // Set the text style to bold
            textView.setBackgroundColor(Color.GRAY); // Set the background color for header cells
            textView.setPadding(16, 8, 16, 8); // Increase horizontal padding for header cells
            tableRow.addView(textView);
        } else {
            String[] parts = text.split("\n");
            String subjectCode = parts.length > 0 ? parts[0] : "";
            String subjectVenue = parts.length > 1 ? parts[1] : "";

            TextView combinedTextView = new TextView(this);
            combinedTextView.setText(subjectCode + "\n" + subjectVenue);
            combinedTextView.setPadding(16, 8, 16, 8); // Increase horizontal padding for cells

            // Add border to the cell
            GradientDrawable drawable = new GradientDrawable();
            drawable.setStroke(1, Color.GRAY);

            // Set the background color based on whether the cell contains a timetable entry
            if (!text.isEmpty()) {
                drawable.setColor(Color.parseColor("#B2C8FC")); // Set the background color for cells with timetable entries
            } else {
                drawable.setColor(Color.parseColor("#FFFFFF")); // Set the background color for empty cells
            }

            LinearLayout cellLayout = new LinearLayout(this);
            cellLayout.setOrientation(LinearLayout.VERTICAL);
            cellLayout.setPadding(0, 0, 0, 0); // Remove padding from the LinearLayout
            cellLayout.setBackground(drawable); // Set the border for the cell
            cellLayout.addView(combinedTextView);

            tableRow.addView(cellLayout);
        }

        return tableRow;
    }
}