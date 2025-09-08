package com.example.akkubattrent.UserProfile;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.Account.ChangePasswordDialog;
import com.example.akkubattrent.Account.MyCardsActivity;
import com.example.akkubattrent.Account.PassportActivity;
import com.example.akkubattrent.EntranceInApp.LoginActivity;
import com.example.akkubattrent.R;

import org.mindrot.jbcrypt.BCrypt;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class MyAccountActivity extends AppCompatActivity {

    private static final int PICK_IMAGE = 1;
    private ImageView circleImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_account);

        TextView textViewName = findViewById(R.id.textViewName);
        TextView textViewNumber = findViewById(R.id.textViewNumber);
        circleImageView = findViewById(R.id.circleImageView);

        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String name = sharedPreferences.getString("name", "Имя не указано");
        String phoneNumber = sharedPreferences.getString("phone_number", "Номер не указан");

        textViewName.setText(name);
        textViewNumber.setText(phoneNumber);

        // Загрузка изображения профиля
        loadProfileImageFromCache(); // Загрузка изображения из кэша
        loadProfileImageFromDatabase(phoneNumber); // Загрузка изображения из базы данных

        findViewById(R.id.profileImageButton).setOnClickListener(v -> openImageChooser());

        findViewById(R.id.backToProfileButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup);
        });

        findViewById(R.id.myCardButton).setOnClickListener(v -> {
            Intent intent = new Intent(MyAccountActivity.this, MyCardsActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        findViewById(R.id.myPassportButton).setOnClickListener(v -> {
            Intent intent = new Intent(MyAccountActivity.this, PassportActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right_login, 0);
        });

        findViewById(R.id.changePasswordButton).setOnClickListener(v -> {
            SharedPreferences sharedPreferences1 = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String currentPassword = sharedPreferences1.getString("password", ""); // Предположим, что пароль сохранен в SharedPreferences

            ChangePasswordDialog dialog = new ChangePasswordDialog(
                    MyAccountActivity.this, newPassword -> updatePasswordInDatabase(phoneNumber, newPassword), currentPassword);
            dialog.show();
        });

        findViewById(R.id.logoutButton).setOnClickListener(v -> {
            SharedPreferences sharedPreferences2 = getSharedPreferences("user_prefs", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences2.edit();
            editor.clear(); // Очистить все данные
            editor.apply();

            Intent intent = new Intent(MyAccountActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            overridePendingTransition(R.anim.slide_in_right_login, R.anim.slide_out_left_login);
            startActivity(intent);
            finish();
        });
    }

    private void loadProfileImageFromCache() {
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        String encodedImage = sharedPreferences.getString("profile_image", null);
        if (encodedImage != null) {
            byte[] decodedString = android.util.Base64.decode(encodedImage, android.util.Base64.DEFAULT);
            Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            circleImageView.setImageBitmap(decodedByte);
        }
    }

    private void loadProfileImageFromDatabase(String phoneNumber) {
        new Thread(() -> {
            try {
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";

                Connection connection = DriverManager.getConnection(url, user, password);
                String sql = "SELECT image FROM users WHERE phone_number = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, phoneNumber);
                ResultSet resultSet = statement.executeQuery();

                if (resultSet.next()) {
                    byte[] imageBytes = resultSet.getBytes("image");
                    if (imageBytes != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(new ByteArrayInputStream(imageBytes));
                        runOnUiThread(() -> {
                            circleImageView.setImageBitmap(bitmap);
                            // Сохранение изображения в кэш
                            saveProfileImageToCache(bitmap);
                        });
                    }
                }

                resultSet.close();
                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MyAccountActivity.this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void openImageChooser() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                circleImageView.setImageBitmap(bitmap);
                saveProfileImageToDatabase(imageUri);
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Ошибка загрузки изображения", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveProfileImageToCache(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        String encodedImage = android.util.Base64.encodeToString(byteArray, android.util.Base64.DEFAULT);
        SharedPreferences sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("profile_image", encodedImage);
        editor.apply();
    }

    private void saveProfileImageToDatabase(Uri imageUri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
            byte[] byteArray = stream.toByteArray();

            // Сохранение изображения в кэш
            saveProfileImageToCache(bitmap);

            SharedPreferences sharedPreferencesPhone = getSharedPreferences("user_prefs", MODE_PRIVATE);
            String phoneNumber = sharedPreferencesPhone.getString("phone_number", "");

            new Thread(() -> {
                try {
                    String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                    String user = "akku_batt_admin";
                    String password = "8rssCz31UiUr512e";

                    Connection connection = DriverManager.getConnection(url, user, password);
                    String sql = "UPDATE users SET image = ? WHERE phone_number = ?";
                    PreparedStatement statement = connection.prepareStatement(sql);
                    statement.setBytes(1, byteArray);
                    statement.setString(2, phoneNumber);
                    int rowsAffected = statement.executeUpdate();

                    runOnUiThread(() -> {
                        if (rowsAffected > 0) {
                            Toast.makeText(MyAccountActivity.this, "Изображение профиля успешно обновлено", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MyAccountActivity.this, "Ошибка при обновлении изображения профиля", Toast.LENGTH_SHORT).show();
                        }
                    });

                    statement.close();
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(MyAccountActivity.this, "Ошибка подключения к базе данных", Toast.LENGTH_SHORT).show());
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка сохранения изображения", Toast.LENGTH_SHORT).show();
        }
    }

    private void updatePasswordInDatabase(String phoneNumber, String newPassword) {
        new Thread(() -> {
            try {
                String url = "jdbc:mysql://89.23.113.93:3306/AkkuBattRent";
                String user = "akku_batt_admin";
                String password = "8rssCz31UiUr512e";

                // Шифруем новый пароль
                String hashedPassword = BCrypt.hashpw(newPassword, BCrypt.gensalt());

                Connection connection = DriverManager.getConnection(url, user, password);
                String sql = "UPDATE users SET password = ? WHERE phone_number = ?";
                PreparedStatement statement = connection.prepareStatement(sql);
                statement.setString(1, hashedPassword); // Используем зашифрованный пароль
                statement.setString(2, phoneNumber);
                int rowsAffected = statement.executeUpdate();

                runOnUiThread(() -> {
                    if (rowsAffected > 0) {
                        Toast.makeText(MyAccountActivity.this, "Пароль успешно изменён", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MyAccountActivity.this, "Ошибка при изменении пароля", Toast.LENGTH_SHORT).show();
                    }
                });

                statement.close();
                connection.close();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MyAccountActivity.this, "Ошибка подключения к базе данных", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}