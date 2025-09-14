package com.example.f_rent.EntranceInApp;

import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_role);

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
        // Обработчик для выбора роли арендатора
        poleWithBuyer.setOnClickListener(v -> setActiveRole(true));

        // Обработчик для выбора роли арендодателя
        poleWithSeller.setOnClickListener(v -> setActiveRole(false));

        // Обработчики для кнопки входа
        loginWithPhone.setOnClickListener(v -> showToastNotImplemented());

        textLoginWithPhone.setOnClickListener(v -> showToastNotImplemented());

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

    private void showToastNotImplemented() {
        startActivity(new Intent(ChooseRole.this, ChoosePostamatNearby.class));
        finish();
    }

    private void goBackToSignUp(){
        startActivity(new Intent(ChooseRole.this, SignUp.class));
        finish();
    }
}