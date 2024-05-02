package com.example.utarapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

public class BaseActivity extends AppCompatActivity {

    private Snackbar snackbar;
    private static final long AUTO_LOGOUT_TIME_MS = 15 * 60 * 1000; // 15 minutes
    private Handler handler = new Handler();
    private SharedPreferences preferences;
    private Snackbar reconnectedSnackbar;
    private boolean wasConnected = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkReceiver, filter);
        checkNetworkStatus();

        long lastInteractionTime = preferences.getLong("last_interaction_time", 0);
        if (System.currentTimeMillis() - lastInteractionTime > AUTO_LOGOUT_TIME_MS) {
            logoutUser();
        } else {
            startUserInactivityCheck();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(networkReceiver);
        if (snackbar != null) {
            snackbar.dismiss();
        }
        handler.removeCallbacks(logoutRunnable);
    }

    private void checkNetworkStatus() {
        boolean isConnected = isConnectedToNetwork();
        if (!isConnected) {
            showOfflineBanner();
            wasConnected = false; // Update the previous state
        } else {
            hideOfflineBanner();
            if (!wasConnected) { // If reconnected
                showReconnectedBanner();
                wasConnected = true; // Update the previous state
            }
        }
    }

    private void showReconnectedBanner() {
        reconnectedSnackbar = Snackbar.make(findViewById(android.R.id.content), "You are online!", Snackbar.LENGTH_INDEFINITE);
        View sbView = reconnectedSnackbar.getView();
        sbView.setBackgroundColor(Color.GREEN);
        reconnectedSnackbar.show();

        // Dismiss the snackbar after 5 seconds
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                reconnectedSnackbar.dismiss();
            }
        }, 5000); // 5000 milliseconds = 5 seconds
    }

    private boolean isConnectedToNetwork() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    private void showOfflineBanner() {
        snackbar = Snackbar.make(findViewById(android.R.id.content), "You are offline!", Snackbar.LENGTH_INDEFINITE);
        View sbView = snackbar.getView();
        sbView.setBackgroundColor(Color.RED);
        snackbar.show();

        // Dismiss the snackbar after 5 seconds
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                snackbar.dismiss();
            }
        }, 5000); // 5000 milliseconds = 5 seconds
    }

    private void hideOfflineBanner() {
        if (snackbar != null && snackbar.isShown()) {
            snackbar.dismiss();
        }
        if (reconnectedSnackbar != null && reconnectedSnackbar.isShown()) {
            reconnectedSnackbar.dismiss();
        }
    }

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            checkNetworkStatus();
        }
    };
    @Override
    public void onUserInteraction() {
        super.onUserInteraction();
        updateLastInteractionTime();
    }

    private void startUserInactivityCheck() {
        handler.postDelayed(logoutRunnable, AUTO_LOGOUT_TIME_MS);
    }

    private void updateLastInteractionTime() {
        preferences.edit().putLong("last_interaction_time", System.currentTimeMillis()).apply();
    }

    private Runnable logoutRunnable = new Runnable() {
        @Override
        public void run() {
            logoutUser();
        }
    };

    private void logoutUser() {
        UserData.getInstance().clearSession();
        Intent intent = new Intent(this, LoginPage.class);
        startActivity(intent);
        finish();
    }
}
