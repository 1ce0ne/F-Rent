package com.example.akkubattrent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.akkubattrent.UserProfile.FAQActivity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class ProlongOrderActivity extends AppCompatActivity {

    private int orderId;
    private boolean isOfficeOrder;
    private double pricePerHour;
    private double pricePerDay;
    private double pricePerMonth;
    private Timestamp currentEndTime;
    private Timestamp startTime;
    private int selectedPeriod = 1; // 1=hour, 24=day, 720=month (in hours)

    private static final String API_KEY = "o4bAvyn0ZXaFeLDr89lk";
    private static final String BASE_URL = "https://akkubatt-rent.ru/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lease_extension);

        // Получаем данные о заказе
        Intent intent = getIntent();
        orderId = intent.getIntExtra("orderId", -1);
        isOfficeOrder = intent.getBooleanExtra("isOfficeOrder", false);
        pricePerHour = intent.getDoubleExtra("pricePerHour", 0);
        pricePerDay = intent.getDoubleExtra("pricePerDay", 0);
        pricePerMonth = intent.getDoubleExtra("pricePerMonth", 0);
        currentEndTime = (Timestamp) intent.getSerializableExtra("endTime");
        startTime = (Timestamp) intent.getSerializableExtra("startTime");
        String productName = intent.getStringExtra("productName");
        byte[] imageBytes = intent.getByteArrayExtra("imageBytes");

        // Инициализация UI
        ImageView productImage = findViewById(R.id.productImage);
        TextView productNameView = findViewById(R.id.productName);
        TextView remainderTimeRent = findViewById(R.id.remainderTimeRent);
        Button hourButton = findViewById(R.id.oneHour);
        Button dayButton = findViewById(R.id.oneDay);
        Button monthButton = findViewById(R.id.oneMonth);
        Button prolongButton = findViewById(R.id.prolongProduct);
        Button closeButton = findViewById(R.id.closePostamatButton);

        // Устанавливаем изображение товара
        if (imageBytes != null) {
            try {
                productImage.setImageBitmap(BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length));
            } catch (Exception e) {
                productImage.setImageResource(R.drawable.placeholder_image);
            }
        }

        // Устанавливаем название товара
        productNameView.setText(productName);

        // Обновляем оставшееся время
        updateRemainingTime(remainderTimeRent);

        // Устанавливаем цены на кнопки
        hourButton.setText(String.format(Locale.getDefault(), "1 час\n%.0f ₽", pricePerHour));
        dayButton.setText(String.format(Locale.getDefault(), "1 день\n%.0f ₽", pricePerDay));
        monthButton.setText(String.format(Locale.getDefault(), "1 месяц\n%.0f ₽", pricePerMonth));

        // Обработчики нажатий на кнопки тарифов
        hourButton.setOnClickListener(v -> {
            selectedPeriod = 1;
            updateButtonSelection(hourButton, dayButton, monthButton);
        });

        dayButton.setOnClickListener(v -> {
            selectedPeriod = 24;
            updateButtonSelection(hourButton, dayButton, monthButton);
        });

        monthButton.setOnClickListener(v -> {
            selectedPeriod = 720;
            updateButtonSelection(hourButton, dayButton, monthButton);
        });

        // Кнопка продления
        prolongButton.setOnClickListener(v -> prolongOrder());

        // Кнопка закрытия
        closeButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_down_window);
        });

        // Выбираем час по умолчанию
        updateButtonSelection(hourButton, dayButton, monthButton);
    }

    private void updateRemainingTime(TextView view) {
        long remainingMillis = currentEndTime.getTime() - System.currentTimeMillis();
        long hours = remainingMillis / (60 * 60 * 1000);
        long minutes = (remainingMillis % (60 * 60 * 1000)) / (60 * 1000);

        // Форматируем текст в зависимости от количества часов
        String timeText;
        if (hours == 0) {
            timeText = String.format(Locale.getDefault(), "%d минут", minutes);
        } else if (hours == 1) {
            timeText = String.format(Locale.getDefault(), "1 час %d минут", minutes);
        } else {
            timeText = String.format(Locale.getDefault(), "%d часов %d минут", hours, minutes);
        }

        view.setText(String.format("Осталось: %s", timeText));
    }

    private void updateButtonSelection(Button hourBtn, Button dayBtn, Button monthBtn) {
        // Сбрасываем стиль всех кнопок
        hourBtn.setSelected(false);
        dayBtn.setSelected(false);
        monthBtn.setSelected(false);

        // Устанавливаем стиль для выбранной кнопки
        switch (selectedPeriod) {
            case 1:
                hourBtn.setSelected(true);
                break;
            case 24:
                dayBtn.setSelected(true);
                break;
            case 720:
                monthBtn.setSelected(true);
                break;
        }
    }

    private void prolongOrder() {
        int additionalAmount;

        // Выбираем правильную цену в зависимости от выбранного периода
        switch (selectedPeriod) {
            case 1: // час
                additionalAmount = (int) Math.round(pricePerHour);
                break;
            case 24: // день
                additionalAmount = (int) Math.round(pricePerDay);
                break;
            case 720: // месяц (30 дней)
                additionalAmount = (int) Math.round(pricePerMonth);
                break;
            default:
                additionalAmount = 0;
                break;
        }

        // Рассчитываем новое время окончания
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(currentEndTime.getTime());
        calendar.add(Calendar.HOUR, selectedPeriod); // добавляем выбранный период в часах

        Timestamp newEndTime = new Timestamp(calendar.getTimeInMillis());

        // Сначала обрабатываем платеж
        processPayment(additionalAmount, newEndTime);
    }

    private void processPayment(int additionalAmount, Timestamp newEndTime) {
        RequestQueue queue = Volley.newRequestQueue(this);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        int userId = prefs.getInt("user_id", -1);

        String url = BASE_URL + "extend-order?user_id=" + userId + "&order_id=" + orderId + "&amount=" + additionalAmount
                + "&order_type=" + "office";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    // Успешный платеж - обновляем данные в БД
                    new ProlongOrderTask(additionalAmount, newEndTime).execute();
                },
                error -> {
                    // Ошибка платежа - переходим на FAQActivity
                    String errorMsg = "Ошибка оплаты";
                    if (error.networkResponse != null) {
                        errorMsg += " (код: " + error.networkResponse.statusCode + ")";
                    }

                    Intent intent = new Intent(ProlongOrderActivity.this, FAQActivity.class);
                    intent.putExtra("errorMessage", errorMsg);
                    startActivity(intent);
                    finish();
                }) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("X-API-Key", API_KEY);
                return headers;
            }
        };

        queue.add(request);
    }

    private class ProlongOrderTask extends AsyncTask<Void, Void, Boolean> {
        private int additionalAmount;
        private Timestamp newEndTime;

        ProlongOrderTask(int additionalAmount, Timestamp newEndTime) {
            this.additionalAmount = additionalAmount;
            this.newEndTime = newEndTime;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String phoneNumber = prefs.getString("phone_number", null);

            if (phoneNumber == null) {
                return false;
            }

            try (Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                    "akku_batt_admin",
                    "8rssCz31UiUr512e")) {

                String tableName = isOfficeOrder ? "office_orders" : "orders";
                String sql = "UPDATE " + tableName + " SET " +
                        "end_time = ?, " +
                        "rental_amount = rental_amount + ?, " +
                        "renewal_time = IFNULL(renewal_time, 0) + ? " +
                        "WHERE order_id = ?";

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setTimestamp(1, newEndTime);
                    stmt.setInt(2, additionalAmount);
                    stmt.setInt(3, selectedPeriod); // hours to add
                    stmt.setInt(4, orderId);

                    int rowsAffected = stmt.executeUpdate();
                    return rowsAffected > 0;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(ProlongOrderActivity.this, "Аренда успешно продлена", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
                overridePendingTransition(0, R.anim.slide_down_window);
            } else {
                Toast.makeText(ProlongOrderActivity.this, "Ошибка при продлении аренды", Toast.LENGTH_SHORT).show();
            }
        }
    }
}