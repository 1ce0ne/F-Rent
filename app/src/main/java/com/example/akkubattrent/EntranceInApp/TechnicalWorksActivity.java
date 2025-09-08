package com.example.akkubattrent.EntranceInApp;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class TechnicalWorksActivity extends AppCompatActivity {

    private TextView textViewEstimatedTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_technical_works);

        // Инициализация TextView
        textViewEstimatedTime = findViewById(R.id.textViewEstimatedTime);

        // Загрузка данных из базы данных
        loadEstimatedTimeFromDatabase();
    }

    private void loadEstimatedTimeFromDatabase() {
        new Thread(() -> {
            try {
                // Подключение к базе данных
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";

                Connection connection = DriverManager.getConnection(url, user, password);

                // Запрос для получения времени окончания работ
                String sql = "SELECT term_of_work FROM technical_works_and_update WHERE work_id = 1";
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    String termOfWork = resultSet.getString("term_of_work");

                    // Обновление UI на главном потоке
                    new Handler(Looper.getMainLooper()).post(() -> {
                        textViewEstimatedTime.setText("Примерное время окончания работ: " + termOfWork);
                    });
                }

                // Закрытие соединений
                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}