package com.example.f_rent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.example.f_rent.EntranceInApp.EntranceInApp;

import java.util.regex.Pattern;

public class UserProfileSettings extends AppCompatActivity {

    private View changeUserNameLayout;
    private EditText editTextText3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.settings_user_profile);

        // Находим кнопку назад и устанавливаем обработчик клика
        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(0, R.anim.slide_out_right_signup);
            }
        });

        // Находим FrameLayout для изменения ФИО и устанавливаем обработчик клика
        FrameLayout frameUserSettings = findViewById(R.id.frameUserSettings);
        frameUserSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showChangeUserNameDialog();
            }
        });

        // Находим FrameLayout удаления аккаунта и устанавливаем обработчик клика
        FrameLayout frameDeleteAccount = findViewById(R.id.frameDeleteAccount);
        frameDeleteAccount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Очищаем SharedPreferences
                SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = userPrefs.edit();
                editor.clear();
                editor.apply();

                // Переходим на окно входа
                Intent intent = new Intent(UserProfileSettings.this, EntranceInApp.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right_login, 0);

                // Финишируем текущую активность
                finish();
            }
        });
    }

    private void showChangeUserNameDialog() {
        // Создаем layout для изменения имени
        ConstraintLayout rootLayout = findViewById(R.id.root_layout_settings); // Нужно добавить этот ID
        if (rootLayout == null) {
            // Если нет специального контейнера, используем корневой элемент
            rootLayout = (ConstraintLayout) ((View) findViewById(R.id.buttonBack).getParent()).getParent();
        }

        changeUserNameLayout = LayoutInflater.from(this).inflate(R.layout.change_user_name, rootLayout, false);

        // Добавляем layout в основной контейнер
        rootLayout.addView(changeUserNameLayout);

        // Находим элементы в диалоге
        editTextText3 = changeUserNameLayout.findViewById(R.id.editTextText3);
        FrameLayout frameLayout3 = changeUserNameLayout.findViewById(R.id.frameLayout3);
        View krestYobannyi = changeUserNameLayout.findViewById(R.id.krestYobannyi);

        // Получаем текущее имя пользователя из SharedPreferences
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String currentUserName = userPrefs.getString("user_name", "");

        // Устанавливаем начальное значение
        if (currentUserName != null && !currentUserName.isEmpty()) {
            editTextText3.setText(currentUserName);
            editTextText3.setTextColor(getResources().getColor(android.R.color.black));
        } else {
            editTextText3.setText("ФИО");
            editTextText3.setTextColor(getResources().getColor(android.R.color.darker_gray));
        }

        // Обработчик для фокуса
        editTextText3.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (editTextText3.getText().toString().equals("ФИО")) {
                        editTextText3.setText("");
                        editTextText3.setTextColor(getResources().getColor(android.R.color.black));
                    }
                } else {
                    // Если поле пустое при потере фокуса, возвращаем "ФИО"
                    if (editTextText3.getText().toString().trim().isEmpty()) {
                        editTextText3.setText("ФИО");
                        editTextText3.setTextColor(getResources().getColor(android.R.color.darker_gray));
                    }
                }
            }
        });

        // Обработчик для клика по EditText
        editTextText3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextText3.getText().toString().equals("ФИО")) {
                    editTextText3.setText("");
                    editTextText3.setTextColor(getResources().getColor(android.R.color.black));
                }
            }
        });

        // Обработчик для кнопки "Продолжить"
        frameLayout3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUserName();
            }
        });

        // Обработчик для кнопки закрытия (крестика)
        krestYobannyi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideChangeUserNameDialog();
            }
        });
    }

    private void saveUserName() {
        String newName = editTextText3.getText().toString().trim();

        // Если поле содержит только "ФИО" или пустое, не сохраняем
        if (newName.equals("ФИО") || newName.isEmpty()) {
            hideChangeUserNameDialog();
            return;
        }

        // Проверяем валидность имени
        if (isValidUserName(newName)) {
            // Сохраняем новое имя в SharedPreferences
            SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = userPrefs.edit();
            editor.putString("user_name", newName);
            editor.apply();

            Toast.makeText(this, "ФИО успешно изменено", Toast.LENGTH_SHORT).show();
            hideChangeUserNameDialog();
        } else {
            Toast.makeText(this, "Введите корректное ФИО (2-3 слова на русском без цифр)", Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidUserName(String name) {
        // Проверка на пустоту
        if (name.isEmpty() || name.equals("ФИО")) {
            return false;
        }

        // Проверка на наличие только русских букв и пробелов
        if (!Pattern.matches("^[а-яА-ЯёЁ\\s]+$", name)) {
            return false;
        }

        // Разделяем на слова
        String[] words = name.trim().split("\\s+");

        // Проверка количества слов (2-3 слова)
        if (words.length < 2 || words.length > 3) {
            return false;
        }

        // Проверка, что каждое слово не пустое
        for (String word : words) {
            if (word.trim().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    private void hideChangeUserNameDialog() {
        if (changeUserNameLayout != null && changeUserNameLayout.getParent() != null) {
            ((ConstraintLayout) changeUserNameLayout.getParent()).removeView(changeUserNameLayout);
            changeUserNameLayout = null;
        }
    }

    @Override
    public void onBackPressed() {
        if (changeUserNameLayout != null) {
            hideChangeUserNameDialog();
        } else {
            super.onBackPressed();
            overridePendingTransition(0, R.anim.slide_out_right_signup);
        }
    }
}