package com.example.f_rent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.f_rent.ProductCards.ProductCardKostum;
import com.example.f_rent.Products.AllCategories;
import com.example.f_rent.Products.ClothesClass;
import com.example.f_rent.Products.ToolsClass;

import java.util.Calendar; // Добавлен импорт

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main_menu); // Предполагается, что layout файл называется activity_main_menu.xml
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- Установка приветствия в зависимости от времени ---
        TextView greetingTextView = findViewById(R.id.greeting);
        if (greetingTextView != null) {
            greetingTextView.setText(getGreetingBasedOnTime());
        }
        // ----------------------------------------------------

        // --- Настройка кликов для каждой категории по отдельности ---
        FrameLayout categoryFrame1 = findViewById(R.id.category_frame_1);
        FrameLayout categoryFrame2 = findViewById(R.id.category_frame_2);
        FrameLayout categoryFrame3 = findViewById(R.id.category_frame_3);
        FrameLayout categoryFrame4 = findViewById(R.id.category_frame_4);
        FrameLayout categoryFrame5 = findViewById(R.id.category_frame_5);
        FrameLayout categoryFrame6 = findViewById(R.id.category_frame_6);
        FrameLayout categoryFrame7 = findViewById(R.id.category_frame_7);
        FrameLayout categoryFrame8 = findViewById(R.id.category_frame_8);
        FrameLayout categoryFrame9 = findViewById(R.id.category_frame_9);
        FrameLayout product_card_2 = findViewById(R.id.product_card_2);

        View.OnClickListener categoryClickListener = v -> {
            Intent intent = new Intent(MainActivity.this, AllCategories.class);
            startActivity(intent);
        };

        View.OnClickListener clothesClickListener = v -> {
            Intent intent = new Intent(MainActivity.this, ClothesClass.class);
            startActivity(intent);
        };

        View.OnClickListener toolsClickListener = v -> {
            Intent intent = new Intent(MainActivity.this, ToolsClass.class);
            startActivity(intent);
        };

        categoryFrame1.setOnClickListener(clothesClickListener);
        categoryFrame2.setOnClickListener(toolsClickListener);
        categoryFrame3.setOnClickListener(categoryClickListener);
        categoryFrame4.setOnClickListener(categoryClickListener);
        categoryFrame5.setOnClickListener(categoryClickListener);
        categoryFrame6.setOnClickListener(categoryClickListener);
        categoryFrame7.setOnClickListener(categoryClickListener);
        categoryFrame8.setOnClickListener(categoryClickListener);
        categoryFrame9.setOnClickListener(categoryClickListener);

        View.OnClickListener product_card_kostum = v -> {
            Intent intent = new Intent(MainActivity.this, ProductCardKostum.class);
            startActivity(intent);
        };

        product_card_2.setOnClickListener(product_card_kostum);

        // --- Настройка кликов для "Смотреть все" ---
        TextView lookAllCategory1 = findViewById(R.id.lookAllCategory1);
        ImageView arrowRight1 = findViewById(R.id.arrowRight1);

        View.OnClickListener lookAllClickListener = v -> {
            // Создать Intent для перехода на ChooseRoleActivity
            Intent intent = new Intent(MainActivity.this, AllCategories.class);
            startActivity(intent);
        };

        // Установить OnClickListener для TextView "Смотреть все"
        lookAllCategory1.setOnClickListener(lookAllClickListener);

        // Установить OnClickListener для ImageView стрелки "Смотреть все"
        arrowRight1.setOnClickListener(lookAllClickListener);
    }

    // --- Метод для определения приветствия ---
    private String getGreetingBasedOnTime() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        if (hourOfDay >= 6 && hourOfDay < 12) {
            return "Доброе утро!";
        } else if (hourOfDay >= 12 && hourOfDay < 18) {
            return "Добрый день!";
        } else if (hourOfDay >= 18 && hourOfDay < 22) {
            return "Добрый вечер!";
        } else {
            // Остальные часы: 22, 23, 0, 1, 2, 3, 4, 5
            return "Доброй ночи!";
        }
    }
}