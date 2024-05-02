package com.example.utarapp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;


public class CourseAdapter extends ArrayAdapter<CourseEntry> {

    public interface CourseAdapterListener {
        void deleteRegistration(String documentId);
    }
    private Context context;
    private List<CourseEntry> coursesList;
    private CourseAdapterListener listener;

    public CourseAdapter(Context context, List<CourseEntry> list, CourseAdapterListener listener) {
        super(context, R.layout.activity_course_list, list);
        this.context = context;
        this.coursesList = list;
        this.listener = listener;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        CourseEntry course = coursesList.get(position);

        // Inflate the view if it's null
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.activity_course_list, parent, false);
        }

        CheckBox checkBoxC = convertView.findViewById(R.id.checkboxSelect); // Replace with your actual checkbox ID

        if (course.isAlreadyRegistered()) {
            checkBoxC.setEnabled(false);
            checkBoxC.setText("Already registered");
        } else {
            checkBoxC.setEnabled(true);
            checkBoxC.setText("Register"); // or any default text if you have one
        }

        TextView tvCourseTitle = convertView.findViewById(R.id.tvCourseTitle);
        TextView tvCourseCode = convertView.findViewById(R.id.tvCourseCode);
        TextView tvVenue = convertView.findViewById(R.id.tvVenue);
        TextView tvStartTime = convertView.findViewById(R.id.tvStartTime);
        TextView tvEndTime = convertView.findViewById(R.id.tvEndTime);
        TextView tvMaxPerson = convertView.findViewById(R.id.tvMaxPerson);
        TextView tvDay = convertView.findViewById(R.id.tvDay);
        TextView tvClassType = convertView.findViewById(R.id.tvClassType);

        tvCourseTitle.setText(course.getCourseTitle());
        tvCourseCode.setText(course.getCourseCode());
        tvVenue.setText("Venue: "+ course.getVenue());
        tvStartTime.setText("Start Time: "+ course.getStartTime());
        tvEndTime.setText("End Time: "+ course.getEndTime());
        tvMaxPerson.setText("Person: "+ String.valueOf(course.getMaxPersons())); // Assuming maxPersons is int, convert to String
        tvDay.setText("Day: "+ course.getDay());
        tvClassType.setText("Class Type: "+ course.getClassType());
        checkBoxC.setChecked(course.isSelected());

        checkBoxC.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                course.setSelected(isChecked);
            }
        });

        Button deleteButton = convertView.findViewById(R.id.deleteRegistrationButton);

        if (course.isAlreadyRegistered()) {
            deleteButton.setVisibility(View.VISIBLE);
            deleteButton.setOnClickListener(v -> {
                listener.deleteRegistration(course.getDocumentId());
            });
        } else {
            deleteButton.setVisibility(View.GONE);
        }

        return convertView;
    }
}

