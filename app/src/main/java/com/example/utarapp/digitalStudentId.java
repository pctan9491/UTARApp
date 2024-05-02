package com.example.utarapp;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class digitalStudentId extends BaseActivity {

    private LinearLayout layIdPhoto;
    private LinearLayout layStudInfo;
    private ImageButton editBtn;
    private TextView studentName, studentId, studentCourse, studentExpiryDate, emailAddress, phoneNum;
    private ImageView studentPhoto;
    private FirebaseFirestore db;
    private ImageView barcodeImage;

    public void studIdFrame(){
        // require a empty public constructor
    }

    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_digital_student_id);

        FirebaseApp.initializeApp(this);
        //Design part
        layIdPhoto = findViewById(R.id.layout_id_photo);
        layStudInfo = findViewById(R.id.layout_stud_info);
        barcodeImage = findViewById(R.id.barcode_image);
        GradientDrawable layColor = (GradientDrawable) getResources().getDrawable(R.drawable.digital_student_id_rounded_square);
        GradientDrawable layColor2 = (GradientDrawable) getResources().getDrawable(R.drawable.digital_student_id_rounded_square);
        layColor.setColor(Color.parseColor("#70FFFFFF"));
        layColor2.setColor(Color.parseColor("#70FFFFFF"));
        layIdPhoto.setBackground(layColor);
        layStudInfo.setBackground(layColor2);

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userType = sharedPreferences.getString("UserType", null);
        TextView txtExpiryDate = findViewById(R.id.txtExpiryDate);

        if (userType.equals("lecturer")) {
            txtExpiryDate.setVisibility(View.GONE);

        }

        //Go to Edit details page
        editBtn = findViewById(R.id.btn_modify);
        editBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent editPageIntent = new Intent(digitalStudentId.this, editDigitalStudentId.class);
                startActivity(editPageIntent);
            }
        });

        //Retrieve data to student ID
        db = FirebaseFirestore.getInstance();
        studentName = findViewById(R.id.studentName);
        studentId = findViewById(R.id.studentId);
        studentCourse = findViewById(R.id.studentCourse);
        studentExpiryDate = findViewById(R.id.studentExpiryDate);
        emailAddress = findViewById(R.id.studentEmailAddress);
        phoneNum = findViewById(R.id.studentPhoneNumber);
        studentPhoto = findViewById(R.id.img_id_photo);
        retrieveUserData();

    }
    private void generateBarcode(String studentId) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            BitMatrix bitMatrix = writer.encode(studentId, BarcodeFormat.CODE_128, 600, 200);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
                }
            }
            barcodeImage.setImageBitmap(bitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void retrieveUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String loginId = sharedPreferences.getString("LoginId", null);
        String userType = sharedPreferences.getString("UserType", null);

        String collectionPath = userType.equals("lecturer") ? "lecturer" : "student";

        db.collection(collectionPath)
                .whereEqualTo(userType.equals("lecturer") ? "lectutId" : "studentID", loginId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);

                            String name = document.getString("name");
                            String id = userType.equals("lecturer") ? document.getString("lectutId") : document.getString("studentID");
                            String course = userType.equals("lecturer") ? document.getString("faculty") : document.getString("course");
                            String email = document.getString("emailAddress");
                            String phone = document.getString("phoneNumber");

                            // Retrieve the expiryDate field as a Timestamp
                            Timestamp expiryTimestamp = document.getTimestamp("expiryDate");
                            String formattedExpiryDate = "";
                            if (expiryTimestamp != null) {
                                Date expiryDate = expiryTimestamp.toDate();
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                formattedExpiryDate = dateFormat.format(expiryDate);
                                studentExpiryDate.setText(formattedExpiryDate);
                            }

                            // Update UI
                            runOnUiThread(() -> {
                                studentName.setText(name);
                                studentId.setText(id);
                                studentCourse.setText(course);
                                emailAddress.setText(email);
                                phoneNum.setText(phone);

                                if (userType.equals("lecturer")) {
                                    // Hide or repurpose the expiry date field for lecturers
                                    studentExpiryDate.setVisibility(View.GONE);
                                }

                                // Load image
                                String imageUrl = document.getString("imageURL");
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    Glide.with(digitalStudentId.this)
                                            .load(imageUrl)
                                            .into(studentPhoto);
                                }
                                generateBarcode(id);
                            });
                        } else {
                            Toast.makeText(this, "No data found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to load data. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    public void onResume() {
        super.onResume();
        retrieveUserData();
    }
}