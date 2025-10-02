package com.example.f_rent.Products;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.f_rent.R;

public class Basket extends AppCompatActivity {

    private TextView myKorzina, inRent, inBack;
    private FrameLayout layoutForWindow;
    private View currentLayout;
    private EditText promoEditText;
    private FrameLayout submitPromoButton;
    private FrameLayout frameLayout5; // Добавлено
    private boolean isPromoApplied = false;
    private int discountPercent = 0;
    private String originalPromoText = "Добавить промокод";
    private int originalTotalPrice = 0;
    private String productType = "";
    private String selectedSize = "XL"; // По умолчанию XL
    private int selectedPrice = 1160; // По умолчанию цена за день
    private String selectedTimeDisplay = "1 день"; // По умолчанию 1 день
    private int selectedHours = 24; // По умолчанию 24 часа (1 день)
    private String selectedTimeType = "day"; // По умолчанию день
    private String nextStepOfRent = null; // Новая переменная для отслеживания шага аренды

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_korzina);

        // Получаем данные из Intent
        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("type_of_product")) {
                productType = intent.getStringExtra("type_of_product");
            }
            if (intent.hasExtra("selected_size")) {
                selectedSize = intent.getStringExtra("selected_size");
            }
            if (intent.hasExtra("selected_price")) {
                selectedPrice = intent.getIntExtra("selected_price", 1160);
            }
            if (intent.hasExtra("selected_time_display")) {
                selectedTimeDisplay = intent.getStringExtra("selected_time_display");
            }
            if (intent.hasExtra("selected_hours")) {
                selectedHours = intent.getIntExtra("selected_hours", 24);
            }
            if (intent.hasExtra("selected_time_type")) {
                selectedTimeType = intent.getStringExtra("selected_time_type");
            }
            // Получаем next_step_of_rent из Intent
            if (intent.hasExtra("next_step_of_rent")) {
                nextStepOfRent = intent.getStringExtra("next_step_of_rent");
            }
        }

        // Находим все View
        myKorzina = findViewById(R.id.myKorzina);
        inRent = findViewById(R.id.inRent);
        inBack = findViewById(R.id.inBack);
        layoutForWindow = findViewById(R.id.layoutForWindow);
        promoEditText = findViewById(R.id.editTextText);
        submitPromoButton = findViewById(R.id.sumbitPromo);
        frameLayout5 = findViewById(R.id.frameLayout5); // Инициализация

        // Устанавливаем начальное состояние (активна "Моя корзина")
        setActiveTab(myKorzina);

        // Загружаем соответствующий layout в зависимости от типа продукта
        if (nextStepOfRent != null && nextStepOfRent.equals("paid")) {
            // Если уже оплатили, показываем вкладку "В аренде"
            setActiveTab(inRent);
            inflateLayout(R.layout.v_arende_blyat);
            if (frameLayout5 != null) {
                frameLayout5.setVisibility(View.INVISIBLE);
            }
        } else if (productType == null || productType.isEmpty()) {
            inflateLayout(R.layout.moya_korzina_pusto);
        } else if (productType.equals("kostum_blyat")) {
            inflateLayout(R.layout.moya_korzina_blyat);
        } else if (productType.equals("rock_blyat")) {
            inflateLayout(R.layout.moya_korzina_blyat_kamen);
        } else {
            inflateLayout(R.layout.moya_korzina_pusto);
        }

        // Обработчики кликов на вкладках
        myKorzina.setOnClickListener(v -> {
            setActiveTab(myKorzina);
            // При переключении вкладок используем стандартный layout
            if (productType == null || productType.isEmpty()) {
                inflateLayout(R.layout.moya_korzina_pusto);
            } else if (productType.equals("kostum_blyat")) {
                inflateLayout(R.layout.moya_korzina_blyat);
            } else if (productType.equals("rock_blyat")) {
                inflateLayout(R.layout.moya_korzina_blyat_kamen);
            } else {
                inflateLayout(R.layout.moya_korzina_pusto);
            }
            // Делаем frameLayout5 видимым
            if (frameLayout5 != null) {
                frameLayout5.setVisibility(View.VISIBLE);
            }
        });

        inRent.setOnClickListener(v -> {
            setActiveTab(inRent);
            handleRentTabClick();
            // Делаем frameLayout5 невидимым
            if (frameLayout5 != null) {
                frameLayout5.setVisibility(View.INVISIBLE);
            }
        });

        inBack.setOnClickListener(v -> {
            setActiveTab(inBack);
            inflateLayout(R.layout.moya_korzina_pusto);
            // Делаем frameLayout5 невидимым
            if (frameLayout5 != null) {
                frameLayout5.setVisibility(View.INVISIBLE);
            }
        });

        // Обработчик кнопки "Назад"
        findViewById(R.id.buttonBack).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup);
        });

        // Обработчик фокуса для EditText
        setupPromoEditText();

        // Обработчик кнопки применения промокода
        setupPromoButton();

        // Обработчик клика на кнопку "Арендовать" (frameLayout3)
        FrameLayout rentButton = findViewById(R.id.frameLayout3);
        rentButton.setOnClickListener(v -> {
            // Открываем диалог оформления заказа
            View dialogView = getLayoutInflater().inflate(R.layout.typo_oplata_nahui, null);

            // Создаем диалог
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setView(dialogView);
            android.app.AlertDialog dialog = builder.create();
            dialog.show();

            // Обработчик закрытия диалога (крестик)
            View closeButton = dialogView.findViewById(R.id.krestYobannyi);
            closeButton.setOnClickListener(v1 -> dialog.dismiss());

            // Обработчик кнопки оплаты
            FrameLayout payButton = dialogView.findViewById(R.id.framePay);
            TextView payText = dialogView.findViewById(R.id.textPay);

            payButton.setOnClickListener(v2 -> {
                // Меняем текст на "Оплата..."
                payText.setText("Оплата...");

                // Через 1.5 секунды показываем успешную оплату
                new Handler().postDelayed(() -> {
                    // Закрываем текущий диалог
                    dialog.dismiss();

                    // Открываем диалог успешной оплаты
                    View successDialogView = getLayoutInflater().inflate(R.layout.typo_oplatil_nahui, null);
                    android.app.AlertDialog.Builder successBuilder = new android.app.AlertDialog.Builder(this);
                    successBuilder.setView(successDialogView);
                    android.app.AlertDialog successDialog = successBuilder.create();
                    successDialog.show();

                    // Обработчик кнопки "Продолжить"
                    FrameLayout continueButton = successDialogView.findViewById(R.id.frameContinue);
                    continueButton.setOnClickListener(v3 -> {
                        successDialog.dismiss();
                        // Переключаемся на вкладку "В аренде"
                        setActiveTab(inRent);
                        // Очищаем type_of_product
                        productType = "";
                        // Устанавливаем next_step_of_rent = paid
                        nextStepOfRent = "paid";
                        // Загружаем layout в аренде
                        inflateLayout(R.layout.v_arende_blyat);
                        // Делаем frameLayout5 невидимым
                        if (frameLayout5 != null) {
                            frameLayout5.setVisibility(View.INVISIBLE);
                        }
                    });
                }, 1500);
            });
        });
    }

    // Новый метод для обработки клика на вкладке "В аренде"
    private void handleRentTabClick() {
        if (nextStepOfRent == null || nextStepOfRent.isEmpty()) {
            // Если next_step_of_rent null - показываем пустую корзину
            inflateLayout(R.layout.moya_korzina_pusto);
        } else if ("paid".equals(nextStepOfRent)) {
            // Если next_step_of_rent = paid - показываем v_arende_blyat
            inflateLayout(R.layout.v_arende_blyat);
        } else {
            // По умолчанию показываем пустую корзину
            inflateLayout(R.layout.moya_korzina_pusto);
        }
    }

    private void setupPromoEditText() {
        promoEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                // При получении фокуса очищаем текст, если это стандартный текст
                if (promoEditText.getText().toString().equals(originalPromoText)) {
                    promoEditText.setText("");
                }
            } else {
                // При потере фокуса, если поле пустое, возвращаем стандартный текст
                if (promoEditText.getText().toString().isEmpty() && !isPromoApplied) {
                    promoEditText.setText(originalPromoText);
                }
            }
        });

        // Добавляем TextWatcher для отслеживания изменений текста
        promoEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                // Если промокод применен, блокируем редактирование
                if (isPromoApplied) {
                    promoEditText.setEnabled(false);
                }
            }
        });
    }

    private void setupPromoButton() {
        submitPromoButton.setOnClickListener(v -> {
            applyPromoCode();
        });

        // Также обрабатываем клик на тексте внутри кнопки
        TextView promoText = submitPromoButton.findViewById(R.id.textView12);
        promoText.setOnClickListener(v -> {
            applyPromoCode();
        });
    }

    private void applyPromoCode() {
        String promoCode = promoEditText.getText().toString().trim();

        if (promoCode.equalsIgnoreCase("DevSquadTop")) {
            // Применяем скидку 10%
            discountPercent = 10;
            applyDiscount();
            promoEditText.setText("Скидка 10%");
            promoEditText.setEnabled(false);
            isPromoApplied = true;

            // Меняем цвет текста на зеленый
            promoEditText.setTextColor(ContextCompat.getColor(this, R.color.green));

        } else if (!promoCode.isEmpty() && !promoCode.equals(originalPromoText)) {
            // Неверный промокод
            showErrorMessage("Код неверный");
        }
    }

    private void applyDiscount() {
        TextView costsBezSkidok = findViewById(R.id.costsBezSkidok);
        TextView costsItogo = findViewById(R.id.costsItogo);

        try {
            // Сохраняем оригинальную цену
            originalTotalPrice = extractPrice(costsBezSkidok.getText().toString());

            // Вычисляем цену со скидкой
            int discountedPrice = originalTotalPrice - (originalTotalPrice * discountPercent / 100);

            // Обновляем цены
            costsBezSkidok.setText(originalTotalPrice + "₽");
            costsItogo.setText(discountedPrice + "₽");

            // Меняем цвет итоговой цены на зеленый
            costsItogo.setTextColor(ContextCompat.getColor(this, R.color.green));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updatePricesWithDiscount() {
        if (discountPercent > 0) {
            TextView costsBezSkidok = findViewById(R.id.costsBezSkidok);
            TextView costsItogo = findViewById(R.id.costsItogo);

            try {
                // Вычисляем цену со скидкой
                int discountedPrice = originalTotalPrice - (originalTotalPrice * discountPercent / 100);

                // Обновляем цены
                costsBezSkidok.setText(originalTotalPrice + "₽");
                costsItogo.setText(discountedPrice + "₽");

                // Меняем цвет итоговой цена на зеленый
                costsItogo.setTextColor(ContextCompat.getColor(this, R.color.green));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void showErrorMessage(String message) {
        // Показываем сообщение об ошибке
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

        // Через 1 секунду очищаем поле
        new Handler().postDelayed(() -> {
            promoEditText.setText("");
            promoEditText.requestFocus();
        }, 1000);
    }

    // Метод для установки активной вкладки
    private void setActiveTab(TextView activeTab) {
        // Сбрасываем все вкладки к обычному стилю
        myKorzina.setTypeface(null, android.graphics.Typeface.NORMAL);
        inRent.setTypeface(null, android.graphics.Typeface.NORMAL);
        inBack.setTypeface(null, android.graphics.Typeface.NORMAL);

        // Устанавливаем жирный шрифт для активной вкладки
        activeTab.setTypeface(null, android.graphics.Typeface.BOLD);
    }

    // Метод для загрузки layout в контейнер
    private void inflateLayout(int layoutResId) {
        layoutForWindow.removeAllViews(); // Очищаем предыдущий контент
        currentLayout = getLayoutInflater().inflate(layoutResId, layoutForWindow, false);
        layoutForWindow.addView(currentLayout);

        // Если это корзина с товарами, добавляем обработчики кнопок
        if (layoutResId == R.layout.moya_korzina_blyat || layoutResId == R.layout.moya_korzina_blyat_kamen) {
            setupButtonHandlers();
            // Устанавливаем переданные данные
            updateProductDetails();
        }

        // Если это layout в аренде, добавляем обработчики кнопок продления и возврата
        if (layoutResId == R.layout.v_arende_blyat) {
            setupRentButtonsHandlers();
        }

        // Обновляем общую сумму
        updateTotalPrice();
    }

    // Метод для установки обработчиков кнопок продления и возврата
    private void setupRentButtonsHandlers() {
        if (currentLayout == null) return;

        // Обработчик кнопки продления аренды (frameLayout34)
        FrameLayout prolongButton = currentLayout.findViewById(R.id.frameLayout34);
        if (prolongButton != null) {
            prolongButton.setOnClickListener(v -> {
                // Открываем окно продления аренды
                View prolongView = getLayoutInflater().inflate(R.layout.prolong_rent_product, null);

                // Создаем диалог
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setView(prolongView);
                android.app.AlertDialog dialog = builder.create();
                dialog.show();

                // Обработчик закрытия диалога (крестик)
                View closeButton = prolongView.findViewById(R.id.krestYobannyiProlong);
                if (closeButton != null) {
                    closeButton.setOnClickListener(v1 -> dialog.dismiss());
                }

                // Обработчик кнопки продолжения в продлении аренды
                FrameLayout continueButton = prolongView.findViewById(R.id.frameLayoutCont);
                if (continueButton != null) {
                    continueButton.setOnClickListener(v2 -> {
                        Toast.makeText(this, "В разработке", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }

        // Обработчик кнопки возврата товара (frameLayout46)
        FrameLayout returnButton = currentLayout.findViewById(R.id.frameLayout46);
        if (returnButton != null) {
            returnButton.setOnClickListener(v -> {
                // Открываем окно возврата товара
                View returnView = getLayoutInflater().inflate(R.layout.return_product_layout, null);

                // Создаем диалог
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setView(returnView);
                android.app.AlertDialog dialog = builder.create();
                dialog.show();

                // Обработчик закрытия диалога (крестик)
                View closeButton = returnView.findViewById(R.id.krestYobannyiReturn);
                if (closeButton != null) {
                    closeButton.setOnClickListener(v1 -> dialog.dismiss());
                }

                // Обработчик кнопки продолжения в возврате товара
                FrameLayout continueButton = returnView.findViewById(R.id.frameLayoutContinueBack);
                if (continueButton != null) {
                    continueButton.setOnClickListener(v2 -> {
                        // Очищаем next_step_of_rent
                        nextStepOfRent = null;

                        // Закрываем диалог возврата
                        dialog.dismiss();

                        // Переходим на ReturnProduct.java
                        Intent returnIntent = new Intent(Basket.this, ReturnProduct.class);
                        // Передаем необходимые данные
                        returnIntent.putExtra("product_type", productType);
                        returnIntent.putExtra("selected_size", selectedSize);
                        returnIntent.putExtra("selected_price", selectedPrice);
                        returnIntent.putExtra("selected_time_display", selectedTimeDisplay);
                        returnIntent.putExtra("selected_hours", selectedHours);
                        returnIntent.putExtra("selected_time_type", selectedTimeType);
                        startActivity(returnIntent);

                        // Закрываем текущую активность
                        finish();
                    });
                }
            });
        }
    }

    private void updateProductDetails() {
        if (currentLayout == null) return;

        try {
            // Устанавливаем выбранный размер
            TextView sizeTextView = currentLayout.findViewById(R.id.textViewSizeDiesel);
            if (sizeTextView != null) {
                sizeTextView.setText(selectedSize);
            }

            // Устанавливаем цену товара
            TextView priceTextView = currentLayout.findViewById(R.id.textViewPriceDiesel);
            if (priceTextView != null) {
                priceTextView.setText(selectedPrice + "₽");
            }

            // Устанавливаем выбранное время (часы/дни)
            TextView durationTextView = currentLayout.findViewById(R.id.textViewDurationDiesel);
            if (durationTextView != null) {
                durationTextView.setText(selectedTimeDisplay);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupButtonHandlers() {
        if (currentLayout == null) return;

        // Для Diesel
        setupItemHandlers(
                R.id.textViewDecreaseDiesel, R.id.textViewIncreaseDiesel,
                R.id.textViewDurationDiesel, R.id.textViewPriceDiesel,
                selectedPrice // Используем переданную цену
        );
    }

    private void setupItemHandlers(int decreaseId, int increaseId, int durationId, int priceId, int pricePerDay) {
        TextView decreaseBtn = currentLayout.findViewById(decreaseId);
        TextView increaseBtn = currentLayout.findViewById(increaseId);
        TextView durationText = currentLayout.findViewById(durationId);
        TextView priceText = currentLayout.findViewById(priceId);

        // Устанавливаем начальное значение дней на основе переданного времени
        int initialDays = calculateInitialDays();
        durationText.setText(formatDays(initialDays));

        ItemData itemData = new ItemData(initialDays, pricePerDay, durationText, priceText);

        decreaseBtn.setOnClickListener(v -> updateDays(itemData, -1));
        increaseBtn.setOnClickListener(v -> updateDays(itemData, 1));
    }

    private int calculateInitialDays() {
        // Если переданы часы, конвертируем в дни (округляем вверх)
        if ("hour".equals(selectedTimeType)) {
            return (selectedHours + 23) / 24; // Округление вверх
        } else if ("custom".equals(selectedTimeType) && selectedHours > 20) {
            // Для кастомного времени больше 20 часов показываем в днях
            return (selectedHours + 23) / 24; // Округление вверх
        }
        // Для дней возвращаем количество дней
        return selectedHours / 24;
    }

    private int extractDays(String text) {
        if (text.contains("день") || text.contains("дня") || text.contains("дней")) {
            String number = text.replaceAll("[^0-9]", "");
            return number.isEmpty() ? 1 : Integer.parseInt(number);
        }
        return 1;
    }

    private void updateDays(ItemData itemData, int change) {
        int newDays = itemData.days + change;

        // Ограничения: от 1 до 6 дней
        if (newDays < 1) newDays = 1;
        if (newDays > 6) newDays = 6;

        itemData.days = newDays;

        // Обновляем текст продолжительности с правильным склонением
        itemData.durationText.setText(formatDays(newDays));

        // Обновляем цену
        int totalPrice = newDays * itemData.pricePerDay;
        itemData.priceText.setText(totalPrice + "₽");

        // Обновляем общую сумму
        updateTotalPrice();
    }

    private String formatDays(int days) {
        if (days == 1) {
            return "1 день";
        } else if (days >= 2 && days <= 4) {
            return days + " дня";
        } else {
            return days + " дней";
        }
    }

    private void updateTotalPrice() {
        if (currentLayout == null) return;

        int total = 0;

        // Суммируем цены всех элементов
        try {
            TextView dieselPrice = currentLayout.findViewById(R.id.textViewPriceDiesel);
            if (dieselPrice != null) {
                total += extractPrice(dieselPrice.getText().toString());
            }

            TextView costsItogo = findViewById(R.id.costsItogo);
            TextView costsBezSkidok = findViewById(R.id.costsBezSkidok);

            // Сохраняем оригинальную цену
            originalTotalPrice = total;

            if (costsItogo != null && costsBezSkidok != null) {
                costsBezSkidok.setText(total + "₽");

                // Применяем скидку, если она активна
                if (discountPercent > 0) {
                    int discountedPrice = total - (total * discountPercent / 100);
                    costsItogo.setText(discountedPrice + "₽");
                    costsItogo.setTextColor(ContextCompat.getColor(this, R.color.green));
                } else {
                    costsItogo.setText(total + "₽");
                    costsItogo.setTextColor(ContextCompat.getColor(this, android.R.color.black));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int extractPrice(String priceText) {
        try {
            String cleanPrice = priceText.replaceAll("[^0-9]", "");
            return cleanPrice.isEmpty() ? 0 : Integer.parseInt(cleanPrice);
        } catch (Exception e) {
            return 0;
        }
    }

    // Вспомогательный класс для хранения данных об элементе
    private static class ItemData {
        int days;
        int pricePerDay;
        TextView durationText;
        TextView priceText;

        ItemData(int days, int pricePerDay, TextView durationText, TextView priceText) {
            this.days = days;
            this.pricePerDay = pricePerDay;
            this.durationText = durationText;
            this.priceText = priceText;
        }
    }
}