package com.example.utarapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ViewPager2Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {


    private List<TimetableEntry> timetableEntries;

    private boolean isEmptyList = false; // Flag to indicate empty list

    private static final int VIEW_TYPE_EMPTY = 0;
    private static final int VIEW_TYPE_ITEM = 1;

    public ViewPager2Adapter(List<TimetableEntry> timetableEntries) {
        if (timetableEntries == null || timetableEntries.isEmpty()) {
            this.isEmptyList = true;
        } else {
            this.timetableEntries = timetableEntries;
        }
    }
    // Method to update the dataset
    public void setTimetableEntries(List<TimetableEntry> timetableEntries) {
        this.timetableEntries = timetableEntries;
        notifyDataSetChanged(); // Notify the adapter to refresh the views
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_EMPTY) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.empty_timetable_message, parent, false);
            return new EmptyViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.page_timetable_entry, parent, false);
            return new TimetableViewHolder(view);
        }
    }


    @Override
    public int getItemViewType(int position) {
        if (isEmptyList) return VIEW_TYPE_EMPTY;
        return VIEW_TYPE_ITEM;
    }


    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof TimetableViewHolder) {
            TimetableEntry entry = timetableEntries.get(position);
            ((TimetableViewHolder) holder).tvCourseName.setText(entry.getSubjectCode());
            ((TimetableViewHolder) holder).tvTime.setText("Time: " + entry.getSubjectTime());
            ((TimetableViewHolder) holder).tvVenue.setText("Venue: " + entry.getSubjectVenue());
        } else {
            // No need to bind data for the empty view holder
        }
    }


    @Override
    public int getItemCount() {
        return isEmptyList ? 1 : timetableEntries.size(); // number of pages
    }

    public static class TimetableViewHolder extends RecyclerView.ViewHolder {
        TextView tvCourseName, tvTime, tvVenue;

        public TimetableViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCourseName = itemView.findViewById(R.id.tvCourseName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvVenue = itemView.findViewById(R.id.tvVenue);
        }
    }

    public static class EmptyViewHolder extends RecyclerView.ViewHolder {
        // You can add fields if you have specific views in the empty message layout

        public EmptyViewHolder(@NonNull View itemView) {
            super(itemView);
            // Initialize your views here if necessary
        }
    }
}

