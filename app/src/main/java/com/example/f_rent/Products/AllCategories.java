package com.example.f_rent.Products; // Замените на ваш пакет

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast; // Для демонстрации, можно убрать

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.f_rent.EntranceInApp.ChooseRole;
import com.example.f_rent.MainActivity;
import com.example.f_rent.R;

public class AllCategories extends AppCompatActivity { // Изменено имя класса

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_all_categories); // Убедитесь, что имя файла правильное

        // ВАЖНО: Убедитесь, что корневой элемент в activity_all_categories.xml имеет android:id="@+id/main"
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Находим кнопку "Назад"
        ImageView buttonBack = findViewById(R.id.buttonBack);
        // Устанавливаем обработчик клика для кнопки "Назад"
        buttonBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup);
        });

        // --- Настройка кликов для каждой категории отдельно ---
        // Все FrameLayout категорий имеют уникальные ID, поэтому находим их напрямую

        // Категория 1: Одежда (FrameLayout category_frame_1)
        LinearLayout categoryLayout1 = findViewById(R.id.category_layout_1); // Используем findViewById по ID
        // Устанавливаем клик на найденный FrameLayout
        categoryLayout1.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategories.this, ClothesClass.class); // Изменено имя класса
            intent.putExtra("CATEGORY_NAME", "Одежда");
            startActivity(intent);
        });

        // Категория 2: Инструменты (FrameLayout category_frame_2)
        LinearLayout categoryLayout2 = findViewById(R.id.category_layout_2);
        categoryLayout2.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategories.this, ToolsClass.class);
            intent.putExtra("CATEGORY_NAME", "Инструменты");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        // Категория 3: Электроника (FrameLayout category_frame_3)
        LinearLayout categoryLayout3 = findViewById(R.id.category_layout_3);
        categoryLayout3.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategories.this, NotHaveProducts.class);
            intent.putExtra("CATEGORY_NAME", "Электроника");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        // Категория 4: Для мероприятий (FrameLayout category_frame_4)
        LinearLayout categoryLayout4 = findViewById(R.id.category_layout_4);
        categoryLayout4.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategories.this, NotHaveProducts.class);
            intent.putExtra("CATEGORY_NAME", "Для мероприятий");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        // Категория 5: Для дома и быта (FrameLayout category_frame_5)
        LinearLayout categoryLayout5 = findViewById(R.id.category_layout_5);
        categoryLayout5.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategories.this, NotHaveProducts.class);
            intent.putExtra("CATEGORY_NAME", "Для дома и быта");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        // Категория 6: Спорт и отдых (FrameLayout category_frame_6)
        LinearLayout categoryLayout6 = findViewById(R.id.category_layout_6);
        categoryLayout6.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategories.this, NotHaveProducts.class);
            intent.putExtra("CATEGORY_NAME", "Спорт и отдых");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        // Категория 7: Для детей (FrameLayout category_frame_7)
        LinearLayout categoryLayout7 = findViewById(R.id.category_layout_7);
        categoryLayout7.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategories.this, NotHaveProducts.class);
            intent.putExtra("CATEGORY_NAME", "Для детей");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        // Категория 8: Аксессуары (FrameLayout category_frame_8)
        LinearLayout categoryLayout8 = findViewById(R.id.category_layout_8);
        categoryLayout8.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategories.this, NotHaveProducts.class);
            intent.putExtra("CATEGORY_NAME", "Аксессуары");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        // Категория 9: Профессиональная сфера (FrameLayout category_frame_9)
        LinearLayout categoryLayout9 = findViewById(R.id.category_layout_9);
        categoryLayout9.setOnClickListener(v -> {
            Intent intent = new Intent(AllCategories.this, NotHaveProducts.class);
            intent.putExtra("CATEGORY_NAME", "Профессиональная сфера");
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });
    }
}