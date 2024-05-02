package com.example.utarapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

public class EmailWebViewActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100; // Request code for sign-in
    private GoogleSignInClient mGoogleSignInClient;
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_email_web_view);

        webView = findViewById(R.id.emailWebView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        // Configure sign-in to request the user's email
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestIdToken("331019317153-35clds7tsncb3ppgo05r3a211srp2hnj.apps.googleusercontent.com")
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d("EmailWebViewActivity", "onActivityResult: requestCode=" + requestCode + ", resultCode=" + resultCode);

        if (requestCode == RC_SIGN_IN) {
            if (resultCode == RESULT_OK) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                Log.d("EmailWebViewActivity", "Signed in successfully, email: " + account.getEmail());
                // Signed in successfully, show authenticated UI.
                if (account != null) {
                    String email = account.getEmail();
                    if (email != null && email.endsWith("@1utar.my")) {
                        Log.d("EmailWebViewActivity", "Email domain is @1utar.my");
                        // Open Gmail in the browser
                        loadGmailWebView(email);
                    } else {
                        Log.d("EmailWebViewActivity", "Email domain is not @1utar.my");
                        // Prompt user to log in with a 1utar.my account
                        promptForCorrectAccount();
                    }
                }
            } catch (ApiException e) {
                // The ApiException status code indicates the detailed failure reason.
                // Log or handle the failure properly in your app.
            }
            } else {
                Log.d("EmailWebViewActivity", "Sign-in operation canceled or failed.");
                promptForCorrectAccount();
            }
        }
    }

    private void openGmailInBrowser() {
        Log.d("EmailWebViewActivity", "Opening Gmail in browser.");
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://mail.google.com/mail/"));
            startActivity(intent);
    }

    private void loadGmailWebView(String email) {
        webView.clearCache(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.loadUrl("https://mail.google.com/mail/");
        // Optionally, customize the URL or actions based on the email
    }

    private void promptForCorrectAccount() {
        Log.d("EmailWebViewActivity", "Prompting for correct account.");
        // Example using Toast
        Toast.makeText(this, "Please sign in with your @1utar.my account.", Toast.LENGTH_LONG).show();

        // Optionally, log out the user from Google Sign-In to prompt re-selection
        mGoogleSignInClient.signOut().addOnCompleteListener(this, task -> {
            // User is signed out. Show sign-in options again or guide the user accordingly.
        });
    }

}