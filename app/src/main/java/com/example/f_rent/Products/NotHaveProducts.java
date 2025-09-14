package com.example.f_rent.Products;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.f_rent.R;

public class NotHaveProducts extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.product_not_have);

        // Получаем данные из Intent
        String categoryName = getIntent().getStringExtra("CATEGORY_NAME");

        // Находим TextView и устанавливаем текст
        TextView textCategory = findViewById(R.id.textCategory);
        if (categoryName != null) {
            textCategory.setText(categoryName);
        }

        // Обработка нажатия на кнопку "Назад"
        ImageView buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> finish()); // Закрываем текущую активность
    }
}