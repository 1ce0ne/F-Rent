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
import com.example.akkubattrent.TutorialClass.GreetingWindowActivity;

public class VerificationWebViewActivity extends AppCompatActivity {

    private String apiKey;
    private int userId;

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
                if (url.startsWith("https://akkubatt-work.ru")) {
                    view.loadUrl(url, getAuthHeaders());
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                // Проверяем успешную авторизацию по содержимому страницы
                view.evaluateJavascript(
                        "(function() { " +
                                "   var successText = document.body.innerText.includes('Авторизация успешна!'); " +
                                "   return {success: successText}; " +
                                "})();",
                        value -> {
                            if (value != null && value.contains("\"success\":true")) {
                                handleSuccess();
                            }
                        }
                );
            }
        });

        String verificationUrl = "https://akkubatt-work.ru/api/telegram/verify-phone=" + userId;
        webView.loadUrl(verificationUrl, getAuthHeaders());
    }

    private void handleSuccess() {
        // Проверяем статус пользователя перед переходом
        new Thread(() -> {
            try {
                Thread.sleep(500); // Даем время для обработки на сервере
                runOnUiThread(() -> {
                    finishWithResult(true);
                    // Сразу переходим на GreetingWindowActivity
                    Intent intent = new Intent(VerificationWebViewActivity.this, GreetingWindowActivity.class);
                    startActivity(intent);
                    overridePendingTransition(R.anim.slide_in_right_login, 0);
                    finish();
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> finishWithResult(false));
            }
        }).start();
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
            runOnUiThread(() -> handleSuccess());
        }
    }
}