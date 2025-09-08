package com.example.akkubattrent.UserProfile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

public class UsagePolicyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_policy);

        findViewById(R.id.backToProfileButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup); // ← Новое слева, текущее вправо
        });

        findViewById(R.id.confidens).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://akkubatt-rent.ru/static/docx/personality.pdf"));
            startActivity(intent);
        });

        findViewById(R.id.public_offert).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://akkubatt-rent.ru/static/docx/ofert.pdf"));
            startActivity(intent);
        });
    }
}
