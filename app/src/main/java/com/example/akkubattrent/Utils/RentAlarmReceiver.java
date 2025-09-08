package com.example.akkubattrent.Utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;


public class RentAlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "RentAlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Получено уведомление о времени аренды");

        String productName = intent.getStringExtra("productName");
        int orderId = intent.getIntExtra("order_id", -1);
        boolean isAutomatic = intent.getBooleanExtra("isAutomatic", false);

        if (orderId == -1) {
            Log.e(TAG, "Неверный ID заказа");
            return;
        }

        Log.d(TAG, "Обработка заказа: ID=" + orderId +
                ", Товар=" + productName +
                ", Авто=" + isAutomatic);

        // Запускаем сервис для обработки
        Intent serviceIntent = new Intent(context, OrderCheckService.class);
        serviceIntent.putExtra("order_id", orderId);
        serviceIntent.putExtra("product_name", productName);
        serviceIntent.putExtra("is_automatic", isAutomatic);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            Log.d(TAG, "Сервис проверки заказов запущен");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при запуске сервиса: " + e.getMessage(), e);
        }
    }
}