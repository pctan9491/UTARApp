package com.example.utarapp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

public class ReminderService extends BroadcastReceiver {
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onReceive(Context context, Intent intent) {
        String subjectCode = intent.getStringExtra("subjectCode");
        String subjectTime = intent.getStringExtra("subjectTime");
        String subjectVenue = intent.getStringExtra("subjectVenue");

        // Create a notification channel (required for Android 8.0 and above)
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        String channelId = "reminder_channel";
        CharSequence channelName = "Reminder Channel";
        int importance = NotificationManager.IMPORTANCE_HIGH;
        NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);
        channel.setDescription("Channel for class reminder notifications");
        channel.enableLights(true);
        channel.setLightColor(Color.BLUE);
        channel.enableVibration(true);
        notificationManager.createNotificationChannel(channel);

        // Build the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Class Reminder")
                .setContentText("Class: " + subjectCode + ", Time: " + subjectTime + ", Venue: " + subjectVenue)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL);

        // Show the notification
        notificationManager.notify(0, builder.build());
    }
}
