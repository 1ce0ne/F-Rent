package com.example.akkubattrent.Office; // Убедитесь, что пакет правильный

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class CreateOrderActivity extends AppCompatActivity {

    private static final String API_KEY = "o4bAvyn0ZXaFeLDr89lk";
    private static final String BASE_URL = "https://akkubatt-rent.ru/api/";

    private ImageView productImage;
    private TextView productName;
    private TextView remainderTimeRent;
    private TextView endTimeRent;
    private TextView statusOfRent;
    private ImageView statusImage;
    private View circleBackgroundView; // Предполагается, что это View с фоном order_circle

    private String productNameStr;
    private String remainderTimeRentStr;
    private String endTimeRentStr;
    private byte[] productImageBytes;
    private int userId;
    private int productId;
    private int officeId;
    private String officeAddress; // Добавлено
    private double productPriceHour;
    private double productPriceDay;
    private double productPriceMonth;
    private int selectedPeriod;
    private int selectedTime = 1;

    private Handler handler = new Handler();
    private static final int DELAY_MILLIS = 3000; // 3 seconds delay

    // Состояния для круга
    private static final int STATE_INITIAL = 0;
    private static final int STATE_CREATED_OK = 1;
    private static final int STATE_CREATED_ERROR = 2;
    private static final int STATE_PAID_OK = 3;
    private static final int STATE_PAID_ERROR = 4;
    private static final int STATE_ISSUED_OK = 5;
    private static final int STATE_ISSUED_ERROR = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_order_window);

        initViews();
        getDataFromIntent();
        setDataToViews();
        startOrderCreationProcess();
    }

    private void initViews() {
        productImage = findViewById(R.id.productImage);
        productName = findViewById(R.id.productName);
        remainderTimeRent = findViewById(R.id.remainderTimeRent);
        endTimeRent = findViewById(R.id.endTimeRent);
        statusOfRent = findViewById(R.id.statusOfRent);
        statusImage = findViewById(R.id.statusImage);
        circleBackgroundView = findViewById(R.id.circleBackgroundView); // ID из вашего layout

        // Установить начальное изображение круга
        updateCircleState(STATE_INITIAL);
    }

    private void getDataFromIntent() {
        Intent intent = getIntent();
        productNameStr = intent.getStringExtra("productName");
        remainderTimeRentStr = intent.getStringExtra("remainderTimeRent");
        endTimeRentStr = intent.getStringExtra("endTimeRent");
        productImageBytes = intent.getByteArrayExtra("productImage");

        userId = intent.getIntExtra("userId", -1);
        productId = intent.getIntExtra("productId", -1);
        officeId = intent.getIntExtra("officeId", -1);
        officeAddress = intent.getStringExtra("officeAddress"); // Добавлено
        productPriceHour = intent.getDoubleExtra("productPriceHour", 0.0);
        productPriceDay = intent.getDoubleExtra("productPriceDay", 0.0);
        productPriceMonth = intent.getDoubleExtra("productPriceMonth", 0.0);
        selectedPeriod = intent.getIntExtra("selectedPeriod", 1); // Default to 1 hour if not passed
    }

    private void setDataToViews() {
        productName.setText(productNameStr);
        remainderTimeRent.setText(remainderTimeRentStr);
        endTimeRent.setText(endTimeRentStr);

        if (productImageBytes != null && productImageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(productImageBytes, 0, productImageBytes.length);
            productImage.setImageBitmap(bitmap);
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }
    }

    private void startOrderCreationProcess() {
        statusOfRent.setText("Создание заказа");
        statusImage.setImageResource(R.drawable.creating_order); // Предполагается наличие этого PNG
        updateCircleState(STATE_INITIAL); // Убедиться, что начальное состояние установлено

        // Wait 3 seconds before creating order
        handler.postDelayed(this::createOrder, DELAY_MILLIS);
    }

    private void createOrder() {
        new CreateOrderTask().execute();
    }

    private class CreateOrderTask extends AsyncTask<Void, Void, Long> {
        private String startTime;
        private String endTime;
        private double rentalAmount;

        @Override
        protected Long doInBackground(Void... voids) {
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet generatedKeys = null;
            long newOrderId = -1;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                if (userId == -1 || productId == -1 || officeId == -1) {
                    Log.e("CreateOrderTask", "Ошибка: userId, productId, officeId не заданы");
                    return -1L;
                }

                // Get current time
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Yekaterinburg"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Yekaterinburg"));
                this.startTime = dateFormat.format(calendar.getTime());

                // Calculate end time based on selected period
                switch (selectedPeriod) {
                    case 1: // Hour
                        calendar.add(Calendar.HOUR, selectedTime);
                        break;
                    case 2: // Day
                        calendar.add(Calendar.DAY_OF_YEAR, selectedTime);
                        break;
                    case 3: // Month
                        calendar.add(Calendar.MONTH, selectedTime);
                        break;
                }
                calendar.add(Calendar.MINUTE, 5); // Add 5 minutes buffer
                this.endTime = dateFormat.format(calendar.getTime());

                // Calculate rental time in hours
                int rentalTimeHours;
                switch (selectedPeriod) {
                    case 1:
                        rentalTimeHours = selectedTime;
                        break;
                    case 2:
                        rentalTimeHours = selectedTime * 24;
                        break;
                    case 3:
                        rentalTimeHours = selectedTime * 24 * 30;
                        break;
                    default:
                        rentalTimeHours = selectedTime;
                }

                // Calculate rental amount
                switch (selectedPeriod) {
                    case 1:
                        this.rentalAmount = productPriceHour;
                        break;
                    case 2:
                        this.rentalAmount = productPriceDay;
                        break;
                    case 3:
                        this.rentalAmount = productPriceMonth;
                        break;
                    default:
                        this.rentalAmount = 0.0;
                }

                // Clear product reservation
                String clearReservationQuery = "UPDATE products SET who_is_reserved = NULL, start_of_reservation = NULL WHERE id = ?";
                preparedStatement = connection.prepareStatement(clearReservationQuery);
                preparedStatement.setInt(1, productId);
                preparedStatement.executeUpdate();

                // Create order
                String insertOrderQuery = "INSERT INTO office_orders " +
                        "(product_id, client_id, start_time, end_time, rental_time, " +
                        "renewal_time, rental_amount, returned, ready_for_return, issued, not_issued, accepted, address_office, is_paid) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                preparedStatement = connection.prepareStatement(insertOrderQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                preparedStatement.setInt(1, productId);
                preparedStatement.setInt(2, userId);
                preparedStatement.setString(3, startTime);
                preparedStatement.setString(4, endTime);
                preparedStatement.setInt(5, rentalTimeHours);
                preparedStatement.setNull(6, java.sql.Types.VARCHAR); // renewal_time
                preparedStatement.setDouble(7, rentalAmount);
                preparedStatement.setInt(8, 0); // returned = false
                preparedStatement.setInt(9, 0); // ready_for_return = false
                preparedStatement.setInt(10, 0); // issued = false
                preparedStatement.setInt(11, 0); // not_issued = false
                preparedStatement.setInt(12, 0); // accepted = false
                preparedStatement.setString(13, officeAddress); // Используем переданный адрес
                preparedStatement.setBoolean(14, false); // is_paid = false

                int rowsInserted = preparedStatement.executeUpdate();
                if (rowsInserted > 0) {
                    generatedKeys = preparedStatement.getGeneratedKeys();
                    if (generatedKeys.next()) {
                        newOrderId = generatedKeys.getLong(1);
                    }
                }
                return newOrderId;
            } catch (Exception e) {
                Log.e("CreateOrderTask", "Ошибка: " + e.getMessage(), e);
                return -1L;
            } finally {
                try {
                    if (generatedKeys != null) generatedKeys.close();
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    Log.e("CreateOrderTask", "Ошибка при закрытии соединения: " + e.getMessage(), e);
                }
            }
        }

        @Override
        protected void onPostExecute(Long orderId) {
            if (orderId != -1) {
                // Update UI for successful order creation
                statusOfRent.setText("Заказ создан");
                updateCircleState(STATE_CREATED_OK); // Дуга 1 желтая

                // Wait 3 seconds before starting payment
                handler.postDelayed(() -> startPayment(orderId), DELAY_MILLIS);
            } else {
                // Update UI for failed order creation
                statusOfRent.setText("Ошибка создания заказа");
                updateCircleState(STATE_CREATED_ERROR); // Дуга 1 красная
                // Exit after 6-7 seconds
                handler.postDelayed(() -> {
                    Intent intent = new Intent(CreateOrderActivity.this, MainMenuActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }, 6500); // Average of 6-7 seconds
            }
        }
    }

    private void startPayment(long orderId) {
        statusImage.setImageResource(R.drawable.credit_card); // Предполагается наличие этого PNG
        statusOfRent.setText("Оплата аренды");
        // Дуга 1 уже желтая, дуги 2 и 3 серые (STATE_CREATED_OK)

        // Process payment
        processPayment(orderId);
    }

    private void processPayment(long orderId) {
        RequestQueue queue = Volley.newRequestQueue(this);

        // Get start time for the order (можно оптимизировать, если оно уже есть)
        new GetOrderStartTimeTask(orderId).execute();
    }

    private class GetOrderStartTimeTask extends AsyncTask<Void, Void, String> {
        private long orderId;

        GetOrderStartTimeTask(long orderId) {
            this.orderId = orderId;
        }

        @Override
        protected String doInBackground(Void... voids) {
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );
                String query = "SELECT start_time FROM office_orders WHERE id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setLong(1, orderId);
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    return resultSet.getString("start_time");
                }
            } catch (Exception e) {
                Log.e("GetOrderStartTime", "Ошибка: " + e.getMessage(), e);
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    Log.e("GetOrderStartTime", "Ошибка при закрытии соединения: " + e.getMessage(), e);
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String startTime) {
            if (startTime != null) {
                String url = BASE_URL + "charge-order?user_id=" + userId + "&order_id=" + orderId + "&order_type=office";
                JsonObjectRequest request = new JsonObjectRequest(
                        Request.Method.POST,
                        url,
                        null,
                        response -> {
                            // Payment successful
                            statusOfRent.setText("Оплата прошла успешно");
                            updateCircleState(STATE_PAID_OK); // Дуги 1 и 2 желтые

                            // Wait 3 seconds before checking product issue status
                            handler.postDelayed(() -> checkProductIssueStatus(orderId, startTime), DELAY_MILLIS);
                        },
                        error -> {
                            // Payment failed
                            statusOfRent.setText("Ошибка оплаты");
                            updateCircleState(STATE_PAID_ERROR); // Дуга 1 желтая, дуга 2 красная

                            String errorMsg = "Ошибка оплаты";
                            try {
                                if (error.networkResponse != null && error.networkResponse.data != null) {
                                    String responseBody = new String(error.networkResponse.data);
                                    JSONObject errorResponse = new JSONObject(responseBody);
                                    errorMsg = errorResponse.optString("message", errorMsg);
                                }
                            } catch (JSONException e) {
                                Log.e("PaymentError", "Error parsing error response", e);
                            }

                            Toast.makeText(CreateOrderActivity.this, errorMsg, Toast.LENGTH_LONG).show();

                            // Update order status in database
                            new UpdateOrderStatusTask(userId, productId, startTime).execute();

                            // Exit after 6-7 seconds
                            handler.postDelayed(() -> {
                                Intent intent = new Intent(CreateOrderActivity.this, MainMenuActivity.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(intent);
                                finish();
                            }, 6500);
                        }) {
                    @Override
                    public Map<String, String> getHeaders() {
                        Map<String, String> headers = new HashMap<>();
                        headers.put("X-API-Key", API_KEY);
                        return headers;
                    }
                };
                RequestQueue queue = Volley.newRequestQueue(CreateOrderActivity.this);
                queue.add(request);
            } else {
                statusOfRent.setText("Ошибка получения данных заказа");
                // Exit after 6-7 seconds
                handler.postDelayed(() -> {
                    Intent intent = new Intent(CreateOrderActivity.this, MainMenuActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                }, 6500);
            }
        }
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

    private void checkProductIssueStatus(long orderId, String startTime) {
        statusImage.setImageResource(R.drawable.order_issued); // Предполагается наличие этого PNG
        statusOfRent.setText("Выдача товара");
        // Дуги 1 и 2 желтые, дуга 3 серая (STATE_PAID_OK)

        // Start checking issue status
        handler.postDelayed(new CheckIssueStatusRunnable(orderId, startTime), DELAY_MILLIS);
    }

    private class CheckIssueStatusRunnable implements Runnable {
        private long orderId;
        private String startTime;

        CheckIssueStatusRunnable(long orderId, String startTime) {
            this.orderId = orderId;
            this.startTime = startTime;
        }

        @Override
        public void run() {
            new CheckOrderStatusTask(orderId, startTime).execute();
        }
    }

    private class CheckOrderStatusTask extends AsyncTask<Void, Void, Integer> {
        private static final int STATUS_ISSUED = 1;
        private static final int STATUS_NOT_ISSUED = 2;
        private static final int STATUS_PENDING = 3;
        private static final int STATUS_ERROR = 4;

        private long orderId;
        private String startTime;

        CheckOrderStatusTask(long orderId, String startTime) {
            this.orderId = orderId;
            this.startTime = startTime;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            Connection connection = null;
            PreparedStatement preparedStatement = null;
            ResultSet resultSet = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );
                String query = "SELECT issued, not_issued FROM office_orders " +
                        "WHERE id = ? AND start_time = ? " +
                        "LIMIT 1";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setLong(1, orderId);
                preparedStatement.setString(2, startTime);
                resultSet = preparedStatement.executeQuery();
                if (resultSet.next()) {
                    int issued = resultSet.getInt("issued");
                    int notIssued = resultSet.getInt("not_issued");
                    if (issued == 1) {
                        return STATUS_ISSUED;
                    } else if (notIssued == 1) {
                        return STATUS_NOT_ISSUED;
                    } else {
                        return STATUS_PENDING;
                    }
                }
                return STATUS_ERROR;
            } catch (Exception e) {
                e.printStackTrace();
                return STATUS_ERROR;
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Integer status) {
            switch (status) {
                case STATUS_ISSUED:
                    statusOfRent.setText("Товар выдан");
                    updateCircleState(STATE_ISSUED_OK); // Все три дуги желтые
                    // Exit after 6-7 seconds
                    handler.postDelayed(() -> {
                        Intent intent = new Intent(CreateOrderActivity.this, MainMenuActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }, 6500);
                    break;
                case STATUS_NOT_ISSUED:
                    statusOfRent.setText("Отказано в выдаче");
                    updateCircleState(STATE_ISSUED_ERROR); // Дуги 1 и 2 желтые, дуга 3 красная
                    // Exit after 6-7 seconds
                    handler.postDelayed(() -> {
                        Intent intent = new Intent(CreateOrderActivity.this, MainMenuActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }, 6500);
                    break;
                case STATUS_PENDING:
                    // Continue checking
                    handler.postDelayed(new CheckIssueStatusRunnable(orderId, startTime), DELAY_MILLIS);
                    break;
                case STATUS_ERROR:
                    statusOfRent.setText("Ошибка при проверке статуса");
                    // Exit after 6-7 seconds
                    handler.postDelayed(() -> {
                        Intent intent = new Intent(CreateOrderActivity.this, MainMenuActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    }, 6500);
                    break;
            }
        }
    }

    // Метод для обновления изображения круга
    private void updateCircleState(int state) {
        if (circleBackgroundView == null) return; // На случай, если View не найдена

        int drawableResId = R.drawable.order_circle_initial; // По умолчанию

        switch (state) {
            case STATE_INITIAL:
                drawableResId = R.drawable.order_circle_initial;
                break;
            case STATE_CREATED_OK:
                drawableResId = R.drawable.order_circle_created_ok;
                break;
            case STATE_CREATED_ERROR:
                drawableResId = R.drawable.order_circle_created_error;
                break;
            case STATE_PAID_OK:
                drawableResId = R.drawable.order_circle_paid_ok;
                break;
            case STATE_PAID_ERROR:
                drawableResId = R.drawable.order_circle_paid_error;
                break;
            case STATE_ISSUED_OK:
                drawableResId = R.drawable.order_circle_issued_ok;
                break;
            case STATE_ISSUED_ERROR:
                drawableResId = R.drawable.order_circle_issued_error;
                break;
            default:
                drawableResId = R.drawable.order_circle_initial;
                break;
        }

        // Предполагается, что circleBackgroundView - это View (например, FrameLayout, RelativeLayout)
        // и у него устанавливается фон через setBackgroundResource
        circleBackgroundView.setBackgroundResource(drawableResId);

        // Если circleBackgroundView - это ImageView, используйте:
        // ((ImageView) circleBackgroundView).setImageResource(drawableResId);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}