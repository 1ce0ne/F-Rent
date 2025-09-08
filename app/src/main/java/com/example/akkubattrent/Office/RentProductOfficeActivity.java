package com.example.akkubattrent.Office;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

public class RentProductOfficeActivity extends AppCompatActivity {

    private ImageView productImage;
    private TextView productName;
    private TextView productDescription;
    private TextView productDescriptionHorizontal;
    private TextView productPrice;
    private Button rentProductButton;
    private Button closePostamatButton;
    private Button hourButton;
    private Button dayButton;
    private Button monthButton;
    private Button reserveProductButton;
    private String officeAddress;
    private int userId;
    private int productId;
    private int officeId;
    private String userPhone;
    private double productPriceHour;
    private double productPriceDay;
    private double productPriceMonth;
    public int selectedPeriod = 0; // Сделал public для доступа из CreateOrderActivity
    private int selectedTime = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rent_product_office);

        productImage = findViewById(R.id.productImage);
        productName = findViewById(R.id.productName);
        productPrice = findViewById(R.id.productPrice);
        productPrice.setVisibility(View.GONE);
        productDescription = findViewById(R.id.productDescription);
        productDescriptionHorizontal = findViewById(R.id.productDescriptionHorizontal);
        rentProductButton = findViewById(R.id.rentProduct);
        closePostamatButton = findViewById(R.id.closePostamatButton);
        hourButton = findViewById(R.id.hourButton);
        dayButton = findViewById(R.id.dayButton);
        monthButton = findViewById(R.id.monthButton);
        reserveProductButton = findViewById(R.id.reserveProduct);

        rentProductButton.setEnabled(false);

        Intent intent = getIntent();
        String name = intent.getStringExtra("productName");
        String description = intent.getStringExtra("productDescription");
        productPriceHour = intent.getDoubleExtra("productPriceHour", 0.0);
        productPriceDay = intent.getDoubleExtra("productPriceDay", 0.0);
        productPriceMonth = intent.getDoubleExtra("productPriceMonth", 0.0);
        byte[] imageBytes = intent.getByteArrayExtra("productImage");
        officeAddress = intent.getStringExtra("officeAddress");
        productId = intent.getIntExtra("productId", -1);
        officeId = intent.getIntExtra("officeId", -1);

        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("id", -1);
        userPhone = formatPhoneNumber(sharedPreferences.getString("phone_number", ""));

        productName.setText(name);

        // Очищаем productDescriptionHorizontal
        productDescriptionHorizontal.setText("");

        // Разделение описания на две части - первые 50 символов в productDescription
        String shortDescription;
        String longDescription;

        if (description != null) {
            if (description.length() > 50) {
                shortDescription = description.substring(0, 50);
                longDescription = description.substring(50);
            } else {
                shortDescription = description;
                longDescription = "";
            }
        } else {
            shortDescription = "";
            longDescription = "";
        }

        productDescription.setText(shortDescription);
        productDescriptionHorizontal.setText(longDescription);

        productPrice.setText("Выберите период аренды");

        if (imageBytes != null && imageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            productImage.setImageBitmap(bitmap);
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }

        hourButton.setText(String.format("1 час\n%.0f ₽", productPriceHour));
        dayButton.setText(String.format("1 день\n%.0f ₽", productPriceDay));
        monthButton.setText(String.format("1 месяц\n%.0f ₽", productPriceMonth));

        hourButton.setOnClickListener(v -> {
            selectedPeriod = 1;
            hourButton.setSelected(true);
            dayButton.setSelected(false);
            monthButton.setSelected(false);
            updatePriceText();
            rentProductButton.setEnabled(true);
        });

        dayButton.setOnClickListener(v -> {
            selectedPeriod = 2;
            hourButton.setSelected(false);
            dayButton.setSelected(true);
            monthButton.setSelected(false);
            updatePriceText();
            rentProductButton.setEnabled(true);
        });

        monthButton.setOnClickListener(v -> {
            selectedPeriod = 3;
            hourButton.setSelected(false);
            dayButton.setSelected(false);
            monthButton.setSelected(true);
            updatePriceText();
            rentProductButton.setEnabled(true);
        });

        rentProductButton.setOnClickListener(v -> {
            if (selectedPeriod == 0) {
                Toast.makeText(RentProductOfficeActivity.this, "Выберите период аренды", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userId != -1 && productId != -1 && officeId != -1) {
                new CheckReservationTask().execute();
            } else {
                Toast.makeText(RentProductOfficeActivity.this,
                        "Ошибка: userId = " + userId +
                                ", productId = " + productId +
                                ", officeId = " + officeId,
                        Toast.LENGTH_LONG).show();
            }
        });

        reserveProductButton.setOnClickListener(v -> {
            if (userId != -1 && productId != -1) {
                new ReserveProductTask().execute();
            } else {
                Toast.makeText(RentProductOfficeActivity.this,
                        "Ошибка: userId = " + userId +
                                ", productId = " + productId,
                        Toast.LENGTH_LONG).show();
            }
        });

        closePostamatButton.setOnClickListener(v -> finish());
    }

    private String formatPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return "";
        // Удаляем все нецифровые символы
        String digits = phone.replaceAll("[^0-9]", "");
        // Если номер начинается с 8, заменяем на 7
        if (digits.startsWith("8") && digits.length() == 11) {
            digits = "7" + digits.substring(1);
        }
        // Если есть + в начале, просто удаляем его
        if (digits.startsWith("7") && digits.length() == 11) {
            return digits;
        }
        return digits;
    }

    private void updatePriceText() {
        String priceText;
        switch (selectedPeriod) {
            case 1:
                priceText = String.format("Цена: %.0f ₽ в час", productPriceHour);
                break;
            case 2:
                priceText = String.format("Цена: %.0f ₽ в день", productPriceDay);
                break;
            case 3:
                priceText = String.format("Цена: %.0f ₽ в месяц", productPriceMonth);
                break;
            default:
                priceText = "Выберите период аренды";
        }
        productPrice.setText(priceText);
    }

    private class CheckReservationTask extends AsyncTask<Void, Void, Boolean> {
        private String reservedBy;

        @Override
        protected Boolean doInBackground(Void... voids) {
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

                // Проверяем, забронирован ли товар
                String query = "SELECT who_is_reserved FROM products WHERE id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, productId);
                resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    reservedBy = resultSet.getString("who_is_reserved");
                    // Если товар не забронирован или забронирован текущим пользователем
                    return reservedBy == null || reservedBy.equals(userPhone);
                }

                return false;

            } catch (Exception e) {
                Log.e("CheckReservationTask", "Ошибка: " + e.getMessage(), e);
                return false;
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    Log.e("CheckReservationTask", "Ошибка при закрытии соединения: " + e.getMessage(), e);
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean canRent) {
            if (canRent) {
                new CheckUserCredentialsTask().execute();
            } else {
                Toast.makeText(RentProductOfficeActivity.this, "Товар забронирован", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class ReserveProductTask extends AsyncTask<Void, Void, Boolean> {
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

                // Получаем текущее время
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Yekaterinburg"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Yekaterinburg"));
                String reservationTime = dateFormat.format(calendar.getTime());

                // Обновляем запись товара
                String updateQuery = "UPDATE products SET who_is_reserved = ?, start_of_reservation = ? WHERE id = ?";
                preparedStatement = connection.prepareStatement(updateQuery);
                preparedStatement.setString(1, userPhone);
                preparedStatement.setString(2, reservationTime);
                preparedStatement.setInt(3, productId);

                int rowsUpdated = preparedStatement.executeUpdate();

                return rowsUpdated > 0;

            } catch (Exception e) {
                Log.e("ReserveProductTask", "Ошибка: " + e.getMessage(), e);
                return false;
            } finally {
                try {
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    Log.e("ReserveProductTask", "Ошибка при закрытии соединения: " + e.getMessage(), e);
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(RentProductOfficeActivity.this, "Товар успешно забронирован на 30 минут", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(RentProductOfficeActivity.this, "Ошибка при бронировании товара", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class CheckUserCredentialsTask extends AsyncTask<Void, Void, Integer> {
        private static final int HAS_BOTH = 0;
        private static final int NO_CARD = 1;
        private static final int NO_PASSPORT = 2;
        private static final int NO_BOTH = 3;

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

                String query = "SELECT have_a_passport, card_token FROM users WHERE id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, userId);
                resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    String passport = resultSet.getString("have_a_passport");
                    String card_token = resultSet.getString("card_token");

                    boolean hasCard = card_token != null && !card_token.isEmpty();
                    boolean hasPassport = passport != null && !passport.isEmpty();

                    if (hasCard && hasPassport) {
                        return HAS_BOTH;
                    } else if (!hasCard && hasPassport) {
                        return NO_CARD;
                    } else if (hasCard && !hasPassport) {
                        return NO_PASSPORT;
                    } else {
                        return NO_BOTH;
                    }
                }

                return NO_BOTH;

            } catch (Exception e) {
                Log.e("CheckUserCredentialsTask", "Ошибка: " + e.getMessage(), e);
                return NO_BOTH;
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    Log.e("CheckUserCredentialsTask", "Ошибка при закрытии соединения: " + e.getMessage(), e);
                }
            }
        }

        @Override
        protected void onPostExecute(Integer result) {
            switch (result) {
                case HAS_BOTH:
                    // --- ИЗМЕНЕНИЕ ЗДЕСЬ ---
                    // Вместо вызова RentProductTask, запускаем новое окно CreateOrderActivity
                    Intent createOrderIntent = new Intent(RentProductOfficeActivity.this, CreateOrderActivity.class);

                    // Передаем данные в новое окно
                    createOrderIntent.putExtra("userId", userId);
                    createOrderIntent.putExtra("productId", productId);
                    createOrderIntent.putExtra("officeId", officeId);
                    createOrderIntent.putExtra("officeAddress", officeAddress); // Добавлено

                    // Данные о продукте для отображения
                    createOrderIntent.putExtra("productName", productName.getText().toString());
                    // Формируем строку тарифа
                    String tariffText;
                    switch (selectedPeriod) {
                        case 1:
                            tariffText = "1 час";
                            break;
                        case 2:
                            tariffText = "1 день";
                            break;
                        case 3:
                            tariffText = "1 месяц";
                            break;
                        default:
                            tariffText = "Не выбран";
                    }
                    // Вычисляем endTimeRent
                    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Yekaterinburg"));
                    switch (selectedPeriod) {
                        case 1:
                            calendar.add(Calendar.HOUR, selectedTime);
                            break;
                        case 2:
                            calendar.add(Calendar.DAY_OF_YEAR, selectedTime);
                            break;
                        case 3:
                            calendar.add(Calendar.MONTH, selectedTime);
                            break;
                    }
                    calendar.add(Calendar.MINUTE, 5); // Добавляем 5 минут буфера
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                    dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Yekaterinburg"));
                    String endTime = "Конец: " + dateFormat.format(calendar.getTime());

                    createOrderIntent.putExtra("remainderTimeRent", "Тариф: " + tariffText);
                    createOrderIntent.putExtra("endTimeRent", endTime);

                    // Передаем изображение
                    createOrderIntent.putExtra("productImage", getIntent().getByteArrayExtra("productImage")); // Передаем как есть

                    // Цены и период
                    createOrderIntent.putExtra("productPriceHour", productPriceHour);
                    createOrderIntent.putExtra("productPriceDay", productPriceDay);
                    createOrderIntent.putExtra("productPriceMonth", productPriceMonth);
                    createOrderIntent.putExtra("selectedPeriod", selectedPeriod); // Передаем selectedPeriod

                    startActivity(createOrderIntent);
                    setResult(RESULT_OK);
                    finish();
                    // --- КОНЕЦ ИЗМЕНЕНИЯ ---
                    break;
                case NO_CARD:
                    Toast.makeText(RentProductOfficeActivity.this, "Добавьте карту для продолжения аренды", Toast.LENGTH_LONG).show();
                    break;
                case NO_PASSPORT:
                    Toast.makeText(RentProductOfficeActivity.this, "Добавьте паспорт для продолжения аренды", Toast.LENGTH_LONG).show();
                    break;
                case NO_BOTH:
                    Toast.makeText(RentProductOfficeActivity.this, "Добавьте карту и паспорт для продолжения аренды", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}