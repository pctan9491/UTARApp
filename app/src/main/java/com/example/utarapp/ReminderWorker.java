package com.example.utarapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class ReminderWorker extends Worker {
    public ReminderWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        String subjectCode = getInputData().getString("subjectCode");
        String subjectTime = getInputData().getString("subjectTime");
        String subjectVenue = getInputData().getString("subjectVenue");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            showNotification(subjectCode, subjectTime, subjectVenue);
        }

        return Result.success();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void showNotification(String subjectCode, String subjectTime, String subjectVenue) {
        // Create a notification channel (required for Android 8.0 and above)
        NotificationManager notificationManager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "reminder_channel";
        CharSequence channelName = "Reminder Channel";
        int importance = NotificationManager.IMPORTANCE_HIGH; // Set the importance to high
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.setDescription("Channel for class reminder notifications");
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);
        channel.enableVibration(true);
        notificationManager.createNotificationChannel(channel);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Class Reminder")
                .setContentText("Class: " + subjectCode + ", Time: " + subjectTime + ", Venue: " + subjectVenue)
                .setPriority(NotificationCompat.PRIORITY_HIGH) // Set the priority to high
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL); // Set default notification options

        // Show the notification
        notificationManager.notify(0, builder.build());
    }
}