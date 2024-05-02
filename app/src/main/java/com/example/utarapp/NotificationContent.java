package com.example.utarapp;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class NotificationContent extends BaseActivity {

    private FirebaseFirestore db;
    private TextView notificationTitle, notificationTime, notificationDescription;
    private LinearLayout filesContainer;
    private GridLayout photosContainer;
    private DownloadManager downloadManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_content);

        db = FirebaseFirestore.getInstance();
        notificationTitle = findViewById(R.id.notificationTitle);
        notificationTime = findViewById(R.id.notificationTime);
        notificationDescription = findViewById(R.id.notificationDescription);
        photosContainer = findViewById(R.id.photosContainer);
        filesContainer = findViewById(R.id.filesContainer);
        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

// Get the document ID from the intent extras
        String docId = getIntent().getStringExtra("docId");
        if (docId != null) {
            fetchNotification(docId);
        }
    }
    private void fetchNotification(String docId) {
        db.collection("notification").document(docId).get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {
                if (documentSnapshot.exists()) {
                    notificationTitle.setText(documentSnapshot.getString("title"));
                    notificationTime.setText(documentSnapshot.getString("date"));
                    notificationDescription.setText(documentSnapshot.getString("description"));

                    // Handle photos
                    List<String> photoURLs = (List<String>) documentSnapshot.get("photoURLs");
                    if (photoURLs != null) {
                        int totalPhotos = photoURLs.size();
                        int rowCount = (int) Math.ceil(totalPhotos / 2.0);

                        GridLayout photosContainer = findViewById(R.id.photosContainer);
                        photosContainer.setColumnCount(1);  // Two photos per row.
                        photosContainer.setRowCount(rowCount);

                        for (String url : photoURLs) {
                            ImageView imageView = new ImageView(NotificationContent.this);

                            // Setting a fixed width and height for the ImageView using the dpToPx function
                            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                            params.width = dpToPx(300);
                            params.height = dpToPx(300);
                            params.setMargins(18, 18, 18, 18);
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
                    List<String> fileURLs = (List<String>) documentSnapshot.get("fileURLs");
                    if (fileURLs != null) {
                        for (final String url : fileURLs) {
                            TextView textView = new TextView(NotificationContent.this);
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
    private void downloadImage(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // Get the file name from the URL
        String fileName = getFileNameFromURL(url);

        // Set the destination directory for the downloaded image
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, fileName);

        // Enqueue the download request
        downloadManager.enqueue(request);

        Toast.makeText(NotificationContent.this, "Image download started", Toast.LENGTH_SHORT).show();
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
            Toast.makeText(NotificationContent.this, "Opening file from local storage", Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(NotificationContent.this, "File downloaded and saved for offline access", Toast.LENGTH_SHORT).show();
                    }
                }
            };

            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            Toast.makeText(NotificationContent.this, "Downloading file...", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean isOffline() {
        // Check if the device is connected to the internet
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo == null || !networkInfo.isConnected();
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
            // Toast.makeText(this, "File saved to local storage", Toast.LENGTH_SHORT).show();

        } catch (IOException e) {
            e.printStackTrace();
            // Handle any errors that occur during file saving
            // Log an error message or show an error toast
            Log.e("File Save", "Error saving file to local storage", e);
            // Toast.makeText(this, "Error saving file to local storage", Toast.LENGTH_SHORT).show();
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
}