package com.example.akkubattrent.EntranceInApp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;

public class LoginConfirmationActivity extends AppCompatActivity {

    private static final String API_KEY = "o4bAvyn0ZXaFeLDr89lk";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance_confirmation);

        FrameLayout smsButton = findViewById(R.id.smsButton);
        FrameLayout telegramButton = findViewById(R.id.telegramButton);

        smsButton.setEnabled(false);
        smsButton.setAlpha(0.5f);

        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        int userId = sharedPreferences.getInt("id", -1);

        telegramButton.setOnClickListener(v -> {
            if (userId != -1) {
                openVerificationWebView(userId);
            } else {
                Toast.makeText(this, "Ошибка: ID пользователя не найден", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openVerificationWebView(int userId) {
        Intent intent = new Intent(this, LoginVerificationWebViewActivity.class);
        intent.putExtra("user_id", userId);
        intent.putExtra("api_key", API_KEY);
        startActivityForResult(intent, 1);
        overridePendingTransition(R.anim.slide_in_right_login, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                sharedPreferences.edit().putBoolean("is_logged_in", true).apply();
                proceedToMainMenu();
            } else if (resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(this, "Ошибка верификации", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    private void proceedToMainMenu() {
        Intent intent = new Intent(this, MainMenuActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right_login, 0);
        finish();
    }
}