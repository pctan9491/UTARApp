package com.example.utarapp;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class NotificationReceiver extends BroadcastReceiver {
    private static final String CHANNEL_ID = "reminder_channel";

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        createNotificationChannel(context);

        String subjectCode = intent.getStringExtra("subjectCode");
        String subjectTime = intent.getStringExtra("subjectTime");
        String subjectVenue = intent.getStringExtra("subjectVenue");

        // Create the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Class Reminder")
                .setContentText(subjectCode + " starts in 1 hour at " + subjectVenue)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        // Set the notification sound
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        // Set the notification vibration pattern
        long[] vibrationPattern = {0, 500, 500, 500}; // Vibrate for 500ms, pause for 500ms, vibrate for 500ms
        builder.setVibrate(vibrationPattern);

        // Show the notification
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(0, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Reminder Channel";
            String description = "Channel for class reminders";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 500, 500});
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}
