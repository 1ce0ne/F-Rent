package com.example.akkubattrent.UserProfile;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.akkubattrent.R;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MyOrdersActivity extends AppCompatActivity {

    private RecyclerView productsContainer;
    private ProductAdapter adapter;
    private List<Product> products;
    private int userId;
    private int currentPage = 0;
    private boolean isLoading = false;
    private boolean hasMoreItems = true;
    private static final int ITEMS_PER_PAGE = 6;
    private LinearLayout loadingLayout;
    private ProgressBar loadingProgressBar;
    private TextView noOrdersText;
    private TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_client_orders);

        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        userId = sharedPreferences.getInt("id", -1);

        productsContainer = findViewById(R.id.productsContainer);
        loadingLayout = findViewById(R.id.loadingLayout);
        loadingProgressBar = findViewById(R.id.loadingProgressBar);
        noOrdersText = findViewById(R.id.noOrdersText);
        loadingText = findViewById(R.id.loadingText);

        // Показываем анимацию загрузки
        loadingLayout.setVisibility(View.VISIBLE);
        noOrdersText.setVisibility(View.GONE);
        productsContainer.setVisibility(View.GONE);

        products = new ArrayList<>();
        adapter = new ProductAdapter(products);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        productsContainer.setLayoutManager(layoutManager);
        productsContainer.setAdapter(adapter);

        loadMoreItems();

        // Добавляем слушатель прокрутки для пагинации
        productsContainer.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!isLoading && hasMoreItems) {
                    int visibleItemCount = layoutManager.getChildCount();
                    int totalItemCount = layoutManager.getItemCount();
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                            && firstVisibleItemPosition >= 0) {
                        loadMoreItems();
                    }
                }
            }
        });

        findViewById(R.id.closeOrdersButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup); // ← Новое слева, текущее вправо
        });
    }

    private void loadMoreItems() {
        isLoading = true;
        new FetchProductsTask().execute(userId, currentPage);
        new FetchOfficeProductsTask().execute(userId, currentPage);
    }

    private static class Product {
        int id;
        String name;
        byte[] imageBytes;
        String startTime;
        String endTime;
        boolean isOfficeOrder;

        Product(int id, String name, byte[] imageBytes, String startTime, String endTime, boolean isOfficeOrder) {
            this.id = id;
            this.name = name;
            this.imageBytes = imageBytes;
            this.startTime = startTime;
            this.endTime = endTime;
            this.isOfficeOrder = isOfficeOrder;
        }
    }

    private Product fetchProductDetails(int productId, String startTime, String endTime, boolean isOfficeOrder) {
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        ResultSet resultSet = null;
        Product product = null;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                    "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                    "akku_batt_admin",
                    "8rssCz31UiUr512e"
            );

            String tableName = isOfficeOrder ? "office_products" : "products";
            String selectQuery = "SELECT name, image FROM " + tableName + " WHERE id = ?";
            preparedStatement = connection.prepareStatement(selectQuery);
            preparedStatement.setInt(1, productId);
            resultSet = preparedStatement.executeQuery();

            if (resultSet.next()) {
                String name = resultSet.getString("name");
                byte[] imageBytes = resultSet.getBytes("image");
                product = new Product(productId, name, imageBytes, startTime, endTime, isOfficeOrder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (preparedStatement != null) preparedStatement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return product;
    }

    private class FetchProductsTask extends AsyncTask<Integer, Void, List<Product>> {
        @Override
        protected List<Product> doInBackground(Integer... params) {
            int userId = params[0];
            int page = params[1];
            List<Product> products = new ArrayList<>();
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

                String selectQuery = "SELECT product_id, start_time, end_time FROM office_orders WHERE client_id = ? LIMIT ? OFFSET ?";
                preparedStatement = connection.prepareStatement(selectQuery);
                preparedStatement.setInt(1, userId);
                preparedStatement.setInt(2, ITEMS_PER_PAGE);
                preparedStatement.setInt(3, page * ITEMS_PER_PAGE);
                resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    int productId = resultSet.getInt("product_id");
                    String startTime = resultSet.getString("start_time");
                    String endTime = resultSet.getString("end_time");
                    Product product = fetchProductDetails(productId, startTime, endTime, false);
                    if (product != null) {
                        products.add(product);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return products;
        }

        @Override
        protected void onPostExecute(List<Product> newProducts) {
            if (newProducts != null && !newProducts.isEmpty()) {
                products.addAll(newProducts);
                adapter.notifyDataSetChanged();
                currentPage++;
                hasMoreItems = newProducts.size() >= ITEMS_PER_PAGE;
                productsContainer.setVisibility(View.VISIBLE);
                noOrdersText.setVisibility(View.GONE);
            } else {
                hasMoreItems = false;
                if (products.isEmpty()) {
                    noOrdersText.setVisibility(View.VISIBLE);
                }
            }
            isLoading = false;
            loadingLayout.setVisibility(View.GONE);
        }
    }

    private class FetchOfficeProductsTask extends AsyncTask<Integer, Void, List<Product>> {
        @Override
        protected List<Product> doInBackground(Integer... params) {
            int userId = params[0];
            int page = params[1];
            List<Product> products = new ArrayList<>();
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

                String selectQuery = "SELECT product_id, start_time, end_time FROM office_orders WHERE client_id = ? LIMIT ? OFFSET ?";
                preparedStatement = connection.prepareStatement(selectQuery);
                preparedStatement.setString(1, String.valueOf(userId)); // client_id is String in your table
                preparedStatement.setInt(2, ITEMS_PER_PAGE);
                preparedStatement.setInt(3, page * ITEMS_PER_PAGE);
                resultSet = preparedStatement.executeQuery();

                while (resultSet.next()) {
                    String productIdStr = resultSet.getString("product_id"); // product_id is String
                    try {
                        int productId = Integer.parseInt(productIdStr);
                        String startTime = resultSet.getString("start_time");
                        String endTime = resultSet.getString("end_time");
                        Product product = fetchProductDetails(productId, startTime, endTime, true);
                        if (product != null) {
                            products.add(product);
                        }
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        // Handle case when product_id is not a number
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (preparedStatement != null) preparedStatement.close();
                    if (connection != null) connection.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
            return products;
        }

        @Override
        protected void onPostExecute(List<Product> newProducts) {
            if (newProducts != null && !newProducts.isEmpty()) {
                products.addAll(newProducts);
                adapter.notifyDataSetChanged();
                currentPage++;
                hasMoreItems = newProducts.size() >= ITEMS_PER_PAGE;
                productsContainer.setVisibility(View.VISIBLE);
                noOrdersText.setVisibility(View.GONE);
            } else {
                hasMoreItems = false;
                if (products.isEmpty()) {
                    noOrdersText.setVisibility(View.VISIBLE);
                }
            }
            isLoading = false;
            loadingLayout.setVisibility(View.GONE);
        }
    }

    private class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
        private List<Product> products;

        ProductAdapter(List<Product> products) {
            this.products = products;
        }

        @NonNull
        @Override
        public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_history_order_card, parent, false);

            // Устанавливаем меньшие отступы для карточки
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutParams.setMargins(26, 23, 26, 13); // Уменьшаем отступы
            view.setLayoutParams(layoutParams);

            return new ProductViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
            Product product = products.get(position);
            holder.productName.setText(product.name);

            // Format the start and end times for display
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());

                // Format start time
                Date startDate = inputFormat.parse(product.startTime);
                String formattedStartTime = outputFormat.format(startDate);
                holder.startTime.setText("Время начала: " + formattedStartTime);

                // Format end time
                Date endDate = inputFormat.parse(product.endTime);
                String formattedEndTime = outputFormat.format(endDate);
                holder.endTime.setText("Конец аренды: " + formattedEndTime);
            } catch (Exception e) {
                holder.startTime.setText("Время начала: " + product.startTime);
                holder.endTime.setText("Конец аренды: " + product.endTime);
            }

            if (product.imageBytes != null && product.imageBytes.length > 0) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(product.imageBytes, 0, product.imageBytes.length);
                holder.productImage.setImageBitmap(bitmap);
            } else {
                holder.productImage.setImageResource(R.drawable.placeholder_image);
            }
        }

        @Override
        public int getItemCount() {
            return products.size();
        }

        class ProductViewHolder extends RecyclerView.ViewHolder {
            ImageView productImage;
            TextView productName;
            TextView startTime;
            TextView endTime;

            ProductViewHolder(View itemView) {
                super(itemView);
                productImage = itemView.findViewById(R.id.productImage);
                productName = itemView.findViewById(R.id.productName);
                startTime = itemView.findViewById(R.id.rentalStartTime);
                endTime = itemView.findViewById(R.id.rentalEndTime);
            }
        }
    }
}