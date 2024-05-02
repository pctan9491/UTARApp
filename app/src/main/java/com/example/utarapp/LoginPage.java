package com.example.utarapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.airbnb.lottie.LottieAnimationView;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.annotations.NotNull;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.SetOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Credentials;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginPage extends BaseActivity {

    private Button loginBtn;
    private EditText loginIdEdit, passwordEdit;
    private FirebaseAuth userAuth;
    private FirebaseFirestore db;
    private LottieAnimationView loadingAnimation;
    private TextView txtForgotPassword;
    private static final String SITE_KEY = "6LfKx4IpAAAAADSLd0HWIEXAX8rMK1NzT7SPi_6z";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_page);

        FirebaseApp.initializeApp(this);
        db = FirebaseFirestore.getInstance();
        loginIdEdit = findViewById(R.id.login_id_edit_text);
        passwordEdit = findViewById(R.id.password_edit_text);
        loadingAnimation = findViewById(R.id.loadingAnimation);
        //Login
        loginBtn = findViewById(R.id.login_btn);
        txtForgotPassword = findViewById(R.id.textViewForgotPassword);

        if (checkLoginState()) {
            Intent intentLogin = new Intent(LoginPage.this, bottomBar.class);
            startActivity(intentLogin);
            finish();
            return;
        }

        loginBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String loginId = loginIdEdit.getText().toString().trim();
                String password = passwordEdit.getText().toString().trim();
                if (loginId.isEmpty() || password.isEmpty()) {
                    Toast.makeText(LoginPage.this, "Please enter both login ID and password", Toast.LENGTH_SHORT).show();
                } else {
                    // Assuming loginId is used as an email in Firebase Auth
                    loginUser(loginId, password);
                }
            }
        });

        txtForgotPassword.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showResetPasswordDialog();
            }
        });
    }
    private void loginUser(String loginId, String password) {
        db.collection("user")
                .whereEqualTo("loginID", loginId)
                .get()
                .addOnCompleteListener(task -> {
                    // Show Lottie Animation
                    loadingAnimation.setVisibility(View.VISIBLE);
                    if (task.isSuccessful()) {
                        if (!task.getResult().isEmpty()) {
                            DocumentSnapshot document = task.getResult().getDocuments().get(0);
                            String storedPassword = document.getString("password");
                            String userType = document.getString("userType");
                            Log.d("Login", "User Type:" + userType);
                            if (password.equals(storedPassword)) {
                                // Save login state
                                saveLoginState(loginId, userType);

                                UserData.getInstance().setLoginId(loginId);
                                UserData.getInstance().setUserType(userType);
                                Intent intentLogin = new Intent(LoginPage.this, bottomBar.class);
                                startActivity(intentLogin);
                                finish();
                            } else {
                                Toast.makeText(LoginPage.this, "Incorrect password.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(LoginPage.this, "Login ID not found.", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(LoginPage.this, "Login failed. Please try again later.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void saveLoginState(String loginId, String userType) {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("IsLoggedIn", true);
        editor.putString("LoginId", loginId); // Optional: Save login ID if needed elsewhere
        editor.putString("UserType", userType);
        editor.putLong("LoginTimestamp", System.currentTimeMillis()); // Save current time as login timestamp
        editor.apply();
    }

    private boolean checkLoginState() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        boolean isLoggedIn = sharedPreferences.getBoolean("IsLoggedIn", false);
        long loginTimestamp = sharedPreferences.getLong("LoginTimestamp", 0);

        // Calculate the difference in time from now to the saved timestamp
        long currentTime = System.currentTimeMillis();
        long oneWeekInMillis = 7 * 24 * 60 * 60 * 1000; // 7 days in milliseconds

        // If more than a week has passed since login, consider the session expired
        if (isLoggedIn && (currentTime - loginTimestamp) < oneWeekInMillis) {
            return true; // Login state is valid
        } else {
            // Clear expired login state
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
            return false; // Login state is expired or not present
        }
    }

    private void showResetPasswordDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginPage.this, R.style.AlertDialogCustom);
        builder.setTitle("Reset Password");

        // Set up the input
        final EditText input = new EditText(LoginPage.this);
        input.setTextColor(Color.BLACK);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        input.setHint("Enter email address");
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Reset", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String email = input.getText().toString().trim();
                resetPassword(email);
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();

        builder.show();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        if (positiveButton != null) { // Always check for null to avoid NullPointerException
            positiveButton.setTextColor(Color.BLACK); // Example color
        }

        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        if (negativeButton != null) { // Always check for null to avoid NullPointerException
            negativeButton.setTextColor(Color.BLACK); // Example color
        }
    }

    private void resetPassword(final String email) {
        if (email.isEmpty()) {
            Toast.makeText(LoginPage.this, "Email field cannot be empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, String> collectionIdFields = new HashMap<>();
        collectionIdFields.put("student", "studentID");
        collectionIdFields.put("lecturer", "lectutId");
        //collectionIdFields.put("admin", "adminID");
        for (Map.Entry<String, String> entry : collectionIdFields.entrySet()) {
            final String collectionName = entry.getKey();
            final String idField = entry.getValue();
            db.collection(collectionName)
                    .whereEqualTo("emailAddress", email)
                    .limit(1)
                    .get()
                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                            if (!task.isSuccessful()) {
                                Toast.makeText(LoginPage.this, "Error checking user.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            List<DocumentSnapshot> documents = task.getResult().getDocuments();
                            if (documents.isEmpty()) {
                                Toast.makeText(LoginPage.this, "User not found.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            // User found, proceed with reset
                            DocumentSnapshot document = documents.get(0);
                            String userId = document.getString(idField);
                            createPasswordResetTokenAndSendEmail(userId, email, collectionName, idField);
                        }
                    });
        }
    }

    private void createPasswordResetTokenAndSendEmail(String userId, String email, String collectionName, String idField) {
        // Generate a secure random token
        String resetToken = generateSecureToken();

        // First, find the document reference with the matching userId
        db.collection(collectionName)
                .whereEqualTo(idField, userId)
                .limit(1)
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (!task.isSuccessful()) {
                            Toast.makeText(LoginPage.this, "Failed to find user document.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot.isEmpty()) {
                            Toast.makeText(LoginPage.this, "No user found with that ID.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        // Get the first document from the results and its reference
                        DocumentReference userDocRef = querySnapshot.getDocuments().get(0).getReference();

                        // Create a map for the reset data
                        Map<String, Object> resetData = new HashMap<>();
                        resetData.put("resetToken", resetToken);
                        resetData.put("tokenExpiry", new Date(System.currentTimeMillis() + (24 * 60 * 60 * 1000))); // 24-hour expiry

                        // Set the reset data on the specific document
                        userDocRef.set(resetData, SetOptions.merge())
                                .addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            Log.d("ResetPassword", "Token saved successfully for: " + email);
                                            Toast.makeText(LoginPage.this, "Sending reset email...", Toast.LENGTH_SHORT).show();
                                            sendResetEmail(email, resetToken); // Implement this method to send the email
                                        } else {
                                            Toast.makeText(LoginPage.this, "Error saving reset token.", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                    }
                });
    }
    private void updatePassword(String userId, String newPassword) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("password", newPassword); // Make sure to hash the password before storing

        db.collection("users").document(userId)
                .update(userData)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginPage.this, "Password updated successfully.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginPage.this, "Failed to update password.", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    // Utility method to generate a secure token
    private String generateSecureToken() {
        // Implement token generation logic here. For example, you could use a UUID.
        return UUID.randomUUID().toString();
    }

    // Implement this method to send an email with the reset token
    private void sendResetEmail(String email, String resetToken) {
        String apiKey = "430baa6f543cd5607c9bc877bbcea34a"; // Remember to secure your API keys
        String apiSecret = "6493a40b5df21cffbbd56c97790d2f22";

        try {
            JSONObject from = new JSONObject();
            from.put("Email", "pctan9491@gmail.com"); // Ensure this is a verified sender email in Mailjet
            from.put("Name", "UTARApp Admin");

            JSONObject to = new JSONObject();
            to.put("Email", email); // Recipient's email

            JSONObject messageJson = new JSONObject();
            messageJson.put("From", from);
            messageJson.put("To", new JSONArray().put(to));
            messageJson.put("Subject", "Password Reset Request");
            messageJson.put("TextPart", "Please click the link to reset your password: https://pctan9491.github.io/UTARApp_admin/resetPw?token=" + resetToken);

            JSONObject emailData = new JSONObject();
            emailData.put("Messages", new JSONArray().put(messageJson));

            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(mediaType, emailData.toString());
            String credentials = Credentials.basic(apiKey, apiSecret);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://api.mailjet.com/v3.1/send")
                    .post(body)
                    .addHeader("Authorization", credentials)
                    .addHeader("Content-Type", "application/json")
                    .build();

            // Execute the request
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.e("Mailjet", "Failed to send reset email", e);
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    if (response.isSuccessful()) {
                        Log.d("Mailjet", "Reset email sent to: " + email);
                    } else {
                        Log.e("Mailjet", "Failed to send reset email. Status: " + response.code() + ", Response: " + response.body().string());
                    }
                }
            });
        } catch (JSONException e) {
            Log.e("Mailjet", "JSON exception", e);
        }
    }






}