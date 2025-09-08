package com.example.akkubattrent.UserProfile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.TutorialClass.GreetingWindowActivityDouble;
import com.example.akkubattrent.R;

public class SupportActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_support);

        FrameLayout toTelegramButton = findViewById(R.id.toTelegramButton);
        FrameLayout tutorialButton = findViewById(R.id.tutorialButton);
        FrameLayout helperNumberButton = findViewById(R.id.helperNumberButton);

        findViewById(R.id.backToProfileButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup); // ← Новое слева, текущее вправо
        });

        toTelegramButton.setOnClickListener(v -> openTelegramProfile());

        tutorialButton.setOnClickListener(v -> {
            Intent intent = new Intent(SupportActivity.this, GreetingWindowActivityDouble.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_up_window, 0);
            finish();

        });

        helperNumberButton.setOnClickListener(v -> showCallDialog());
    }

    private void openTelegramProfile() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/akkubatt_rent_bot"));
        startActivity(intent);
    }

    private void showCallDialog() {
        final String phoneNumber = "+79260134385";

        // Создаем Intent для набора номера
        Intent callIntent = new Intent(Intent.ACTION_DIAL);
        callIntent.setData(Uri.parse("tel:" + phoneNumber));

        // Запускаем стандартное приложение для звонков
        try {
            startActivity(callIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть приложение для звонков",
                    Toast.LENGTH_SHORT).show();
        }
    }
}