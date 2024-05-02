package com.example.utarapp;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class AssignmentDetailActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private TextView assignmentTitle, assignmentDueDate, assignmentDescription;
    private LinearLayout filesContainer;
    private GridLayout photosContainer;
    private DownloadManager downloadManager;
    private String studId;
    private ListenerRegistration likeListenerRegistration;
    private ListenerRegistration commentListenerRegistration;
    private EditText commentInput;
    private Button postCommentButton;
    private RecyclerView commentsRecyclerView;
    private CommentsAdapter commentsAdapter;
    private List<Comment> commentsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assignment_detail);

        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        studId = sharedPreferences.getString("LoginId", null);
        db = FirebaseFirestore.getInstance();
        assignmentTitle = findViewById(R.id.assignmentTitle);
        assignmentDueDate = findViewById(R.id.assignmentDueDate);
        assignmentDescription = findViewById(R.id.assignmentDescription);
        photosContainer = findViewById(R.id.photosAssignmentContainer);
        filesContainer = findViewById(R.id.filesAssignmentContainer);
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        final Button likeButton = findViewById(R.id.likeButton);
        commentInput = findViewById(R.id.commentInput);
        commentInput.setTextColor(Color.BLACK);
        postCommentButton = findViewById(R.id.postCommentButton);

        // Initialize RecyclerView
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsAdapter = new CommentsAdapter(commentsList);
        commentsRecyclerView.setAdapter(commentsAdapter);

        // Retrieve the docId passed from AssignmentTask
        String docId = getIntent().getStringExtra("DOC_ID");

        // Log the docId to confirm it's received
        Log.d("AssignmentDetailActivity", "Received docId: " + docId);
        if (docId != null) {
            fetchAssignment(docId);
        }

        db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .build();
        db.setFirestoreSettings(settings);

        FirebaseFirestore.getInstance().collection("assignments").document(docId).collection("comments")
                .orderBy("pin", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot querySnapshot) {
                        if (!querySnapshot.getMetadata().isFromCache()) {
                            // Data has been updated from the server
                            // Refresh your UI or perform any necessary operations
                            Log.d("AssignmentDetailActivity", "Data synced from server");
                            Toast.makeText(AssignmentDetailActivity.this, "Data synced from server.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Handle the error
                        Log.w("AssignmentDetailActivity", "Error syncing data from server", e);
                    }
                });



        fetchUpdatedLikeCount(studId, docId);
        likeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleLike(docId, studId); // Assuming docId and studId are available
            }
        });

        postCommentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                postComment(docId, commentInput.getText().toString());
                commentInput.setText(""); // Clear the input field after posting
            }
        });

        // Fetch and display comments
        fetchComments(docId);

    }
    private void fetchAssignment(String docId) {
        db.collection("assignments").document(docId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    assignmentTitle.setText(documentSnapshot.getString("title"));
                    assignmentDueDate.setText("Due Date: " + documentSnapshot.getString("dueDate"));
                    String descriptionHtml = documentSnapshot.getString("description");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // For API Level 24 and above
                        assignmentDescription.setText(Html.fromHtml(descriptionHtml, Html.FROM_HTML_MODE_LEGACY));
                    } else {
                        // For below API Level 24
                        assignmentDescription.setText(Html.fromHtml(descriptionHtml));
                    }

                    // Handle photos
                    List<String> photoURLs = (List<String>) documentSnapshot.get("imageUrls");
                    if (photoURLs != null) {
                        int totalPhotos = photoURLs.size();
                        int rowCount = (int) Math.ceil(totalPhotos / 2.0);

                        GridLayout photosContainer = findViewById(R.id.photosAssignmentContainer);
                        photosContainer.setColumnCount(2);  // Two photos per row.
                        photosContainer.setRowCount(rowCount);

                        for (String url : photoURLs) {
                            ImageView imageView = new ImageView(AssignmentDetailActivity.this);

                            // Setting a fixed width and height for the ImageView using the dpToPx function
                            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                            params.width = dpToPx(150);
                            params.height = dpToPx(150);
                            params.setMargins(18, 18, 8, 18);
                            imageView.setLayoutParams(params);

                            Picasso.get().load(url).into(imageView);
                            photosContainer.addView(imageView);

                            // Set long click listener on the ImageView
                            imageView.setOnLongClickListener(new View.OnLongClickListener() {
                                @Override
                                public boolean onLongClick(View v) {
                                    downloadImage(url);
                                    return true;
                                }
                            });
                        }
                    }


                    // Handle files
                    List<String> fileURLs = (List<String>) documentSnapshot.get("documentUrls");
                    if (fileURLs != null) {
                        for (final String url : fileURLs) {
                            TextView textView = new TextView(AssignmentDetailActivity.this);
                            final String fileName = getFileNameFromURL(url);
                            textView.setText(fileName);

                            // Styling to make it resemble an attachment
                            textView.setTextColor(Color.BLUE);
                            textView.setBackgroundResource(R.drawable.rounded_border);
                            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_attachment, 0, 0, 0);
                            textView.setCompoundDrawablePadding(16);
                            textView.setPadding(16, 16, 16, 16);

                            textView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    downloadAndOpenFile(url, fileName);
                                }
                            });
                            filesContainer.addView(textView);
                        }
                    }


                }
            }
        });
    }
    private String getFileNameFromURL(String url) {
        return Uri.parse(url).getLastPathSegment();
    }
    private int dpToPx(int dp) {
        float density = getApplicationContext().getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }
    private void downloadAndOpenFile(String url, final String fileName) {
        File localFile = new File(getFilesDir(), fileName);
        if (localFile.exists()) {
            // File exists in local storage, open it directly
            openFile(localFile.getAbsolutePath());
            Toast.makeText(AssignmentDetailActivity.this, "Opening file from local storage", Toast.LENGTH_SHORT).show();
        } else {
            // Download the file using DownloadManager
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);

            final long downloadId = downloadManager.enqueue(request);

            BroadcastReceiver onComplete = new BroadcastReceiver() {
                public void onReceive(Context ctxt, Intent intent) {
                    long referenceId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (downloadId == referenceId) {
                        // Save the downloaded file to local storage
                        File downloadedFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
                        saveFileToLocalStorage(downloadedFile);
                        openFile(downloadedFile.getAbsolutePath());
                        Toast.makeText(AssignmentDetailActivity.this, "File downloaded and saved for offline access", Toast.LENGTH_SHORT).show();
                    }
                }
            };

            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            Toast.makeText(AssignmentDetailActivity.this, "Downloading file...", Toast.LENGTH_SHORT).show();
        }
    }

    private void downloadImage(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // Get the file name from the URL
        String fileName = getFileNameFromURL(url);

        // Set the destination directory for the downloaded image
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, fileName);

        // Enqueue the download request
        downloadManager.enqueue(request);

        Toast.makeText(AssignmentDetailActivity.this, "Image download started", Toast.LENGTH_SHORT).show();
    }

    private void saveFileToLocalStorage(File sourceFile) {
        try {
            // Create a destination file in the app's internal storage
            File destinationFile = new File(getFilesDir(), sourceFile.getName());

            // Create a FileInputStream for the source file
            FileInputStream inputStream = new FileInputStream(sourceFile);

            // Create a FileOutputStream for the destination file
            FileOutputStream outputStream = new FileOutputStream(destinationFile);

            // Create a buffer to hold the data during copying
            byte[] buffer = new byte[1024];
            int length;

            // Copy the contents of the source file to the destination file
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            // Close the input and output streams
            inputStream.close();
            outputStream.close();

            // Delete the source file if it's a temporary downloaded file
            if (sourceFile.exists() && sourceFile.getAbsolutePath().contains(Environment.DIRECTORY_DOWNLOADS)) {
                sourceFile.delete();
            }

            // Log a message or show a toast to indicate successful file saving
            Log.d("File Save", "File saved to local storage: " + destinationFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
            // Handle any errors that occur during file saving
            // Log an error message or show an error toast
            Log.e("File Save", "Error saving file to local storage", e);
        }
    }

    private void openFile(String fileName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);
        if (file.exists()) {
            Uri path = FileProvider.getUriForFile(this, "com.example.utarapp.fileprovider", file);
            Intent openIntent = new Intent(Intent.ACTION_VIEW);
            openIntent.setDataAndType(path, getMimeType(fileName));
            openIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(openIntent);
        }
    }

    private String getMimeType(String fileName) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }

    private void toggleLike(final String docId, final String studId) {
        final DocumentReference docRef = db.collection("assignments").document(docId);
        db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef);
            List<String> likes = (List<String>) snapshot.get("likes");
            if (likes == null) {
                likes = new ArrayList<>();
            }

            // Toggle logic
            if (likes.contains(studId)) {
                likes.remove(studId); // Unlike
            } else {
                likes.add(studId); // Like
            }

            transaction.update(docRef, "likes", likes);
            return null; // Firestore transactions do not support returning custom results
        }).addOnSuccessListener(aVoid -> {
            // Instead of returning the like count from the transaction,
            // fetch the updated document or maintain the count in a variable.
            fetchUpdatedLikeCount(docId, studId); // Fetch updated likes to update UI
        }).addOnFailureListener(e -> {
            // Handle the failure case when offline
            Log.w("AssignmentDetailActivity", "Error toggling like while offline", e);
            Toast.makeText(AssignmentDetailActivity.this, "Like will be save and will updated until internet is connected", Toast.LENGTH_SHORT).show();
            // You can show a snackbar or a dialog to inform the user about the offline state
        });
    }

    private ListenerRegistration fetchUpdatedLikeCount(final String docId, final String studId) {
        final DocumentReference docRef = db.collection("assignments").document(docId);
        if (likeListenerRegistration == null) {
            return docRef.addSnapshotListener((documentSnapshot, e) -> {
                if (e != null) {
                    Log.w("AssignmentDetailActivity", "Listen failed.", e);
                    return;
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    List<String> likes = (List<String>) documentSnapshot.get("likes");
                    int likeCount = likes != null ? likes.size() : 0;
                    updateLikeButtonUI(studId, docId, likeCount); // Now with the like count

                    // Check if the data is from the server or the local cache
                    if (!documentSnapshot.getMetadata().isFromCache()) {
                        Log.d("AssignmentDetailActivity", "Like data synced from server");
                        Toast.makeText(AssignmentDetailActivity.this, "Like data synced from server", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
        return likeListenerRegistration;
    }


    // Updated to accept likeCount for UI update
    private void updateLikeButtonUI(final String studId, String docId, int likeCount) {
        DocumentReference docRef = db.collection("assignments").document(docId);
        docRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document != null && document.exists()) {
                    List<String> likes = (List<String>) document.get("likes");
                    Button likeButton = findViewById(R.id.likeButton);
                    TextView likeCountText = findViewById(R.id.likeCount); // Ensure you have this TextView in your layout

                    // Set the button's UI based on whether likes contains studId
                    if (likes != null && likes.contains(studId)) {
                        likeButton.setText("Unlike"); // Liked state
                    } else {
                        likeButton.setText("Like"); // Unliked state
                    }

                    // Update the like count display
                    String likeCountDisplay = likeCount + (likeCount == 1 ? " Like" : " Likes");
                    likeCountText.setText(likeCountDisplay);
                }
            }
        });
    }
    private void postComment(String docId, String commentText) {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("LoginId", null);
        if (commentText.isEmpty() || studId == null) {
            return; // Don't post empty comments
        }

        Map<String, Object> comment = new HashMap<>();
        comment.put("userId", studId);
        comment.put("commentText", commentText);
        comment.put("timestamp", FieldValue.serverTimestamp());
        comment.put("pin", false);

        db.collection("assignments").document(docId)
                .collection("comments").add(comment)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Log.d("AssignmentDetail", "Comment posted successfully");
                        // Optionally, refresh the comments list to include the new comment
                        fetchComments(docId);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("AssignmentDetail", "Error posting comment while offline", e);
                        Toast.makeText(AssignmentDetailActivity.this, "Comment is posting and it will updated until internet is connected", Toast.LENGTH_SHORT).show();
                        // You can show a snackbar or a dialog to inform the user about the offline state
                    }
                });
    }
    private ListenerRegistration fetchComments(String docId) {
        return db.collection("assignments").document(docId).collection("comments")
                .orderBy("pin", Query.Direction.DESCENDING)
                .orderBy("timestamp", Query.Direction.DESCENDING) // Order comments by descending timestamp
                .orderBy(FieldPath.documentId(), Query.Direction.DESCENDING)
                .addSnapshotListener((queryDocumentSnapshots, e) -> {
                    if (e != null) {
                        Log.w("TAG", "Listen failed.", e);
                        return;
                    }

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        List<Comment> tempCommentsList = new ArrayList<>();
                        AtomicInteger processedComments = new AtomicInteger(queryDocumentSnapshots.size());

                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Comment comment = doc.toObject(Comment.class);
                            // Asynchronously resolve each username
                            resolveUserName(comment, newUserName -> {
                                comment.setUserName(newUserName); // Update the username for the comment

                                // Synchronized addition to list to handle concurrent modifications
                                synchronized (tempCommentsList) {
                                    tempCommentsList.add(comment);

                                    // Check if all comments have been processed
                                    if (processedComments.decrementAndGet() == 0) {
                                        // Sort the list again by pin and timestamp in case the asynchronous operations altered the order
                                        tempCommentsList.sort((c1, c2) -> {
                                            // Handle cases where either timestamp might be null
                                            if (c1.getTimestamp() == null && c2.getTimestamp() == null) {
                                                return 0; // Both are null, treat as equal
                                            } else if (c1.getTimestamp() == null) {
                                                return 1; // Null timestamps treated as later (or you can choose -1 if earlier)
                                            } else if (c2.getTimestamp() == null) {
                                                return -1;
                                            }

                                            // Existing comparison for non-null timestamps
                                            return c2.getTimestamp().compareTo(c1.getTimestamp());
                                        });


                                        // Update the UI
                                        commentsList.clear();
                                        commentsList.addAll(tempCommentsList);
                                        commentsAdapter.notifyDataSetChanged();
                                    }
                                }
                            });
                        }
                    }
                });
    }



    private void resolveUserName(Comment comment, Consumer<String> onUserNameResolved) {
        if (comment.getUserId() == null) {
            onUserNameResolved.accept("Unknown User"); // Handle null userId case
            return;
        }
        db.collection("user").whereEqualTo("loginID", comment.getUserId())
                .get()
                .addOnCompleteListener(task -> {
                    Log.w("TAG", "User id: " + comment.getUserId());
                    if (task.isSuccessful() && !task.getResult().isEmpty()) {
                        DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                        String userType = userDoc.getString("userType");
                        String userIdField = userType.equals("student") ? "studentID" : "lectutId";
                        String collectionPath = userType.equals("student") ? "student" : "lecturer";

                        db.collection(collectionPath).whereEqualTo(userIdField, comment.getUserId())
                                .get()
                                .addOnSuccessListener(userQueryDocumentSnapshots -> {
                                    if (!userQueryDocumentSnapshots.isEmpty()) {
                                        DocumentSnapshot nameDoc = userQueryDocumentSnapshots.getDocuments().get(0);
                                        String name = nameDoc.getString("name");
                                        onUserNameResolved.accept(name);
                                    } else {
                                        onUserNameResolved.accept("Unknown User");
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Log.w("TAG", "Error fetching user name", e);
                                    onUserNameResolved.accept("Unknown User");
                                });
                    } else {
                        Log.w("TAG", "Error fetching user type");
                        onUserNameResolved.accept("Unknown User");
                    }
                });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Retrieve the docId each time the activity starts, in case it changes
        String docId = getIntent().getStringExtra("DOC_ID");

        Log.d("AssignmentDetailActivity", "Received docId: " + docId);
        if (docId != null && studId != null) {
            likeListenerRegistration = fetchUpdatedLikeCount(docId, studId);
            commentListenerRegistration = fetchComments(docId);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (likeListenerRegistration != null) {
            likeListenerRegistration.remove(); // Properly remove the listener
            likeListenerRegistration = null; // Avoid memory leaks
        }
        if (commentListenerRegistration != null) {
            commentListenerRegistration.remove(); // Properly remove the listener
            commentListenerRegistration = null; // Avoid memory leaks
        }
    }
}