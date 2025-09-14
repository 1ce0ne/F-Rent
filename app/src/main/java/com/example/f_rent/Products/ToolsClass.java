package com.example.f_rent.Products;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.f_rent.R;

public class ToolsClass extends AppCompatActivity{

    private ImageView buttonBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_activity_tools); // Убедитесь, что имя файла разметки правильное

        buttonBack = findViewById(R.id.buttonBack);

        // Устанавливаем обработчик клика на кнопку "Назад"
        buttonBack.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup);
        });
    }
}
