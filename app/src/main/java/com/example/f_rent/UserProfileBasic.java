package com.example.f_rent; // Замените на ваш пакет

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.f_rent.EntranceInApp.ChoosePostamatNearby;

public class UserProfileBasic extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.profile_user_basic);

        // Находим TextView для имени пользователя
        TextView textUserName = findViewById(R.id.textUserName);

        // Получаем имя пользователя из SharedPreferences
        SharedPreferences userPrefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String userName = userPrefs.getString("user_name", "");

        // Устанавливаем имя пользователя или "Unknow", если пусто
        if (userName != null && !userName.isEmpty()) {
            textUserName.setText(userName);
        } else {
            textUserName.setText("Unknow");
        }

        // Находим кнопку назад и устанавливаем обработчик клика
        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
                overridePendingTransition(0, R.anim.slide_out_right_signup);
            }
        });

        findViewById(R.id.frameUserSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserProfileBasic.this, UserProfileSettings.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right_login, 0);
            }
        });

        findViewById(R.id.linearLayoutPassport).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(UserProfileBasic.this, UserProfilePassport.class);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_right_login, 0);
            }
        });


        // Находим LinearLayout поддержки и устанавливаем обработчик клика
        LinearLayout linearLayoutHelp = findViewById(R.id.linearLayoutHelp);
        linearLayoutHelp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://t.me/mraid189456");
            }
        });

        // Находим LinearLayout пользовательского соглашения и устанавливаем обработчик клика
        LinearLayout linearLayoutUsagePolicy = findViewById(R.id.linearLayoutUsagePolicy);
        linearLayoutUsagePolicy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openUrl("https://f-rent.ru/#services");
            }
        });

        // Находим LinearLayout частых вопросов и устанавливаем обработчик клика
        LinearLayout linearLayoutFaq = findViewById(R.id.linearLayoutFaq);
        linearLayoutFaq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(UserProfileBasic.this, "В разработке", Toast.LENGTH_SHORT).show();
            }
        });

        // Находим LinearLayout госуслуг и устанавливаем обработчик клика
        LinearLayout linearLayoutGosUslugi = findViewById(R.id.linearLayoutGosUslugi);
        linearLayoutGosUslugi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(UserProfileBasic.this, "В разработке", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Метод для открытия URL в браузере
    private void openUrl(String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(url));
        startActivity(intent);
    }
}