package com.example.utarapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class editDigitalStudentId extends BaseActivity {

    private LinearLayout layIdPhoto;
    private LinearLayout layStudInfo;

    private TextView studentName, studentId, studentCourse, studentExpiryDate;
    private EditText emailAddress, phoneNum;
    private ImageView studentPhoto;
    private FirebaseFirestore db;
    private static final int SELECT_PHOTO = 100;
    private Uri newImageUri;
    private String currentImageUrl;
    private String email, phone;
    private LottieAnimationView loadingAnimation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_digital_student_id);

        FirebaseApp.initializeApp(this);
        //Design Part
        layIdPhoto = findViewById(R.id.layout_id_photo);
        layStudInfo = findViewById(R.id.layout_stud_info);
        GradientDrawable layColor = (GradientDrawable) getResources().getDrawable(R.drawable.digital_student_id_rounded_square);
        GradientDrawable layColor2 = (GradientDrawable) getResources().getDrawable(R.drawable.digital_student_id_rounded_square);
        layColor.setColor(Color.parseColor("#70FFFFFF"));
        layColor2.setColor(Color.parseColor("#70FFFFFF"));
        layIdPhoto.setBackground(layColor);
        layStudInfo.setBackground(layColor2);

        //retrieve info data
        db = FirebaseFirestore.getInstance();
        studentName = findViewById(R.id.studentName);
        studentId = findViewById(R.id.studentId);
        studentCourse = findViewById(R.id.studentCourse);
        studentExpiryDate = findViewById(R.id.studentExpiryDate);
        emailAddress = findViewById(R.id.editStudentEmailAddress);
        phoneNum = findViewById(R.id.editStudentPhoneNumber);
        studentPhoto = findViewById(R.id.img_id_photo);
        retrieveUserData();

        //Upload photo
        Button upload = findViewById(R.id.upload_photo);
        upload.setOnClickListener(v->selectImageFromGallery());

        //Submit the modification
        Button submit = findViewById(R.id.submit_edit_details);
        submit.setOnClickListener(v -> updateStudentData());

        loadingAnimation = findViewById(R.id.loadingAnimation);
        // Register the network callback
        registerNetworkCallback();
    }
    private boolean isValidEmail(String email) {
        // Use a regular expression to validate the email format
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }
    private boolean isValidPhoneNumber(String phoneNumber) {
        // Use a regular expression to validate the phone number format
        String phoneRegex = "^(01)[0-9]-?[0-9]{7,8}$";
        return phoneNumber.matches(phoneRegex);
    }
    private void retrieveUserData() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String loginId = sharedPreferences.getString("LoginId", null);
        String userType = sharedPreferences.getString("UserType", null);

        String collectionPath = userType.equals("lecturer") ? "lecturer" : "student";
        String userIdField = userType.equals("lecturer") ? "lectutId" : "studentID";

        db.collection(collectionPath)
                .whereEqualTo(userIdField, loginId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);

                            String name = document.getString("name");
                            String id = document.getString(userIdField);
                            String courseOrFaculty = userType.equals("lecturer") ? document.getString("faculty") : document.getString("course");
                            email = document.getString("emailAddress");
                            phone = document.getString("phoneNumber");

                            // Retrieve the expiryDate field as a Timestamp// Different handling for students and lecturers
                            if ("student".equals(userType)) {
                                Timestamp expiryTimestamp = document.getTimestamp("expiryDate");
                                String formattedExpiryDate = "";

                                if (expiryTimestamp != null) {
                                    Date expiryDate = expiryTimestamp.toDate();
                                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                    formattedExpiryDate = dateFormat.format(expiryDate);
                                }
                                studentExpiryDate.setText(formattedExpiryDate);
                            } else if ("lecturer".equals(userType)) {
                                studentExpiryDate.setVisibility(View.GONE); // Or repurpose this TextView for something relevant to lecturers
                            }

                            // Common handling for both user types
                            studentName.setText(name);
                            studentId.setText(id);
                            emailAddress.setText(email);
                            phoneNum.setText(phone);
                            studentCourse.setText(courseOrFaculty);

                            // Image loading remains the same
                            String imageUrl = document.getString("imageURL");
                            currentImageUrl = imageUrl;
                            if (imageUrl != null && !imageUrl.isEmpty()) {
                                Glide.with(this)
                                        .load(imageUrl)
                                        .into(studentPhoto);
                            }
                        } else {
                            Toast.makeText(this, "No data found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Failed to load data. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, SELECT_PHOTO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SELECT_PHOTO && resultCode == RESULT_OK && data != null) {
            newImageUri = data.getData();  // Save the selected image URI

            // Use the Glide library to load and display the image
            Glide.with(this)
                    .load(newImageUri)
                    .into(studentPhoto);
        }
    }

    private void saveChangesToLocalStorage(String email, String phone, Uri imageUri) {
        SharedPreferences sharedPreferences = getSharedPreferences("PendingChanges", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("email", email);
        editor.putString("phone", phone);
        editor.putString("imageUri", imageUri != null ? imageUri.toString() : null);
        editor.putBoolean("changesPending", true);
        editor.apply();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnected();
    }


    private void updateStudentData() {
        // Show Lottie Animation
        loadingAnimation.setVisibility(View.VISIBLE);
        Log.d("updateStudentData", "Starting update process");
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String loginId = sharedPreferences.getString("LoginId", null);
        String userType = sharedPreferences.getString("UserType", null);
        String collectionPath = userType.equals("lecturer") ? "lecturer" : "student";
        String userIdField = userType.equals("lecturer") ? "lectutId" : "studentID";
        String newEmail = emailAddress.getText().toString().isEmpty() ? email : emailAddress.getText().toString();
        String newPhone = phoneNum.getText().toString().isEmpty() ? phone : phoneNum.getText().toString();

        // Check if email is empty
        if (newEmail.isEmpty()) {
            Toast.makeText(editDigitalStudentId.this, "Email cannot be empty", Toast.LENGTH_SHORT).show();
            loadingAnimation.setVisibility(View.GONE);
            return;
        }

        // Validate email format
        if (!newEmail.isEmpty() && !isValidEmail(newEmail)) {
            Toast.makeText(editDigitalStudentId.this, "Invalid email format", Toast.LENGTH_SHORT).show();
            loadingAnimation.setVisibility(View.GONE);
            return;
        }

        // Check if phone number is empty
        if (newPhone.isEmpty()) {
            Toast.makeText(editDigitalStudentId.this, "Phone number cannot be empty", Toast.LENGTH_SHORT).show();
            loadingAnimation.setVisibility(View.GONE);
            return;
        }

        // Validate phone number format
        if (!newPhone.isEmpty() && !isValidPhoneNumber(newPhone)) {
            Toast.makeText(editDigitalStudentId.this, "Invalid phone number format", Toast.LENGTH_SHORT).show();
            loadingAnimation.setVisibility(View.GONE);
            return;
        }
        if (isNetworkAvailable()) {
        Runnable updateFirestore = () -> {
            // Find the document with the matching studentID
            db.collection(collectionPath).whereEqualTo(userIdField, loginId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                    // Define what to update
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("emailAddress", newEmail);
                    updates.put("phoneNumber", newPhone);

                    // If there's a new image to upload
                    if (newImageUri != null) {
                        // Upload the new photo to Firebase Storage
                        StorageReference newPhotoRef = FirebaseStorage.getInstance().getReference().child("Student/" + System.currentTimeMillis() + "_" + newImageUri.getLastPathSegment());
                        newPhotoRef.putFile(newImageUri).addOnSuccessListener(taskSnapshot -> {
                            newPhotoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                updates.put("imageURL", uri.toString()); // Add imageURL to update
                                db.collection(collectionPath).document(document.getId())
                                        .update(updates)
                                        .addOnSuccessListener(aVoid1 -> {

                                            SharedPreferences pendingChangesPrefs = getSharedPreferences("PendingChanges", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = pendingChangesPrefs.edit();
                                            editor.remove("email");
                                            editor.remove("phone");
                                            editor.remove("imageUri");
                                            editor.putBoolean("changesPending", false);
                                            editor.apply();
                                            loadingAnimation.setVisibility(View.GONE);
                                            Toast.makeText(editDigitalStudentId.this, "Information updated successfully!", Toast.LENGTH_SHORT).show();
                                        })
                                        .addOnFailureListener(e -> {
                                            Toast.makeText(editDigitalStudentId.this, "Failed to update information: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                        });
                            });
                        }).addOnFailureListener(e -> {
                            Toast.makeText(editDigitalStudentId.this, "Failed to upload new image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            loadingAnimation.setVisibility(View.GONE);
                        });
                    } else {
                        // No new image, just update other fields
                        db.collection(collectionPath).document(document.getId())
                                .update(updates)
                                .addOnSuccessListener(aVoid1 -> {
                                    SharedPreferences pendingChangesPrefs = getSharedPreferences("PendingChanges", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = pendingChangesPrefs.edit();
                                    editor.remove("email");
                                    editor.remove("phone");
                                    editor.remove("imageUri");
                                    editor.putBoolean("changesPending", false);
                                    editor.apply();
                                    loadingAnimation.setVisibility(View.GONE);
                                    Toast.makeText(editDigitalStudentId.this, "Information updated successfully!", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(editDigitalStudentId.this, "Failed to update information: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    }
                } else {
                    Toast.makeText(editDigitalStudentId.this, "Failed to find student with ID: " + loginId, Toast.LENGTH_LONG).show();
                    loadingAnimation.setVisibility(View.GONE);
                }
            });
        };

        // Check if there's an existing photo to delete
        if (newImageUri != null && currentImageUrl != null && !currentImageUrl.isEmpty()) {
            // Delete the old photo from Firebase Storage
            StorageReference photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(currentImageUrl);
            photoRef.delete().addOnSuccessListener(aVoid -> updateFirestore.run())
                    .addOnFailureListener(e -> {
                        // If deletion fails, proceed with update anyway (might not exist)
                        Log.d("updateStudentData", "Failed to delete old image, proceeding with update: " + e.getMessage());
                        updateFirestore.run();
                    });
        } else {
            // No existing photo to delete, or no new photo, proceed with update
            updateFirestore.run();
        }
        } else {
            // Device is offline, save changes to local storage
            saveChangesToLocalStorage(newEmail, newPhone, newImageUri);
            Toast.makeText(editDigitalStudentId.this, "Changes saved locally. Will sync when online.", Toast.LENGTH_SHORT).show();
            loadingAnimation.setVisibility(View.GONE);
        }
    }

    private void syncChangesWithFirebase() {
        SharedPreferences sharedPreferences = getSharedPreferences("PendingChanges", MODE_PRIVATE);
        boolean changesPending = sharedPreferences.getBoolean("changesPending", false);

        if (changesPending) {
            String email = sharedPreferences.getString("email", "");
            String phone = sharedPreferences.getString("phone", "");
            String imageUriString = sharedPreferences.getString("imageUri", null);
            Uri imageUri = imageUriString != null ? Uri.parse(imageUriString) : null;

            // Retrieve the user data (loginId, userType, collectionPath, userIdField) from SharedPreferences
            SharedPreferences loginPrefs = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
            String loginId = loginPrefs.getString("LoginId", null);
            String userType = loginPrefs.getString("UserType", null);
            String collectionPath = userType.equals("lecturer") ? "lecturer" : "student";
            String userIdField = userType.equals("lecturer") ? "lectutId" : "studentID";

            // Find the document with the matching user ID
            db.collection(collectionPath).whereEqualTo(userIdField, loginId).get().addOnCompleteListener(task -> {
                if (task.isSuccessful() && !task.getResult().isEmpty()) {
                    DocumentSnapshot document = task.getResult().getDocuments().get(0);
                    // Define what to update
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("emailAddress", email);
                    updates.put("phoneNumber", phone);

                    // If there's a new image to upload
                    if (imageUri != null) {
                        // Upload the new photo to Firebase Storage
                        StorageReference newPhotoRef = FirebaseStorage.getInstance().getReference().child("Student/" + System.currentTimeMillis() + "_" + imageUri.getLastPathSegment());
                        newPhotoRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
                            newPhotoRef.getDownloadUrl().addOnSuccessListener(uri -> {
                                updates.put("imageURL", uri.toString()); // Add imageURL to update
                                updateFirestoreDocument(collectionPath, document.getId(), updates, sharedPreferences);
                            });
                        }).addOnFailureListener(e -> {
                            Toast.makeText(editDigitalStudentId.this, "Failed to upload new image: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    } else {
                        // No new image, just update other fields
                        updateFirestoreDocument(collectionPath, document.getId(), updates, sharedPreferences);
                    }
                } else {
                    Toast.makeText(editDigitalStudentId.this, "Failed to find user with ID: " + loginId, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void updateFirestoreDocument(String collectionPath, String documentId, Map<String, Object> updates, SharedPreferences sharedPreferences) {
        db.collection(collectionPath).document(documentId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Clear the pending changes from SharedPreferences
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.remove("email");
                    editor.remove("phone");
                    editor.remove("imageUri");
                    editor.putBoolean("changesPending", false);
                    editor.apply();

                    Toast.makeText(editDigitalStudentId.this, "Changes synced with Firebase successfully!", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(editDigitalStudentId.this, "Failed to sync changes with Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void registerNetworkCallback() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest.Builder builder = new NetworkRequest.Builder();

        connectivityManager.registerNetworkCallback(builder.build(), new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                // Network is available, sync changes with Firebase on the main thread
                runOnUiThread(() -> syncChangesWithFirebase());
            }
        });
    }



}