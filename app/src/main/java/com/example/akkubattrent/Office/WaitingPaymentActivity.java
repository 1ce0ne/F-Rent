package com.example.akkubattrent.Office;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.akkubattrent.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

public class WaitingPaymentActivity extends AppCompatActivity {

    private static final String API_KEY = "o4bAvyn0ZXaFeLDr89lk";
    private static final String BASE_URL = "https://akkubatt-rent.ru/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_waiting_payment);

        Intent intent = getIntent();
        if (intent == null) {
            showErrorAndFinish("Ошибка: Intent не найден");
            return;
        }

        processPayment();
    }

    private void processPayment() {
        RequestQueue queue = Volley.newRequestQueue(this);

        int userId = getIntent().getIntExtra("userId", -1);
        long orderId = getIntent().getLongExtra("orderId", -1);
        int productId = getIntent().getIntExtra("productId", -1);
        String startTime = getIntent().getStringExtra("startTime");

        String url = BASE_URL + "charge-order?user_id=" + userId + "&order_id=" + orderId + "&order_type=office";

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                null,
                response -> {
                    // Успешный ответ (200 OK) — переход к выдаче товара
                    proceedToProductIssue();
                },
                error -> {
                    // Обработка ошибки платежа
                    String errorMsg = "Ошибка оплаты";
                    String reasonCode = null;
                    String reason = null;

                    try {
                        if (error.networkResponse != null && error.networkResponse.data != null) {
                            String responseBody = new String(error.networkResponse.data);
                            JSONObject errorResponse = new JSONObject(responseBody);
                            reasonCode = errorResponse.optString("reason_code");
                            reason = errorResponse.optString("reason");
                            errorMsg = errorResponse.optString("message", errorMsg);
                        }
                    } catch (JSONException e) {
                        Log.e("PaymentError", "Error parsing error response", e);
                    }

                    if (error.networkResponse != null) {
                        errorMsg += " (код: " + error.networkResponse.statusCode + ")";
                    }

                    // Обновляем статус заказа в базе данных
                    new UpdateOrderStatusTask(userId, productId, startTime).execute();

                    // Переходим на экран ошибки с передачей кода и сообщения
                    Intent errorIntent = new Intent(WaitingPaymentActivity.this, NotHaveMoneyActivity.class);
                    errorIntent.putExtra("errorMessage", errorMsg);
                    errorIntent.putExtra("reasonCode", reasonCode);
                    errorIntent.putExtra("reason", reason);
                    startActivity(errorIntent);
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

    private class UpdateOrderStatusTask extends AsyncTask<Void, Void, Boolean> {
        private int userId;
        private int productId;
        private String startTime;

        UpdateOrderStatusTask(int userId, int productId, String startTime) {
            this.userId = userId;
            this.productId = productId;
            this.startTime = startTime;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            Connection connection = null;
            PreparedStatement preparedStatement = null;

            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                String query = "UPDATE office_orders SET not_issued = 1, issued = 0, accepted = 1, returned = 1 WHERE product_id = ? AND client_id = ? AND start_time = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, productId);
                preparedStatement.setInt(2, userId);
                preparedStatement.setString(3, startTime);

                int rowsUpdated = preparedStatement.executeUpdate();
                return rowsUpdated > 0;

            } catch (Exception e) {
                Log.e("UpdateOrderStatus", "Ошибка: " + e.getMessage(), e);
                return false;
            } finally {
                try {
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (Exception e) {
                    Log.e("UpdateOrderStatus", "Ошибка при закрытии соединения: " + e.getMessage(), e);
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            Log.d("UpdateOrderStatus", "Статус заказа обновлен: " + (success ? "успешно" : "с ошибкой"));
        }
    }

    private void proceedToProductIssue() {
        try {
            Intent intent = new Intent(this, GiveProductActivity.class);
            intent.putExtra("orderId", getIntent().getLongExtra("orderId", -1));
            intent.putExtra("productId", getIntent().getIntExtra("productId", -1));
            intent.putExtra("startTime", getIntent().getStringExtra("startTime"));
            intent.putExtra("userId", getIntent().getIntExtra("userId", -1));
            intent.putExtra("officeId", getIntent().getIntExtra("officeId", -1));

            startActivity(intent);
            finish();
        } catch (Exception e) {
            Log.e("PaymentError", "Transition error", e);
            showErrorAndFinish("System error");
        }
    }

    private void showErrorAndFinish(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        finish();
    }
}