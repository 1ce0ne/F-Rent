package com.example.f_rent.EntranceInApp;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;

import com.example.f_rent.R;

public class LoginNumber extends AppCompatActivity {

    private EditText editTextNumber;
    private boolean isFormatting = false; // Флаг для предотвращения рекурсии
    private String hintTextPhone = "(987)-654-32-10";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login_number);

        // Инициализация поля ввода номера телефона
        editTextNumber = findViewById(R.id.editTextNumber);
        editTextNumber.setText(hintTextPhone);

        // Обработчик фокуса для номера телефона
        editTextNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (editTextNumber.getText().toString().equals(hintTextPhone)) {
                    editTextNumber.setText("");
                }
            } else {
                if (editTextNumber.getText().toString().isEmpty()) {
                    editTextNumber.setText(hintTextPhone);
                }
            }
        });

        // Обработчик клика для номера телефона
        editTextNumber.setOnClickListener(v -> {
            if (editTextNumber.getText().toString().equals(hintTextPhone)) {
                editTextNumber.setText("");
            }
        });

        // TextWatcher для автоформатирования номера телефона
        editTextNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return; // Предотвращаем рекурсию

                String input = s.toString();

                // Если это текст подсказки, не форматируем
                if (input.equals(hintTextPhone)) {
                    return;
                }

                // Убираем все нецифровые символы
                String digits = input.replaceAll("[^\\d]", "");

                // Ограничиваем длину до 10 цифр
                if (digits.length() > 10) {
                    digits = digits.substring(0, 10);
                }

                // Форматируем номер телефона
                String formatted = formatPhoneNumber(digits);

                // Устанавливаем отформатированный номер
                isFormatting = true;
                editTextNumber.setText(formatted);
                editTextNumber.setSelection(formatted.length()); // Ставим курсор в конец
                isFormatting = false;
            }
        });
    }

    // Метод для форматирования номера телефона
    private String formatPhoneNumber(String digits) {
        if (digits.isEmpty()) {
            return "";
        }

        StringBuilder formatted = new StringBuilder();

        // Добавляем скобки для первых 3 цифр
        formatted.append("(");
        if (digits.length() > 3) {
            formatted.append(digits.substring(0, 3));
            formatted.append(") ");
            digits = digits.substring(3);
        } else {
            formatted.append(digits);
            return formatted.toString();
        }

        // Добавляем следующие 3 цифры
        if (digits.length() > 3) {
            formatted.append(digits.substring(0, 3));
            formatted.append(" ");
            digits = digits.substring(3);
        } else {
            formatted.append(digits);
            return formatted.toString();
        }

        // Добавляем следующие 2 цифры
        if (digits.length() > 2) {
            formatted.append(digits.substring(0, 2));
            formatted.append("-");
            digits = digits.substring(2);
        } else {
            formatted.append(digits);
            return formatted.toString();
        }

        // Добавляем оставшиеся 2 цифры
        formatted.append(digits);

        return formatted.toString();
    }
}