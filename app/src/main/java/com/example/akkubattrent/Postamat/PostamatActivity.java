package com.example.akkubattrent.Postamat;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.Classes.Product;
import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class PostamatActivity extends AppCompatActivity {

    private TextView productsInfoText;
    private TextView addressText;
    private Button closePostamatButton;
    private Button returnProductButton;
    private GridLayout productsContainer;
    private LinearLayout loadingLayout;
    private TextView noProductsText;
    private ProgressBar loadingProgressBar;
    private ImageView sadFaceIcon;

    private ArrayList<Product> currentProducts;
    private String postamatAddress;
    private int userId;
    private int postamatId;
    private boolean hasProducts = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_postamat);

        closePostamatButton = findViewById(R.id.closePostamatButton);
        returnProductButton = findViewById(R.id.returnProductButton);
        addressText = findViewById(R.id.textAddress);
        productsContainer = findViewById(R.id.productsContainer);
        loadingLayout = findViewById(R.id.loadingLayout);
        noProductsText = findViewById(R.id.noProductsText);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        sadFaceIcon = findViewById(R.id.sadFaceIcon);

        // Показываем анимацию загрузки и скрываем контейнер с товарами
        loadingLayout.setVisibility(View.VISIBLE);
        productsContainer.setVisibility(View.GONE);
        noProductsText.setVisibility(View.GONE);
        sadFaceIcon.setVisibility(View.GONE);

        // Получаем userId из SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("id", -1);

        if (userId == -1) {
            Log.e("PostamatActivity", "userId не передан или равен -1");
        }

        Intent intent = getIntent();
        postamatId = intent.getIntExtra("postamatId", -1);
        postamatAddress = intent.getStringExtra("address");

        // Загрузите данные о товарах заново
        loadProductsForPostamat(postamatId);

        addressText.setText(postamatAddress != null ? postamatAddress : "Адрес не доступен");

        closePostamatButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_down_window);
        });

        returnProductButton.setOnClickListener(v -> {
            Intent returnIntent = new Intent(PostamatActivity.this, ReturnProductActivity.class);
            returnIntent.putExtra("userId", userId);
            returnIntent.putExtra("postamatId", postamatId);
            returnIntent.putExtra("address", postamatAddress);
            startActivity(returnIntent);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            refreshProducts();
        }
    }

    private void refreshProducts() {
        // Очищаем контейнер перед загрузкой новых товаров
        productsContainer.removeAllViews();
        loadingLayout.setVisibility(View.VISIBLE);
        productsContainer.setVisibility(View.GONE);
        noProductsText.setVisibility(View.GONE);
        sadFaceIcon.setVisibility(View.GONE);
        hasProducts = false;

        loadProductsForPostamat(postamatId);
    }

    private void loadProductsForPostamat(int postamatId) {
        new Thread(() -> {
            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );

                String cellsQuery = "SELECT cell_id, product_id FROM cells WHERE parcel_automat_id = ?";
                PreparedStatement cellsStatement = connection.prepareStatement(cellsQuery);
                cellsStatement.setInt(1, postamatId);
                ResultSet cellsResultSet = cellsStatement.executeQuery();

                List<Integer> productIds = new ArrayList<>();
                List<Integer> cellsIds = new ArrayList<>();
                while (cellsResultSet.next()) {
                    productIds.add(cellsResultSet.getInt("product_id"));
                    cellsIds.add(cellsResultSet.getInt("cell_id"));
                }

                cellsResultSet.close();
                cellsStatement.close();

                for (int i = 0; i < productIds.size(); i++) {
                    int productId = productIds.get(i);
                    int cellId = cellsIds.get(i);

                    Product product = fetchProduct(productId, cellId);
                    if (product != null) {
                        hasProducts = true;
                        runOnUiThread(() -> addProductToGridLayout(product));
                    }
                }

                runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    if (hasProducts) {
                        productsContainer.setVisibility(View.VISIBLE);
                    } else {
                        noProductsText.setText("Тут ничего нет :(");
                        noProductsText.setVisibility(View.VISIBLE);
                        sadFaceIcon.setVisibility(View.VISIBLE);
                    }
                });

                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    loadingLayout.setVisibility(View.GONE);
                    noProductsText.setText("Тут ничего нет :(");
                    noProductsText.setVisibility(View.VISIBLE);
                    sadFaceIcon.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private Product fetchProduct(int productId, int cellId) {
        try {
            Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                    "akku_batt_admin",
                    "8rssCz31UiUr512e"
            );
            String query = "SELECT id, name, description, price_per_hour, price_per_day, price_per_month,  image, product_uuid, size FROM products WHERE id = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setInt(1, productId);
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String description = resultSet.getString("description");
                double price_per_hour = resultSet.getDouble("price_per_hour");
                double price_per_day = resultSet.getDouble("price_per_day");
                double price_per_month = resultSet.getDouble("price_per_month");
                byte[] image = resultSet.getBytes("image");
                String productUuid = resultSet.getString("product_uuid");
                String size = resultSet.getString("size");

                resultSet.close();
                statement.close();
                connection.close();

                return new Product(id, name, description, price_per_hour, price_per_day, price_per_month, image, productUuid, size, cellId);
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void addProductToGridLayout(Product product) {
        View productView = getLayoutInflater().inflate(R.layout.fragment_product_card, productsContainer, false);

        ImageView productImage = productView.findViewById(R.id.productImage);
        TextView productName = productView.findViewById(R.id.productName);
        TextView aboutOfProduct = productView.findViewById(R.id.aboutOfProduct);
        TextView costsOfProduct = productView.findViewById(R.id.costsOfProduct);

        if (product.getImage() != null && product.getImage().length > 0) {
            Bitmap originalBitmap = BitmapFactory.decodeByteArray(product.getImage(), 0, product.getImage().length);
            productImage.setImageBitmap(originalBitmap);
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }

        productName.setText(product.getName());

        String description = product.getDescription();
        if (description != null && description.length() > 35) {
            description = description.substring(0, 15) + "...";
        }
        aboutOfProduct.setText(description != null ? description : "");

        costsOfProduct.setText(String.format("%dр/час", (int)product.getPriceHour()));

        productView.setOnClickListener(v -> {
            Intent intent = new Intent(PostamatActivity.this, RentProductActivity.class);
            intent.putExtra("productName", product.getName());
            intent.putExtra("productDescription", product.getDescription());
            intent.putExtra("productPriceHour", product.getPriceHour());
            intent.putExtra("productPriceDay", product.getPriceDay());
            intent.putExtra("productPriceMonth", product.getPriceMonth());
            intent.putExtra("productImage", product.getImage());
            intent.putExtra("productId", product.getId());
            intent.putExtra("productUuid", product.getProductUuid());
            intent.putExtra("size", product.getSize());
            intent.putExtra("postamatId", postamatId);
            intent.putExtra("cellId", product.getCellsId());
            startActivityForResult(intent, 1);
        });

        productsContainer.addView(productView);
    }
}