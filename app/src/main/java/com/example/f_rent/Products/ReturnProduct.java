package com.example.f_rent.Products;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.f_rent.MainActivity;
import com.example.f_rent.R;

public class ReturnProduct extends AppCompatActivity {

    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 200;
    private FrameLayout frameTakePhoto;
    private FrameLayout frameContBack;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.return_product_window);

        frameTakePhoto = findViewById(R.id.frameTakePhoto);
        frameContBack = findViewById(R.id.frameContBack);

        // Запрашиваем разрешение на камеру при входе
        requestCameraPermission();

        // Настройка кнопки "Назад"
        ImageView buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Обработчик для съемки фото
        frameTakePhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCamera();
            }
        });

        // Обработчик для возврата на главный экран
        frameContBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearNextStepAndReturnToMain();
            }
        });
    }

    // Метод для запроса разрешения на камеру
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Разрешение уже предоставлено
            Toast.makeText(this, "Разрешение на камеру предоставлено", Toast.LENGTH_SHORT).show();
        }
    }

    // Обработчик результата запроса разрешений
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Разрешение предоставлено
                Toast.makeText(this, "Разрешение на камеру предоставлено", Toast.LENGTH_SHORT).show();
            } else {
                // Разрешение отклонено
                Toast.makeText(this, "Для съемки фото необходимо разрешение на камеру",
                        Toast.LENGTH_LONG).show();

                // Можно предложить пользователю перейти в настройки
                // или продолжить без возможности съемки
                frameTakePhoto.setEnabled(false);
                Toast.makeText(this, "Функция съемки отключена", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {
        // Проверяем разрешение перед открытием камеры
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(this, "Необходимо предоставить разрешение на камеру",
                    Toast.LENGTH_SHORT).show();
            requestCameraPermission();
            return;
        }

        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
        } else {
            Toast.makeText(this, "Камера не доступна на этом устройстве",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == CAMERA_REQUEST_CODE && data != null) {
                // Получаем фото из камеры
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        // Устанавливаем фото как background
                        frameTakePhoto.setBackground(new BitmapDrawable(getResources(), imageBitmap));

                        // Скрываем элементы предварительного просмотра
                        findViewById(R.id.textPinPhoto).setVisibility(View.GONE);
                        findViewById(R.id.imageView4).setVisibility(View.GONE);

                        Toast.makeText(this, "Фото сделано успешно", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(this, "Съемка отменена", Toast.LENGTH_SHORT).show();
        }
    }

    private void clearNextStepAndReturnToMain() {
        // Очищаем next_step_of_rent в SharedPreferences
        if (preferences != null) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.remove("next_step_of_rent");
            editor.apply();
        }

        // Переходим на MainActivity и очищаем back stack
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Сохранение состояния, если фото было сделано
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Восстановление состояния
    }
}