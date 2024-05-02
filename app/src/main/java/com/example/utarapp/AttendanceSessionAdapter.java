package com.example.utarapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

class AttendanceSessionAdapter extends RecyclerView.Adapter<AttendanceSessionAdapter.ViewHolder> {
    private List<AttendanceSession> attendanceSessions;

    public AttendanceSessionAdapter(List<AttendanceSession> attendanceSessions) {
        this.attendanceSessions = attendanceSessions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_attendance_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AttendanceSession session = attendanceSessions.get(position);
        holder.tvDate.setText(session.date);
        holder.tvVenue.setText(session.venue);
        holder.tvClassType.setText(session.classType);
        holder.tvAttendanceStatus.setText(session.attended ? "Present" : "Absent");
    }

    @Override
    public int getItemCount() {
        return attendanceSessions.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvDate, tvVenue, tvClassType, tvAttendanceStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvVenue = itemView.findViewById(R.id.tvVenue);
            tvClassType = itemView.findViewById(R.id.tvClassType);
            tvAttendanceStatus = itemView.findViewById(R.id.tvAttendanceStatus);
        }
    }
}
