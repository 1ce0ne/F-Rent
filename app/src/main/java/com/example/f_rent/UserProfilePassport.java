package com.example.f_rent;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class UserProfilePassport extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passport_user_profile);

        // Находим кнопку "Назад"
        ImageView buttonBack = findViewById(R.id.buttonBack);

        // Устанавливаем обработчик клика для кнопки "Назад"
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Завершаем текущую активити
                finish();
                overridePendingTransition(0, R.anim.slide_out_right_signup);
            }
        });

        // Находим все FrameLayout кнопки
        FrameLayout pinPhotoPassport = findViewById(R.id.pinPhotoPassport);
        FrameLayout frameFullName = findViewById(R.id.frameFullName);
        FrameLayout frameNumberPassport = findViewById(R.id.frameNumberPassport);
        FrameLayout frameDatesPassport = findViewById(R.id.frameDatesPassport);

        // Устанавливаем обработчики кликов для всех кнопок с сообщением "В разработке"
        View.OnClickListener developmentClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(UserProfilePassport.this, "В разработке", Toast.LENGTH_SHORT).show();
            }
        };

        pinPhotoPassport.setOnClickListener(developmentClickListener);
        frameFullName.setOnClickListener(developmentClickListener);
        frameNumberPassport.setOnClickListener(developmentClickListener);
        frameDatesPassport.setOnClickListener(developmentClickListener);
    }
}