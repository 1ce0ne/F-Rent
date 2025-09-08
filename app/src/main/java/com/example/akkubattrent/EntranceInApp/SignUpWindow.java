package com.example.akkubattrent.EntranceInApp;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.transition.TransitionManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.regex.Pattern;

public class SignUpWindow extends AppCompatActivity {

    private EditText etPhone, etName, etPassword, etConfirmPassword;
    private ImageView eyeOpenFirst, eyeOpenSecond, pingMarkFirst, pingMarkSecond;
    private LinearLayout passwordHintLayout;
    private LinearLayout frameLayout2;
    private TextView passwordLengthHint, passwordDigitHint, passwordSpecialCharHint, passwordLetterHint;
    private boolean isRegisterAttemptInProgress = false;
    private static final long BUTTON_DELAY = 5000;

    private static final String ENTRANCE_PREFS = "user_prefs_about_entrance";
    private static final String PREF_GREETING_SHOWN = "greeting_shown_for_user_";

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[0-9])(?=.*[!@#$%^&*()_+\\-=$$$${};':\"\\\\|,.<>\\/?])(?=.*[a-zA-Z]).{8,}$"
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        etPhone = findViewById(R.id.editTextPhone);
        etName = findViewById(R.id.editTextText);
        etPassword = findViewById(R.id.editTextTextPassword);
        etConfirmPassword = findViewById(R.id.editTextTextPassword2);

        eyeOpenFirst = findViewById(R.id.eye_open_first);
        eyeOpenSecond = findViewById(R.id.eye_open_second);
        pingMarkFirst = findViewById(R.id.ping_mark_first);
        pingMarkSecond = findViewById(R.id.ping_mark_second);

        passwordHintLayout = findViewById(R.id.passwordHintLayout);
        passwordLengthHint = findViewById(R.id.passwordLengthHint);
        passwordDigitHint = findViewById(R.id.passwordDigitHint);
        passwordSpecialCharHint = findViewById(R.id.passwordSpecialCharHint);
        passwordLetterHint = findViewById(R.id.passwordLetterHint);

        pingMarkFirst.setVisibility(View.INVISIBLE);
        pingMarkSecond.setVisibility(View.INVISIBLE);
        passwordHintLayout.setVisibility(View.GONE);

        setupPhoneField(etPhone);
        setupPasswordField(etPassword, eyeOpenFirst, pingMarkFirst);
        setupConfirmPasswordField(etConfirmPassword, eyeOpenSecond, pingMarkSecond);

        frameLayout2 = findViewById(R.id.frameLayout2);

        findViewById(R.id.registerButton).setOnClickListener(v -> {
            if (!isRegisterAttemptInProgress) {
                if (validateAllFields()) {
                    showTermsOfUseDialog();
                }
            } else {
                Toast.makeText(this, "Подождите несколько секунд перед повторной попыткой", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.registerTextView).setOnClickListener(v -> {
            startActivity(new Intent(SignUpWindow.this, LoginActivity.class));
            overridePendingTransition(R.anim.slide_in_left_signup, R.anim.slide_out_right_signup);
            finish();
        });

        etPhone.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordHintLayout.setVisibility(View.GONE);
            }
        });

        etName.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordHintLayout.setVisibility(View.GONE);
            }
        });
    }

    private void setupPhoneField(EditText phoneField) {
        // Устанавливаем "+7" при первом фокусе
        phoneField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus && phoneField.getText().toString().isEmpty()) {
                phoneField.setText("+7");
                phoneField.setSelection(phoneField.getText().length());
            }
        });

        phoneField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Запрещаем удаление "+7"
                if (s.length() < 2 && s.toString().startsWith("+")) {
                    phoneField.post(() -> {
                        phoneField.setText("+7");
                        phoneField.setSelection(phoneField.getText().length());
                    });
                }

                // Удаляем лишние символы, если превышен лимит
                if (s.length() > 12) {
                    String newText = s.toString().substring(0, 12);
                    phoneField.setText(newText);
                    phoneField.setSelection(newText.length());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Устанавливаем "+7" при старте, если поле пустое
        if (phoneField.getText().toString().isEmpty()) {
            phoneField.setText("+7");
            phoneField.setSelection(phoneField.getText().length());
        }
    }

    private void setupPasswordField(EditText passwordField, ImageView eyeIcon, ImageView pingMark) {
        eyeIcon.setVisibility(View.INVISIBLE);
        pingMark.setVisibility(View.INVISIBLE);

        passwordField.setPadding(0, passwordField.getPaddingTop(), passwordField.getPaddingRight(), passwordField.getPaddingBottom());

        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                eyeIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.INVISIBLE);
                pingMark.setVisibility(s.length() > 0 && !PASSWORD_PATTERN.matcher(s.toString()).matches() ? View.VISIBLE : View.INVISIBLE);
                updatePasswordHints();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        passwordField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordHintLayout.setVisibility(View.VISIBLE);
                updatePasswordHints();
                animateLayoutChanges();
            } else {
                passwordHintLayout.setVisibility(View.GONE);
                animateLayoutChanges();
            }
        });

        eyeIcon.setOnClickListener(v -> {
            if (passwordField.getTransformationMethod() instanceof PasswordTransformationMethod) {
                showPassword(passwordField, eyeIcon);
            } else {
                hidePassword(passwordField, eyeIcon);
            }
        });

        pingMark.setOnClickListener(v -> {
            passwordHintLayout.setVisibility(passwordHintLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            updatePasswordHints();
        });
    }

    private void animateLayoutChanges() {
        TransitionManager.beginDelayedTransition(frameLayout2);
    }

    private void setupConfirmPasswordField(EditText passwordField, ImageView eyeIcon, ImageView pingMark) {
        eyeIcon.setVisibility(View.INVISIBLE);
        pingMark.setVisibility(View.INVISIBLE);

        passwordField.setPadding(0, passwordField.getPaddingTop(), passwordField.getPaddingRight(), passwordField.getPaddingBottom());

        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                eyeIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.INVISIBLE);
                String password = etPassword.getText().toString();
                pingMark.setVisibility(s.length() > 0 && !s.toString().equals(password) ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        passwordField.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                passwordHintLayout.setVisibility(View.GONE);
            }
        });

        eyeIcon.setOnClickListener(v -> {
            if (passwordField.getTransformationMethod() instanceof PasswordTransformationMethod) {
                showPassword(passwordField, eyeIcon);
            } else {
                hidePassword(passwordField, eyeIcon);
            }
        });
    }

    private void showPassword(EditText passwordField, ImageView eyeIcon) {
        passwordField.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
        eyeIcon.setImageResource(R.drawable.eye_close);
        passwordField.setSelection(passwordField.getText().length());
    }

    private void hidePassword(EditText passwordField, ImageView eyeIcon) {
        passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
        eyeIcon.setImageResource(R.drawable.eye_open);
        passwordField.setSelection(passwordField.getText().length());
    }

    private void updatePasswordHints() {
        String password = etPassword.getText().toString();

        if (password.length() >= 8) {
            passwordLengthHint.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            passwordLengthHint.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        if (password.matches(".*[0-9].*")) {
            passwordDigitHint.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            passwordDigitHint.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        if (password.matches(".*[!@#$%^&*()_+\\-=$$$${};':\"\\\\|,.<>\\/?].*")) {
            passwordSpecialCharHint.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            passwordSpecialCharHint.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        if (password.matches(".*[a-zA-Z].*")) {
            passwordLetterHint.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            passwordLetterHint.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        }

        if (PASSWORD_PATTERN.matcher(password).matches()) {
            pingMarkFirst.setVisibility(View.INVISIBLE);
        }
    }

    private boolean validateAllFields() {
        String phone = formatPhoneNumber(etPhone.getText().toString().trim());
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (phone.isEmpty() || name.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!phone.matches("^[78]\\d{10}$")) {
            Toast.makeText(this, "Введите корректный номер телефона", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            Toast.makeText(this,
                    "Пароль должен содержать:\n" +
                            "- минимум 8 символов\n" +
                            "- минимум 1 цифру\n" +
                            "- минимум 1 спецсимвол\n" +
                            "- только английские буквы",
                    Toast.LENGTH_LONG).show();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void showTermsOfUseDialog() {
        try {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_terms_improved);

            // Установка прозрачного фона и размеров
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            }

            CheckBox agreeCheckBox = dialog.findViewById(R.id.agreeCheckBox);
            Button acceptButton = dialog.findViewById(R.id.acceptButton);
            TextView privacyPolicyLink = dialog.findViewById(R.id.privacyPolicyLink);
            TextView publicOfferLink = dialog.findViewById(R.id.publicOfferLink);

            // Обработчики для ссылок
            View.OnClickListener linkClickListener = v -> {
                try {
                    String url = v == privacyPolicyLink ?
                            "https://akkubatt-rent.ru/static/docx/personality.pdf" :
                            "https://akkubatt-rent.ru/static/docx/ofert.pdf";

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    startActivity(browserIntent);
                } catch (Exception e) {
                    Toast.makeText(this, "Не удалось открыть документ", Toast.LENGTH_SHORT).show();
                }
            };

            privacyPolicyLink.setOnClickListener(linkClickListener);
            publicOfferLink.setOnClickListener(linkClickListener);

            // Эффект нажатия для ссылок
            View.OnTouchListener linkTouchListener = (v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN: v.setAlpha(0.7f); break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL: v.setAlpha(1.0f); break;
                }
                return false;
            };

            privacyPolicyLink.setOnTouchListener(linkTouchListener);
            publicOfferLink.setOnTouchListener(linkTouchListener);

            // Обработчик для чекбокса
            agreeCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (acceptButton != null) {
                    acceptButton.setEnabled(isChecked);
                }
            });

            // Обработчик для кнопки "Принять"
            acceptButton.setOnClickListener(v -> {
                dialog.dismiss();
                attemptRegistration();
            });

            dialog.show();
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка при отображении диалога", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void attemptRegistration() {
        String phone = formatPhoneNumber(etPhone.getText().toString().trim());
        String name = etName.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (validateAllFields()) {
            isRegisterAttemptInProgress = true;
            new RegisterApiTask(password).execute(name, phone, password);
        }
    }

    private String formatPhoneNumber(String phone) {
        if (phone.startsWith("+7")) {
            return phone.substring(1);
        } else if (phone.startsWith("8")) {
            return "7" + phone.substring(1);
        }
        return phone;
    }

    private class RegisterApiTask extends AsyncTask<String, Void, JSONObject> {
        private String errorMessage = "";
        private String password; // Добавляем поле для хранения пароля

        // Конструктор для передачи пароля
        public RegisterApiTask(String password) {
            this.password = password;
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                URL url = new URL("https://akkubatt-work.ru/mobile/auth/register  ");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("X-API-Key", "o4bAvyn0ZXaFeLDr89lk");
                connection.setDoOutput(true);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("name", params[0]);
                jsonInput.put("phone_number", params[1]);
                jsonInput.put("password", params[2]);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInput.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Scanner scanner = new Scanner(connection.getInputStream());
                    StringBuilder response = new StringBuilder();
                    while (scanner.hasNext()) {
                        response.append(scanner.nextLine());
                    }
                    scanner.close();
                    return new JSONObject(response.toString());
                } else {
                    Scanner scanner = new Scanner(connection.getErrorStream());
                    StringBuilder errorResponse = new StringBuilder();
                    while (scanner.hasNext()) {
                        errorResponse.append(scanner.nextLine());
                    }
                    scanner.close();
                    errorMessage = "Ошибка сервера: " + responseCode;
                    return null;
                }
            } catch (Exception e) {
                errorMessage = "Ошибка подключения к серверу";
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject response) {
            isRegisterAttemptInProgress = false;

            if (response != null) {
                try {
                    boolean success = response.getBoolean("success");
                    boolean banned = response.getBoolean("banned");

                    if (success && !banned) {
                        // Успешная регистрация
                        String token = response.getString("token");
                        int userId = response.getInt("user_id");
                        String name = response.getString("name");
                        String phoneNumber = formatPhoneNumber(etPhone.getText().toString().trim());
                        String message = response.getString("message");

                        Toast.makeText(SignUpWindow.this, message, Toast.LENGTH_SHORT).show();

                        // Сохраняем данные в SharedPreferences
                        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("phone_number", phoneNumber);
                        editor.putString("name", name);
                        editor.putString("password", password); // Сохраняем пароль в открытом виде
                        editor.putInt("id", userId);
                        editor.putString("token", token);
                        editor.putBoolean("is_logged_in", true);
                        editor.apply();

                        // Отмечаем, что приветствие еще не показывалось
                        SharedPreferences entrancePrefs = getSharedPreferences(ENTRANCE_PREFS, MODE_PRIVATE);
                        entrancePrefs.edit()
                                .putBoolean(PREF_GREETING_SHOWN + userId, false)
                                .apply();

                        // Переход к экрану подтверждения регистрации
                        Intent intent = new Intent(SignUpWindow.this, RegisterConfirmationActivity.class);
                        startActivity(intent);
                        overridePendingTransition(R.anim.slide_in_right_login, 0);
                        finish();

                    } else if (banned) {
                        // Пользователь забанен
                        String banReason = response.optString("ban_reason", "Неизвестная причина");
                        String message = response.optString("message", "Аккаунт заблокирован");

                        Toast.makeText(SignUpWindow.this, message + ": " + banReason, Toast.LENGTH_LONG).show();

                    } else {
                        // Ошибка регистрации
                        String message = response.optString("message", "Ошибка регистрации");
                        Toast.makeText(SignUpWindow.this, message, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    errorMessage = "Ошибка обработки ответа";
                    Toast.makeText(SignUpWindow.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            } else {
                if (!errorMessage.isEmpty()) {
                    Toast.makeText(SignUpWindow.this, errorMessage, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(SignUpWindow.this, "Ошибка регистрации", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}