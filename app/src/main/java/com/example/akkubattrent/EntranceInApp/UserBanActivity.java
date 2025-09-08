package com.example.akkubattrent.EntranceInApp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

public class UserBanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_user_ban);

        // Получаем данные из Intent
        Intent intent = getIntent();
        String bannedUserNumber = intent.getStringExtra("banned_user_number");
        String reasonBanName = intent.getStringExtra("reason_ban_name");
        String banEndTime = intent.getStringExtra("ban_end_time");

        // Находим TextView для отображения информации
        TextView textViewEstimatedTime = findViewById(R.id.textViewEstimatedTime);

        // Формируем текст с информацией о бане
        String banMessage;
        if (banEndTime.equals("Никогда")) {
            banMessage = "Пользователь " + bannedUserNumber + " забанен. \nПричина бана: " + reasonBanName + ". \n Дата разблокировки: Никогда";
        } else {
            banMessage = "Пользователь " + bannedUserNumber + " забанен. Причина бана: " + reasonBanName + ". Дата разблокировки: " + banEndTime;
        }

        // Устанавливаем текст в TextView
        textViewEstimatedTime.setText(banMessage);
    }

    @Override
    public void onBackPressed() {
        // Закрываем приложение при нажатии кнопки "Назад"
        super.onBackPressed();
        finishAffinity(); // Закрывает все активности и завершает приложение
    }
}