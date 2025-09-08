package com.example.akkubattrent.Postamat;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
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
import com.example.akkubattrent.Utils.RentAlarmReceiver;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class RentProductActivity extends AppCompatActivity {

    private ImageView productImage;
    private TextView productName;
    private TextView productDescription;
    private TextView productPrice;
    private Button rentProductButton;
    private Button closePostamatButton;
    private Button hourButton;
    private Button dayButton;
    private Button monthButton;

    private int userId;
    private int productId;
    private int postamatId;
    private int cellId;
    private double productPriceHour;
    private double productPriceDay;
    private double productPriceMonth;
    private int selectedPeriod = 0; // 0 - не выбрано, 1 - час, 2 - день, 3 - месяц
    private int selectedTime = 1; // Количество выбранных периодов

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.rent_product);

        productImage = findViewById(R.id.productImage);
        productPrice = findViewById(R.id.productPrice);
        productPrice.setVisibility(View.GONE);
        productName = findViewById(R.id.productName);
        productDescription = findViewById(R.id.productDescription);
        rentProductButton = findViewById(R.id.rentProduct);
        closePostamatButton = findViewById(R.id.closePostamatButton);
        hourButton = findViewById(R.id.hourButton);
        dayButton = findViewById(R.id.dayButton);
        monthButton = findViewById(R.id.monthButton);

        // Сначала делаем кнопку аренды неактивной
        rentProductButton.setEnabled(false);

        Intent intent = getIntent();
        String name = intent.getStringExtra("productName");
        String description = intent.getStringExtra("productDescription");
        productPriceHour = intent.getDoubleExtra("productPriceHour", 0.0);
        productPriceDay = intent.getDoubleExtra("productPriceDay", 0.0);
        productPriceMonth = intent.getDoubleExtra("productPriceMonth", 0.0);
        byte[] imageBytes = intent.getByteArrayExtra("productImage");
        productId = intent.getIntExtra("productId", -1);
        postamatId = intent.getIntExtra("postamatId", -1);
        cellId = intent.getIntExtra("cellId", -1);

        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("id", -1);

        productName.setText(name);
        productDescription.setText(description);
        productPrice.setText("Выберите период аренды");

        if (imageBytes != null && imageBytes.length > 0) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
            productImage.setImageBitmap(bitmap);
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }

        // Установка текста для кнопок периода
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
                Toast.makeText(RentProductActivity.this, "Выберите период аренды", Toast.LENGTH_SHORT).show();
                return;
            }

            if (userId != -1 && productId != -1 && postamatId != -1 && cellId != -1) {
                // Сначала проверяем наличие карты и паспорта у пользователя
                new CheckUserCredentialsTask().execute();
            } else {
                Toast.makeText(RentProductActivity.this,
                        "Ошибка: userId = " + userId +
                                ", productId = " + productId +
                                ", postamatId = " + postamatId +
                                ", cellId = " + cellId,
                        Toast.LENGTH_LONG).show();
            }
        });

        closePostamatButton.setOnClickListener(v -> finish());
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

                String query = "SELECT cards, have_a_passport FROM users WHERE id = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, userId);
                resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    String cards = resultSet.getString("cards");
                    String passport = resultSet.getString("have_a_passport");

                    boolean hasCard = cards != null && !cards.isEmpty();
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
                    // Если есть и карта, и паспорт, начинаем процесс аренды
                    new RentProductTask(productName.getText().toString(), postamatId, cellId).execute();
                    break;
                case NO_CARD:
                    Toast.makeText(RentProductActivity.this, "Добавьте карту для продолжения аренды", Toast.LENGTH_LONG).show();
                    break;
                case NO_PASSPORT:
                    Toast.makeText(RentProductActivity.this, "Добавьте паспорт для продолжения аренды", Toast.LENGTH_LONG).show();
                    break;
                case NO_BOTH:
                    Toast.makeText(RentProductActivity.this, "Добавьте карту и паспорт для продолжения аренды", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    private class RentProductTask extends AsyncTask<Void, Void, Boolean> {

        private String endTime;
        private String productName;
        private int postamatId;
        private int cellId;

        RentProductTask(String productName, int postamatId, int cellId) {
            this.productName = productName;
            this.postamatId = postamatId;
            this.cellId = cellId;
        }

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

                if (userId == -1 || productId == -1 || postamatId == -1 || cellId == -1) {
                    Log.e("RentProductTask", "Ошибка: userId, productId, postamatId или cellsId не заданы");
                    return false;
                }

                // Обновление таблицы products: установка address_of_postamat в NULL для productId
                String updateProductQuery = "UPDATE products SET address_of_postamat = NULL WHERE id = ?";
                preparedStatement = connection.prepareStatement(updateProductQuery);
                preparedStatement.setInt(1, productId);
                int productRowsUpdated = preparedStatement.executeUpdate();

                if (productRowsUpdated == 0) {
                    Log.e("RentProductTask", "Не удалось обновить продукт с id = " + productId);
                    return false;
                }

                // Обновление таблицы cells: установка product_id в NULL для product_id
                String updateCellsQuery = "UPDATE cells SET product_id = NULL WHERE product_id = ?";
                preparedStatement = connection.prepareStatement(updateCellsQuery);
                preparedStatement.setInt(1, productId);
                int cellsRowsUpdated = preparedStatement.executeUpdate();

                if (cellsRowsUpdated == 0) {
                    Log.e("RentProductTask", "Не удалось обновить ячейку с product_id = " + productId);
                    return false;
                }

                // Создание записи о заказе
                Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Yekaterinburg"));
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Yekaterinburg"));

                String startTime = dateFormat.format(calendar.getTime());

                // Устанавливаем endTime в зависимости от выбранного периода
                switch (selectedPeriod) {
                    case 1: // Часы
                        calendar.add(Calendar.HOUR, selectedTime);
                        break;
                    case 2: // Дни
                        calendar.add(Calendar.DAY_OF_YEAR, selectedTime);
                        break;
                    case 3: // Месяцы
                        calendar.add(Calendar.MONTH, selectedTime);
                        break;
                }

                endTime = dateFormat.format(calendar.getTime());

                // Рассчитываем rental_time в часах
                int rentalTimeHours;
                switch (selectedPeriod) {
                    case 1:
                        rentalTimeHours = selectedTime; // часы
                        break;
                    case 2:
                        rentalTimeHours = selectedTime * 24; // дни в часы
                        break;
                    case 3:
                        rentalTimeHours = selectedTime * 24 * 30; // месяцы в часы (примерно)
                        break;
                    default:
                        rentalTimeHours = selectedTime;
                }

                double rentalAmount;
                switch (selectedPeriod) {
                    case 1: // Часы
                        rentalAmount = productPriceHour;
                        break;
                    case 2: // Дни
                        rentalAmount = productPriceDay;
                        break;
                    case 3: // Месяцы
                        rentalAmount = productPriceMonth;
                        break;
                    default:
                        rentalAmount = 0.0;
                }

                // Вставка записи в таблицу orders
                String insertOrderQuery = "INSERT INTO orders (product_id, client_id, start_time, end_time, rental_time, renewal_time, rental_amount, returned) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
                preparedStatement = connection.prepareStatement(insertOrderQuery);
                preparedStatement.setInt(1, productId);
                preparedStatement.setInt(2, userId);
                preparedStatement.setString(3, startTime);
                preparedStatement.setString(4, endTime);
                preparedStatement.setInt(5, rentalTimeHours);
                preparedStatement.setNull(6, java.sql.Types.VARCHAR);
                preparedStatement.setDouble(7, rentalAmount);
                preparedStatement.setBoolean(8, false);
                preparedStatement.executeUpdate();

                return true;

            } catch (Exception e) {
                Log.e("RentProductTask", "Ошибка: " + e.getMessage(), e);
                return false;
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    Log.e("RentProductTask", "Ошибка при закрытии соединения: " + e.getMessage(), e);
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(RentProductActivity.this, "Товар успешно арендован", Toast.LENGTH_SHORT).show();

                SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("end_time", endTime);
                editor.apply();

                AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(RentProductActivity.this, RentAlarmReceiver.class);
                intent.putExtra("productName", productName);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(RentProductActivity.this, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Yekaterinburg"));
                try {
                    Date endDate = dateFormat.parse(endTime);
                    if (endDate != null) {
                        Calendar calendar = Calendar.getInstance();
                        calendar.setTime(endDate);
                        if (alarmManager != null) {
                            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                        }
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                    Toast.makeText(RentProductActivity.this, "Ошибка при установке времени уведомления", Toast.LENGTH_SHORT).show();
                }

                setResult(RESULT_OK);
                finish();
            } else {
                Toast.makeText(RentProductActivity.this, "Ошибка при аренде товара", Toast.LENGTH_SHORT).show();
            }
        }
    }
}