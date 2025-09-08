package com.example.akkubattrent.UserProfile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class UserProfileActivity extends AppCompatActivity {
    private TextView textViewName;
    private TextView textViewNumber;
    private ImageView circleImageView;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        textViewName = findViewById(R.id.textViewName);
        textViewNumber = findViewById(R.id.textViewNumber);
        circleImageView = findViewById(R.id.circleImageView);
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        setupButtonListeners();
        loadUserData();
    }

    private void setupButtonListeners() {

        findViewById(R.id.backButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup); // ← Новое слева, текущее вправо
        });

        findViewById(R.id.myAccountButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, MyAccountActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        findViewById(R.id.helpButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, SupportActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        findViewById(R.id.myOrdersButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, MyOrdersActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        findViewById(R.id.faqButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, FAQActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        findViewById(R.id.usagePolicy).setOnClickListener(v -> {
            Intent intent = new Intent(this, UsagePolicyActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

    }

    private void loadUserData() {
        String name = sharedPreferences.getString("name", "Имя не указано");
        String phoneNumber = sharedPreferences.getString("phone_number", "Номер не указан");

        textViewName.setText(name);
        textViewNumber.setText(phoneNumber);

        loadProfileImageFromCache();
        loadProfileImageFromDatabase(phoneNumber);
    }

    private void loadProfileImageFromCache() {
        String encodedImage = sharedPreferences.getString("profile_image", null);
        if (encodedImage != null) {
            byte[] decodedString = android.util.Base64.decode(encodedImage, android.util.Base64.DEFAULT);
            circleImageView.setImageBitmap(BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length));
        }
    }

    private void loadProfileImageFromDatabase(String phoneNumber) {
        new Thread(() -> {
            try (Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                    "akku_batt_admin",
                    "8rssCz31UiUr512e")) {

                try (PreparedStatement stmt = connection.prepareStatement(
                        "SELECT image FROM users WHERE phone_number = ?")) {
                    stmt.setString(1, phoneNumber);
                    ResultSet rs = stmt.executeQuery();

                    if (rs.next()) {
                        byte[] imageBytes = rs.getBytes("image");
                        if (imageBytes != null) {
                            Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(imageBytes));
                            runOnUiThread(() -> {
                                circleImageView.setImageBitmap(bitmap);
                                saveProfileImageToCache(bitmap);
                            });
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("ProfileImage", "Ошибка загрузки изображения", e);
            }
        }).start();
    }

    private void saveProfileImageToCache(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        sharedPreferences.edit()
                .putString("profile_image",
                        android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.DEFAULT))
                .apply();
    }
}