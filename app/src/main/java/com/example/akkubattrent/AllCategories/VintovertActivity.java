package com.example.akkubattrent.AllCategories;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.Classes.ProductWithCoordinates;
import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

public class VintovertActivity extends AppCompatActivity {

    private LinearLayout productsContainer;
    private ProgressBar loadingProgressBar;
    private TextView loadingText;
    private TextView noProductsText;
    private boolean hasProducts = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.categories_vintovert);

        productsContainer = findViewById(R.id.productsContainer);
        loadingProgressBar = findViewById(R.id.loadingProgressBar); // Добавьте в layout
        loadingText = findViewById(R.id.loadingText); // Добавьте в layout
        noProductsText = findViewById(R.id.noProductsText); // Добавьте в layout

        Button closeCategoryButton = findViewById(R.id.closeCategoryButton);
        closeCategoryButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_left); // ← Новое слева, текущее вправо
        });

        showLoading();
        loadConsolesFromDatabase();
    }

    private void showLoading() {
        runOnUiThread(() -> {
            if (loadingProgressBar != null && loadingText != null) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                loadingText.setVisibility(View.VISIBLE);
                noProductsText.setVisibility(View.GONE);
                loadingText.setText("Загрузка винтовертов...");
            }
        });
    }

    private void showNoProducts() {
        runOnUiThread(() -> {
            if (loadingProgressBar != null && loadingText != null && noProductsText != null) {
                loadingProgressBar.setVisibility(View.GONE);
                loadingText.setVisibility(View.GONE);
                noProductsText.setVisibility(View.VISIBLE);
                noProductsText.setText("Винтоверты не найдены");
            }
        });
    }

    private void loadConsolesFromDatabase() {
        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                // Попробуйте разные варианты категорий
                String query = "SELECT id, name, description, price_per_hour, price_per_day, price_per_month, image, address_of_postamat FROM products WHERE (category = 'vintovert' OR category = 'винтоверт') AND address_of_postamat IS NOT NULL";
                PreparedStatement statement = connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery();

                ArrayList<ProductWithCoordinates> consoles = new ArrayList<>();
                while (resultSet.next()) {
                    int id = resultSet.getInt("id");
                    String name = resultSet.getString("name");
                    String description = resultSet.getString("description");
                    double pricePerHour = resultSet.getDouble("price_per_hour");
                    double pricePerDay = resultSet.getDouble("price_per_day");
                    double pricePerMonth = resultSet.getDouble("price_per_month");
                    byte[] image = resultSet.getBytes("image");
                    String address = resultSet.getString("address_of_postamat");

                    // Получаем координаты для адреса офиса
                    double firstCoordinate = 0.0;
                    double secondCoordinate = 0.0;

                    if (address != null && !address.isEmpty()) {
                        String coordQuery = "SELECT first_coordinate, second_coordinate " +
                                "FROM office WHERE address = ?";
                        PreparedStatement coordStmt = connection.prepareStatement(coordQuery);
                        coordStmt.setString(1, address);
                        ResultSet coordRs = coordStmt.executeQuery();

                        if (coordRs.next()) {
                            firstCoordinate = coordRs.getDouble("first_coordinate");
                            secondCoordinate = coordRs.getDouble("second_coordinate");
                        }
                        coordRs.close();
                        coordStmt.close();
                    }

                    consoles.add(new ProductWithCoordinates(id, name, description, pricePerHour, pricePerDay,
                            pricePerMonth, image, null, address, -1, firstCoordinate, secondCoordinate));
                }

                resultSet.close();
                statement.close();
                connection.close();

                if (consoles.isEmpty()) {
                    showNoProducts();
                } else {
                    hasProducts = true;
                    displayConsoles(consoles);
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    showNoProducts();
                    // Добавьте Toast для отображения ошибки
                    Toast.makeText(VintovertActivity.this, "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private void displayConsoles(ArrayList<ProductWithCoordinates> consoles) {
        runOnUiThread(() -> {
            loadingProgressBar.setVisibility(View.GONE);
            loadingText.setVisibility(View.GONE);
            productsContainer.removeAllViews();

            if (consoles == null || consoles.isEmpty()) {
                showNoProducts();
                return;
            }

            for (ProductWithCoordinates console : consoles) {
                View productCard = getLayoutInflater().inflate(R.layout.product_card_for_category, productsContainer, false);

                ImageView productImage = productCard.findViewById(R.id.productImage);
                TextView productName = productCard.findViewById(R.id.productName);
                TextView aboutOfProduct = productCard.findViewById(R.id.aboutOfProduct);
                TextView costsOfProduct = productCard.findViewById(R.id.costsOfProduct);
                TextView addressOfProducts = productCard.findViewById(R.id.addressOfProducts);

                try {
                    if (console.getImage() != null && console.getImage().length > 0) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(console.getImage(), 0, console.getImage().length);
                        productImage.setImageBitmap(bitmap);
                    } else {
                        productImage.setImageResource(R.drawable.placeholder_image);
                    }

                    productName.setText(console.getName() != null ? console.getName() : "");

                    String description = console.getDescription();
                    if (description != null && description.length() > 35) {
                        description = description.substring(0, 35) + "...";
                    }
                    aboutOfProduct.setText(description != null ? description : "");

                    costsOfProduct.setText(String.format("%dр/час", (int) console.getPriceHour()));

                    if (console.getAddress() != null) {
                        addressOfProducts.setText(console.getAddress());
                    } else {
                        addressOfProducts.setText("Адрес не указан");
                    }

                    productCard.setOnClickListener(v -> {
                        Intent intent = new Intent(VintovertActivity.this, RouteToPostamat.class);
                        intent.putExtra("productName", console.getName());
                        intent.putExtra("productDescription", console.getDescription());
                        intent.putExtra("price_per_hour", console.getPriceHour());
                        intent.putExtra("price_per_day", console.getPriceDay());
                        intent.putExtra("price_per_month", console.getPriceMonth());
                        intent.putExtra("productImage", console.getImage());
                        intent.putExtra("productId", console.getId());
                        intent.putExtra("firstCoordinate", console.getFirstCoordinate());
                        intent.putExtra("secondCoordinate", console.getSecondCoordinate());
                        startActivity(intent);
                    });

                    productsContainer.addView(productCard);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}