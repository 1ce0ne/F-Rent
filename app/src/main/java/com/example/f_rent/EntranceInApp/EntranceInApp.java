package com.example.f_rent.EntranceInApp;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.f_rent.MainActivity;
import com.example.f_rent.R;

import org.w3c.dom.Text;

public class EntranceInApp extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance_in_app);

        // Инициализация элементов
        FrameLayout loginWithPhone = findViewById(R.id.login_with_phone);
        TextView textLoginWithPhone = findViewById(R.id.textLoginWithPhone);

        FrameLayout loginWithTelegram = findViewById(R.id.login_with_telegram);
        TextView textLoginWithTelegram = findViewById(R.id.textLoginWithTelegram);
        // ImageView в данном случае
        View telegramIcon = findViewById(R.id.telegram_icon);

        LinearLayout signUpButton = findViewById(R.id.signUpButton);
        TextView textIfNotHaveAccount = findViewById(R.id.textIfNotHaveAccount);
        TextView textSignUp = findViewById(R.id.textSignUp);

        ImageView miniLogo = findViewById(R.id.miniLogo);

        // Обработчик для входа по телефону
        View.OnClickListener phoneLoginListener = v -> {
            Intent intent = new Intent(EntranceInApp.this, LoginNumber.class);
            startActivity(intent);
            finish();
        };

        loginWithPhone.setOnClickListener(phoneLoginListener);
        textLoginWithPhone.setOnClickListener(phoneLoginListener);

        // Обработчик для входа через Telegram
        View.OnClickListener telegramLoginListener = v -> {
            Toast.makeText(EntranceInApp.this, "Абоба", Toast.LENGTH_SHORT).show();
        };

        loginWithTelegram.setOnClickListener(telegramLoginListener);
        textLoginWithTelegram.setOnClickListener(telegramLoginListener);
        telegramIcon.setOnClickListener(telegramLoginListener);

        View.OnClickListener SignUpListener = v -> {
            Intent intent = new Intent(EntranceInApp.this, SignUp.class);
            startActivity(intent);
            finish();
        };

        signUpButton.setOnClickListener(SignUpListener);
        textIfNotHaveAccount.setOnClickListener(SignUpListener);
        textSignUp.setOnClickListener(SignUpListener);

        View.OnClickListener gotoMain = v -> {
            Intent intent = new Intent(EntranceInApp.this, MainActivity.class);
            startActivity(intent);
            finish();
        };
        miniLogo.setOnClickListener(gotoMain);
    }
}