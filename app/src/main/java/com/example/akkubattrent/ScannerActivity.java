package com.example.akkubattrent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.Classes.Product;
import com.example.akkubattrent.Postamat.PostamatActivity;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.BarcodeView;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ScannerActivity extends AppCompatActivity {
    private BarcodeView barcodeView;
    private ImageView qrFrame;
    private TextView qrInfoText;
    private boolean isScanning = false;
    private SharedPreferences sharedPreferences;
    private boolean isFlashOn = false;
    private ImageButton flashButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_scanner);

        barcodeView = findViewById(R.id.zxing_barcode_surface);
        qrFrame = findViewById(R.id.qr_frame);
        qrInfoText = findViewById(R.id.qr_info_text);
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        flashButton = findViewById(R.id.flashButton);

        // Настройка сканера
        barcodeView.decodeContinuous(new BarcodeCallback() {
            @Override
            public void barcodeResult(BarcodeResult result) {
                if (!isScanning) return;
                isScanning = false;
                String qrCode = result.getText();
                new FetchPostamatDataTask().execute(qrCode);
            }

            @Override
            public void possibleResultPoints(List<ResultPoint> resultPoints) {}
        });

        // Кнопка закрытия
        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_down_window);
        });

        // Кнопка вспышки
        flashButton.setOnClickListener(v -> toggleFlash());
    }

    private void toggleFlash() {
        if (isFlashOn) {
            barcodeView.setTorch(false);
            isFlashOn = false;
            flashButton.setImageResource(R.drawable.flash_button);
        } else {
            barcodeView.setTorch(true);
            isFlashOn = true;
            flashButton.setImageResource(R.drawable.flash_button);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        isScanning = true;
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
        isScanning = false;
        if (isFlashOn) {
            barcodeView.setTorch(false);
            isFlashOn = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        barcodeView.pause();
        if (isFlashOn) {
            barcodeView.setTorch(false);
            isFlashOn = false;
        }
    }

    // Остальной код класса остается без изменений
    private class FetchPostamatDataTask extends AsyncTask<String, Void, PostamatData> {
        @Override
        protected PostamatData doInBackground(String... params) {
            String qrCode = params[0];
            PostamatData postamatData = null;

            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );
                String query = "SELECT id, address FROM parcel_automats WHERE qr_code_id = ?";
                PreparedStatement preparedStatement = connection.prepareStatement(query);
                preparedStatement.setString(1, qrCode);
                ResultSet resultSet = preparedStatement.executeQuery();

                if (resultSet.next()) {
                    int postamatId = resultSet.getInt("id");
                    String address = resultSet.getString("address");

                    String cellsQuery = "SELECT product_id FROM cells WHERE parcel_automat_id = ?";
                    PreparedStatement cellsStatement = connection.prepareStatement(cellsQuery);
                    cellsStatement.setInt(1, postamatId);
                    ResultSet cellsResultSet = cellsStatement.executeQuery();

                    List<Integer> productIds = new ArrayList<>();
                    while (cellsResultSet.next()) {
                        productIds.add(cellsResultSet.getInt("product_id"));
                    }

                    int userId = sharedPreferences.getInt("id", -1);
                    List<Product> productList = fetchProducts(productIds);
                    postamatData = new PostamatData(address, productList, userId, postamatId);

                    cellsResultSet.close();
                    cellsStatement.close();
                }

                resultSet.close();
                preparedStatement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return postamatData;
        }

        @Override
        protected void onPostExecute(PostamatData postamatData) {
            if (postamatData != null) {
                Intent intent = new Intent(ScannerActivity.this, PostamatActivity.class);
                intent.putExtra("address", postamatData.getAddress());
                intent.putExtra("userId", postamatData.getUserId());
                intent.putExtra("postamatId", postamatData.getPostamatId());
                startActivity(intent);
                finish();
            } else {
                qrInfoText.setText("Постамат не найден");
                qrInfoText.setVisibility(TextView.VISIBLE);
                isScanning = true; // Возобновляем сканирование после ошибки
            }
        }
    }

    private List<Product> fetchProducts(List<Integer> productIds) {
        List<Product> products = new ArrayList<>();
        if (productIds.isEmpty()) return products;

        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                    "akku_batt_admin",
                    "8rssCz31UiUr512e"
            );
            Statement statement = connection.createStatement();
            String query = "SELECT id, name, description, price_per_hour, price_per_day, price_per_month, image, product_uuid, size FROM products WHERE id IN ("
                    + productIds.stream().map(String::valueOf).collect(Collectors.joining(",")) + ")";
            ResultSet resultSet = statement.executeQuery(query);

            while (resultSet.next()) {
                int id = resultSet.getInt("id");
                String name = resultSet.getString("name");
                String description = resultSet.getString("description");
                double price_per_hour = resultSet.getDouble("price_per_hour");
                double price_per_day = resultSet.getDouble("price_per_day");
                double price_per_month = resultSet.getDouble("price_per_month");
                byte[] image = resultSet.getBytes("image");
                String productUuid = resultSet.getString("product_uuid");
                String size = resultSet.getString("size");

                int cellsId = getCellsIdForProduct(connection, id);
                products.add(new Product(id, name, description, price_per_hour, price_per_day, price_per_month, image, productUuid, size, cellsId));
            }

            resultSet.close();
            statement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }

    private int getCellsIdForProduct(Connection connection, int productId) throws Exception {
        int cellsId = -1;
        String query = "SELECT id FROM cells WHERE product_id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(query);
        preparedStatement.setInt(1, productId);
        ResultSet resultSet = preparedStatement.executeQuery();

        if (resultSet.next()) {
            cellsId = resultSet.getInt("id");
        }

        resultSet.close();
        preparedStatement.close();
        return cellsId;
    }

    private static class PostamatData {
        private final String address;
        private final List<Product> products;
        private final int userId;
        private final int postamatId;

        public PostamatData(String address, List<Product> products, int userId, int postamatId) {
            this.address = address;
            this.products = products;
            this.userId = userId;
            this.postamatId = postamatId;
        }

        public String getAddress() {
            return address;
        }

        public List<Product> getProducts() {
            return products;
        }

        public int getUserId() {
            return userId;
        }

        public int getPostamatId() {
            return postamatId;
        }
    }
}