package com.example.f_rent.EntranceInApp;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
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
import androidx.core.app.NotificationCompat;

import com.example.f_rent.MainActivity;
import com.example.f_rent.R;

import java.util.Random;

public class VerificationCodeSignUp extends AppCompatActivity {

    private static final String CHANNEL_ID = "SMS_CHANNEL";

    private EditText firstNumber, secondNumber, thirdNumber, fourthNumber, fifthNumber;
    private FrameLayout loginButton;
    private TextView loginButtonText, textSignUp;
    private String generatedCode;
    private boolean isCodeVerified = false;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private int attemptCount = 0;
    private long lastSendTime = 0;
    private int[] intervals = {30, 60, 120}; // интервалы в секундах

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

        initViews();
        setupTextWatchers();
        createNotificationChannel();
        generateAndShowCode();
        setupClickListeners();
        updateButtonState();
        startTimer();
    }

    private void initViews() {
        firstNumber = findViewById(R.id.firstNumber);
        secondNumber = findViewById(R.id.secondNumber);
        thirdNumber = findViewById(R.id.thirdNumber);
        fourthNumber = findViewById(R.id.fourthNumber);
        fifthNumber = findViewById(R.id.fiNumber);
        loginButton = findViewById(R.id.login_with_phone);
        loginButtonText = findViewById(R.id.textLoginWithPhone);
        textSignUp = findViewById(R.id.textSignUp);
    }

    private void setupTextWatchers() {
        // Первое поле
        firstNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    secondNumber.requestFocus();
                }
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Второе поле
        secondNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    thirdNumber.requestFocus();
                } else if (s.length() == 0) {
                    firstNumber.requestFocus();
                }
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Третье поле
        thirdNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    fourthNumber.requestFocus();
                } else if (s.length() == 0) {
                    secondNumber.requestFocus();
                }
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Четвертое поле
        fourthNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 1) {
                    fifthNumber.requestFocus();
                } else if (s.length() == 0) {
                    thirdNumber.requestFocus();
                }
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Пятое поле
        fifthNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    fourthNumber.requestFocus();
                }
                updateButtonState();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void updateButtonState() {
        String enteredCode = getEnteredCode();
        isCodeVerified = enteredCode.length() == 5;

        // Блокируем или разблокируем кнопку
        loginButton.setClickable(isCodeVerified);
        loginButtonText.setClickable(isCodeVerified);

        // Можно также изменить визуальное состояние кнопки
        if (isCodeVerified) {
            loginButton.setAlpha(1.0f); // Нормальная прозрачность
        } else {
            loginButton.setAlpha(0.5f); // Полупрозрачная кнопка
        }
    }

    private String getEnteredCode() {
        StringBuilder code = new StringBuilder();
        if (firstNumber.getText().toString().trim().length() > 0) code.append(firstNumber.getText().toString().trim());
        if (secondNumber.getText().toString().trim().length() > 0) code.append(secondNumber.getText().toString().trim());
        if (thirdNumber.getText().toString().trim().length() > 0) code.append(thirdNumber.getText().toString().trim());
        if (fourthNumber.getText().toString().trim().length() > 0) code.append(fourthNumber.getText().toString().trim());
        if (fifthNumber.getText().toString().trim().length() > 0) code.append(fifthNumber.getText().toString().trim());
        return code.toString();
    }

    private void generateAndShowCode() {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            code.append(random.nextInt(10));
        }
        generatedCode = code.toString();
        showNotification("Ваш код подтверждения: " + generatedCode);
        lastSendTime = System.currentTimeMillis();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SMS уведомления",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Уведомления о кодах подтверждения");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private void showNotification(String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("SMS")
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        getSystemService(NotificationManager.class).notify(new Random().nextInt(), builder.build());
    }

    private void setupClickListeners() {
        View.OnClickListener phoneLoginListener = v -> {
            if (isCodeVerified) {
                String enteredCode = getEnteredCode();
                if (enteredCode.equals(generatedCode)) {
                    // Код верный - переходим на MainActivity
                    startActivity(new Intent(this, ChooseRole.class));
                    finish(); // Закрываем текущую активность
                } else {
                    // Код неверный - показываем Toast
                    Toast.makeText(this, "Неверный код подтверждения", Toast.LENGTH_SHORT).show();
                }
            }
        };

        View.OnClickListener resendClickListener = v -> {
            if (canResendCode()) {
                attemptCount++;
                generateAndShowCode();
                clearCodeFields();
                startTimer();
            } else {
                Toast.makeText(this, "Пожалуйста, подождите перед повторной отправкой", Toast.LENGTH_SHORT).show();
            }
        };

        loginButton.setOnClickListener(phoneLoginListener);
        loginButtonText.setOnClickListener(phoneLoginListener);
        textSignUp.setOnClickListener(resendClickListener);

        findViewById(R.id.buttonBack).setOnClickListener(v ->
                startActivity(new Intent(this, SignUp.class)));
    }

    private boolean canResendCode() {
        if (attemptCount >= intervals.length) {
            return false; // Больше нельзя отправлять
        }

        long currentTime = System.currentTimeMillis();
        long timePassed = (currentTime - lastSendTime) / 1000; // в секундах
        int requiredInterval = intervals[attemptCount];

        return timePassed >= requiredInterval;
    }

    private void startTimer() {
        if (timerHandler != null) {
            timerHandler.removeCallbacks(timerRunnable);
        } else {
            timerHandler = new Handler();
        }

        timerRunnable = new Runnable() {
            @Override
            public void run() {
                updateTimerText();
                if (canResendCode()) {
                    textSignUp.setText("Отправить код заново");
                } else {
                    timerHandler.postDelayed(this, 1000); // обновляем каждую секунду
                }
            }
        };

        timerHandler.post(timerRunnable);
    }

    private void updateTimerText() {
        if (attemptCount >= intervals.length) {
            textSignUp.setText("Достигнут лимит отправки кодов");
            textSignUp.setClickable(false);
            return;
        }

        long currentTime = System.currentTimeMillis();
        long timePassed = (currentTime - lastSendTime) / 1000; // в секундах
        int requiredInterval = intervals[attemptCount];
        long timeLeft = requiredInterval - timePassed;

        if (timeLeft > 0) {
            textSignUp.setText("Отправить заново через (" + timeLeft + "s)");
        } else {
            textSignUp.setText("Отправить код заново");
        }
    }

    private void clearCodeFields() {
        firstNumber.setText("");
        secondNumber.setText("");
        thirdNumber.setText("");
        fourthNumber.setText("");
        fifthNumber.setText("");
        firstNumber.requestFocus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timerHandler != null && timerRunnable != null) {
            timerHandler.removeCallbacks(timerRunnable);
        }
    }
}