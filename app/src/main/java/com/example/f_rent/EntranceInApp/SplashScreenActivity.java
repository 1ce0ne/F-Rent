package com.example.f_rent.EntranceInApp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.f_rent.MainActivity;
import com.example.f_rent.R;

public class SplashScreenActivity extends AppCompatActivity {

    private static final int SPLASH_DELAY = 1000; // 1 секунда

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        // Установка версии приложения
        setAppVersion();

        // Задержка и переход на MainActivity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkUserLoginAndStartActivity();
            }
        }, SPLASH_DELAY);
    }

    private void setAppVersion() {
        TextView versionText = findViewById(R.id.textOfVersionApp);
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            versionText.setText(versionName);
        } catch (PackageManager.NameNotFoundException e) {
            versionText.setText("Ошибка"); // значение по умолчанию
        }
    }

    private void checkUserLoginAndStartActivity() {
        // Получаем SharedPreferences с именем "user_prefs"
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Проверяем значение user_login (по умолчанию false если ключа нет)
        boolean isUserLoggedIn = sharedPreferences.getBoolean("user_login", false);

        Intent intent;
        if (isUserLoggedIn) {
            // Если пользователь залогинен - переходим на MainActivity
            intent = new Intent(SplashScreenActivity.this, MainActivity.class);
        } else {
            // Если пользователь не залогинен - переходим на EntranceInApp
            intent = new Intent(SplashScreenActivity.this, EntranceInApp.class);
        }

        startActivity(intent);
        // Анимация перехода - старое активити пропадает, новое появляется
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish(); // Завершаем SplashScreenActivity
    }
}