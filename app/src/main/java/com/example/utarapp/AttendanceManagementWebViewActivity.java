package com.example.utarapp;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AttendanceManagementWebViewActivity extends AppCompatActivity {

    private WebView webView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.attendance_management_web_view); // Ensure you have this layout file with a WebView

        webView = (WebView) findViewById(R.id.attendance_web_view); // Make sure your WebView in the layout has this ID
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                String userId = getUserId();
                if (userId != null) {
                    // Inject the user ID into the WebView's local storage
                    view.evaluateJavascript("localStorage.setItem('saveLoginID', " + userId + ");", null);
                    view.evaluateJavascript("localStorage.getItem('saveLoginID');", value -> {
                        Log.d("WebViewLocalStorage", "UserID in localStorage after set: " + value);
                    });
                } else {
                    Log.d("WebViewLocalStorage", "UserID is null.");
                }
            }
        });

        webView.loadUrl("https://pctan9491.github.io/UTARApp_admin/attendanceTimetableList.html");
    }

    private String getUserId() {
        SharedPreferences sharedPreferences = getSharedPreferences("LoginPrefs", MODE_PRIVATE);
        String userId = sharedPreferences.getString("LoginId", null);
        Log.d("WebViewLocalStorage", "Retrieved UserID: " + userId);
        return userId;
    }
}