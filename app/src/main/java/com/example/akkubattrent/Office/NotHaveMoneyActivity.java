package com.example.akkubattrent.Office;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.MainMenuActivity;
import com.example.akkubattrent.R;

public class NotHaveMoneyActivity extends AppCompatActivity {

    private static final int DELAY_MILLIS = 5000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Получаем код ошибки из Intent
        String reasonCode = getIntent().getStringExtra("reasonCode");
        String errorMessage = getIntent().getStringExtra("errorMessage");

        // Устанавливаем соответствующий layout
        if (reasonCode != null) {
            switch (reasonCode) {
                case "5054": // Карта просрочена
                    setContentView(R.layout.dialog_card_is_expired);
                    break;
                case "5061": // Превышена сумма
                    setContentView(R.layout.dialog_amount_exceeded);
                    break;
                case "5063": // Карта заблокирована
                    setContentView(R.layout.dialog_card_is_blocked);
                    break;
                default: // Все остальные ошибки (включая 5051)
                    setContentView(R.layout.dialog_not_have_money);
                    break;
            }
        } else {
            setContentView(R.layout.dialog_not_have_money);
        }

        // Устанавливаем текст ошибки (если есть)
        if (errorMessage != null) {
            TextView errorText = findViewById(R.id.textWaiting);
            if (errorText != null) {
                errorText.setText(errorMessage);
            }
        }

        // Задержка и переход в главное меню
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(NotHaveMoneyActivity.this, MainMenuActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, DELAY_MILLIS);
    }
}