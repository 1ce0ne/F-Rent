package com.example.akkubattrent.Office;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class GiveProductForPeopleActivity extends AppCompatActivity {

    private int productId;
    private int userId;
    private int officeId;

    private Handler handler;
    private Runnable checkStatusRunnable;
    private static final long CHECK_INTERVAL = 5000; // 5 секунд

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_give_product_back);

        // Получаем данные из предыдущего активити
        Intent intent = getIntent();
        productId = intent.getIntExtra("productId", -1);
        userId = intent.getIntExtra("userId", -1);
        officeId = intent.getIntExtra("officeId", -1);

        if (productId == -1 || userId == -1) {
            finish();
            return;
        }

        // Инициализируем Handler для периодической проверки статуса
        handler = new Handler(Looper.getMainLooper());
        checkStatusRunnable = new Runnable() {
            @Override
            public void run() {
                checkOrderStatus();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };

        // Сразу делаем запрос на возврат при создании активности
        makeRequestForReturn();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Запускаем проверку статуса при возобновлении активности
        handler.post(checkStatusRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Останавливаем проверку статуса при приостановке активности
        handler.removeCallbacks(checkStatusRunnable);
    }

    private void makeRequestForReturn() {
        new Thread(() -> {
            Connection connection = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                // Устанавливаем флаг ready_for_return в 1
                String updateQuery = "UPDATE office_orders SET ready_for_return = 1 WHERE product_id = ? AND client_id = ? AND returned = 0";
                PreparedStatement updateStatement = connection.prepareStatement(updateQuery);
                updateStatement.setInt(1, productId);
                updateStatement.setInt(2, userId);
                updateStatement.executeUpdate();
                updateStatement.close();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (connection != null) {
                        connection.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void checkOrderStatus() {
        new Thread(() -> {
            Connection connection = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                // Начинаем транзакцию
                connection.setAutoCommit(false);

                String query = "SELECT accepted, ready_for_return FROM office_orders WHERE product_id = ? AND client_id = ? AND returned = 0";
                PreparedStatement statement = connection.prepareStatement(query);
                statement.setInt(1, productId);
                statement.setInt(2, userId);

                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    int acceptedStatus = resultSet.getInt("accepted");
                    int readyForReturn = resultSet.getInt("ready_for_return");

                    if (acceptedStatus == 1) {
                        // 1. Получаем адрес офиса
                        String officeAddress = "";
                        String getAddressQuery = "SELECT address FROM office WHERE id = ?";
                        PreparedStatement getAddressStatement = connection.prepareStatement(getAddressQuery);
                        getAddressStatement.setInt(1, officeId);
                        ResultSet addressResult = getAddressStatement.executeQuery();

                        if (addressResult.next()) {
                            officeAddress = addressResult.getString("address");
                        }
                        addressResult.close();
                        getAddressStatement.close();

                        // 2. Обновляем адрес офиса в таблице products
                        String updateProductQuery = "UPDATE products SET address_of_postamat = ? WHERE id = ?";
                        PreparedStatement updateProductStatement = connection.prepareStatement(updateProductQuery);
                        updateProductStatement.setString(1, officeAddress);
                        updateProductStatement.setInt(2, productId);
                        updateProductStatement.executeUpdate();
                        updateProductStatement.close();

                        // 3. Очищаем товар из всех ячеек во всех офисах
                        String clearCellsQuery = "UPDATE office_cells SET office_product_id = NULL WHERE office_product_id = ?";
                        PreparedStatement clearCellsStatement = connection.prepareStatement(clearCellsQuery);
                        clearCellsStatement.setInt(1, productId);
                        clearCellsStatement.executeUpdate();
                        clearCellsStatement.close();

                        // 4. Находим и занимаем первую свободную ячейку в нужном офисе
                        int freeCellId = -1;
                        String findFreeCellQuery = "SELECT office_cell_id FROM office_cells WHERE office_id = ? AND office_product_id IS NULL LIMIT 1 FOR UPDATE";
                        PreparedStatement findFreeCellStatement = connection.prepareStatement(findFreeCellQuery);
                        findFreeCellStatement.setInt(1, officeId); // Подставляем officeId
                        ResultSet freeCellResult = findFreeCellStatement.executeQuery();

                        if (freeCellResult.next()) {
                            freeCellId = freeCellResult.getInt("office_cell_id");

                            // Обновляем только конкретную ячейку
                            String updateCellQuery = "UPDATE office_cells SET office_product_id = ? WHERE office_cell_id = ? AND office_id = ?";
                            PreparedStatement updateCellStatement = connection.prepareStatement(updateCellQuery);
                            updateCellStatement.setInt(1, productId);
                            updateCellStatement.setInt(2, freeCellId);
                            updateCellStatement.setInt(3, officeId);
                            updateCellStatement.executeUpdate();
                            updateCellStatement.close();
                        }
                        freeCellResult.close();
                        findFreeCellStatement.close();

                        // 5. Сбрасываем флаг ready_for_return и устанавливаем returned в 1
                        String updateOrderQuery = "UPDATE office_orders SET returned = 1, ready_for_return = 0 WHERE product_id = ? AND client_id = ? AND returned != 1";
                        PreparedStatement updateOrderStatement = connection.prepareStatement(updateOrderQuery);
                        updateOrderStatement.setInt(1, productId);
                        updateOrderStatement.setInt(2, userId);
                        updateOrderStatement.executeUpdate();
                        updateOrderStatement.close();

                        // Подтверждаем транзакцию
                        connection.commit();

                        // Если статус изменился на 1, переходим в MainMenuActivity
                        runOnUiThread(() -> {
                            Intent intent = new Intent(GiveProductForPeopleActivity.this, MainMenuActivity.class);
                            intent.putExtra("userId", userId);
                            intent.putExtra("officeId", officeId);
                            startActivity(intent);
                            finish();
                        });
                        return;
                    }
                }

                resultSet.close();
                statement.close();
            } catch (Exception e) {
                try {
                    if (connection != null) {
                        connection.rollback();
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                e.printStackTrace();
            } finally {
                try {
                    if (connection != null) {
                        connection.setAutoCommit(true);
                        connection.close();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}