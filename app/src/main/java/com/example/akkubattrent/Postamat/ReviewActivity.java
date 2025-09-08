package com.example.akkubattrent.Postamat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class ReviewActivity extends AppCompatActivity {

    private LinearLayout starsLayout;
    private EditText feedbackEditText;
    private Button submitButton;
    private int selectedRating = 0;
    private int productId;
    private int userId;
    private int postamatId;
    private Bitmap photoBitmap;
    private AlertDialog loadingDialog;
    private boolean isProcessing = false;
    private int cellId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review_postamats);

        starsLayout = findViewById(R.id.starsLayout);
        feedbackEditText = findViewById(R.id.feedbackEditText);
        submitButton = findViewById(R.id.submitButton);

        Intent intent = getIntent();
        productId = intent.getIntExtra("productId", -1);
        userId = intent.getIntExtra("userId", -1);
        postamatId = intent.getIntExtra("postamatId", -1);
        cellId = intent.getIntExtra("cellId", -1);
        String photoPath = intent.getStringExtra("photoPath");

        if (photoPath != null) {
            File imgFile = new File(photoPath);
            if (imgFile.exists()) {
                photoBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
            }
        }

        for (int i = 0; i < 5; i++) {
            ImageView star = (ImageView) starsLayout.getChildAt(i);
            final int rating = i + 1;
            star.setOnClickListener(v -> {
                selectedRating = rating;
                updateStars();
                updateFeedbackHint();
            });
        }

        feedbackEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 250) {
                    feedbackEditText.setText(s.subSequence(0, 250));
                    feedbackEditText.setSelection(250);
                    Toast.makeText(ReviewActivity.this, "Максимум 250 символов", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        submitButton.setOnClickListener(v -> {
            if (isProcessing) {
                Toast.makeText(ReviewActivity.this, "Действие уже выполняется", Toast.LENGTH_SHORT).show();
                return;
            }

            if (selectedRating == 0) {
                Toast.makeText(ReviewActivity.this, "Выберите оценку", Toast.LENGTH_SHORT).show();
                return;
            }

            String feedback = feedbackEditText.getText().toString().trim();
            if (feedback.isEmpty()) {
                Toast.makeText(ReviewActivity.this, "Введите отзыв", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoadingDialog();
            isProcessing = true;

            processReturnAndReview(selectedRating, feedback);
        });
    }

    private void processReturnAndReview(int rating, String feedback) {
        new Thread(() -> {
            Connection connection = null;
            try {
                Class.forName("com.mysql.jdbc.Driver");
                connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                // 1. Получаем адрес постамата из parcel_automats
                String postamatAddress = "";
                String getAddressQuery = "SELECT address FROM parcel_automats WHERE id = ?";
                PreparedStatement getAddressStatement = connection.prepareStatement(getAddressQuery);
                getAddressStatement.setInt(1, postamatId);
                ResultSet addressResult = getAddressStatement.executeQuery();

                if (addressResult.next()) {
                    postamatAddress = addressResult.getString("address");
                }
                addressResult.close();
                getAddressStatement.close();

                // 2. Обновляем адрес постамата в таблице products
                String updateProductQuery = "UPDATE products SET address_of_postamat = ? WHERE id = ?";
                PreparedStatement updateProductStatement = connection.prepareStatement(updateProductQuery);
                updateProductStatement.setString(1, postamatAddress);
                updateProductStatement.setInt(2, productId);
                updateProductStatement.executeUpdate();
                updateProductStatement.close();

                // 3. Обновляем ячейку
                String updateCellQuery = "UPDATE cells SET product_id = ? WHERE cell_id = ?";
                PreparedStatement updateCellStatement = connection.prepareStatement(updateCellQuery);
                updateCellStatement.setInt(1, productId);
                updateCellStatement.setInt(2, cellId);
                updateCellStatement.executeUpdate();
                updateCellStatement.close();

                // 4. Обновляем заказ
                String updateOrderQuery = "UPDATE orders SET returned = 1 WHERE product_id = ? AND client_id = ?";
                PreparedStatement updateOrderStatement = connection.prepareStatement(updateOrderQuery);
                updateOrderStatement.setInt(1, productId);
                updateOrderStatement.setInt(2, userId);
                updateOrderStatement.executeUpdate();
                updateOrderStatement.close();

                // 5. Вставляем отзыв
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                if (photoBitmap != null) {
                    photoBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                }
                byte[] photoBytes = stream.toByteArray();

                String reportQuery = "INSERT INTO report_to_send (product_photo, report_send, product_id, feedback, review) VALUES (?, ?, ?, ?, ?)";
                PreparedStatement reportStatement = connection.prepareStatement(reportQuery);
                reportStatement.setBytes(1, photoBytes);
                reportStatement.setInt(2, 0);
                reportStatement.setInt(3, productId);
                reportStatement.setString(4, feedback);
                reportStatement.setInt(5, rating);
                reportStatement.executeUpdate();
                reportStatement.close();

                runOnUiThread(() -> {
                    dismissLoadingDialog();
                    // Переход на MainMenuActivity
                    Intent intent = new Intent(ReviewActivity.this, MainMenuActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    finish();
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    dismissLoadingDialog();
                    isProcessing = false;
                    Toast.makeText(ReviewActivity.this, "Ошибка при обработке возврата", Toast.LENGTH_SHORT).show();
                });
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

    private void showLoadingDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_loading_return, null);
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

    private void updateStars() {
        for (int i = 0; i < 5; i++) {
            ImageView star = (ImageView) starsLayout.getChildAt(i);
            if (i < selectedRating) {
                star.setImageResource(R.drawable.star_gold);
            } else {
                star.setImageResource(R.drawable.star);
            }
        }
    }

    private void updateFeedbackHint() {
        if (selectedRating == 5) {
            feedbackEditText.setHint("Что понравилось больше всего?");
        } else {
            feedbackEditText.setHint("Что испортило Ваше впечатление?");
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dismissLoadingDialog();
    }
}