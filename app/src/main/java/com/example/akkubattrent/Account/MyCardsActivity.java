package com.example.akkubattrent.Account;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;
import com.example.akkubattrent.UserProfile.MyAccountActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MyCardsActivity extends AppCompatActivity {

    private int userId;
    private Connection dbConnection;
    private static final String API_KEY = "o4bAvyn0ZXaFeLDr89lk"; // Ваш API ключ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_cards);

        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("id", -1);

        new Thread(() -> {
            try {
                dbConnection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );
                runOnUiThread(this::checkUserCard);
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка подключения к БД", Toast.LENGTH_LONG).show()
                );
            }
        }).start();

        FrameLayout addNewCardButton = findViewById(R.id.addNewCardButton);
        addNewCardButton.setOnClickListener(v -> generateSecureLink(userId, "bind_card"));

        Button backToProfileButton = findViewById(R.id.backToProfileButton);
        backToProfileButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left_signup, R.anim.slide_out_right_signup); // ← Новое слева, текущее вправо
        });
    }

    private void checkUserCard() {
        new Thread(() -> {
            try {
                String query = "SELECT card_token, last4_digits FROM users WHERE id = ?";
                PreparedStatement stmt = dbConnection.prepareStatement(query);
                stmt.setInt(1, userId);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String cardToken = rs.getString("card_token");
                    String last4Digits = rs.getString("last4_digits");

                    if (cardToken != null && last4Digits != null) {
                        runOnUiThread(() -> showCardView(last4Digits));
                    } else {
                        runOnUiThread(() ->
                                findViewById(R.id.addNewCardButton).setVisibility(View.VISIBLE)
                        );
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Ошибка проверки карты", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void showCardView(String last4Digits) {
        findViewById(R.id.addNewCardButton).setVisibility(View.GONE);
        androidx.cardview.widget.CardView cardView = findViewById(R.id.cardView);
        cardView.setVisibility(View.VISIBLE);

        TextView cardNumberView = cardView.findViewById(R.id.cardNumber);
        cardNumberView.setText("**** **** **** " + last4Digits);

        cardView.findViewById(R.id.textExpiryDate).setVisibility(View.GONE);
        cardView.findViewById(R.id.expiryDate).setVisibility(View.GONE);

        ImageView deleteBtn = cardView.findViewById(R.id.deleteCardButton);
        deleteBtn.setOnClickListener(v -> deleteCardViaLink(String.valueOf(userId)));
    }

    private void generateSecureLink(int userId, String linkType) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Генерация безопасной ссылки...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                String apiUrl = "https://akkubatt-rent.ru/card-binding-form?user_id=" + userId;

                URL url = new URL(apiUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("X-API-Key", API_KEY);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Получаем HTML-контент
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder htmlContent = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        htmlContent.append(line).append("\n");
                    }

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        if (linkType.equals("bind_card")) {
                            // Открываем HTML в AddNewCardActivity
                            Intent intent = new Intent(MyCardsActivity.this, AddNewCardActivity.class);
                            startActivity(intent);
                            overridePendingTransition(R.anim.slide_in_right_login, R.anim.slide_out_left_login);
                            finish();
                            intent.putExtra("html_content", htmlContent.toString());
                            startActivity(intent);
                        } else if (linkType.equals("delete_card")) {
                            // Для удаления карты (если нужно)
                            // Здесь можно обработать HTML или изменить логику
                            Toast.makeText(this, "Функция удаления карты", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    private void deleteCardViaLink(String userId) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Удаление карты...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        new Thread(() -> {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            try {
                String deleteUrl = "https://akkubatt-rent.ru/api/delete-card?user_id=" + userId;

                URL url = new URL(deleteUrl);
                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("DELETE");
                connection.setRequestProperty("X-API-Key", API_KEY); // Добавляем API ключ в заголовок
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        try {
                            JSONObject jsonResponse = new JSONObject(response.toString());
                            if (jsonResponse.has("success")) {
                                boolean success = jsonResponse.getBoolean("success");
                                if (success) {
                                    Toast.makeText(this, "Карта успешно удалена", Toast.LENGTH_SHORT).show();
                                    findViewById(R.id.cardView).setVisibility(View.GONE);
                                    findViewById(R.id.addNewCardButton).setVisibility(View.VISIBLE);
                                } else {
                                    Toast.makeText(this, "Ошибка при удалении карты", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "Карта удалена", Toast.LENGTH_SHORT).show();
                                findViewById(R.id.cardView).setVisibility(View.GONE);
                                findViewById(R.id.addNewCardButton).setVisibility(View.VISIBLE);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(this, "Ошибка обработки ответа", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(this, "Ошибка сервера: " + responseCode, Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbConnection != null) {
            try {
                dbConnection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}