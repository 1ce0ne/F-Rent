package com.example.akkubattrent.Postamat;

import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class OpenPostamatActivity extends AppCompatActivity {

    private int productId;
    private int userId;
    private int postamatId;
    private String photoPath;
    private int cellId = -1;
    private AlertDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_postamat);

        Intent intent = getIntent();
        productId = intent.getIntExtra("productId", -1);
        userId = intent.getIntExtra("userId", -1);
        postamatId = intent.getIntExtra("postamatId", -1);
        photoPath = intent.getStringExtra("photoPath");

        Button doorNotOpenedButton = findViewById(R.id.doorNotOpenedButton);
        Button toReviewButton = findViewById(R.id.toReviewButton);

        showLoadingDialog();

        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                String productSizeQuery = "SELECT size FROM products WHERE id = ?";
                PreparedStatement productSizeStatement = connection.prepareStatement(productSizeQuery);
                productSizeStatement.setInt(1, productId);
                ResultSet productSizeResultSet = productSizeStatement.executeQuery();
                String productSize = null;
                if (productSizeResultSet.next()) {
                    productSize = productSizeResultSet.getString("size");
                }
                productSizeResultSet.close();
                productSizeStatement.close();

                if (productSize == null) {
                    runOnUiThread(() -> {
                        dismissLoadingDialog();
                        Toast.makeText(OpenPostamatActivity.this, "Ошибка: размер продукта не найден", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                    return;
                }

                String cellQuery;
                switch (productSize.toLowerCase()) {
                    case "small":
                        cellQuery = "SELECT cell_id FROM cells WHERE parcel_automat_id = ? " +
                                "AND (size = 'small' OR size = 'medium' OR size = 'large') " +
                                "AND product_id IS NULL LIMIT 1";
                        break;
                    case "medium":
                        cellQuery = "SELECT cell_id FROM cells WHERE parcel_automat_id = ? " +
                                "AND (size = 'medium' OR size = 'large') " +
                                "AND product_id IS NULL LIMIT 1";
                        break;
                    case "large":
                        cellQuery = "SELECT cell_id FROM cells WHERE parcel_automat_id = ? " +
                                "AND size = 'large' " +
                                "AND product_id IS NULL LIMIT 1";
                        break;
                    default:
                        runOnUiThread(() -> {
                            dismissLoadingDialog();
                            Toast.makeText(OpenPostamatActivity.this, "Неизвестный размер товара", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                        return;
                }

                PreparedStatement cellStatement = connection.prepareStatement(cellQuery);
                cellStatement.setInt(1, postamatId);
                ResultSet cellResultSet = cellStatement.executeQuery();

                if (cellResultSet.next()) {
                    cellId = cellResultSet.getInt("cell_id");
                    runOnUiThread(() -> dismissLoadingDialog());
                } else {
                    runOnUiThread(() -> {
                        dismissLoadingDialog();
                        Toast.makeText(OpenPostamatActivity.this, "Нет свободных ячеек для возврата", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }
                cellResultSet.close();
                cellStatement.close();
                connection.close();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    dismissLoadingDialog();
                    Toast.makeText(OpenPostamatActivity.this, "Ошибка при поиске ячейки", Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        }).start();

        doorNotOpenedButton.setOnClickListener(v -> {
            Toast.makeText(this, "Пожалуйста, обратитесь в поддержку", Toast.LENGTH_SHORT).show();
        });

        toReviewButton.setOnClickListener(v -> {
            if (cellId == -1) {
                Toast.makeText(this, "Не удалось определить ячейку для возврата", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent postamatPhotoIntent = new Intent(OpenPostamatActivity.this, PostamatPhotoActivity.class);
            postamatPhotoIntent.putExtra("productId", productId);
            postamatPhotoIntent.putExtra("userId", userId);
            postamatPhotoIntent.putExtra("postamatId", postamatId);
            postamatPhotoIntent.putExtra("photoPath", photoPath);
            postamatPhotoIntent.putExtra("cellId", cellId);
            startActivity(postamatPhotoIntent);
        });
    }

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        loadingDialog = builder.create();
        loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        loadingDialog.show();
    }

    private void dismissLoadingDialog() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }
}