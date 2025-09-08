package com.example.akkubattrent;
import android.Manifest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;

import android.util.Log;

import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.appcompat.app.AppCompatActivity;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.akkubattrent.AllCategories.AllCategoriesActivity;
import com.example.akkubattrent.UserProfile.AboutAppActivity;
import com.example.akkubattrent.UserProfile.MyOrdersActivity;
import com.example.akkubattrent.Office.OfficeActivity;
import com.example.akkubattrent.Postamat.PostamatActivity;
import com.example.akkubattrent.UserProfile.UserProfileActivity;
import com.example.akkubattrent.UserProfile.UserProfileActivityUpdate;

import org.osmdroid.config.Configuration;

import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;

import org.osmdroid.tileprovider.tilesource.TileSourceFactory;

import org.osmdroid.util.GeoPoint;

import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
public class MainMenuActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 2;
    private static final int PROLONG_ORDER_REQUEST_CODE = 3;
    private static final double DEFAULT_LATITUDE = 55.045617;
    private static final double DEFAULT_LONGITUDE = 60.107683;
    private static final double DEFAULT_ZOOM = 15.0;
    private static final String[] COMPLIMENTS = {
            "Вы сегодня прекрасны!",
            "Ваша улыбка очаровательна!",
            "Ваш стиль поистине уникален!",
            "Ваша энергия вдохновляет!",
            "Вы невероятно талантливы!"
    };
    private MapView mapView;
    private MyLocationNewOverlay myLocationOverlay;
    private ArrayList<Marker> markers = new ArrayList<>();
    private ArrayList<Marker> officeMarkers = new ArrayList<>();
    private boolean markersLoaded = false;
    private boolean locationPermissionGranted = false;
    private ImageButton locationButton;
    private Random random = new Random();
    private LinearLayout postamatInfoLayout;
    private TextView postamatName;
    private TextView postamatAddress;
    private TextView postamatProducts;
    private ImageView postamatImage;
    private LinearLayout qrInfoLayout;
    private TextView qrInfoText;
    private TextView complimentText;
    private Handler complimentHandler = new Handler();
    private Runnable updateComplimentPositionRunnable;
    private FrameLayout activeOrdersContainer;
    private List<ActiveOrder> activeOrders = new ArrayList<>();
    private int currentOrderIndex = 0;

    // Поля для новых кнопок меню пользователя
    private ImageButton userMenuIcon; // Главная кнопка меню пользователя
    private ImageButton userProfileIcon;
    private ImageButton orderHistoryIcon;
    private ImageButton settingsIcon;
    private boolean isUserMenuExpanded = false; // Флаг состояния меню пользователя

    private static class ActiveOrder {
        String productName;
        byte[] imageBytes;
        Timestamp endTime;
        int orderId;
        double pricePerHour;
        double pricePerDay;
        double pricePerMonth;
        boolean isOfficeOrder;
        String officeAddress;
        int rentalAmount;
        Timestamp startTime;
        public ActiveOrder(String productName, byte[] imageBytes, Timestamp endTime,
                           int orderId, double pricePerHour, double pricePerDay, double pricePerMonth,
                           boolean isOfficeOrder, String officeAddress, int rentalAmount, Timestamp startTime) {
            this.productName = productName;
            this.imageBytes = imageBytes;
            this.endTime = endTime;
            this.orderId = orderId;
            this.pricePerHour = pricePerHour;
            this.pricePerDay = pricePerDay;
            this.pricePerMonth = pricePerMonth;
            this.isOfficeOrder = isOfficeOrder;
            this.officeAddress = officeAddress;
            this.rentalAmount = rentalAmount;
            this.startTime = startTime;
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Configuration.getInstance().setCacheMapTileCount((short) 400);
        Configuration.getInstance().setCacheMapTileOvershoot((short) 150);
        Configuration.getInstance().load(this, androidx.preference.PreferenceManager.getDefaultSharedPreferences(this));
        setContentView(R.layout.activity_main);
        initializeViews();
        setupMap();
        setupLocationOverlay();
        setupMapListeners();
        requestPermissions();
        handleTargetCoordinates();
        if (savedInstanceState != null) {
            markersLoaded = savedInstanceState.getBoolean("markersLoaded", false);
        }
        if (!markersLoaded) {
            new FetchDataFromDatabase().execute();
        }
        loadActiveOrders();
    }
    private void initializeViews() {
        mapView = findViewById(R.id.map);
        postamatInfoLayout = findViewById(R.id.postamat_info_layout);
        postamatName = findViewById(R.id.postamat_name);
        postamatAddress = findViewById(R.id.postamat_address);
        postamatProducts = findViewById(R.id.postamat_products);
        postamatImage = findViewById(R.id.postamat_image);
        qrInfoLayout = findViewById(R.id.qr_info_layout);
        qrInfoText = findViewById(R.id.qr_info_text);
        complimentText = findViewById(R.id.compliment_text);
        locationButton = findViewById(R.id.location_button);
        activeOrdersContainer = findViewById(R.id.active_orders_container);

        // Инициализация новых кнопок
        userMenuIcon = findViewById(R.id.user_menu_icon); // Главная кнопка меню пользователя
        userProfileIcon = findViewById(R.id.user_profile_icon);
        orderHistoryIcon = findViewById(R.id.order_history_icon);
        settingsIcon = findViewById(R.id.settings_icon);

        findViewById(R.id.close_button).setOnClickListener(v -> hidePostamatInfo());
        findViewById(R.id.qr_close_button).setOnClickListener(v -> hideQrInfo());
        locationButton.setOnClickListener(v -> centerOnMyLocation());

        // Обработчик для главной кнопки меню пользователя
        userMenuIcon.setOnClickListener(v -> toggleUserMenu());

        // Обработчики для кнопок внутри меню пользователя
        userProfileIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, UserProfileActivityUpdate.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0); // Используйте нужную анимацию
        });

        orderHistoryIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, MyOrdersActivity.class); // Убедитесь, что MyOrdersActivity существует
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0); // Используйте нужную анимацию
        });

        settingsIcon.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, AboutAppActivity.class); // Убедитесь, что MyOrdersActivity существует
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0); // Используйте нужную анимацию
        });

        // Обработчик для кнопки категорий (ранее menu_icon)
        findViewById(R.id.menu_icon).setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, AllCategoriesActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_left_signup, 0);
        });

        findViewById(R.id.imageButton).setOnClickListener(v -> {
            checkCameraPermission();
            overridePendingTransition(R.anim.slide_up_window, 0);
        });

        complimentText.setVisibility(View.GONE);
        complimentText.setBackgroundResource(R.drawable.compliment_background);
    }

    private int getUserIdByPhoneNumber(Connection connection, String phoneNumber) throws SQLException {
        String sql = "SELECT id FROM users WHERE phone_number = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, phoneNumber);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        }
        return -1;
    }

    // Метод для переключения состояния меню пользователя
    private void toggleUserMenu() {
        if (isUserMenuExpanded) {
            hideUserMenuButtons();
        } else {
            showUserMenuButtons();
        }
        isUserMenuExpanded = !isUserMenuExpanded;
    }

    // Метод для показа кнопок меню пользователя с анимацией
    private void showUserMenuButtons() {
        userProfileIcon.setVisibility(View.VISIBLE);
        orderHistoryIcon.setVisibility(View.VISIBLE);
        settingsIcon.setVisibility(View.VISIBLE);

        // Анимация появления (fade in)
        userProfileIcon.animate().alpha(1.0f).setDuration(300).start();
        orderHistoryIcon.animate().alpha(1.0f).setDuration(300).start();
        settingsIcon.animate().alpha(1.0f).setDuration(300).start();

        // Дополнительная анимация масштабирования для эффекта
        userProfileIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
        orderHistoryIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
        settingsIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
    }

    // Метод для скрытия кнопок меню пользователя с анимацией
    private void hideUserMenuButtons() {
        // Анимация исчезновения (fade out)
        userProfileIcon.animate().alpha(0.0f).setDuration(300)
                .withEndAction(() -> {
                    if (!isUserMenuExpanded) { // Проверка состояния, чтобы избежать конфликтов
                        userProfileIcon.setVisibility(View.INVISIBLE);
                    }
                }).start();
        orderHistoryIcon.animate().alpha(0.0f).setDuration(300)
                .withEndAction(() -> {
                    if (!isUserMenuExpanded) {
                        orderHistoryIcon.setVisibility(View.INVISIBLE);
                    }
                }).start();
        settingsIcon.animate().alpha(0.0f).setDuration(300)
                .withEndAction(() -> {
                    if (!isUserMenuExpanded) {
                        settingsIcon.setVisibility(View.INVISIBLE);
                    }
                }).start();

        // Возвращаем масштаб к 1.0 при скрытии
        userProfileIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
        orderHistoryIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
        settingsIcon.animate().scaleX(1.0f).scaleY(1.0f).setDuration(300).start();
    }

    private void loadActiveOrders() {
        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String phoneNumber = prefs.getString("phone_number", null);
        if (phoneNumber == null) {
            Log.e("ActiveOrdersDebug", "Phone number not found in SharedPreferences");
            runOnUiThread(() -> Toast.makeText(this, "Ошибка: номер телефона не найден", Toast.LENGTH_SHORT).show());
            return;
        }
        new Thread(() -> {
            try (Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                    "akku_batt_admin",
                    "8rssCz31UiUr512e")) {
                int userId = getUserIdByPhoneNumber(connection, phoneNumber);
                if (userId == -1) {
                    Log.e("ActiveOrdersDebug", "User not found in database");
                    runOnUiThread(() -> Toast.makeText(MainMenuActivity.this, "Пользователь не найден", Toast.LENGTH_SHORT).show());
                    return;
                }
                activeOrders.clear();
//                loadRegularOrders(connection, userId);
                loadOfficeOrders(connection, userId);
                runOnUiThread(this::updateOrdersUI);
            } catch (Exception e) {
                Log.e("DatabaseError", "Ошибка загрузки заказов", e);
                runOnUiThread(() -> {
                    Toast.makeText(MainMenuActivity.this, "Ошибка загрузки заказов", Toast.LENGTH_SHORT).show();
                    activeOrdersContainer.setVisibility(View.GONE);
                });
            }
        }).start();
    }
    //    private void loadRegularOrders(Connection connection, int userId) throws Exception {
//        String sql = "SELECT o.order_id, o.product_id, o.end_time, o.start_time, " +
//                "p.name, p.image, p.price_per_hour, o.rental_amount " +
//                "FROM orders o JOIN products p ON o.product_id = p.id " +
//                "WHERE o.client_id = ? AND o.returned = 0";
//
//        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
//            stmt.setInt(1, userId);
//            ResultSet rs = stmt.executeQuery();
//
//            while (rs.next()) {
//                activeOrders.add(new ActiveOrder(
//                        rs.getString("name"),
//                        rs.getBytes("image"),
//                        rs.getTimestamp("end_time"),
//                        rs.getInt("order_id"),
//                        rs.getDouble("price_per_hour"),
//                        false,
//                        null,
//                        rs.getInt("rental_amount"),
//                        rs.getTimestamp("start_time")
//                ));
//            }
//        }
//    }
    private void loadOfficeOrders(Connection connection, int userId) throws Exception {
        String sql = "SELECT o.order_id, o.product_id, o.end_time, o.start_time, " +
                "p.name, p.image, p.price_per_hour, p.price_per_day, p.price_per_month, " +  // Added comma here
                "o.address_office, o.rental_amount " +
                "FROM office_orders o JOIN products p ON o.product_id = p.id " +
                "WHERE o.client_id = ? AND o.returned = 0";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                activeOrders.add(new ActiveOrder(
                        rs.getString("name"),
                        rs.getBytes("image"),
                        rs.getTimestamp("end_time"),
                        rs.getInt("order_id"),
                        rs.getDouble("price_per_hour"),
                        rs.getDouble("price_per_day"),
                        rs.getDouble("price_per_month"),
                        true,
                        rs.getString("address_office"),
                        rs.getInt("rental_amount"),
                        rs.getTimestamp("start_time")
                ));
            }
        }
    }
    private void updateOrdersUI() {
        runOnUiThread(() -> {
            if (activeOrders != null && !activeOrders.isEmpty()) {
                activeOrdersContainer.setVisibility(View.VISIBLE);
                currentOrderIndex = 0;
                showCurrentOrder();
            } else {
                activeOrdersContainer.setVisibility(View.INVISIBLE);
                Log.d("ActiveOrdersDebug", "No active orders to display");
            }
        });
    }
    private void showCurrentOrder() {
        if (activeOrders.isEmpty()) {
            activeOrdersContainer.setVisibility(View.INVISIBLE);
            Log.d("ActiveOrdersDebug", "No orders to show");
            return;
        }
        ActiveOrder currentOrder = activeOrders.get(currentOrderIndex);
        Log.d("ActiveOrdersDebug", "Showing order: " + currentOrder.productName);
        ImageView productImage = activeOrdersContainer.findViewById(R.id.productImage);
        TextView productName = activeOrdersContainer.findViewById(R.id.productName);
        TextView timeOfRent = activeOrdersContainer.findViewById(R.id.timeOfRent);
        TextView costsOfProduct = activeOrdersContainer.findViewById(R.id.costsOfProduct);
        Button backButton = activeOrdersContainer.findViewById(R.id.backButton);
        Button nextButton = activeOrdersContainer.findViewById(R.id.nextButton);
        productName.setText(currentOrder.productName);
        if (currentOrder.isOfficeOrder) {
            timeOfRent.setText(String.format(Locale.getDefault(),
                    "До: %s Офис: %s Сумма: %d руб.",
            formatEndTime(currentOrder.endTime),
                    currentOrder.officeAddress,
                    currentOrder.rentalAmount));
        } else {
            timeOfRent.setText(String.format(Locale.getDefault(),
                    "До: %s Сумма: %d руб.",
            formatEndTime(currentOrder.endTime),
                    currentOrder.rentalAmount));
        }
        costsOfProduct.setText(String.format(Locale.getDefault(), "%.0fР/Час", currentOrder.pricePerHour));
        if (currentOrder.imageBytes != null) {
            try {
                Bitmap bitmap = BitmapFactory.decodeByteArray(
                        currentOrder.imageBytes, 0, currentOrder.imageBytes.length);
                productImage.setImageBitmap(bitmap);
            } catch (Exception e) {
                Log.e("ActiveOrdersDebug", "Error decoding image", e);
                productImage.setImageResource(R.drawable.placeholder_image);
            }
        } else {
            productImage.setImageResource(R.drawable.placeholder_image);
        }
        backButton.setVisibility(currentOrderIndex > 0 ? View.VISIBLE : View.INVISIBLE);
        nextButton.setVisibility(currentOrderIndex < activeOrders.size() - 1 ? View.VISIBLE : View.INVISIBLE);
        backButton.setOnClickListener(v -> {
            if (currentOrderIndex > 0) {
                currentOrderIndex--;
                showCurrentOrder();
            }
        });
        nextButton.setOnClickListener(v -> {
            if (currentOrderIndex < activeOrders.size() - 1) {
                currentOrderIndex++;
                showCurrentOrder();
            }
        });
        activeOrdersContainer.setVisibility(View.VISIBLE);
        setupOrderClickListener(currentOrder);
    }
    private void setupOrderClickListener(ActiveOrder currentOrder) {
        activeOrdersContainer.setOnClickListener(v -> {
            Intent intent = new Intent(MainMenuActivity.this, ProlongOrderActivity.class);
            intent.putExtra("orderId", currentOrder.orderId);
            intent.putExtra("isOfficeOrder", currentOrder.isOfficeOrder);
            intent.putExtra("pricePerHour", currentOrder.pricePerHour);
            intent.putExtra("pricePerDay", currentOrder.pricePerDay);
            intent.putExtra("pricePerMonth", currentOrder.pricePerMonth);
            intent.putExtra("endTime", currentOrder.endTime);
            intent.putExtra("startTime", currentOrder.startTime);
            intent.putExtra("rentalAmount", currentOrder.rentalAmount);
            intent.putExtra("productName", currentOrder.productName);
            intent.putExtra("imageBytes", currentOrder.imageBytes);
            startActivityForResult(intent, PROLONG_ORDER_REQUEST_CODE);
            overridePendingTransition(R.anim.slide_up_window, 0);
        });
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PROLONG_ORDER_REQUEST_CODE && resultCode == RESULT_OK) {
            loadActiveOrders();
        }
    }
    private String formatEndTime(Timestamp endTime) {
        return new SimpleDateFormat("HH:mm, dd MMMM", Locale.getDefault()).format(endTime);
    }
    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(DEFAULT_ZOOM);
        mapView.getController().setCenter(new GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE));
    }
    private void setupLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this), mapView) {
            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent e, MapView mapView) {
                if (isMyLocationEnabled() && getMyLocation() != null) {
                    android.graphics.Point screenPosition = mapView.getProjection().toPixels(getMyLocation(), null);
                    float distance = (float) Math.sqrt(Math.pow(e.getX() - screenPosition.x, 2) + Math.pow(e.getY() - screenPosition.y, 2));
                    if (distance < 100) {
                        showRandomCompliment();
                        return true;
                    }
                }
                return super.onSingleTapConfirmed(e, mapView);
            }
        };
        Drawable locationIcon = ContextCompat.getDrawable(this, R.drawable.my_location_icon);
        if (locationIcon != null) {
            Bitmap bitmap = Bitmap.createBitmap(125, 165, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            locationIcon.setBounds(0, 0, 125, 165);
            locationIcon.draw(canvas);
            myLocationOverlay.setPersonIcon(new BitmapDrawable(getResources(), bitmap).getBitmap());
            myLocationOverlay.setPersonAnchor(0.5f, 1.0f);
        }
        mapView.getOverlays().add(myLocationOverlay);
    }
    private void setupMapListeners() {
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                updateComplimentPosition();
                return true;
            }
            @Override
            public boolean onZoom(ZoomEvent event) {
                updateComplimentPosition();
                return true;
            }
        });
        updateComplimentPositionRunnable = new Runnable() {
            @Override
            public void run() {
                updateComplimentPosition();
                complimentHandler.postDelayed(this, 100);
            }
        };
    }
    private void showRandomCompliment() {
        if (myLocationOverlay.getMyLocation() == null) return;
        String compliment = COMPLIMENTS[random.nextInt(COMPLIMENTS.length)];
        complimentHandler.removeCallbacksAndMessages(null);
        AlphaAnimation fadeIn = new AlphaAnimation(0.0f, 1.0f);
        fadeIn.setDuration(300);
        fadeIn.setFillAfter(true);
        complimentText.setText(compliment);
        complimentText.startAnimation(fadeIn);
        complimentText.setVisibility(View.VISIBLE);
        updateComplimentPosition();
        complimentHandler.postDelayed(updateComplimentPositionRunnable, 100);
        complimentHandler.postDelayed(() -> {
            AlphaAnimation fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeOut.setDuration(300);
            fadeOut.setFillAfter(true);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override public void onAnimationStart(Animation animation) {}
                @Override public void onAnimationEnd(Animation animation) {
                    complimentText.setVisibility(View.GONE);
                    complimentHandler.removeCallbacks(updateComplimentPositionRunnable);
                }
                @Override public void onAnimationRepeat(Animation animation) {}
            });
            complimentText.startAnimation(fadeOut);
        }, 2000);
    }
    private void updateComplimentPosition() {
        if (complimentText.getVisibility() != View.VISIBLE || myLocationOverlay.getMyLocation() == null) return;
        android.graphics.Point screenPosition = mapView.getProjection().toPixels(myLocationOverlay.getMyLocation(), null);
        int x = screenPosition.x + 50;
        int y = screenPosition.y - 50;
        int textWidth = complimentText.getWidth();
        if (x + textWidth > mapView.getWidth()) x = mapView.getWidth() - textWidth - 20;
        if (y < 0) y = 20;
        complimentText.setX(x);
        complimentText.setY(y);
    }
    private void centerOnMyLocation() {
        if (locationPermissionGranted) {
            if (myLocationOverlay.getMyLocation() != null) {
                mapView.getController().animateTo(myLocationOverlay.getMyLocation(), DEFAULT_ZOOM, 1000L);
            } else {
                Toast.makeText(this, "Определение местоположения...", Toast.LENGTH_SHORT).show();
                myLocationOverlay.enableMyLocation();
            }
        } else {
            Toast.makeText(this, "Необходимо дать разрешение на геолокацию", Toast.LENGTH_SHORT).show();
            requestPermissions();
        }
    }
    private void checkCameraPermission() {
        if (ActivityCompat.checkSelfPermission(MainMenuActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainMenuActivity.this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            startActivity(new Intent(MainMenuActivity.this, ScannerActivity.class));
        }
    }
    private void requestPermissions() {
        List<String> permissionsToRequest = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.CAMERA);
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            locationPermissionGranted = true;
            myLocationOverlay.enableMyLocation();
        }
    }
    private void addPostamatMarker(GeoPoint point, String title, String address, int postamatId, double firstCoordinate, double secondCoordinate) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        Drawable originalDrawable = ContextCompat.getDrawable(this, R.drawable.postamat);
        Drawable scaledDrawable = getScaledDrawable(originalDrawable, 115, 150);
        marker.setIcon(scaledDrawable);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setRelatedObject(new PostamatInfo(address, postamatId, firstCoordinate, secondCoordinate));
        markers.add(marker);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
        marker.setOnMarkerClickListener((marker1, mapView1) -> {
            showPostamatInfo(marker1);
            return true;
        });
    }
    private void addOfficeMarker(GeoPoint point, String title, String address,
                                 int officeId, double firstCoordinate,
                                 double secondCoordinate, boolean isTarget) {
        Marker marker = new Marker(mapView);
        marker.setPosition(point);
        marker.setTitle(title);
        Drawable originalDrawable = ContextCompat.getDrawable(this, R.drawable.office_point);
        Drawable scaledDrawable = getScaledDrawable(originalDrawable, 115, 150);
        marker.setIcon(scaledDrawable);
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        marker.setRelatedObject(new OfficeInfo(address, officeId,
                firstCoordinate, secondCoordinate, isTarget));
        officeMarkers.add(marker);
        mapView.getOverlays().add(marker);
        mapView.invalidate();
        if (isTarget) {
            animateMarker(marker);
        }
        marker.setOnMarkerClickListener((marker1, mapView1) -> {
            showOfficeInfo(marker1);
            return true;
        });
    }
    private Drawable getScaledDrawable(Drawable drawable, int widthPx, int heightPx) {
        if (drawable == null) return null;
        Bitmap bitmap = Bitmap.createBitmap(
                widthPx,
                heightPx,
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        Rect srcRect = new Rect(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        Rect dstRect = new Rect(0, 0, widthPx, heightPx);
        drawable.setBounds(dstRect);
        drawable.draw(canvas);
        BitmapDrawable scaledDrawable = new BitmapDrawable(getResources(), bitmap);
        scaledDrawable.setBounds(0, 0, widthPx, heightPx);
        return scaledDrawable;
    }
    private void showPostamatInfo(Marker marker) {
        PostamatInfo info = (PostamatInfo) marker.getRelatedObject();
        Intent intent = new Intent(MainMenuActivity.this, PostamatActivity.class);
        intent.putExtra("postamatId", info.getPostamatId());
        intent.putExtra("address", info.getAddress());
        intent.putExtra("firstCoordinate", info.getFirstCoordinate());
        intent.putExtra("secondCoordinate", info.getSecondCoordinate());
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        intent.putExtra("userId", userId);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_up_window, 0);
    }
    private void showOfficeInfo(Marker marker) {
        OfficeInfo info = (OfficeInfo) marker.getRelatedObject();
        Intent intent = new Intent(MainMenuActivity.this, OfficeActivity.class);
        intent.putExtra("officeId", info.getOfficeId());
        intent.putExtra("address", info.getAddress());
        intent.putExtra("firstCoordinate", info.getFirstCoordinate());
        intent.putExtra("secondCoordinate", info.getSecondCoordinate());
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        int userId = prefs.getInt("userId", -1);
        intent.putExtra("userId", userId);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_up_window, 0);
    }
    private void hidePostamatInfo() {
        postamatInfoLayout.animate()
                .translationY(postamatInfoLayout.getHeight())
                .setDuration(300)
                .withEndAction(() -> {
                    postamatInfoLayout.setVisibility(View.GONE);
                    postamatImage.setVisibility(View.GONE);
                });
    }
    private void showQrInfo(String info) {
        qrInfoText.setText(info);
        qrInfoLayout.setVisibility(View.VISIBLE);
        qrInfoLayout.setTranslationY(qrInfoLayout.getHeight());
        qrInfoLayout.animate()
                .translationY(0)
                .setDuration(300)
                .start();
    }
    private void hideQrInfo() {
        qrInfoLayout.animate()
                .translationY(qrInfoLayout.getHeight())
                .setDuration(300)
                .withEndAction(() -> qrInfoLayout.setVisibility(View.GONE));
    }
    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
        myLocationOverlay.enableMyLocation();
        loadActiveOrders();
        // При возвращении на активность, если меню было открыто, скрываем его
        if (isUserMenuExpanded) {
            hideUserMenuButtons();
            isUserMenuExpanded = false;
        }
    }
    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        myLocationOverlay.disableMyLocation();
        complimentHandler.removeCallbacksAndMessages(null);
    }
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("markersLoaded", markersLoaded);
        // Сохраняем состояние меню
        outState.putBoolean("isUserMenuExpanded", isUserMenuExpanded);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Восстанавливаем состояние меню
        isUserMenuExpanded = savedInstanceState.getBoolean("isUserMenuExpanded", false);
        // Если меню было открыто, показываем кнопки
        if (isUserMenuExpanded) {
            // Устанавливаем видимость и альфа без анимации, так как состояние восстанавливается
            userProfileIcon.setVisibility(View.VISIBLE);
            orderHistoryIcon.setVisibility(View.VISIBLE);
            settingsIcon.setVisibility(View.VISIBLE);
            userProfileIcon.setAlpha(1.0f);
            orderHistoryIcon.setAlpha(1.0f);
            settingsIcon.setAlpha(1.0f);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDetach();
        complimentHandler.removeCallbacksAndMessages(null);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean locationGranted = false;
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.ACCESS_FINE_LOCATION) &&
                        grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    locationGranted = true;
                    break;
                }
            }
            if (locationGranted) {
                locationPermissionGranted = true;
                myLocationOverlay.enableMyLocation();
            } else {
                Toast.makeText(this, "Для работы с картой необходимо разрешение на геолокацию", Toast.LENGTH_SHORT).show();
            }
        }
    }
    private class FetchDataFromDatabase extends AsyncTask<Void, Void, DatabaseData> {
        @Override
        protected DatabaseData doInBackground(Void... voids) {
            ArrayList<PostamatData> postamatDataList = new ArrayList<>();
            ArrayList<OfficeData> officeDataList = new ArrayList<>();
            try {
                Class.forName("com.mysql.jdbc.Driver");
                Connection connection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e"
                );
                Statement statement = connection.createStatement();
                ResultSet postamatResultSet = statement.executeQuery("SELECT id, address, first_coordinate, second_coordinate FROM parcel_automats");
                while (postamatResultSet.next()) {
                    int postamatId = postamatResultSet.getInt("id");
                    String address = postamatResultSet.getString("address");
                    double firstCoordinate = postamatResultSet.getDouble("first_coordinate");
                    double secondCoordinate = postamatResultSet.getDouble("second_coordinate");
                    postamatDataList.add(new PostamatData(postamatId, address, firstCoordinate, secondCoordinate));
                }
                ResultSet officeResultSet = statement.executeQuery("SELECT id, address, first_coordinate, second_coordinate FROM office");
                while (officeResultSet.next()) {
                    int officeId = officeResultSet.getInt("id");
                    String address = officeResultSet.getString("address");
                    double firstCoordinate = officeResultSet.getDouble("first_coordinate");
                    double secondCoordinate = officeResultSet.getDouble("second_coordinate");
                    officeDataList.add(new OfficeData(officeId, address, firstCoordinate, secondCoordinate));
                }
                postamatResultSet.close();
                officeResultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return new DatabaseData(postamatDataList, officeDataList);
        }
        @Override
        protected void onPostExecute(DatabaseData databaseData) {
            for (Marker marker : markers) {
                mapView.getOverlays().remove(marker);
            }
            markers.clear();
            for (Marker marker : officeMarkers) {
                mapView.getOverlays().remove(marker);
            }
            officeMarkers.clear();
            // Получаем целевые координаты из Intent
            Intent intent = getIntent();
            double targetLat = intent != null ? intent.getDoubleExtra("targetLatitude", 0) : 0;
            double targetLon = intent != null ? intent.getDoubleExtra("targetLongitude", 0) : 0;
            for (OfficeData data : databaseData.getOfficeDataList()) {
                boolean isTarget = (Math.abs(data.getFirstCoordinate() - targetLat) < 0.0001 &&
                        Math.abs(data.getSecondCoordinate() - targetLon) < 0.0001);
                addOfficeMarker(new GeoPoint(data.getFirstCoordinate(), data.getSecondCoordinate()),
                        "Офис", data.getAddress(), data.getOfficeId(),
                        data.getFirstCoordinate(), data.getSecondCoordinate(), isTarget);
            }
            markersLoaded = true;
            mapView.invalidate();
        }
    }
    private void handleTargetCoordinates() {
        Intent intent = getIntent();
        if (intent != null &&
                intent.hasExtra("targetLatitude") &&
                intent.hasExtra("targetLongitude")) {
            double latitude = intent.getDoubleExtra("targetLatitude", 0.0);
            double longitude = intent.getDoubleExtra("targetLongitude", 0.0);
            if (latitude != 0.0 && longitude != 0.0) {
                mapView.getController().animateTo(new GeoPoint(latitude, longitude));
                // Помечаем маркер как целевой при создании
                new Handler().postDelayed(() -> {
                    for (Marker marker : officeMarkers) {
                        GeoPoint point = marker.getPosition();
                        if (Math.abs(point.getLatitude() - latitude) < 0.0001 &&
                                Math.abs(point.getLongitude() - longitude) < 0.0001) {
                            OfficeInfo info = (OfficeInfo) marker.getRelatedObject();
                            marker.setRelatedObject(new OfficeInfo(
                                    info.getAddress(), info.getOfficeId(),
                                    info.getFirstCoordinate(), info.getSecondCoordinate(),
                                    true // Помечаем как целевой
                            ));
                            animateMarker(marker);
                            break;
                        }
                    }
                }, 500); // Небольшая задержка для гарантии загрузки маркеров
            }
        }
    }
    private void animateTargetMarker(double latitude, double longitude) {
        for (Marker marker : markers) {
            GeoPoint point = marker.getPosition();
            if (point.getLatitude() == latitude && point.getLongitude() == longitude) {
                animateMarker(marker);
                break;
            }
        }
        for (Marker marker : officeMarkers) {
            GeoPoint point = marker.getPosition();
            if (point.getLatitude() == latitude && point.getLongitude() == longitude) {
                animateMarker(marker);
                break;
            }
        }
    }
    private void animateMarker(final Marker marker) {
        final Handler handler = new Handler();
        final long duration = 370; // длительность одной фазы анимации (мс)
        final float scaleUp = 1.15f; // коэффициент увеличения
        final float scaleDown = 1.0f; // обычный размер
        final int repeatCount = 3; // количество циклов анимации
        // Получаем оригинальный Drawable маркера
        final Drawable originalIcon = marker.getIcon();
        final int originalWidth = originalIcon.getIntrinsicWidth();
        final int originalHeight = originalIcon.getIntrinsicHeight();
        Runnable animation = new Runnable() {
            int count = 0;
            @Override
            public void run() {
                if (count >= repeatCount * 2) {
                    // Возвращаем оригинальный размер
                    marker.setIcon(originalIcon);
                    return;
                }
                // Чередуем увеличение и уменьшение
                float scale = (count % 2 == 0) ? scaleUp : scaleDown;
                // Создаем скалированную версию иконки
                Bitmap bitmap = Bitmap.createBitmap(
                        (int)(originalWidth * scale),
                        (int)(originalHeight * scale),
                        Bitmap.Config.ARGB_8888
                );
                Canvas canvas = new Canvas(bitmap);
                originalIcon.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                originalIcon.draw(canvas);
                // Устанавливаем новую иконку
                marker.setIcon(new BitmapDrawable(getResources(), bitmap));
                mapView.invalidate();
                count++;
                handler.postDelayed(this, duration);
            }
        };
        // Запускаем анимацию
        handler.post(animation);
    }
    private static class PostamatInfo {
        private String address;
        private int postamatId;
        private double firstCoordinate;
        private double secondCoordinate;
        public PostamatInfo(String address, int postamatId, double firstCoordinate, double secondCoordinate) {
            this.address = address;
            this.postamatId = postamatId;
            this.firstCoordinate = firstCoordinate;
            this.secondCoordinate = secondCoordinate;
        }
        public String getAddress() {
            return address;
        }
        public int getPostamatId() {
            return postamatId;
        }
        public double getFirstCoordinate() {
            return firstCoordinate;
        }
        public double getSecondCoordinate() {
            return secondCoordinate;
        }
    }
    private static class OfficeInfo {
        private String address;
        private int officeId;
        private double firstCoordinate;
        private double secondCoordinate;
        private boolean isTarget;
        public OfficeInfo(String address, int officeId, double firstCoordinate,
                          double secondCoordinate, boolean isTarget) {
            this.address = address;
            this.officeId = officeId;
            this.firstCoordinate = firstCoordinate;
            this.secondCoordinate = secondCoordinate;
            this.isTarget = isTarget;
        }
        public String getAddress() {
            return address;
        }
        public int getOfficeId() {
            return officeId;
        }
        public double getFirstCoordinate() {
            return firstCoordinate;
        }
        public double getSecondCoordinate() {
            return secondCoordinate;
        }
        public boolean isTarget() {
            return isTarget;
        }
    }
    private static class PostamatData {
        private int postamatId;
        private String address;
        private double firstCoordinate;
        private double secondCoordinate;
        public PostamatData(int postamatId, String address, double firstCoordinate, double secondCoordinate) {
            this.postamatId = postamatId;
            this.address = address;
            this.firstCoordinate = firstCoordinate;
            this.secondCoordinate = secondCoordinate;
        }
        public int getPostamatId() {
            return postamatId;
        }
        public String getAddress() {
            return address;
        }
        public double getFirstCoordinate() {
            return firstCoordinate;
        }
        public double getSecondCoordinate() {
            return secondCoordinate;
        }
    }
    private static class OfficeData {
        private int officeId;
        private String address;
        private double firstCoordinate;
        private double secondCoordinate;
        public OfficeData(int officeId, String address, double firstCoordinate, double secondCoordinate) {
            this.officeId = officeId;
            this.address = address;
            this.firstCoordinate = firstCoordinate;
            this.secondCoordinate = secondCoordinate;
        }
        public int getOfficeId() {
            return officeId;
        }
        public String getAddress() {
            return address;
        }
        public double getFirstCoordinate() {
            return firstCoordinate;
        }
        public double getSecondCoordinate() {
            return secondCoordinate;
        }
    }
    private static class DatabaseData {
        private ArrayList<PostamatData> postamatDataList;
        private ArrayList<OfficeData> officeDataList;
        public DatabaseData(ArrayList<PostamatData> postamatDataList, ArrayList<OfficeData> officeDataList) {
            this.postamatDataList = postamatDataList;
            this.officeDataList = officeDataList;
        }
        public ArrayList<PostamatData> getPostamatDataList() {
            return postamatDataList;
        }
        public ArrayList<OfficeData> getOfficeDataList() {
            return officeDataList;
        }
    }
}