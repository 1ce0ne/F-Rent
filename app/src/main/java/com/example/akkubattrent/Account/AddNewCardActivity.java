package com.example.akkubattrent.Account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

public class AddNewCardActivity extends AppCompatActivity {

    private WebView webView;
    private ProgressDialog progressDialog;
    private Button backToProfileButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_new_card);

        backToProfileButton = findViewById(R.id.backToProfileButton);
        webView = findViewById(R.id.webView);

        // Setup ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Загрузка...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Configure WebView
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (progressDialog != null && progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
                Toast.makeText(AddNewCardActivity.this, "Ошибка загрузки: " + description, Toast.LENGTH_SHORT).show();
            }
        });

        // Handle incoming data
        Intent intent = getIntent();
        if (intent != null) {
            // Check for HTML content first
            if (intent.hasExtra("html_content")) {
                String htmlContent = intent.getStringExtra("html_content");
                if (htmlContent != null && !htmlContent.isEmpty()) {
                    webView.loadDataWithBaseURL(
                            "https://akkubatt-rent.ru",
                            htmlContent,
                            "text/html",
                            "UTF-8",
                            null
                    );
                } else {
                    handleError("HTML content is empty");
                }
            }
            // Fallback to URL if no HTML content
            else if (intent.hasExtra("url")) {
                String url = intent.getStringExtra("url");
                if (url != null && !url.isEmpty()) {
                    webView.loadUrl(url);
                } else {
                    handleError("URL is empty");
                }
            } else {
                handleError("No data provided");
            }
        }

        backToProfileButton.setOnClickListener(v -> {
            Intent intent1 = new Intent(AddNewCardActivity.this, MyCardsActivity.class);
            startActivity(intent1);
            overridePendingTransition(R.anim.slide_in_left_signup, R.anim.slide_out_right_signup); // ← Новое слева, текущее вправо
            finish();
        });
    }

    private void handleError(String errorMessage) {
        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show();
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        finish();
    }

    @Override
    protected void onDestroy() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
        super.onDestroy();
    }
}