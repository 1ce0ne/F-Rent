package com.example.f_rent.ProductCards;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.f_rent.Products.Basket;
import com.example.f_rent.R;

public class ProductCardKostum extends AppCompatActivity {

    private static final int HOURLY_PRICE = 20;  // 160₽ за час
    private static final int DAILY_PRICE = 340;  // 1160₽ за день

    private FrameLayout oneHourButton;
    private FrameLayout oneDayButton;
    private FrameLayout oneMonthButton;
    private FrameLayout mediumButton;
    private FrameLayout extraLargeButton;
    private FrameLayout extraExtraLargeButton;
    private TextView oneHourText;
    private TextView oneDayText;
    private TextView oneMonthText;
    private TextView mediumText;
    private TextView extraLargeText;
    private TextView extraExtraLargeText;
    private TextView textHowCosts;
    private TextView textInDay;
    private boolean isHourlySelected = false;

    // Для кастомного срока аренды
    private View customTimeLayout;
    private TextView timeText;
    private TextView costsText;
    private TextView minusButton;
    private TextView plusButton;
    private int customHours = 1;
    private int selectedHours = 24; // По умолчанию 1 день (24 часа)
    private String selectedTimeType = "day"; // hour, day, custom
    private String selectedSize = "XL"; // По умолчанию XL
    private int selectedPrice = 1160; // По умолчанию цена за день
    private String selectedTimeDisplay = "1 день"; // По умолчанию 1 день

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_card_kostum);

        // Находим все View
        initViews();

        // Устанавливаем начальное состояние (выбран 1 день)
        setSelectedState(false);
        setSelectedSize(extraLargeButton, extraLargeText);

        // Устанавливаем обработчики кликов
        setupClickListeners();

        // Обработчик кнопки "Назад"
        findViewById(R.id.buttonBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup);
        });

        // Обработчики для кнопок размеров
        setupSizeClickListeners();

        // Обработчики для кнопок срока аренды
        setupRentalPeriodClickListeners();

        // Обработчик кнопки "Продолжить" (добавить в корзину)
        setupContinueButton();
    }

    private void initViews() {
        oneHourButton = findViewById(R.id.oneHourButton);
        oneDayButton = findViewById(R.id.oneDayButton);
        oneMonthButton = findViewById(R.id.oneMonthButton);
        mediumButton = findViewById(R.id.mediumButton);
        extraLargeButton = findViewById(R.id.extraLargeButton);
        extraExtraLargeButton = findViewById(R.id.extraExtraLargeButton);

        oneHourText = findViewById(R.id.oneHour);
        oneDayText = findViewById(R.id.oneDay);
        oneMonthText = findViewById(R.id.oneMonth);
        mediumText = findViewById(R.id.medium);
        extraLargeText = findViewById(R.id.extraLarge);
        extraExtraLargeText = findViewById(R.id.extraExtraLarge);

        textHowCosts = findViewById(R.id.textHowCosts);
        textInDay = findViewById(R.id.textInDay);
    }

    private void setupRentalPeriodClickListeners() {
        oneHourButton.setOnClickListener(v -> {
            setSelectedState(true);
            selectedHours = 1;
            selectedTimeType = "hour";
            selectedPrice = HOURLY_PRICE;
            selectedTimeDisplay = "1 час";
        });

        oneDayButton.setOnClickListener(v -> {
            setSelectedState(false);
            selectedHours = 24;
            selectedTimeType = "day";
            selectedPrice = DAILY_PRICE;
            selectedTimeDisplay = "1 день";
        });

        oneMonthButton.setOnClickListener(v -> {
            showCustomTimeLayout();
            selectedTimeType = "custom";
        });
    }

    private void setupContinueButton() {
        FrameLayout continueButton = findViewById(R.id.frameLayout3);
        continueButton.setOnClickListener(v -> {
            // Переходим в корзину с выбранными параметрами
            goToBasket();
        });
    }

    private void goToBasket() {
        Intent intent = new Intent(ProductCardKostum.this, Basket.class);
        intent.putExtra("type_of_product", "kostum_blyat");

        // Передаем выбранный размер
        intent.putExtra("selected_size", selectedSize);

        // Передаем выбранную цену
        String priceText = textHowCosts.getText().toString().replace(" ", "").replace("₽", "");
        try {
            int price = Integer.parseInt(priceText);
            intent.putExtra("selected_price", price);
        } catch (NumberFormatException e) {
            intent.putExtra("selected_price", selectedPrice);
        }

        // Передаем выбранное время (часы/дни)
        intent.putExtra("selected_time_display", selectedTimeDisplay);
        intent.putExtra("selected_hours", selectedHours);
        intent.putExtra("selected_time_type", selectedTimeType);

        startActivity(intent);
    }

    private void setupSizeClickListeners() {
        mediumButton.setOnClickListener(v -> {
            setSelectedSize(mediumButton, mediumText);
            selectedSize = "60нм";
        });

        extraLargeButton.setOnClickListener(v -> {
            setSelectedSize(extraLargeButton, extraLargeText);
            selectedSize = "90нм";
        });

        extraExtraLargeButton.setOnClickListener(v -> {
            setSelectedSize(extraExtraLargeButton, extraExtraLargeText);
            selectedSize = "140нм";
        });
    }

    private void setSelectedState(boolean isHourly) {
        isHourlySelected = isHourly;
        resetRentalPeriodButtons();

        if (isHourly) {
            // Выбраны часы
            oneHourButton.setBackgroundResource(R.drawable.chooze_size_active);
            oneHourText.setTextColor(getResources().getColor(android.R.color.black));

            // Обновляем цену и текст
            textHowCosts.setText(HOURLY_PRICE + "₽");
            textInDay.setText("/час");
            selectedPrice = HOURLY_PRICE;
            selectedTimeDisplay = "1 час";
        } else {
            // Выбран день
            oneDayButton.setBackgroundResource(R.drawable.chooze_size_active);
            oneDayText.setTextColor(getResources().getColor(android.R.color.black));

            // Обновляем цену и текст
            textHowCosts.setText("340₽");
            textInDay.setText("/сутки");
            selectedPrice = DAILY_PRICE;
            selectedTimeDisplay = "1 день";
        }

        // Сбрасываем кастомное время если выбраны стандартные варианты
        if (customTimeLayout != null) {
            closeCustomTimeLayout();
        }
    }

    private void setSelectedSize(FrameLayout button, TextView text) {
        // Сбрасываем все кнопки размеров
        resetSizeButtons();

        // Выделяем выбранную кнопку
        button.setBackgroundResource(R.drawable.chooze_size_active);
        text.setTextColor(getResources().getColor(android.R.color.black));
    }

    private void resetRentalPeriodButtons() {
        oneHourButton.setBackgroundResource(R.drawable.chooze_size_not_active);
        oneDayButton.setBackgroundResource(R.drawable.chooze_size_not_active);
        oneMonthButton.setBackgroundResource(R.drawable.chooze_size_not_active);

        oneHourText.setTextColor(getResources().getColor(android.R.color.black));
        oneDayText.setTextColor(getResources().getColor(android.R.color.black));
        oneMonthText.setTextColor(getResources().getColor(android.R.color.black));
    }

    private void resetSizeButtons() {
        mediumButton.setBackgroundResource(R.drawable.chooze_size_not_active);
        extraLargeButton.setBackgroundResource(R.drawable.chooze_size_not_active);
        extraExtraLargeButton.setBackgroundResource(R.drawable.chooze_size_not_active);

        mediumText.setTextColor(getResources().getColor(android.R.color.black));
        extraLargeText.setTextColor(getResources().getColor(android.R.color.black));
        extraExtraLargeText.setTextColor(getResources().getColor(android.R.color.black));
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

            // Кнопка закрытия (крестик) - krestYobannyi
            customTimeLayout.findViewById(R.id.krestYobannyi).setOnClickListener(v -> closeCustomTimeLayout());

            // Кнопка применить в кастомном окне
            customTimeLayout.findViewById(R.id.frameLayout3).setOnClickListener(v -> {
                // Применяем выбранный срок к кнопке oneMonthButton
                applyCustomTimeToButton();
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
        }
    }

    private void increaseTime() {
        // Ограничение: максимум 7 дней (168 часов)
        if (customHours < 168) {
            customHours++;
            updateCustomTimeDisplay();
        }
    }

    private void updateCustomTimeDisplay() {
        if (timeText != null && costsText != null) {
            // Обновляем текст с количеством часов/дней
            String timeDisplay;
            if (customHours > 20) {
                // Если больше 20 часов, показываем в сутках
                int days = (customHours + 23) / 24; // Округление вверх
                if (days > 7) days = 7; // Максимум 7 дней
                timeDisplay = days + " " + getDaysText(days);
                selectedTimeDisplay = timeDisplay;
            } else {
                // Показываем в часах
                timeDisplay = customHours + " " + getHoursText(customHours);
                selectedTimeDisplay = timeDisplay;
            }
            timeText.setText(timeDisplay);

            // Обновляем стоимость с правильным расчетом
            int totalCost = calculateRentalCost(customHours);
            costsText.setText("+" + totalCost + "₽");

            // Обновляем основную цену на экране
            textHowCosts.setText(totalCost + "₽");
            if (customHours > 20) {
                textInDay.setText("/сутки");
            } else {
                textInDay.setText("/час");
            }

            // Сохраняем выбранное время и цену
            selectedHours = customHours;
            selectedPrice = totalCost;
        }
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

    // Новый метод для применения кастомного времени к кнопке
    private void applyCustomTimeToButton() {
        if (customHours > 0) {
            // Выделяем кнопку oneMonthButton
            oneMonthButton.setBackgroundResource(R.drawable.chooze_size_active);
            oneMonthText.setTextColor(getResources().getColor(android.R.color.black));

            // Сбрасываем другие кнопки
            oneHourButton.setBackgroundResource(R.drawable.chooze_size_not_active);
            oneDayButton.setBackgroundResource(R.drawable.chooze_size_not_active);
            oneHourText.setTextColor(getResources().getColor(android.R.color.black));
            oneDayText.setTextColor(getResources().getColor(android.R.color.black));

            // Обновляем текст на кнопке и стоимость
            if (customHours > 20) {
                // Если больше 20 часов, показываем в сутках
                int days = (customHours + 23) / 24; // Округление вверх
                if (days > 7) days = 7; // Максимум 7 дней

                String timeDisplay = days + " " + getDaysText(days);
                oneMonthText.setText(timeDisplay);
                selectedTimeDisplay = timeDisplay;

                // Обновляем цену по суточному тарифу
                int totalCost = days * DAILY_PRICE;
                textHowCosts.setText(totalCost + "₽");
                textInDay.setText("/сутки");
                selectedPrice = totalCost;
            } else {
                // Показываем в часах
                String timeDisplay = customHours + " " + getHoursText(customHours);
                oneMonthText.setText(timeDisplay);
                selectedTimeDisplay = timeDisplay;

                // Обновляем цену по почасовому тарифу
                int totalCost = customHours * HOURLY_PRICE;
                textHowCosts.setText(totalCost + "₽");
                textInDay.setText("/час");
                selectedPrice = totalCost;
            }

            // Сохраняем выбранное время
            selectedHours = customHours;
            selectedTimeType = "custom";
        }
    }

    // Метод для получения правильного окончания для дней
    private String getDaysText(int days) {
        if (days == 1) {
            return "день";
        } else if (days >= 2 && days <= 4) {
            return "дня";
        } else {
            return "дней";
        }
    }

    private void closeCustomTimeLayout() {
        if (customTimeLayout != null) {
            ViewGroup rootView = (ViewGroup) findViewById(android.R.id.content);
            rootView.removeView(customTimeLayout);
            customTimeLayout = null;
        }
    }

    private void setupClickListeners() {
        // Все обработчики уже установлены в соответствующих методах
    }
}