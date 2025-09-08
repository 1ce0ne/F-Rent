package com.example.akkubattrent.EntranceInApp;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

public class ServiceNotAvailableActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_service_not_available);

        // Здесь можно добавить логику для проверки восстановления сервиса
        // Например, периодически опрашивать сервер и закрывать активность, если сервис восстановлен
    }
}