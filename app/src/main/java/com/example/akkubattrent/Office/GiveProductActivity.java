package com.example.akkubattrent.Office;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class GiveProductActivity extends AppCompatActivity {

    private static final int POLLING_INTERVAL = 3000;
    private int productId;
    private int userId;
    private String startTime;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_give_product_for_people);

        productId = getIntent().getIntExtra("productId", -1);
        startTime = getIntent().getStringExtra("startTime"); // Получаем переданное время
        userId = getIntent().getIntExtra("userId", -1);

        if (userId == -1 || productId == -1 || startTime == null) {
            Toast.makeText(this, "Ошибка параметров:\nstartTime=" + startTime, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        startCheckingStatus();
    }

    private void startCheckingStatus() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                new CheckOrderStatusTask().execute();
            }
        }, POLLING_INTERVAL);
    }

    private class CheckOrderStatusTask extends AsyncTask<Void, Void, Integer> {
        private static final int STATUS_ISSUED = 1;
        private static final int STATUS_NOT_ISSUED = 2;
        private static final int STATUS_PENDING = 3;
        private static final int STATUS_ERROR = 4;

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
                        "WHERE product_id = ? AND client_id = ? AND start_time = ? " +
                        "LIMIT 1";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, productId);
                preparedStatement.setInt(2, userId);
                preparedStatement.setString(3, startTime);
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
                    handler.removeCallbacksAndMessages(null);
                    new UpdateProductLocationTask().execute();
                    break;
                case STATUS_NOT_ISSUED:
                    handler.removeCallbacksAndMessages(null);
                    Intent declineIntent = new Intent(GiveProductActivity.this, DeclineGiveProductActivity.class);
                    declineIntent.putExtra("productId", productId);
                    declineIntent.putExtra("startTime", startTime);
                    startActivity(declineIntent);
                    finish();
                    break;
                case STATUS_PENDING:
                    startCheckingStatus();
                    break;
                case STATUS_ERROR:
                    Toast.makeText(GiveProductActivity.this, "Ошибка при проверке статуса заказа", Toast.LENGTH_SHORT).show();
                    startCheckingStatus();
                    break;
            }
        }
    }

    private class UpdateProductLocationTask extends AsyncTask<Void, Void, Boolean> {
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

                // Отключаем автокоммит для транзакции
                connection.setAutoCommit(false);

                // 1. Обновляем продукт
                String updateProductQuery = "UPDATE products SET address_of_postamat = NULL WHERE id = ?";
                preparedStatement = connection.prepareStatement(updateProductQuery);
                preparedStatement.setInt(1, productId);
                int productRowsUpdated = preparedStatement.executeUpdate();

                if (productRowsUpdated == 0) {
                    connection.rollback();
                    return false;
                }

                // 2. Обновляем ячейку офиса
                String updateCellsQuery = "UPDATE office_cells SET office_product_id = NULL WHERE office_product_id = ?";
                preparedStatement = connection.prepareStatement(updateCellsQuery);
                preparedStatement.setInt(1, productId);
                int cellsRowsUpdated = preparedStatement.executeUpdate();

                if (cellsRowsUpdated == 0) {
                    connection.rollback();
                    return false;
                }

                // 3. Обновляем статус заказа (добавлено)
                String updateOrderQuery = "UPDATE office_orders SET issued = 1, not_issued = 0 WHERE product_id = ? AND client_id = ? AND start_time = ?";
                preparedStatement = connection.prepareStatement(updateOrderQuery);
                preparedStatement.setInt(1, productId);
                preparedStatement.setInt(2, userId);
                preparedStatement.setString(3, startTime);
                int orderRowsUpdated = preparedStatement.executeUpdate();

                if (orderRowsUpdated == 0) {
                    connection.rollback();
                    return false;
                }

                // Если все прошло успешно - коммитим транзакцию
                connection.commit();
                return true;

            } catch (Exception e) {
                try {
                    if (connection != null) connection.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) {
                        connection.setAutoCommit(true); // Включаем автокоммит обратно
                        connection.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(GiveProductActivity.this, "Товар успешно выдан", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(GiveProductActivity.this, "Ошибка при обновлении данных товара", Toast.LENGTH_SHORT).show();
            }
            Intent intent = new Intent(GiveProductActivity.this, MainMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}