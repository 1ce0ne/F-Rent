package com.example.f_rent.EntranceInApp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.example.f_rent.R;

public class ChooseRole extends AppCompatActivity {

    private FrameLayout poleWithBuyer;
    private FrameLayout poleWithSeller;
    private FrameLayout loginWithPhone;
    private View textLoginWithPhone;
    private ImageView buttonBack;
    private boolean isBuyerSelected = true; // по умолчанию выбран покупатель
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_role);

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);

        initViews();
        setupClickListeners();
    }

    private void initViews() {
        poleWithBuyer = findViewById(R.id.poleWithBuyer);
        poleWithSeller = findViewById(R.id.poleWithSeller);
        loginWithPhone = findViewById(R.id.login_with_phone);
        textLoginWithPhone = findViewById(R.id.textLoginWithPhone);
        buttonBack = findViewById(R.id.buttonBack);
    }

    private void setupClickListeners() {
        // Обработчик для выбора роли арендатора (покупателя)
        poleWithBuyer.setOnClickListener(v -> {
            setActiveRole(true);
            isBuyerSelected = true;
        });

        // Обработчик для выбора роли арендодателя (продавца)
        poleWithSeller.setOnClickListener(v -> {
            setActiveRole(false);
            isBuyerSelected = false;
        });

        // Обработчики для кнопки входа
        View.OnClickListener nextClickListener = v -> {
            saveUserRole();
            navigateToNextScreen();
        };

        loginWithPhone.setOnClickListener(nextClickListener);
        textLoginWithPhone.setOnClickListener(nextClickListener);

        buttonBack.setOnClickListener(v -> goBackToSignUp());
    }

    private void setActiveRole(boolean isBuyerActive) {
        if (isBuyerActive) {
            // Арендатор активен
            poleWithBuyer.setBackground(ContextCompat.getDrawable(this, R.drawable.choose_role_active));
            poleWithSeller.setBackground(ContextCompat.getDrawable(this, R.drawable.choose_role_not_active));
        } else {
            // Арендодатель активен
            poleWithSeller.setBackground(ContextCompat.getDrawable(this, R.drawable.choose_role_active));
            poleWithBuyer.setBackground(ContextCompat.getDrawable(this, R.drawable.choose_role_not_active));
        }
    }

    private void navigateToNextScreen() {
        startActivity(new Intent(ChooseRole.this, ChoosePostamatNearby.class));
        overridePendingTransition(R.anim.slide_in_right_login, 0);
    }

    private void goBackToSignUp(){
        startActivity(new Intent(ChooseRole.this, SignUp.class));
        finish();
        overridePendingTransition(0, R.anim.slide_out_right_signup);
    }

    // Метод для сохранения роли пользователя в SharedPreferences
    private void saveUserRole() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String userRole = isBuyerSelected ? "buyer" : "seller";
        editor.putString("user_role", userRole);
        editor.apply();

//        Toast.makeText(this, "Данные сохранены", Toast.LENGTH_SHORT).show();
    }
}