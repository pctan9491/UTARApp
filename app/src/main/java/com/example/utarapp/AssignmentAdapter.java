package com.example.utarapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.ViewHolder> {
    private List<Assignment> assignments;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String docId);
    }

    public AssignmentAdapter(List<Assignment> assignments, OnItemClickListener listener) {
        this.assignments = assignments;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_assignment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Assignment assignment = assignments.get(position);
        holder.bind(assignment);
        holder.itemView.setOnClickListener(view -> {
            if (listener != null) {
                listener.onItemClick(assignment.getDocId());
            }
        });
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView courseTitleTextView;
        private TextView titleTextView;
        private TextView dueDateTextView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseTitleTextView = itemView.findViewById(R.id.courseTitleTextView);
            titleTextView = itemView.findViewById(R.id.titleTextView);
            dueDateTextView = itemView.findViewById(R.id.dueDateTextView);
        }

        public void bind(Assignment assignment) {
            courseTitleTextView.setText(assignment.getCourse());
            titleTextView.setText(assignment.getTitle());
            dueDateTextView.setText(assignment.getDueDate());
        }
    }
}