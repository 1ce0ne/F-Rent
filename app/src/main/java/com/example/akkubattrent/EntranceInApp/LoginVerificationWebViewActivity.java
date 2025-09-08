package com.example.akkubattrent.EntranceInApp;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

public class LoginVerificationWebViewActivity extends AppCompatActivity {

    private String apiKey;
    private int userId;
    private boolean verificationSuccess = false;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_webview);

        userId = getIntent().getIntExtra("user_id", -1);
        apiKey = getIntent().getStringExtra("api_key");

        if (userId == -1 || apiKey == null) {
            finishWithResult(false);
            return;
        }

        WebView webView = findViewById(R.id.webView);
        setupWebView(webView);
    }

    private void setupWebView(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setJavaScriptEnabled(true);
        webSettings.setDomStorageEnabled(true);

        webView.addJavascriptInterface(new WebAppInterface(), "Android");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.startsWith("https://akkubatt-rent.ru")) {
                    view.loadUrl(url, getAuthHeaders());
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (verificationSuccess) return;

                view.evaluateJavascript(
                        "(function() { " +
                                "   var successElement = document.querySelector('.verification-success'); " +
                                "   return successElement !== null; " +
                                "})();",
                        value -> {
                            if (value != null && value.equals("true")) {
                                verificationSuccess = true;
                                handleSuccess();
                            }
                        }
                );
            }
        });

        String verificationUrl = "https://akkubatt-rent.ru/login-telegram/?app_user_id=" + userId;
        webView.loadUrl(verificationUrl, getAuthHeaders());
    }

    private void handleSuccess() {
        runOnUiThread(() -> {
            finishWithResult(true);
            Toast.makeText(this, "Верификация успешна", Toast.LENGTH_SHORT).show();
        });
    }

    private void finishWithResult(boolean success) {
        Intent resultIntent = new Intent();
        setResult(success ? Activity.RESULT_OK : Activity.RESULT_CANCELED, resultIntent);
        finish();
    }

    private java.util.Map<String, String> getAuthHeaders() {
        java.util.Map<String, String> headers = new java.util.HashMap<>();
        headers.put("X-API-Key", apiKey);
        headers.put("Accept", "application/json");
        return headers;
    }

    private class WebAppInterface {
        @JavascriptInterface
        public void onAuthSuccess() {
            if (!verificationSuccess) {
                verificationSuccess = true;
                handleSuccess();
            }
        }
    }

    @Override
    public void onBackPressed() {
        finishWithResult(false);
        super.onBackPressed();
    }
}