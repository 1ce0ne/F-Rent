package com.example.akkubattrent.Utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class RentCheckService extends Service {

    private static final String CHANNEL_ID = "rent_service_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("RentCheckService", "Сервис создан");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();
        Notification notification = createNotification();
        startForeground(1, notification);
        checkRentTime(intent); // Передаем intent для получения названия товара
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("RentCheckService", "Сервис остановлен");
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Rent Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {
        // Создаем "тихое" уведомление (без текста и заголовка)
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();
    }

    private void checkRentTime(Intent intent) {
        Log.d("RentCheckService", "Проверка времени аренды...");
        String productName = intent.getStringExtra("productName"); // Получаем название товара
        sendNotification(productName); // Передаем название товара в уведомление
    }

    private void sendNotification(String productName) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Время аренды " + productName + " истекло")
                    .setContentText("Аренда была автоматически продлена.")
                    .setPriority(NotificationCompat.PRIORITY_HIGH);

            notificationManager.notify((int) System.currentTimeMillis(), builder.build()); // Используем текущее время как уникальный ID
        }
    }
}
