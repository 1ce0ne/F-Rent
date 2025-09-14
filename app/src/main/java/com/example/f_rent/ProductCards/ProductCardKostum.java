package com.example.f_rent.ProductCards;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.f_rent.MainActivity;
import com.example.f_rent.Products.AllCategories;
import com.example.f_rent.R;
import androidx.core.content.ContextCompat;

public class ProductCardKostum extends AppCompatActivity {

    // Кнопки размеров
    private FrameLayout mediumButton, extraLargeButton, extraExtraLargeButton;

    // Кнопки срока аренды
    private FrameLayout oneHourButton, oneDayButton, oneMonthButton;

    // Кнопки навигации
    private ImageView buttonBack, buttonShare, buttonInfo;

    // Текстовые поля
    private TextView medium, extraLarge, extraExtraLarge;
    private TextView oneHour, oneDay, oneMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_card_kostum);

        initViews();
        setClickListeners();
    }

    private void initViews() {
        // Инициализация кнопок размеров
        mediumButton = findViewById(R.id.mediumButton);
        extraLargeButton = findViewById(R.id.extraLargeButton);
        extraExtraLargeButton = findViewById(R.id.extraExtraLargeButton);

        // Инициализация текста размеров
        medium = findViewById(R.id.medium);
        extraLarge = findViewById(R.id.extraLarge);
        extraExtraLarge = findViewById(R.id.extraExtraLarge);

        // Инициализация кнопок срока аренды
        oneHourButton = findViewById(R.id.oneHourButton);
        oneDayButton = findViewById(R.id.oneDayButton);
        oneMonthButton = findViewById(R.id.oneMonthButton);

        // Инициализация текста аренды
        oneHour = findViewById(R.id.oneHour);
        oneDay = findViewById(R.id.oneDay);
        oneMonth = findViewById(R.id.oneMonth);

        // Инициализация кнопок навигации
        buttonBack = findViewById(R.id.buttonBack);
        buttonShare = findViewById(R.id.buttonShare);
        buttonInfo = findViewById(R.id.buttonInfo);
    }

    private void setClickListeners() {
        // Обработчики выбора размера
        mediumButton.setOnClickListener(v -> selectSize(mediumButton, medium));
        extraLargeButton.setOnClickListener(v -> selectSize(extraLargeButton, extraLarge));
        extraExtraLargeButton.setOnClickListener(v -> selectSize(extraExtraLargeButton, extraExtraLarge));

        // Обработчики выбора срока аренды
        oneHourButton.setOnClickListener(v -> selectRental(oneHourButton, oneHour));
        oneDayButton.setOnClickListener(v -> selectRental(oneDayButton, oneDay));
        oneMonthButton.setOnClickListener(v -> selectRental(oneMonthButton, oneMonth));

        // Обработчики навигационных кнопок
        // Находим кнопку "Назад"
        ImageView buttonBack = findViewById(R.id.buttonBack);
        // Устанавливаем обработчик клика для кнопки "Назад"
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Создаем Intent для перехода на MainActivity
                Intent intent = new Intent(ProductCardKostum.this, MainActivity.class); // Изменено имя класса
                startActivity(intent);
                // Закрываем текущую активность, чтобы при нажатии "Назад" в MainActivity не вернуться сюда
                finish();
            }
        });

        buttonShare.setOnClickListener(v -> Toast.makeText(this, "Поделиться", Toast.LENGTH_SHORT).show());
        buttonInfo.setOnClickListener(v -> Toast.makeText(this, "Информация", Toast.LENGTH_SHORT).show());
    }

    private void selectSize(FrameLayout button, TextView textView) {
        resetSizeButtons();
        button.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_active));
        textView.setTextColor(Color.BLACK);
    }

    private void selectRental(FrameLayout button, TextView textView) {
        resetRentalButtons();
        button.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_active));
        textView.setTextColor(Color.BLACK);
    }

    private void resetSizeButtons() {
        mediumButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        extraLargeButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        extraExtraLargeButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));

        medium.setTextColor(Color.BLACK);
        extraLarge.setTextColor(Color.BLACK);
        extraExtraLarge.setTextColor(Color.BLACK);
    }

    private void resetRentalButtons() {
        oneHourButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        oneDayButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        oneMonthButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));

        oneHour.setTextColor(Color.BLACK);
        oneDay.setTextColor(Color.BLACK);
        oneMonth.setTextColor(Color.BLACK);
    }
}