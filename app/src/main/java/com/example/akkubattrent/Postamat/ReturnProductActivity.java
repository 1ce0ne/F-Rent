package com.example.akkubattrent.Postamat;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class ReturnProductActivity extends AppCompatActivity {

    private LinearLayout productsContainer;
    private ArrayList<ProductWithEndTime> returnableProducts;
    private int userId;
    private int postamatId;
    private String postamatAddress;

    private class ProductWithEndTime {
        int id;
        String name;
        byte[] image;
        String endTime;

        public ProductWithEndTime(int id, String name, byte[] image, String endTime) {
            this.id = id;
            this.name = name;
            this.image = image;
            this.endTime = endTime;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public byte[] getImage() { return image; }
        public String getEndTime() { return endTime; }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_return_product);

        productsContainer = findViewById(R.id.productsContainer);

        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", -1);
        postamatId = intent.getIntExtra("postamatId", -1);
        postamatAddress = intent.getStringExtra("address");

        if (userId == -1) {
            Log.e("ReturnProductActivity", "userId не передан или равен -1");
        }

        FrameLayout backToPostamatButton = findViewById(R.id.backToPostamatButton);
        backToPostamatButton.setOnClickListener(v -> {
            Intent backIntent = new Intent(ReturnProductActivity.this, PostamatActivity.class);
            backIntent.putExtra("userId", userId);
            backIntent.putExtra("postamatId", postamatId);
            backIntent.putExtra("address", postamatAddress);
            startActivity(backIntent);
            finish();
        });

        loadReturnableProducts(userId);
    }

    private void loadReturnableProducts(int userId) {
        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                // Получаем product_id и end_time для каждого заказа
                String ordersQuery = "SELECT product_id, end_time FROM orders WHERE client_id = ? AND returned = 0";
                PreparedStatement ordersStatement = connection.prepareStatement(ordersQuery);
                ordersStatement.setInt(1, userId);
                ResultSet ordersResultSet = ordersStatement.executeQuery();

                // Собираем map: product_id -> end_time
                Map<Integer, String> productEndTimes = new HashMap<>();
                while (ordersResultSet.next()) {
                    int productId = ordersResultSet.getInt("product_id");
                    String endTime = ordersResultSet.getString("end_time");
                    productEndTimes.put(productId, endTime);
                }

                // Получаем список ID продуктов
                List<Integer> productIds = new ArrayList<>(productEndTimes.keySet());

                // Получаем информацию о продуктах
                List<ProductWithEndTime> products = fetchProductsWithEndTimes(productIds, productEndTimes);

                runOnUiThread(() -> {
                    productsContainer.removeAllViews();
                    if (products.isEmpty()) {
                        TextView noProductsText = new TextView(this);
                        noProductsText.setText("Нет товаров для возврата.");
                        noProductsText.setTextSize(18f);
                        noProductsText.setGravity(View.TEXT_ALIGNMENT_CENTER);
                        productsContainer.addView(noProductsText);
                    } else {
                        for (ProductWithEndTime product : products) {
                            addProductToLayout(product);
                        }
                    }
                });

                ordersResultSet.close();
                ordersStatement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private List<ProductWithEndTime> fetchProductsWithEndTimes(List<Integer> productIds, Map<Integer, String> productEndTimes) {
        List<ProductWithEndTime> products = new ArrayList<>();
        if (productIds.isEmpty()) return products;

        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                    "akku_batt_admin",
                    "8rssCz31UiUr512e"
            );
            String query = "SELECT id, name, image FROM products WHERE id IN ("
                    + productIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";

            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                byte[] image = resultSet.getBytes("image");
                String endTime = productEndTimes.get(id);

                // Форматируем дату, если нужно
                if (endTime != null) {
                    try {
                        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                        SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                        Date date = inputFormat.parse(endTime);
                        endTime = outputFormat.format(date);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                products.add(new ProductWithEndTime(id, name, image, endTime));
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }

    private void addProductToLayout(ProductWithEndTime product) {
        View orderView = getLayoutInflater().inflate(R.layout.order_card, productsContainer, false);

        // Устанавливаем меньшие отступы для карточки
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(26, 28, 26, 13); // Уменьшаем отступы сверху и снизу
        orderView.setLayoutParams(layoutParams);

        ImageView productImage = orderView.findViewById(R.id.productImage);
        TextView productName = orderView.findViewById(R.id.productName);
        TextView rentalTime = orderView.findViewById(R.id.rentalTime);

        if (product.getImage() != null && product.getImage().length > 0) {
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(product.getImage(), 0, product.getImage().length);
            productImage.setImageBitmap(originalBitmap);
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }

        productName.setText(product.getName());
        rentalTime.setText(product.getEndTime() != null ?
                "До: " + product.getEndTime() :
                "Время возврата не указано");

        // Скрываем выпадающее меню и кнопки продления
        View dropdownView = orderView.findViewById(R.id.dropdownView);
        dropdownView.setVisibility(View.GONE);

        orderView.setOnClickListener(v -> {
            Intent intent = new Intent(ReturnProductActivity.this, ReturnConfirmationActivity.class);
            intent.putExtra("productId", product.getId());
            intent.putExtra("userId", userId);
            intent.putExtra("postamatId", postamatId);
            startActivity(intent);
        });

        productsContainer.addView(orderView);
    }
}