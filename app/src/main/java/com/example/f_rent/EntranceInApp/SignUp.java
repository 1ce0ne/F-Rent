package com.example.f_rent.EntranceInApp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.f_rent.R;

public class SignUp extends AppCompatActivity {

    private EditText editTextName;
    private EditText editTextNumber;
    private boolean isFormatting = false;
    private String hintTextPhone = "(987)-654-32-10";
    private int hintColor = Color.parseColor("#858585");
    private int textColor = Color.BLACK;
    private SharedPreferences sharedPreferences;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    navigateToVerification();
                } else {
                    Toast.makeText(this,
                            "Без разрешения на уведомления вы не сможете получить код подтверждения",
                            Toast.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        initViews();
        setupNameField();
        setupPhoneField();
        setupClickListeners();
    }

    private void initViews() {
        editTextName = findViewById(R.id.editTextName);
        editTextNumber = findViewById(R.id.editTextNumber);
        editTextName.setText("ФИО");
        editTextName.setTextColor(hintColor);
        editTextNumber.setText(hintTextPhone);
        editTextNumber.setTextColor(hintColor);
    }

    private void setupNameField() {
        editTextName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (editTextName.getText().toString().equals("ФИО")) {
                    editTextName.setText("");
                    editTextName.setTextColor(textColor);
                }
            } else {
                if (editTextName.getText().toString().isEmpty()) {
                    editTextName.setText("ФИО");
                    editTextName.setTextColor(hintColor);
                } else {
                    editTextName.setTextColor(textColor);
                }
            }
        });

        editTextName.setOnClickListener(v -> {
            if (editTextName.getText().toString().equals("ФИО")) {
                editTextName.setText("");
                editTextName.setTextColor(textColor);
            }
        });

        // Слушатель для изменения цвета при вводе текста
        editTextName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals("ФИО") && s.length() > 0) {
                    editTextName.setTextColor(textColor);
                } else if (s.toString().equals("ФИО")) {
                    editTextName.setTextColor(hintColor);
                }
            }
        });
    }

    private void setupPhoneField() {
        editTextNumber.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                if (editTextNumber.getText().toString().equals(hintTextPhone)) {
                    editTextNumber.setText("");
                    editTextNumber.setTextColor(textColor);
                }
            } else {
                if (editTextNumber.getText().toString().isEmpty()) {
                    editTextNumber.setText(hintTextPhone);
                    editTextNumber.setTextColor(hintColor);
                } else {
                    editTextNumber.setTextColor(textColor);
                }
            }
        });

        editTextNumber.setOnClickListener(v -> {
            if (editTextNumber.getText().toString().equals(hintTextPhone)) {
                editTextNumber.setText("");
                editTextNumber.setTextColor(textColor);
            }
        });

        editTextNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return;

                String input = s.toString();
                if (input.equals(hintTextPhone)) {
                    editTextNumber.setTextColor(hintColor);
                    return;
                } else if (input.length() > 0) {
                    editTextNumber.setTextColor(textColor);
                }

                String digits = input.replaceAll("[^\\d]", "");
                if (digits.length() > 10) {
                    digits = digits.substring(0, 10);
                }

                String formatted = formatPhoneNumber(digits);
                isFormatting = true;
                editTextNumber.setText(formatted);
                editTextNumber.setSelection(formatted.length());
                isFormatting = false;
            }
        });
    }

    private void setupClickListeners() {
        View.OnClickListener phoneLoginListener = v -> {
            if (validateInputs()) {
                // Сохранение данных в SharedPreferences перед переходом
                saveUserData();
                checkAndRequestNotificationPermission();
            }
        };

        findViewById(R.id.login_with_phone).setOnClickListener(phoneLoginListener);
        findViewById(R.id.textLoginWithPhone).setOnClickListener(phoneLoginListener);

//        View.OnClickListener goToLoginListener = v -> {
//            startActivity(new Intent(SignUp.this, EntranceInApp.class));
//            finish();
//            overridePendingTransition(0, R.anim.slide_out_right_signup);
//        };
//
//        findViewById(R.id.signUpButton).setOnClickListener(goToLoginListener);
//        findViewById(R.id.textIfNotHaveAccount).setOnClickListener(goToLoginListener);
//        findViewById(R.id.textSignUp).setOnClickListener(goToLoginListener);
    }

    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED) {
                navigateToVerification();
            } else {
                // Прямой системный запрос без диалога
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            // Для версий ниже Android 13 разрешение не требуется
            navigateToVerification();
        }
    }

    private void navigateToVerification() {
        startActivity(new Intent(SignUp.this, VerificationCodeSignUp.class));
        finish();
        overridePendingTransition(R.anim.slide_in_right_login, 0);
    }

    private boolean validateInputs() {
        String name = editTextName.getText().toString().trim();
        if (name.equals("ФИО") || name.isEmpty()) {
            Toast.makeText(this, "Введите ФИО", Toast.LENGTH_SHORT).show();
            return false;
        }

        String[] nameParts = name.split("\\s+");
        if (nameParts.length < 2) {
            Toast.makeText(this, "Введите как минимум имя и фамилию", Toast.LENGTH_SHORT).show();
            return false;
        }
        if (nameParts.length > 3) {
            Toast.makeText(this, "ФИО не должно содержать более 3 слов", Toast.LENGTH_SHORT).show();
            return false;
        }

        String phoneNumber = editTextNumber.getText().toString();
        if (phoneNumber.equals(hintTextPhone) || phoneNumber.isEmpty()) {
            Toast.makeText(this, "Введите номер телефона", Toast.LENGTH_SHORT).show();
            return false;
        }

        String digitsOnly = phoneNumber.replaceAll("[^\\d]", "");
        if (digitsOnly.length() != 10) {
            Toast.makeText(this, "Номер телефона должен содержать ровно 10 цифр", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String formatPhoneNumber(String digits) {
        if (digits.isEmpty()) return "";

        StringBuilder formatted = new StringBuilder("(");

        if (digits.length() > 3) {
            formatted.append(digits.substring(0, 3)).append(") ");
            digits = digits.substring(3);
        } else {
            return formatted.append(digits).toString();
        }

        if (digits.length() > 3) {
            formatted.append(digits.substring(0, 3)).append(" ");
            digits = digits.substring(3);
        } else {
            return formatted.append(digits).toString();
        }

        if (digits.length() > 2) {
            formatted.append(digits.substring(0, 2)).append("-");
            digits = digits.substring(2);
        } else {
            return formatted.append(digits).toString();
        }

        return formatted.append(digits).toString();
    }

    // Метод для сохранения данных пользователя в SharedPreferences
    private void saveUserData() {
        String userName = editTextName.getText().toString().trim();
        String phoneNumber = editTextNumber.getText().toString().trim();

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("user_name", userName);
        editor.putString("phone_number", phoneNumber);
        editor.apply();

        // Для отладки можно добавить Toast
//        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
    }
}