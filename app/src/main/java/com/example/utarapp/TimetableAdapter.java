package com.example.utarapp;

import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimetableAdapter extends RecyclerView.Adapter<TimetableAdapter.TimetableViewHolder> {

    private List<TimetableEntry> timetableEntries = new ArrayList<>();
    private TimetableEntry selectedEntry;

    public TimetableAdapter() {
    }

    public TimetableEntry getSelectedEntry() {
        return selectedEntry;
    }

    public void setTimetableEntries(List<TimetableEntry> entries) {
        this.timetableEntries = entries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TimetableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_timetable_entry, parent, false);
        return new TimetableViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull TimetableViewHolder holder, int position) {
        TimetableEntry entry = timetableEntries.get(position);


        holder.subjectClassView.setText(entry.getSubjectClass());
        holder.subjectTimeView.setText(entry.getSubjectTime());
        holder.subjectVenueView.setText(entry.getSubjectVenue());
        holder.subjectDayView.setText(entry.getSubjectDay());
        holder.dateTextView.setText(new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).format(entry.getDate()));

        // Check if it's a replacement entry
        if (entry.isReplacement()) {
            holder.subjectCodeView.setText(entry.getSubjectCode() + " (Replacement)");
            holder.subjectCodeView.setTextColor(Color.BLUE);
            holder.subjectNameView.setTextColor(Color.BLUE);
            holder.subjectClassView.setTextColor(Color.BLUE);
            holder.subjectTimeView.setTextColor(Color.BLUE);
            holder.subjectVenueView.setTextColor(Color.BLUE);
            holder.subjectDayView.setTextColor(Color.BLUE);
            holder.dateTextView.setTextColor(Color.BLUE);
            // You can also add a visual hint like an icon or a label if you prefer
        } else if(entry.isCancelled()){
            holder.subjectCodeView.setText(entry.getSubjectCode() + " (Cancellation)");
            holder.subjectCodeView.setTextColor(Color.RED);
            holder.subjectNameView.setTextColor(Color.RED);
            holder.subjectClassView.setTextColor(Color.RED);
            holder.subjectTimeView.setTextColor(Color.RED);
            holder.subjectVenueView.setTextColor(Color.RED);
            holder.subjectDayView.setTextColor(Color.RED);
            holder.dateTextView.setTextColor(Color.RED);
        }else {
            holder.subjectCodeView.setText(entry.getSubjectCode());
            holder.subjectCodeView.setTextColor(Color.BLACK); // Reset color for non-replacement
            holder.subjectTimeView.setTextColor(Color.BLACK);
            holder.subjectNameView.setTextColor(Color.BLACK);
            holder.subjectClassView.setTextColor(Color.BLACK);
            holder.subjectVenueView.setTextColor(Color.BLACK);
            holder.subjectDayView.setTextColor(Color.BLACK);
            holder.dateTextView.setTextColor(Color.BLACK);
        }

        String courseCode = entry.getSubjectCode();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("course")
                .whereEqualTo("courseCode", courseCode)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!queryDocumentSnapshots.isEmpty()) {
                        String courseTitle = queryDocumentSnapshots.getDocuments().get(0).getString("courseTitle");
                        holder.subjectNameView.setText(courseTitle);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("TIMETABLE_ADAPTER", "Error fetching course title", e);
                });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectedEntry = entry;
                notifyDataSetChanged();
            }
        });

        // Highlight the selected entry
        if (entry.equals(selectedEntry)) {
            holder.itemView.setBackgroundColor(Color.LTGRAY);
        } else {
            holder.itemView.setBackgroundColor(Color.WHITE);
        }
    }

    @Override
    public int getItemCount() {
        return timetableEntries.size();
    }

    static class TimetableViewHolder extends RecyclerView.ViewHolder {
        TextView subjectCodeView, subjectNameView, subjectClassView, subjectDayView, subjectTimeView, subjectVenueView, dateTextView;

        TimetableViewHolder(@NonNull View itemView) {
            super(itemView);
            subjectCodeView = itemView.findViewById(R.id.subject_code);
            subjectNameView = itemView.findViewById(R.id.subject_name);
            subjectClassView = itemView.findViewById(R.id.subject_class);
            subjectDayView = itemView.findViewById(R.id.subject_day);
            subjectTimeView = itemView.findViewById(R.id.subject_time);
            subjectVenueView = itemView.findViewById(R.id.subject_venue);
            dateTextView = itemView.findViewById(R.id.subject_date);
        }
    }
}
