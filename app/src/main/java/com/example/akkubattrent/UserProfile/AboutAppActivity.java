package com.example.akkubattrent.UserProfile; // Убедитесь, что путь соответствует структуре вашего проекта

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;
import com.example.akkubattrent.TutorialClass.GreetingWindowActivityDouble;

public class AboutAppActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Находим кнопку "Назад"
        View backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finishWithAnimation());

        // Находим TextView для отображения версии
        TextView textViewVersion = findViewById(R.id.textViewVersion);

        // Получаем и отображаем версию приложения
        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = packageInfo.versionName;
            // Отображаем версию в нужном формате
            textViewVersion.setText("Версия приложения\n" + versionName);
        } catch (PackageManager.NameNotFoundException e) {
            textViewVersion.setText("Версия приложения\nне определена");
//            e.printStackTrace();
        }

        // Настройка кликов для кнопок меню "О приложении"
        setupMenuButtons();
    }

    private void setupMenuButtons() {
        FrameLayout helperButton = findViewById(R.id.helperButton);
        FrameLayout faqButton = findViewById(R.id.faqButton);
        FrameLayout tutorialButton = findViewById(R.id.tutorialButton);
        FrameLayout agreementButton = findViewById(R.id.agreementButton);

        helperButton.setOnClickListener(v -> {
            Intent intent = new Intent(AboutAppActivity.this, SupportActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        faqButton.setOnClickListener(v -> {
            Intent intent = new Intent(AboutAppActivity.this, FAQActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        tutorialButton.setOnClickListener(v -> {
            Intent intent = new Intent(AboutAppActivity.this, GreetingWindowActivityDouble.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        agreementButton.setOnClickListener(v -> {
            Intent intent = new Intent(AboutAppActivity.this, UsagePolicyActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });
    }

    // Метод для завершения активности с анимацией
    private void finishWithAnimation() {
        finish();
        overridePendingTransition(0, R.anim.slide_out_right_signup); // Убедитесь, что анимация slide_out_right_signup существует
    }

    // Переопределяем кнопку "Назад" системы для корректной анимации
//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        overridePendingTransition(0, R.anim.slide_out_right_signup);
//    }
}