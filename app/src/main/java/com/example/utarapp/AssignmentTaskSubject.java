package com.example.utarapp;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

public class AssignmentTaskSubject extends BaseActivity {

    private LinearLayout btnSubAssView;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_task_subject);

        //Choose for particular subject
        btnSubAssView = findViewById(R.id.subject_ass_view);
        btnSubAssView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent assTaskInt = new Intent(AssignmentTaskSubject.this, AssignmentTask.class);
                startActivity(assTaskInt);
            }
        });

    }
}