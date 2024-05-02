package com.example.utarapp;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationView;

public class reviewTimetableBtmBar extends Fragment implements NavigationView.OnNavigationItemSelectedListener {

    private OnDaySelectedListener mListener;
    private OnAllButtonClickListener mListenerAll;

    public interface OnDaySelectedListener {
        void onDaySelected(String day);
    }

    public interface OnAllButtonClickListener {
        void onAllButtonClick(int weekNumber);
    }


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mListener = (OnDaySelectedListener) context;
            mListenerAll = (OnAllButtonClickListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnDaySelectedListener and OnAllButtonClickListener");
        }
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_review_timetable_btm_bar, container, false);
        LinearLayout mondayLayout = view.findViewById(R.id.mondayLayout);
        LinearLayout tuesdayLayout = view.findViewById(R.id.tuesdayLayout);
        LinearLayout wednesdayLayout = view.findViewById(R.id.wednesdayLayout);
        LinearLayout thursdayLayout = view.findViewById(R.id.thursdayLayout);
        LinearLayout fridayLayout = view.findViewById(R.id.fridayLayout);
        LinearLayout saturdayLayout = view.findViewById(R.id.saturdayLayout);
        LinearLayout allLayout = view.findViewById(R.id.AllLayout);


        // Step 2: Set up the OnClickListeners
        mondayLayout.setOnClickListener(v -> onDayClicked("Monday"));
        tuesdayLayout.setOnClickListener(v -> onDayClicked("Tuesday"));
        wednesdayLayout.setOnClickListener(v -> onDayClicked("Wednesday"));
        thursdayLayout.setOnClickListener(v -> onDayClicked("Thursday"));
        fridayLayout.setOnClickListener(v -> onDayClicked("Friday"));
        saturdayLayout.setOnClickListener(v -> onDayClicked("Saturday"));
        allLayout.setOnClickListener(v -> {
            if (mListenerAll != null) {
                mListenerAll.onAllButtonClick(((reviewTimetable) getActivity()).getCurrentWeekNumber());
            }
        });
        return view;
    }
    private void onDayClicked(String day) {
        // Step 3: Notify the parent activity
        if (mListener != null) {
            mListener.onDaySelected(day);
        }
    }
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        return false;
    }
}