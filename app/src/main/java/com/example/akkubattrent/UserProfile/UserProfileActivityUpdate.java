package com.example.akkubattrent.UserProfile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.Account.ChangePasswordDialog;
import com.example.akkubattrent.Account.MyCardsActivity;
import com.example.akkubattrent.Account.PassportActivity;
import com.example.akkubattrent.EntranceInApp.LoginActivity;
import com.example.akkubattrent.R;
import com.example.akkubattrent.EntranceInApp.SplashScreenActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class UserProfileActivityUpdate extends AppCompatActivity {
    private TextView textViewName;
    private TextView textViewNumber;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile_update);

        textViewName = findViewById(R.id.textViewName);
        textViewNumber = findViewById(R.id.textViewNumber);
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        loadProfileFromAPI(); // ← Новый метод
        setupButtonListeners();
    }

    private void loadProfileFromAPI() {
        new Thread(() -> {
            try {
                String token = sharedPreferences.getString("token", "");
                if (token.isEmpty()) {
                    runOnUiThread(() -> Toast.makeText(this, "Токен не найден", Toast.LENGTH_SHORT).show());
                    return;
                }

                URL url = new URL("https://akkubatt-work.ru/mobile/profile/me");
                HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-API-Key", "o4bAvyn0ZXaFeLDr89lk");
                connection.setRequestProperty("Authorization", "Bearer " + token);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();

                    JSONObject jsonResponse = new JSONObject(response.toString());
                    boolean success = jsonResponse.getBoolean("success");

                    if (success) {
                        String name = jsonResponse.getString("name");
                        String phoneNumber = jsonResponse.getString("phone_number");
                        boolean banned = jsonResponse.getBoolean("banned");

                        if (banned) {
                            runOnUiThread(() -> {
                                Toast.makeText(this, "Аккаунт заблокирован", Toast.LENGTH_SHORT).show();
                                Intent intent = new Intent(UserProfileActivityUpdate.this, SplashScreenActivity.class);
                                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                finish();
                            });
                        } else {
                            runOnUiThread(() -> {
                                textViewName.setText(name);
                                textViewNumber.setText(formatPhoneNumber(phoneNumber));
                            });
                        }
                    } else {
                        runOnUiThread(() -> Toast.makeText(this, "Ошибка получения профиля", Toast.LENGTH_SHORT).show());
                    }
                } else {
                    runOnUiThread(() -> Toast.makeText(this, "Ошибка: " + responseCode, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Ошибка подключения", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private String formatPhoneNumber(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return "Номер не указан";
        }

        if (phoneNumber.startsWith("7") || phoneNumber.startsWith("8")) {
            phoneNumber = "+7" + phoneNumber.substring(1);
        }

        if (phoneNumber.startsWith("+7") && phoneNumber.length() == 12) {
            return String.format(
                    "%s (%s) %s-%s-%s",
                    phoneNumber.substring(0, 2),
                    phoneNumber.substring(2, 5),
                    phoneNumber.substring(5, 8),
                    phoneNumber.substring(8, 10),
                    phoneNumber.substring(10, 12)
            );
        }

        return phoneNumber;
    }

    private void setupButtonListeners() {
        String phoneNumber = sharedPreferences.getString("phone_number", "Номер не указан");
        String dbPhoneNumber = phoneNumber.replace("+7", "7")
                .replace(" ", "")
                .replace("(", "")
                .replace(")", "")
                .replace("-", "");

        findViewById(R.id.backButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup);
        });

        findViewById(R.id.myCardsButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, MyCardsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        findViewById(R.id.myPassportButton).setOnClickListener(v -> {
            Intent intent = new Intent(this, PassportActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        findViewById(R.id.changePasswordButton).setOnClickListener(v -> {
            String currentPassword = sharedPreferences.getString("password", "");
            ChangePasswordDialog dialog = new ChangePasswordDialog(
                    UserProfileActivityUpdate.this,
                    newPassword -> updatePasswordInDatabase(dbPhoneNumber, newPassword),
                    currentPassword
            );
            dialog.show();
        });

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();

            Intent intent = new Intent(UserProfileActivityUpdate.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, R.anim.slide_out_left_login);
            finish();
        });
    }

    private void updatePasswordInDatabase(String phoneNumber, String newPassword) {
        new Thread(() -> {
            try {
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";

                String hashedPassword = org.mindrot.jbcrypt.BCrypt.hashpw(newPassword, org.mindrot.jbcrypt.BCrypt.gensalt());

                java.sql.Connection connection = java.sql.DriverManager.getConnection(url, user, password);
                String sql = "UPDATE users SET password = ? WHERE phone_number = ?";
                java.sql.PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, hashedPassword);
                statement.setString(2, phoneNumber);
                int rowsAffected = statement.executeUpdate();

                runOnUiThread(() -> {
                    if (rowsAffected > 0) {
                        Toast.makeText(UserProfileActivityUpdate.this, "Пароль успешно изменён", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(UserProfileActivityUpdate.this, "Ошибка при изменении пароля", Toast.LENGTH_SHORT).show();
                    }
                });

                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(UserProfileActivityUpdate.this, "Ошибка подключения к базе данных", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}