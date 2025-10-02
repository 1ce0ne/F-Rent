package com.example.f_rent.ProductCards;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.f_rent.R;

public class ProductCardDrill extends AppCompatActivity {

    private static final int HOURLY_PRICE = 140;  // 140₽ за час
    private static final int DAILY_PRICE = 950;   // 950₽ за день

    // Кнопки размеров
    private FrameLayout mediumButton, extraLargeButton, extraExtraLargeButton, extra2, extra140;

    // Кнопки срока аренды
    private FrameLayout oneHourButton, oneDayButton, oneMonthButton;

    // Кнопки навигации
    private ImageView buttonBack, buttonShare, buttonInfo;

    // Текстовые поля
    private TextView medium, extraLarge, extraExtraLarge, extra22, extra1400;
    private TextView oneHour, oneDay, oneMonth;
    private TextView textHowCosts; // Для отображения цены
    private TextView textInDay;    // Для отображения единиц измерения

    // Для кастомного срока аренды
    private View customTimeLayout;
    private TextView timeText;
    private TextView costsText;
    private TextView minusButton;
    private TextView plusButton;
    private int customHours = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_card_drel);

        initViews();
        setClickListeners();

        // Устанавливаем начальную цену (по умолчанию 1 день)
        updatePriceDisplay(DAILY_PRICE, "/сутки");
    }

    private void initViews() {
        // Инициализация кнопок размеров
        mediumButton = findViewById(R.id.mediumButton);
        extraLargeButton = findViewById(R.id.extraLargeButton);
        extraExtraLargeButton = findViewById(R.id.extraExtraLargeButton);
        extra2 = findViewById(R.id.extra2);
        extra140 = findViewById(R.id.extra140);

        // Инициализация текста размеров
        medium = findViewById(R.id.medium);
        extraLarge = findViewById(R.id.extraLarge);
        extraExtraLarge = findViewById(R.id.extraExtraLarge);
        extra22 = findViewById(R.id.extra22);
        extra1400 = findViewById(R.id.extra1400);


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
        extraExtraLargeButton.setOnClickListener(v -> selectSize(extraExtraLargeButton, extraExtraLarge));
        extra2.setOnClickListener(v -> selectSize(extra2, extra22));
        extra140.setOnClickListener(v -> selectSize(extra140, extra1400));

        // Обработчики выбора срока аренды с изменением цены
        oneHourButton.setOnClickListener(v -> {
            selectRental(oneHourButton, oneHour);
            updatePriceDisplay(HOURLY_PRICE, "/час");
        });

        oneDayButton.setOnClickListener(v -> {
            selectRental(oneDayButton, oneDay);
            updatePriceDisplay(DAILY_PRICE, "/сутки");
        });

        oneMonthButton.setOnClickListener(v -> showCustomTimeLayout());

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
        extraExtraLargeButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        extra2.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        extra140.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));

        medium.setTextColor(Color.BLACK);
        extraLarge.setTextColor(Color.BLACK);
        extraExtraLarge.setTextColor(Color.BLACK);
        extra22.setTextColor(Color.BLACK);
        extra1400.setTextColor(Color.BLACK);
    }

    private void resetRentalButtons() {
        oneHourButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        oneDayButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));
        oneMonthButton.setBackground(ContextCompat.getDrawable(this, R.drawable.chooze_size_not_active));

        oneHour.setTextColor(Color.BLACK);
        oneDay.setTextColor(Color.BLACK);
        oneMonth.setTextColor(Color.BLACK);
    }

    private void showCustomTimeLayout() {
        // Создаем и показываем кастомный layout
        customTimeLayout = getLayoutInflater().inflate(R.layout.custom_time_for_rent,
                (ViewGroup) findViewById(android.R.id.content), false);

        // Добавляем layout в корневой контейнер
        ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
        rootView.addView(customTimeLayout);

        // Находим элементы в кастомном layout
        initCustomTimeViews();

        // Устанавливаем обработчики для кастомного layout
        setupCustomTimeClickListeners();

        // Инициализируем значения
        updateCustomTimeDisplay();
    }

    private void initCustomTimeViews() {
        if (customTimeLayout != null) {
            timeText = customTimeLayout.findViewById(R.id.time);
            costsText = customTimeLayout.findViewById(R.id.costs);
            minusButton = customTimeLayout.findViewById(R.id.minus);
            plusButton = customTimeLayout.findViewById(R.id.plus);

            // Кнопка закрытия (крестик)
            customTimeLayout.findViewById(R.id.krestYobannyi).setOnClickListener(v -> closeCustomTimeLayout());

            // Кнопка продолжить
            customTimeLayout.findViewById(R.id.frameLayout3).setOnClickListener(v -> {
                closeCustomTimeLayout();
            });
        }
    }

    private void setupCustomTimeClickListeners() {
        if (minusButton != null) {
            minusButton.setOnClickListener(v -> decreaseTime());
        }

        if (plusButton != null) {
            plusButton.setOnClickListener(v -> increaseTime());
        }
    }

    private void decreaseTime() {
        if (customHours > 1) {
            customHours--;
            updateCustomTimeDisplay();
            updateMainPriceDisplay(); // Обновляем цену на основном экране
        }
    }

    private void increaseTime() {
        if (customHours < 168) { // Ограничение на 168 часов (7 дней)
            customHours++;
            updateCustomTimeDisplay();
            updateMainPriceDisplay(); // Обновляем цену на основном экране
        }
    }

    private void updateCustomTimeDisplay() {
        if (timeText != null && costsText != null) {
            // Обновляем текст с количеством часов
            String hoursText = customHours + " " + getHoursText(customHours);
            timeText.setText(hoursText);

            // Обновляем стоимость с правильным расчетом
            int totalCost = calculateRentalCost(customHours);
            costsText.setText("+" + totalCost + "₽");
        }
    }

    private void updateMainPriceDisplay() {
        // Обновляем цену на основном экране при кастомном выборе
        int totalCost = calculateRentalCost(customHours);
        updatePriceDisplay(totalCost, "/час");
    }

    private int calculateRentalCost(int hours) {
        if (hours <= 24) {
            // До 24 часов - почасовая оплата
            return hours * HOURLY_PRICE;
        } else {
            // Более 24 часов - суточный тариф
            int days = hours / 24;
            int remainingHours = hours % 24;
            return days * DAILY_PRICE + remainingHours * HOURLY_PRICE;
        }
    }

    private String getHoursText(int hours) {
        if (hours == 1) {
            return "час";
        } else if (hours >= 2 && hours <= 4) {
            return "часа";
        } else {
            return "часов";
        }
    }

    private void closeCustomTimeLayout() {
        if (customTimeLayout != null) {
            ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
            rootView.removeView(customTimeLayout);
            customTimeLayout = null;
        }
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