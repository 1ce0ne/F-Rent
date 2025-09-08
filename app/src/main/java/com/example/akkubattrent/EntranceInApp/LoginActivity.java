package com.example.akkubattrent.EntranceInApp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;
import com.example.akkubattrent.TutorialClass.GreetingWindowActivity;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;

public class LoginActivity extends AppCompatActivity {

    private EditText etPhone, etPassword;
    private ImageView eyeIcon;
    private long lastAttemptTime = 0;
    private static final long ATTEMPT_DELAY = 5000;
    private CountDownLatch latch = new CountDownLatch(1);
    private View overlay;
    private FrameLayout loadingContainer;
    private ProgressBar progressBar;
    private static final String ENTRANCE_PREFS = "user_prefs_about_entrance";
    private static final String PREF_GREETING_SHOWN = "greeting_shown_for_user_";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etPhone = findViewById(R.id.editTextPhone);
        etPassword = findViewById(R.id.editTextTextPassword);
        eyeIcon = findViewById(R.id.eye_open);
        overlay = findViewById(R.id.overlay);
        loadingContainer = findViewById(R.id.loadingContainer);
        progressBar = findViewById(R.id.progressBar);

        setupPhoneField(etPhone);
        setupPasswordField(etPassword, eyeIcon);

        findViewById(R.id.loginButton).setOnClickListener(v -> attemptLogin());

        findViewById(R.id.registerTextView).setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, SignUpWindow.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, R.anim.slide_out_left_login);
            finish();
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

    private void setupPasswordField(EditText passwordField, ImageView eyeIcon) {
        eyeIcon.setVisibility(View.INVISIBLE);

        passwordField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                eyeIcon.setVisibility(s.length() > 0 ? View.VISIBLE : View.INVISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {}
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

    private void attemptLogin() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAttemptTime < ATTEMPT_DELAY) {
            Toast.makeText(this, "Попробуйте снова через пару секунд", Toast.LENGTH_SHORT).show();
            return;
        }

        lastAttemptTime = currentTime;

        String phoneNumber = formatPhoneNumber(etPhone.getText().toString().trim());
        String password = etPassword.getText().toString().trim();

        if (validateInput(phoneNumber, password)) {
            showLoading();
            new LoginApiTask().execute(phoneNumber, password);
        }
    }

    private void showLoading() {
        runOnUiThread(() -> {
            overlay.setClickable(true);
            overlay.setVisibility(View.VISIBLE);
            loadingContainer.setVisibility(View.VISIBLE);

            overlay.setAlpha(0f);
            loadingContainer.setAlpha(0f);
            overlay.animate().alpha(1f).setDuration(300).start();
            loadingContainer.animate().alpha(1f).setDuration(300).start();

            findViewById(R.id.loginButton).setEnabled(false);
        });
    }

    private void hideLoading() {
        runOnUiThread(() -> {
            overlay.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                overlay.setVisibility(View.GONE);
                overlay.setClickable(false);
            }).start();

            loadingContainer.animate().alpha(0f).setDuration(300).withEndAction(() -> {
                loadingContainer.setVisibility(View.GONE);
            }).start();

            findViewById(R.id.loginButton).setEnabled(true);
        });
    }

    private String formatPhoneNumber(String phone) {
        if (phone.startsWith("+7")) {
            return phone.substring(1);
        } else if (phone.startsWith("7")) {
            return phone;
        } else if (phone.startsWith("8")) {
            return "7" + phone.substring(1);
        }
        return phone;
    }

    private boolean validateInput(String phone, String password) {
        if (phone.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Все поля должны быть заполнены", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private class LoginApiTask extends AsyncTask<String, Void, JSONObject> {
        private String errorMessage = "";
        private String password; // Добавляем поле для хранения пароля

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Сохраняем пароль перед выполнением задачи
            password = etPassword.getText().toString().trim();
        }

        @Override
        protected JSONObject doInBackground(String... params) {
            try {
                URL url = new URL("https://akkubatt-work.ru/mobile/auth/login  ");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("X-API-Key", "o4bAvyn0ZXaFeLDr89lk");
                connection.setDoOutput(true);

                JSONObject jsonInput = new JSONObject();
                jsonInput.put("phone_number", params[0]);
                jsonInput.put("password", params[1]);

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
                    Log.e("LoginActivity", "Server error: " + errorResponse.toString());
                    return null;
                }
            } catch (Exception e) {
                errorMessage = "Ошибка подключения к серверу";
                Log.e("LoginActivity", "API error", e);
                return null;
            }
        }

        @Override
        protected void onPostExecute(JSONObject response) {
            hideLoading();

            if (response != null) {
                try {
                    boolean success = response.getBoolean("success");
                    boolean banned = response.getBoolean("banned");

                    if (success && !banned) {
                        // Успешная авторизация
                        String token = response.getString("token");
                        int userId = response.getInt("user_id");
                        String name = response.getString("name");
                        String phoneNumber = formatPhoneNumber(etPhone.getText().toString().trim());
                        String message = response.getString("message");

                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_SHORT).show();

                        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("phone_number", phoneNumber);
                        editor.putString("name", name);
                        editor.putString("password", password); // Теперь password доступен
                        editor.putInt("id", userId);
                        editor.putString("token", token);
                        editor.putBoolean("is_logged_in", true);
                        editor.apply();

                        // Переход к следующему экрану
                        SharedPreferences entrancePrefs = getSharedPreferences(ENTRANCE_PREFS, MODE_PRIVATE);
                        String greetingKey = PREF_GREETING_SHOWN + userId;
                        boolean isGreetingShown = entrancePrefs.getBoolean(greetingKey, false);

                        if (!isGreetingShown) {
                            entrancePrefs.edit()
                                    .putBoolean(greetingKey, true)
                                    .apply();

                            Intent intent = new Intent(LoginActivity.this, GreetingWindowActivity.class);
                            startActivity(intent);
                        } else {
                            Intent intent = new Intent(LoginActivity.this, MainMenuActivity.class);
                            startActivity(intent);
                        }
                        finish();

                    } else if (banned) {
                        // Пользователь забанен
                        String banReason = response.optString("ban_reason", "Неизвестная причина");
                        String message = response.optString("message", "Аккаунт заблокирован");

                        Intent intent = new Intent(LoginActivity.this, UserBanActivity.class);
                        intent.putExtra("banned_user_number", formatPhoneNumber(etPhone.getText().toString().trim()));
                        intent.putExtra("reason_ban_name", banReason);
                        intent.putExtra("ban_end_time", "Никогда"); // или получить из ответа если есть
                        startActivity(intent);
                        finish();
                    } else {
                        // Ошибка авторизации
                        String message = response.optString("message", "Ошибка авторизации");
                        Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    errorMessage = "Ошибка обработки ответа";
                    Log.e("LoginActivity", "Response parsing error", e);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            } else {
                if (!errorMessage.isEmpty()) {
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(LoginActivity.this, "Ошибка авторизации", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}