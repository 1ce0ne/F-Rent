package com.example.akkubattrent.Utils;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class OrderCheckService extends Service {
    private static final String TAG = "OrderCheckService";
    private ScheduledExecutorService scheduler;
    private SharedPreferences sharedPreferences;

    @Override
    public void onCreate() {
        super.onCreate();
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        Log.d(TAG, "Service created");
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "order_check_channel",
                    "Order Check Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, "order_check_channel")
                .setContentTitle("АккуБатт Аренда")
                .setContentText("Служба проверки заказов активна")
                .setSmallIcon(R.drawable.logotip_akkkubatt)
                .build();

        startForeground(1, notification);

        checkAndExtendOrders();
        startPeriodicCheck();

        return START_STICKY;
    }

    private void startPeriodicCheck() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(this::checkAndExtendOrders, 0, 30, TimeUnit.SECONDS);
    }

    private void checkAndExtendOrders() {
        int userId = sharedPreferences.getInt("id", -1);
        if (userId == -1) return;

        new Thread(() -> {
            try (Connection connection = DriverManager.getConnection(
                    "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                    "akku_batt_admin",
                    "8rssCz31UiUr512e")) {

                // Добавляем логирование текущего времени
                Log.d(TAG, "Текущее время сервера: " + new Timestamp(System.currentTimeMillis()));

                // Улучшенный запрос - проверяем заказы, которые уже просрочены
                String sql = "SELECT o.order_id, o.end_time, p.name " +
                        "FROM orders o JOIN products p ON o.product_id = p.id " +
                        "WHERE o.client_id = ? AND o.returned = 0 AND o.end_time <= NOW()";

                try (PreparedStatement stmt = connection.prepareStatement(sql)) {
                    stmt.setInt(1, userId);
                    ResultSet rs = stmt.executeQuery();

                    while (rs.next()) {
                        int orderId = rs.getInt("order_id");
                        Timestamp endTime = rs.getTimestamp("end_time");
                        String productName = rs.getString("name");

                        Log.d(TAG, "Найден просроченный заказ: ID=" + orderId +
                                ", Время окончания=" + endTime);

                        // Продлеваем заказ
                        extendOrder(connection, orderId, endTime, productName);
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking orders", e);
            }
        }).start();
    }

    private void extendOrder(Connection connection, int orderId, Timestamp oldEndTime, String productName) {
        try {
            // Продлеваем на 15 минут от текущего времени сервера
            String timeSql = "SELECT NOW() as current_time";
            Timestamp currentTime;

            try (PreparedStatement timeStmt = connection.prepareStatement(timeSql);
                 ResultSet rs = timeStmt.executeQuery()) {
                rs.next();
                currentTime = rs.getTimestamp("current_time");
            }

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentTime);
            calendar.add(Calendar.MINUTE, 15);
            Timestamp newEndTime = new Timestamp(calendar.getTimeInMillis());

            Log.d(TAG, "Продление заказа: ID=" + orderId +
                    ", Старое время=" + oldEndTime +
                    ", Новое время=" + newEndTime);

            // Обновляем время окончания в базе данных
            String updateSql = "UPDATE orders SET end_time = ? WHERE order_id = ?";
            try (PreparedStatement stmt = connection.prepareStatement(updateSql)) {
                stmt.setTimestamp(1, newEndTime);
                stmt.setInt(2, orderId);
                int updated = stmt.executeUpdate();

                if (updated > 0) {
                    Log.d(TAG, "Заказ успешно продлен: ID=" + orderId);

                    setNewAlarm(newEndTime, productName, orderId, true);
                    showNotification(productName, "Автоматически продлено до " + newEndTime);
                } else {
                    Log.e(TAG, "Не удалось продлить заказ: ID=" + orderId);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка продления заказа ID=" + orderId, e);
        }
    }

    private void setNewAlarm(Timestamp endTime, String productName, int orderId, boolean isAutomatic) {
        try {
            AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
            Intent intent = new Intent(this, RentAlarmReceiver.class);
            intent.putExtra("productName", productName);
            intent.putExtra("order_id", orderId);
            intent.putExtra("isAutomatic", isAutomatic);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    this,
                    orderId,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        endTime.getTime(),
                        pendingIntent
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarm", e);
        }
    }

    private void showNotification(String title, String message) {
        try {
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager == null) return;

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "order_check_channel")
                    .setSmallIcon(R.drawable.logotip_akkkubatt)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true);

            manager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (Exception e) {
            Log.e(TAG, "Error showing notification", e);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}