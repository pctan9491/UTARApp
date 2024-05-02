package com.example.utarapp;

import android.content.Context;
import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class CourseAttendanceAdapter extends RecyclerView.Adapter<CourseAttendanceAdapter.ViewHolder> {

    private List<Course> mCourses; // Now using a list of Course objects
    private Context mContext;

    public CourseAttendanceAdapter(Context context, List<Course> courses) {
        this.mContext = context;
        this.mCourses = courses;
    }

    // Method to update the dataset
    public void setCourses(List<Course> courses) {
        this.mCourses = courses;
        notifyDataSetChanged(); // Notify the adapter of the dataset change
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_course, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Course course = mCourses.get(position);
        String fullText = course.getCourseCode() + "\n" + course.getCourseTitle() + "\n Attendance Percentage: " + calculateAttendancePercentage(course) + "%";
        // Here you could also set other course details, or handle a click to expand details
        SpannableString spannableString = new SpannableString(fullText);
        spannableString.setSpan(new StyleSpan(Typeface.BOLD), 0, course.getCourseCode().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        spannableString.setSpan(new RelativeSizeSpan(0.9f), course.getCourseCode().length() + 1, fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.courseName.setText(spannableString);

        double attendancePercentage = calculateAttendancePercentage(course);
        if (attendancePercentage >= 80) {
            holder.attendanceList.setBackgroundResource(R.drawable.bg_card); // Assuming yellow is defined in your bg_card_high_percentage drawable
        } else {
            holder.attendanceList.setBackgroundResource(R.drawable.bg_card_low_percentage); // Assuming red is defined in your bg_card_low_percentage drawable
        }

        // Set the visibility based on whether the course is expanded or not
        holder.attendanceDetailsLayout.setVisibility(course.isExpanded() ? View.VISIBLE : View.GONE);
        holder.attendanceDetailsLayout.removeAllViews();

        for (AttendanceSession session : course.getAttendanceSessions()) {
            TextView textView = new TextView(mContext);
            textView.setTextAppearance(mContext, R.style.AttendanceRecordTextStyle);

            // Set the text color based on the attendance status
            int textColor = session.attended ? ContextCompat.getColor(mContext, R.color.present) : ContextCompat.getColor(mContext, R.color.black);
            textView.setTextColor(textColor);

            String attendanceStatus = "Attendance Status: " + (session.attended ? "Present" : "Absent");
            String detailText = "\nDate: " + session.date + "\n" + "Venue: " + session.venue + "\n" + "Class Type: " + session.classType + "\n" + attendanceStatus + "\n";
            textView.setText(detailText);
            holder.attendanceDetailsLayout.addView(textView);
        }
        // Toggle expand/collapse on click
        holder.itemView.setOnClickListener(v -> {
            // Toggle expand/collapse state
            course.setExpanded(!course.isExpanded());
            notifyItemChanged(position);
        });
    }

    @Override
    public int getItemCount() {
        return mCourses.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView courseName;
        public LinearLayout attendanceDetailsLayout;
        public LinearLayout attendanceList;

        public ViewHolder(View itemView) {
            super(itemView);
            attendanceList = itemView.findViewById(R.id.llAttendanceList);
            courseName = itemView.findViewById(R.id.tvCourseName); // Ensure this ID is in item_course.xml
            attendanceDetailsLayout = itemView.findViewById(R.id.attendanceDetailsLayout);
        }
    }
    private double calculateAttendancePercentage(Course course) {
        int totalSessions = course.getAttendanceSessions().size();
        int attendedSessions = 0;

        for (AttendanceSession session : course.getAttendanceSessions()) {
            if (session.attended) {
                attendedSessions++;
            }
        }

        return totalSessions > 0 ? (double) attendedSessions / totalSessions * 100 : 0;
    }
    private int getAttendanceStatusColor(boolean attended) {
        return attended ? 0xFF00FF00 : 0xFF000000; // Green color: #00FF00, Black color: #000000
    }
}
