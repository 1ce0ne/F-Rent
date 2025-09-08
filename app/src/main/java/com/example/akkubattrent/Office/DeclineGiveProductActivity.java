package com.example.akkubattrent.Office;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DeclineGiveProductActivity extends AppCompatActivity {

    private static final int DELAY_MILLIS = 4500; // 8 seconds delay
    private int productId;
    private int userId;
    private String startTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_decline_give_for_people);

        productId = getIntent().getIntExtra("productId", -1);
        startTime = getIntent().getStringExtra("startTime");

        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("id", -1);

        // Start the handler to close this activity after 8 seconds
        new Handler().postDelayed(() -> {
            new UpdateOrderStatusTask().execute();
        }, DELAY_MILLIS);
    }

    private class UpdateOrderStatusTask extends AsyncTask<Void, Void, Boolean> {
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

                // Update the order status to mark it as not issued and returned
                String query = "UPDATE office_orders SET not_issued = 1, issued = 0, accepted = 1, returned = 1 WHERE product_id = ? AND client_id = ? AND start_time = ?";
                preparedStatement = connection.prepareStatement(query);
                preparedStatement.setInt(1, productId);
                preparedStatement.setInt(2, userId);
                preparedStatement.setString(3, startTime);

                int rowsUpdated = preparedStatement.executeUpdate();
                return rowsUpdated > 0;

            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                try {
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            // Return to main menu regardless of success
            Intent intent = new Intent(DeclineGiveProductActivity.this, MainMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }
    }
}