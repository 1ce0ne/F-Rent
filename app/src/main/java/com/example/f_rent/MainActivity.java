package com.example.f_rent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.f_rent.EntranceInApp.ChoosePostamatNearby;
import com.example.f_rent.ProductCards.ProductCardDrill;
import com.example.f_rent.ProductCards.ProductCardKostum;
import com.example.f_rent.ProductCards.ProductCardPlayStation;
import com.example.f_rent.Products.AllCategories;
import com.example.f_rent.Products.Basket;
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

        // --- Установка имени пользователя ---
        setUserName();

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

        FrameLayout product_card_1 = findViewById(R.id.product_card_1);
        FrameLayout product_card_2 = findViewById(R.id.product_card_2);
        FrameLayout product_card_3 = findViewById(R.id.product_card_3);
        FrameLayout product_card_4 = findViewById(R.id.product_card_4);
        FrameLayout product_card_5 = findViewById(R.id.product_card_5);
        FrameLayout product_card_6 = findViewById(R.id.product_card_6);
        FrameLayout product_card_7 = findViewById(R.id.product_card_7);
        FrameLayout product_card_8 = findViewById(R.id.product_card_8);

        ImageView button_korzina = findViewById(R.id.button_korzina);
        ImageView search_setting_icon = findViewById(R.id.search_setting_icon);
        ImageView search_icon = findViewById(R.id.search_icon);
        ImageView сhange_nearby_location = findViewById(R.id.сhange_nearby_location);
        ImageView button_user_profile = findViewById(R.id.button_user_profile);

        FrameLayout reklama_1 = findViewById(R.id.reklama_1);
        FrameLayout reklama_2 = findViewById(R.id.reklama_2);
        FrameLayout reklama_3 = findViewById(R.id.reklama_3);


        View.OnClickListener productNotUsable = v ->{
            Toast.makeText(this, "В разработке", Toast.LENGTH_SHORT).show();
        };

        product_card_1.setOnClickListener(productNotUsable);
        product_card_3.setOnClickListener(productNotUsable);
        product_card_4.setOnClickListener(productNotUsable);
        product_card_5.setOnClickListener(productNotUsable);
        product_card_6.setOnClickListener(productNotUsable);
        product_card_7.setOnClickListener(productNotUsable);
        product_card_8.setOnClickListener(productNotUsable);

        search_setting_icon.setOnClickListener(productNotUsable);
        search_icon.setOnClickListener(productNotUsable);

        View.OnClickListener reclama = v ->{
            Toast.makeText(this, "Типа акции", Toast.LENGTH_SHORT).show();
        };

        reklama_1.setOnClickListener(reclama);
        reklama_2.setOnClickListener(reclama);
        reklama_3.setOnClickListener(reclama);

        View.OnClickListener categoryClickListener = v -> {
            Intent intent = new Intent(MainActivity.this, AllCategories.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        };

        View.OnClickListener clothesClickListener = v -> {
            Intent intent = new Intent(MainActivity.this, ClothesClass.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        };

        View.OnClickListener toolsClickListener = v -> {
            Intent intent = new Intent(MainActivity.this, ToolsClass.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        };

        View.OnClickListener button_korzina_open = v -> {
            Intent intent = new Intent(MainActivity.this, Basket.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        };

        View.OnClickListener button_location = v -> {
            Intent intent = new Intent(MainActivity.this, ChoosePostamatNearby.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        };

        View.OnClickListener button_profile = v -> {
            // Получаем SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

            // Получаем роль пользователя
            String userRole = sharedPreferences.getString("user_role", "");

            // Проверяем роль и открываем соответствующую активность
            Intent intent;
            if ("buyer".equals(userRole)) {
                intent = new Intent(MainActivity.this, UserProfileBasic.class);
            } else if ("seller".equals(userRole)) {
                intent = new Intent(MainActivity.this, UserProfileSeller.class);
            } else {
                // Если роль не определена, открываем базовый профиль
                intent = new Intent(MainActivity.this, UserProfileBasic.class);
            }

            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        };

        button_user_profile.setOnClickListener(button_profile);

        сhange_nearby_location.setOnClickListener(button_location);

        button_korzina.setOnClickListener(button_korzina_open);

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
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        };

        product_card_2.setOnClickListener(product_card_kostum);


        // --- Настройка кликов для "Смотреть все" ---
        TextView lookAllCategory1 = findViewById(R.id.lookAllCategory1);
        ImageView arrowRight1 = findViewById(R.id.arrowRight1);

        View.OnClickListener lookAllClickListener = v -> {
            // Создать Intent для перехода на ChooseRoleActivity
            Intent intent = new Intent(MainActivity.this, AllCategories.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        };

        // Установить OnClickListener для TextView "Смотреть все"
        lookAllCategory1.setOnClickListener(lookAllClickListener);

        // Установить OnClickListener для ImageView стрелки "Смотреть все"
        arrowRight1.setOnClickListener(lookAllClickListener);
    }

    // --- Метод для установки имени пользователя ---
    private void setUserName() {
        // Получаем SharedPreferences с именем "user_prefs"
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        // Получаем значение user_name (по умолчанию пустая строка)
        String userName = sharedPreferences.getString("user_name", "");

        // Находим TextView some_name
        TextView someNameTextView = findViewById(R.id.some_name);

        if (someNameTextView != null) {
            if (userName != null && !userName.isEmpty()) {
                // Разбиваем строку на слова
                String[] words = userName.trim().split("\\s+");

                // Если есть хотя бы два слова, берем второе
                if (words.length >= 2) {
                    someNameTextView.setText(words[1]);
                } else {
                    // Если нет второго слова, ставим "unknow"
                    someNameTextView.setText("unknow");
                }
            } else {
                // Если user_name пустой или null, ставим "unknow"
                someNameTextView.setText("unknow");
            }
        }
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