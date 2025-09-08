package com.example.akkubattrent.EntranceInApp;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class SplashScreenActivity extends AppCompatActivity {
    private CountDownLatch latch;
    private static final String MARKERS_PREFS = "MarkersPrefs";
    private static final String MARKERS_COUNT_KEY = "MarkersCount";
    private static final String MARKER_PREFIX = "Marker_";

    private TextView devSquadText;
    private TextView xSymbol;
    private ImageView logoImageView;
    private boolean animationCompleted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        startLoadingAnimation();
    }

    private void startLoadingAnimation() {
        ImageView devSquadImage = findViewById(R.id.devSquadImage);
        ImageView logoImageView = findViewById(R.id.logoImageView);

        // 1. Анимация для DevSquad (появление и исчезновение)
        AnimatorSet devSquadAnimator = new AnimatorSet();

        // Плавное появление (500ms)
        ObjectAnimator fadeInDevSquad = ObjectAnimator.ofFloat(devSquadImage, "alpha", 0f, 1f);
        fadeInDevSquad.setDuration(350);
        fadeInDevSquad.setInterpolator(new AccelerateDecelerateInterpolator());

        // Задержка перед исчезновением (500ms)
        ObjectAnimator stayDevSquad = ObjectAnimator.ofFloat(devSquadImage, "alpha", 1f, 1f);
        stayDevSquad.setDuration(450);

        // Плавное исчезновение (500ms)
        ObjectAnimator fadeOutDevSquad = ObjectAnimator.ofFloat(devSquadImage, "alpha", 1f, 0f);
        fadeOutDevSquad.setDuration(350);
        fadeOutDevSquad.setInterpolator(new AccelerateDecelerateInterpolator());

        devSquadAnimator.playSequentially(
                fadeInDevSquad,
                stayDevSquad,
                fadeOutDevSquad
        );

        // 2. Анимация для Akkubatt (появление после DevSquad)
        AnimatorSet akkubattAnimator = new AnimatorSet();

        // Плавное появление (500ms)
        ObjectAnimator fadeInAkkubatt = ObjectAnimator.ofFloat(logoImageView, "alpha", 0f, 1f);
        fadeInAkkubatt.setDuration(350);
        fadeInAkkubatt.setInterpolator(new AccelerateDecelerateInterpolator());

        // Просто остается на экране
        ObjectAnimator stayAkkubatt = ObjectAnimator.ofFloat(logoImageView, "alpha", 1f, 1f);
        stayAkkubatt.setDuration(90);

        akkubattAnimator.playSequentially(
                fadeInAkkubatt,
                stayAkkubatt
        );

        // Общая последовательность анимаций
        AnimatorSet fullAnimation = new AnimatorSet();
        fullAnimation.playSequentially(
                devSquadAnimator,
                akkubattAnimator
        );

        fullAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                animationCompleted = true;
                checkTechnicalWorksAndUpdates();
            }
        });

        // Показываем элементы перед анимацией
        devSquadImage.setVisibility(View.VISIBLE);
        logoImageView.setVisibility(View.VISIBLE);

        fullAnimation.start();
    }

    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void checkTechnicalWorksAndUpdates() {
        new Thread(() -> {
            try {
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";

                Connection connection = DriverManager.getConnection(url, user, password);
                String sql = "SELECT work_id, visibility_enabled FROM technical_works_and_update WHERE work_id IN (1, 2, 3)";
                PreparedStatement statement = connection.prepareStatement(sql);
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    int workId = resultSet.getInt("work_id");
                    int visibilityEnabled = resultSet.getInt("visibility_enabled");

                    if (visibilityEnabled == 1) {
                        switch (workId) {
                            case 1:
                                openTechnicalWorksWindow();
                                return;
                            case 2:
                                openAppUpdateWindow();
                                return;
                            case 3:
                                openServiceNotAvailableWindow();
                                return;
                        }
                    }
                }

                resultSet.close();
                statement.close();
                connection.close();
                runOnUiThread(this::continueWithMainChecks);

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(this::continueWithMainChecks);
            }
        }).start();
    }

    private void continueWithMainChecks() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        boolean isFirstRun = sharedPreferences.getBoolean("is_first_run", true);

        if (isFirstRun) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            // Не очищаем все данные, а только устанавливаем флаги
            editor.putBoolean("is_first_run", false);
            // Сохраняем текущее состояние входа, если оно есть
            boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);
            if (!isLoggedIn) {
                editor.putBoolean("is_logged_in", false);
            }
            editor.apply();
        }

        boolean isLoggedIn = sharedPreferences.getBoolean("is_logged_in", false);
        int taskCount = isLoggedIn ? 5 : 1;
        latch = new CountDownLatch(taskCount);

        loadPostamats();

        if (isLoggedIn) {
            String phoneNumber = sharedPreferences.getString("phone_number", "");
            int userId = sharedPreferences.getInt("id", -1);

            if (!phoneNumber.isEmpty()) {
                checkIfUserIsBanned(phoneNumber);
                checkIfAccountExists(phoneNumber);
                loadProfileImageFromDatabase(phoneNumber);
            } else {
                latch.countDown();
                latch.countDown();
                latch.countDown();
            }

            if (userId != -1) {
                loadUserOrders(userId);
            } else {
                latch.countDown();
            }
        }

        new Thread(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            new Handler(Looper.getMainLooper()).post(() -> {
                Intent intent = new Intent(SplashScreenActivity.this,
                        isLoggedIn ? MainMenuActivity.class : LoginActivity.class);
                startActivity(intent);
                finish();
                overridePendingTransition(0, R.anim.fade_out_splash);
            });
        }).start();
    }

    private void loadPostamats() {
        new Thread(() -> {
            Connection connection = null;
            PreparedStatement statement = null;
            ResultSet resultSet = null;
            try {
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";

                connection = DriverManager.getConnection(url, user, password);
                String sql = "SELECT id, address, first_coordinate, second_coordinate FROM parcel_automats";
                statement = connection.prepareStatement(sql);
                resultSet = statement.executeQuery();

                List<PostamatData> postamatList = new ArrayList<>();
                while (resultSet.next()) {
                    int postamatId = resultSet.getInt("id");
                    String address = resultSet.getString("address");
                    double firstCoordinate = resultSet.getDouble("first_coordinate");
                    double secondCoordinate = resultSet.getDouble("second_coordinate");
                    postamatList.add(new PostamatData(postamatId, address, firstCoordinate, secondCoordinate));
                }

                savePostamatsToCache(postamatList);

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (statement != null) statement.close();
                    if (connection != null) connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                latch.countDown();
            }
        }).start();
    }

    private void savePostamatsToCache(List<PostamatData> postamatList) {
        SharedPreferences prefs = getSharedPreferences(MARKERS_PREFS, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.putInt(MARKERS_COUNT_KEY, postamatList.size());

        for (int i = 0; i < postamatList.size(); i++) {
            PostamatData data = postamatList.get(i);
            String key = MARKER_PREFIX + i;
            String value = data.postamatId + "|" + data.address + "|" +
                    data.firstCoordinate + "|" + data.secondCoordinate;
            editor.putString(key, value);
        }
        editor.apply();
    }

    private static class PostamatData {
        int postamatId;
        String address;
        double firstCoordinate;
        double secondCoordinate;

        PostamatData(int postamatId, String address, double firstCoordinate, double secondCoordinate) {
            this.postamatId = postamatId;
            this.address = address;
            this.firstCoordinate = firstCoordinate;
            this.secondCoordinate = secondCoordinate;
        }
    }

    private void openTechnicalWorksWindow() {
        Intent intent = new Intent(this, TechnicalWorksActivity.class);
        startActivity(intent);
        finish();
    }

    private void openAppUpdateWindow() {
        Intent intent = new Intent(this, AppUpdateActivity.class);
        startActivity(intent);
        finish();
    }

    private void openServiceNotAvailableWindow() {
        Intent intent = new Intent(this, ServiceNotAvailableActivity.class);
        startActivity(intent);
        finish();
    }

    private void checkIfUserIsBanned(String phoneNumber) {
        new Thread(() -> {
            Connection connection = null;
            PreparedStatement statement = null;
            ResultSet resultSet = null;

            try {
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";
                connection = DriverManager.getConnection(url, user, password);

                String sql = "SELECT reason_id, ban_end_time FROM banned_users WHERE banned_user_number = ?";
                statement = connection.prepareStatement(sql);
                statement.setString(1, phoneNumber);
                resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    int reasonId = resultSet.getInt("reason_id");
                    String banEndTime = resultSet.getString("ban_end_time");
                    String reasonBanName = getBanReasonName(reasonId, connection);

                    // Если бан "Никогда" или время бана еще не истекло
                    if (banEndTime.equals("Никогда")) {
                        runOnUiThread(() -> {
                            Intent intent = new Intent(SplashScreenActivity.this, UserBanActivity.class);
                            intent.putExtra("banned_user_number", phoneNumber);
                            intent.putExtra("reason_ban_name", reasonBanName);
                            intent.putExtra("ban_end_time", "Никогда");
                            startActivity(intent);
                            finish();
                        });
                        return;
                    } else {
                        Timestamp banEndTimestamp = Timestamp.valueOf(banEndTime);
                        Timestamp currentTimestamp = new Timestamp(System.currentTimeMillis());

                        if (banEndTimestamp.after(currentTimestamp)) {
                            runOnUiThread(() -> {
                                Intent intent = new Intent(SplashScreenActivity.this, UserBanActivity.class);
                                intent.putExtra("banned_user_number", phoneNumber);
                                intent.putExtra("reason_ban_name", reasonBanName);
                                intent.putExtra("ban_end_time", banEndTime);
                                startActivity(intent);
                                finish();
                            });
                            return;
                        }
                    }
                }

                // Если пользователь не забанен или бан истек
                latch.countDown();

            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (statement != null) statement.close();
                    if (connection != null) connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void checkIfAccountExists(String phoneNumber) {
        new Thread(() -> {
            try {
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";

                Connection connection = DriverManager.getConnection(url, user, password);
                String sql = "SELECT id FROM users WHERE phone_number = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, phoneNumber);
                ResultSet resultSet = statement.executeQuery();

                if (!resultSet.next()) {
                    runOnUiThread(() -> {
                        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.clear();
                        editor.putBoolean("is_first_run", false);
                        editor.apply();

                        Intent intent = new Intent(SplashScreenActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    });
                    return;
                }

                latch.countDown();

                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
                latch.countDown();
            }
        }).start();
    }

    private String getBanReasonName(int reasonId, Connection connection) {
        try {
            String reasonSql = "SELECT reason_ban_name FROM reasons_for_ban WHERE reason_id = ?";
            PreparedStatement reasonStatement = connection.prepareStatement(reasonSql);
            reasonStatement.setInt(1, reasonId);
            ResultSet reasonResultSet = reasonStatement.executeQuery();

            if (reasonResultSet.next()) {
                return reasonResultSet.getString("reason_ban_name");
            }

            reasonResultSet.close();
            reasonStatement.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "Неизвестная причина";
    }

    private void loadProfileImageFromDatabase(String phoneNumber) {
        new Thread(() -> {
            try {
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";

                Connection connection = DriverManager.getConnection(url, user, password);
                String sql = "SELECT image FROM users WHERE phone_number = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, phoneNumber);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    byte[] imageBytes = resultSet.getBytes("image");
                    if (imageBytes != null) {
                        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                        saveImageToCache(bitmap);
                    }
                }

                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                latch.countDown();
            }
        }).start();
    }

    private void saveImageToCache(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        String encodedImage = android.util.Base64.encodeToString(baos.toByteArray(), android.util.Base64.DEFAULT);

        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("profile_image", encodedImage);
        editor.apply();
    }

    private void loadUserOrders(int userId) {
        new Thread(() -> {
            Connection connection = null;
            PreparedStatement statement = null;
            ResultSet resultSet = null;

            try {
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";

                connection = DriverManager.getConnection(url, user, password);
                String sql = "SELECT * FROM orders WHERE client_id = ? AND returned != 1";
                statement = connection.prepareStatement(sql);
                statement.setInt(1, userId);
                resultSet = statement.executeQuery();

                List<Order> orders = new ArrayList<>();
                while (resultSet.next()) {
                    int orderId = resultSet.getInt("order_id");
                    int productId = resultSet.getInt("product_id");
                    Timestamp endTime = resultSet.getTimestamp("end_time");
                    boolean isReturned = resultSet.getInt("returned") == 1;
                    int prolongationCount = resultSet.getInt("prolongation");

                    orders.add(new Order(orderId, productId, endTime, isReturned, prolongationCount));
                }

                SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("active_orders", new Gson().toJson(orders));
                editor.apply();

            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (resultSet != null) resultSet.close();
                    if (statement != null) statement.close();
                    if (connection != null) connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                latch.countDown();
            }
        }).start();
    }

    private static class Order {
        int orderId;
        int productId;
        Timestamp endTime;
        boolean isReturned;
        int prolongationCount;

        Order(int orderId, int productId, Timestamp endTime, boolean isReturned, int prolongationCount) {
            this.orderId = orderId;
            this.productId = productId;
            this.endTime = endTime;
            this.isReturned = isReturned;
            this.prolongationCount = prolongationCount;
        }
    }
}