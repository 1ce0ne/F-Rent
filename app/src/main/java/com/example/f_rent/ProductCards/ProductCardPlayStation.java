package com.example.f_rent.ProductCards;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.f_rent.R;

public class ProductCardPlayStation extends AppCompatActivity {

    private static final int HOURLY_PRICE = 180;  // 160₽ за час
    private static final int DAILY_PRICE = 1600;  // 1160₽ за день

    // Кнопки размеров
    private FrameLayout mediumButton, extraLargeButton;

    // Кнопки срока аренды
    private FrameLayout oneHourButton, oneDayButton, oneMonthButton;

    // Кнопки навигации
    private ImageView buttonBack, buttonShare, buttonInfo;

    // Текстовые поля
    private TextView medium, extraLarge;
    private TextView oneHour, oneDay, oneMonth;
    private TextView textHowCosts; // Для отображения цены
    private TextView textInDay;    // Для отображения единиц измерения

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_card_playstation);

        initViews();
        setClickListeners();

        // Устанавливаем начальную цену (по умолчанию 1 день)
        updatePriceDisplay(DAILY_PRICE, "/сутки");
    }

    private void initViews() {
        // Инициализация кнопок размеров
        mediumButton = findViewById(R.id.mediumButton);
        extraLargeButton = findViewById(R.id.extraLargeButton);

        // Инициализация текста размеров
        medium = findViewById(R.id.medium);
        extraLarge = findViewById(R.id.extraLarge);

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

        // Инициализация полей для отображения цены
        textHowCosts = findViewById(R.id.textHowCosts);
        textInDay = findViewById(R.id.textInDay);
    }

    private void setClickListeners() {
        // Обработчики выбора размера
        mediumButton.setOnClickListener(v -> selectSize(mediumButton, medium));
        extraLargeButton.setOnClickListener(v -> selectSize(extraLargeButton, extraLarge));

        // Обработчики выбора срока аренды с изменением цены
        oneHourButton.setOnClickListener(v -> {
            selectRental(oneHourButton, oneHour);
            updatePriceDisplay(HOURLY_PRICE, "/час");
        });

        oneDayButton.setOnClickListener(v -> {
            selectRental(oneDayButton, oneDay);
            updatePriceDisplay(DAILY_PRICE, "/сутки");
        });

        oneMonthButton.setOnClickListener(v -> {
            selectRental(oneMonthButton, oneMonth);
            // Здесь можно реализовать логику для "Свой срок"
            updatePriceDisplay(DAILY_PRICE, "/сутки"); // Пока показываем цену за день
        });

        // Обработчики навигационных кнопок
        buttonBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup);
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

        medium.setTextColor(Color.BLACK);
        extraLarge.setTextColor(Color.BLACK);
    }

    private void resetRentalButtons() {
        oneHourButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        oneDayButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        oneMonthButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));

        oneHour.setTextColor(Color.BLACK);
        oneDay.setTextColor(Color.BLACK);
        oneMonth.setTextColor(Color.BLACK);
    }

    private void updatePriceDisplay(int price, String unit) {
        if (textHowCosts != null) {
            textHowCosts.setText(price + "₽");
        }
        if (textInDay != null) {
            textInDay.setText(unit);
        }
    }
}