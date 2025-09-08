package com.example.akkubattrent.Account;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.example.akkubattrent.R;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import android.util.Base64;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PassportActivity extends AppCompatActivity {

    private static final String TAG = "PassportActivity";

    private FrameLayout cardContainer;
    private FrameLayout addNewPassportButton;
    private String phoneNumber;
    private String encryptionKey;
    private Connection dbConnection;
    private boolean isConnectionActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_passport);

        cardContainer = findViewById(R.id.cardContainer);
        addNewPassportButton = findViewById(R.id.addNewPassportButton);

        SharedPreferences prefs = getSharedPreferences("user_prefs", MODE_PRIVATE);
        phoneNumber = prefs.getString("phone_number", null);

        findViewById(R.id.backToProfileButton).setOnClickListener(v -> {
            finish();
            overridePendingTransition(0, R.anim.slide_out_right_signup);
        });

        addNewPassportButton.setOnClickListener(v -> {
            try {
                showAddPassportDialog();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при открытии диалога", e);
                showToast("Ошибка при открытии формы");
            }
        });

        establishDatabaseConnection();
    }

    private void establishDatabaseConnection() {
        new Thread(() -> {
            try {
                // Закрываем предыдущее соединение, если оно есть
                if (dbConnection != null && !dbConnection.isClosed()) {
                    dbConnection.close();
                }

                dbConnection = DriverManager.getConnection(
                        "jdbc:mysql://89.23.113.93:3306/AkkuBattRent",
                        "akku_batt_admin",
                        "8rssCz31UiUr512e");

                isConnectionActive = true;
                Log.d(TAG, "Соединение с БД успешно установлено");

                runOnUiThread(() -> {
                    if (phoneNumber != null) {
                        ensureEncryptionKeyAvailable(() -> {
                            checkUserPassport();
                        });
                    } else {
                        Log.e(TAG, "Номер телефона не найден");
                        showToast("Ошибка: номер телефона не найден");
                    }
                });
            } catch (SQLException e) {
                isConnectionActive = false;
                Log.e(TAG, "Ошибка подключения к БД", e);
                runOnUiThread(() -> {
                    showToast("Ошибка подключения к БД");
                    cardContainer.removeAllViews();
                    addNewPassportButton.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    // Метод для гарантированного получения ключа шифрования
    private void ensureEncryptionKeyAvailable(Runnable onSuccess) {
        if (encryptionKey != null && !encryptionKey.isEmpty()) {
            Log.d(TAG, "Ключ шифрования уже доступен");
            onSuccess.run();
            return;
        }

        Log.d(TAG, "Ключ шифрования отсутствует, запрашиваем новый");
        fetchEncryptionKeyFromServer(key -> {
            encryptionKey = key;
            if (encryptionKey != null) {
                Log.d(TAG, "Ключ шифрования успешно получен и установлен");
                onSuccess.run();
            } else {
                Log.e(TAG, "Ошибка получения ключа шифрования");
                runOnUiThread(() -> showToast("Ошибка получения ключа шифрования"));
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeDatabaseConnection();
    }

    private void closeDatabaseConnection() {
        if (dbConnection != null) {
            try {
                if (!dbConnection.isClosed()) {
                    dbConnection.close();
                    isConnectionActive = false;
                    Log.d(TAG, "Соединение с БД закрыто");
                }
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка при закрытии соединения", e);
            }
        }
    }

    private void fetchEncryptionKeyFromServer(OnKeyFetchedListener listener) {
        if (!isConnectionActive) {
            Log.e(TAG, "Нет соединения с сервером при попытке получить ключ");
            showToast("Нет соединения с сервером");
            listener.onKeyFetched(null);
            return;
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "Попытка получения ключа шифрования для пользователя: " + phoneNumber);

                String query = "SELECT encryption_key FROM server_passport_keys WHERE user_number = ?";
                PreparedStatement stmt = dbConnection.prepareStatement(query);
                stmt.setString(1, phoneNumber);
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    String key = rs.getString("encryption_key");
                    Log.d(TAG, "Найден существующий ключ шифрования");
                    if (key != null && !key.trim().isEmpty()) {
                        Log.d(TAG, "Ключ шифрования загружен из БД");
                        listener.onKeyFetched(key);
                        return;
                    }
                }

                // Если ключа нет, генерируем новый
                Log.d(TAG, "Ключ не найден, генерируем новый");
                String newKey = generateNewKey();
                if (newKey == null || newKey.trim().isEmpty()) {
                    Log.e(TAG, "Ошибка генерации нового ключа - пустой ключ");
                    listener.onKeyFetched(null);
                    return;
                }

                String insertQuery = "INSERT INTO server_passport_keys (encryption_key, user_number) VALUES (?, ?)";
                PreparedStatement insertStmt = dbConnection.prepareStatement(insertQuery);
                insertStmt.setString(1, newKey);
                insertStmt.setString(2, phoneNumber);
                int affectedRows = insertStmt.executeUpdate();

                if (affectedRows > 0) {
                    Log.d(TAG, "Новый ключ успешно сохранен в БД");
                    listener.onKeyFetched(newKey);
                } else {
                    Log.e(TAG, "Ошибка сохранения нового ключа в БД");
                    listener.onKeyFetched(null);
                }
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка при получении ключа шифрования", e);
                listener.onKeyFetched(null);
            }
        }).start();
    }

    private void checkUserPassport() {
        if (!isConnectionActive) {
            showToast("Нет соединения с сервером");
            return;
        }

        new Thread(() -> {
            try {
                String checkQuery = "SELECT have_a_passport FROM users WHERE phone_number = ?";
                PreparedStatement checkStmt = dbConnection.prepareStatement(checkQuery);
                checkStmt.setString(1, phoneNumber);
                ResultSet checkRs = checkStmt.executeQuery();

                if (checkRs.next()) {
                    String passportId = checkRs.getString("have_a_passport");
                    if (passportId != null && !passportId.trim().isEmpty()) {
                        loadPassportData();
                        return;
                    }
                }

                runOnUiThread(() -> {
                    cardContainer.removeAllViews();
                    addNewPassportButton.setVisibility(View.VISIBLE);
                });
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка проверки паспорта", e);
                runOnUiThread(() -> {
                    showToast("Ошибка проверки паспорта");
                    cardContainer.removeAllViews();
                    addNewPassportButton.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void loadPassportData() {
        if (!isConnectionActive) {
            showToast("Нет соединения с сервером");
            return;
        }

        new Thread(() -> {
            try {
                String passportQuery = "SELECT * FROM passports WHERE user_passport_phone_number = ?";
                PreparedStatement passportStmt = dbConnection.prepareStatement(passportQuery);
                passportStmt.setString(1, phoneNumber);
                ResultSet passportRs = passportStmt.executeQuery();

                if (passportRs.next()) {
                    String[] encryptedFields = new String[6];
                    encryptedFields[0] = passportRs.getString("user_passport_name");
                    encryptedFields[1] = passportRs.getString("user_passport_birthday");
                    encryptedFields[2] = passportRs.getString("user_passport_serial_number");
                    encryptedFields[3] = passportRs.getString("user_passport_date_of_issue");
                    encryptedFields[4] = passportRs.getString("user_passport_code");
                    encryptedFields[5] = passportRs.getString("user_passport_issued");

                    String[] decryptedData = new String[6];
                    for (int i = 0; i < encryptedFields.length; i++) {
                        if (encryptedFields[i] == null || encryptedFields[i].trim().isEmpty()) {
                            throw new SQLException("Пустое поле паспорта с индексом " + i);
                        }
                        decryptedData[i] = decrypt(encryptedFields[i]);
                        if (decryptedData[i] == null) {
                            throw new SQLException("Ошибка дешифрования поля с индексом " + i);
                        }
                    }

                    runOnUiThread(() -> {
                        showPassportCard(decryptedData);
                        addNewPassportButton.setVisibility(View.INVISIBLE);
                    });
                } else {
                    runOnUiThread(() -> {
                        cardContainer.removeAllViews();
                        addNewPassportButton.setVisibility(View.VISIBLE);
                    });
                }
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка загрузки паспорта", e);
                runOnUiThread(() -> {
                    showToast("Ошибка загрузки паспорта");
                    cardContainer.removeAllViews();
                    addNewPassportButton.setVisibility(View.VISIBLE);
                });
            }
        }).start();
    }

    private void showAddPassportDialog() {
        try {
            if (isFinishing() || isDestroyed()) {
                return;
            }

            // Проверяем наличие ключа перед открытием диалога
            if (encryptionKey == null || encryptionKey.isEmpty()) {
                Log.d(TAG, "Ключ шифрования отсутствует при открытии диалога, запрашиваем новый");
                ensureEncryptionKeyAvailable(() -> {
                    runOnUiThread(() -> showAddPassportDialogInternal());
                });
            } else {
                showAddPassportDialogInternal();
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании диалога", e);
            showToast("Ошибка при создании формы: " + e.getMessage());
        }
    }

    private void showAddPassportDialogInternal() {
        try {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_add_passport);

            // Настройка диалога на полную ширину
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            dialog.setCancelable(true);

            // Находим элементы по ID из dialog_add_passport.xml
            EditText etName = dialog.findViewById(R.id.etName);
            EditText etPassportSeries = dialog.findViewById(R.id.etPassportSeries);
            EditText etPassportNumber = dialog.findViewById(R.id.etPassportNumber);
            EditText etBirthDate = dialog.findViewById(R.id.etBirthDate);
            EditText etIssueDate = dialog.findViewById(R.id.etIssueDate);
            EditText etDepartmentCode = dialog.findViewById(R.id.etDepartmentCode);
            EditText etIssuedBy = dialog.findViewById(R.id.etIssuedBy);
            Button btnSave = dialog.findViewById(R.id.saveButton);

            if (etName == null || etPassportSeries == null || etPassportNumber == null ||
                    etBirthDate == null || etIssueDate == null || etDepartmentCode == null ||
                    etIssuedBy == null || btnSave == null) {
                throw new RuntimeException("Не все элементы интерфейса найдены");
            }

            // TextWatcher для серии паспорта (4 цифры)
            etPassportSeries.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String text = s.toString().replaceAll("[^\\d]", "");

                    if (text.length() > 4) {
                        text = text.substring(0, 4);
                    }

                    if (!s.toString().equals(text)) {
                        etPassportSeries.setText(text);
                        etPassportSeries.setSelection(text.length());
                    }

                    // Автопереход на следующее поле
                    if (text.length() == 4) {
                        etPassportNumber.requestFocus();
                    }
                }
            });

            // TextWatcher для номера паспорта (6 цифр)
            etPassportNumber.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String text = s.toString().replaceAll("[^\\d]", "");

                    if (text.length() > 6) {
                        text = text.substring(0, 6);
                    }

                    if (!s.toString().equals(text)) {
                        etPassportNumber.setText(text);
                        etPassportNumber.setSelection(text.length());
                    }

                    // Автопереход на следующее поле
                    if (text.length() == 6) {
                        etBirthDate.requestFocus();
                    }
                }
            });

            // TextWatcher для даты рождения
            etBirthDate.addTextChangedListener(new TextWatcher() {
                private boolean isFormatting;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isFormatting) return;

                    isFormatting = true;

                    String text = s.toString().replaceAll("[^\\d]", "");
                    if (text.length() > 8) {
                        text = text.substring(0, 8);
                    }

                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < text.length(); i++) {
                        if (i == 2 || i == 4) {
                            formatted.append(".");
                        }
                        formatted.append(text.charAt(i));
                    }

                    // Проверка валидности даты
                    if (text.length() >= 2) {
                        try {
                            int day = Integer.parseInt(text.substring(0, 2));
                            if (day < 1 || day > 31) {
                                etBirthDate.setError("День должен быть от 1 до 31");
                            } else {
                                etBirthDate.setError(null);
                            }
                        } catch (NumberFormatException e) {
                            etBirthDate.setError("Неверный формат");
                        }
                    }
                    if (text.length() >= 4) {
                        try {
                            int month = Integer.parseInt(text.substring(2, 4));
                            if (month < 1 || month > 12) {
                                etBirthDate.setError("Месяц должен быть от 1 до 12");
                            } else {
                                etBirthDate.setError(null);
                            }
                        } catch (NumberFormatException e) {
                            etBirthDate.setError("Неверный формат");
                        }
                    }
                    if (text.length() == 8) {
                        try {
                            int year = Integer.parseInt(text.substring(4, 8));
                            Calendar minCalendar = Calendar.getInstance();
                            minCalendar.add(Calendar.YEAR, -14);

                            Calendar birthCalendar = Calendar.getInstance();
                            birthCalendar.set(year, Integer.parseInt(text.substring(2, 4)) - 1, Integer.parseInt(text.substring(0, 2)));

                            if (birthCalendar.after(minCalendar)) {
                                etBirthDate.setError("Возраст должен быть не менее 14 лет");
                            } else {
                                etBirthDate.setError(null);
                            }
                        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                            etBirthDate.setError("Неверный формат");
                        }
                    }

                    if (!s.toString().equals(formatted.toString())) {
                        etBirthDate.setText(formatted.toString());
                        etBirthDate.setSelection(formatted.length());
                    }

                    // Автопереход на следующее поле
                    if (text.length() == 8) {
                        etIssueDate.requestFocus();
                    }

                    isFormatting = false;
                }
            });

            // TextWatcher для даты выдачи
            etIssueDate.addTextChangedListener(new TextWatcher() {
                private boolean isFormatting;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isFormatting) return;

                    isFormatting = true;

                    String text = s.toString().replaceAll("[^\\d]", "");
                    if (text.length() > 8) {
                        text = text.substring(0, 8);
                    }

                    StringBuilder formatted = new StringBuilder();
                    for (int i = 0; i < text.length(); i++) {
                        if (i == 2 || i == 4) {
                            formatted.append(".");
                        }
                        formatted.append(text.charAt(i));
                    }

                    // Проверка валидности даты
                    if (text.length() >= 2) {
                        try {
                            int day = Integer.parseInt(text.substring(0, 2));
                            if (day < 1 || day > 31) {
                                etIssueDate.setError("День должен быть от 1 до 31");
                            } else {
                                etIssueDate.setError(null);
                            }
                        } catch (NumberFormatException e) {
                            etIssueDate.setError("Неверный формат");
                        }
                    }
                    if (text.length() >= 4) {
                        try {
                            int month = Integer.parseInt(text.substring(2, 4));
                            if (month < 1 || month > 12) {
                                etIssueDate.setError("Месяц должен быть от 1 до 12");
                            } else {
                                etIssueDate.setError(null);
                            }
                        } catch (NumberFormatException e) {
                            etIssueDate.setError("Неверный формат");
                        }
                    }
                    if (text.length() == 8) {
                        try {
                            int year = Integer.parseInt(text.substring(4, 8));
                            Calendar today = Calendar.getInstance();
                            Calendar issueDate = Calendar.getInstance();
                            issueDate.set(year, Integer.parseInt(text.substring(2, 4)) - 1, Integer.parseInt(text.substring(0, 2)));

                            if (issueDate.after(today)) {
                                etIssueDate.setError("Дата выдачи не может быть в будущем");
                            } else {
                                etIssueDate.setError(null);
                            }
                        } catch (NumberFormatException | StringIndexOutOfBoundsException e) {
                            etIssueDate.setError("Неверный формат");
                        }
                    }

                    if (!s.toString().equals(formatted.toString())) {
                        etIssueDate.setText(formatted.toString());
                        etIssueDate.setSelection(formatted.length());
                    }

                    // Автопереход на следующее поле
                    if (text.length() == 8) {
                        etDepartmentCode.requestFocus();
                    }

                    isFormatting = false;
                }
            });

            // TextWatcher для кода подразделения
            etDepartmentCode.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    String text = s.toString().replaceAll("[^\\d]", "");

                    if (text.length() > 6) {
                        text = text.substring(0, 6);
                    }

                    if (text.length() > 3) {
                        text = text.substring(0, 3) + "-" + text.substring(3);
                    }

                    if (!s.toString().equals(text)) {
                        etDepartmentCode.setText(text);
                        etDepartmentCode.setSelection(text.length());
                    }

                    // Автопереход на следующее поле
                    if (text.replace("-", "").length() == 6) {
                        etIssuedBy.requestFocus();
                    }
                }
            });

            // TextWatcher для кем выдан (все буквы большими)
            etIssuedBy.addTextChangedListener(new TextWatcher() {
                private boolean isFormatting = false;

                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}

                @Override
                public void afterTextChanged(Editable s) {
                    if (isFormatting) return;

                    isFormatting = true;
                    String text = s.toString();

                    // Только русские буквы, пробелы и дефисы
                    text = text.replaceAll("[^а-яА-ЯёЁ\\s\\-]", "");
                    text = text.toUpperCase();

                    if (!s.toString().equals(text)) {
                        etIssuedBy.setText(text);
                        etIssuedBy.setSelection(text.length());
                    }
                    isFormatting = false;
                }
            });

            btnSave.setOnClickListener(v -> {
                try {
                    String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                    String birthDate = etBirthDate.getText() != null ? etBirthDate.getText().toString().trim() : "";
                    String passportSeries = etPassportSeries.getText() != null ? etPassportSeries.getText().toString().trim() : "";
                    String passportNumber = etPassportNumber.getText() != null ? etPassportNumber.getText().toString().trim() : "";
                    String issueDate = etIssueDate.getText() != null ? etIssueDate.getText().toString().trim() : "";
                    String departmentCode = etDepartmentCode.getText() != null ? etDepartmentCode.getText().toString().trim().replace("-", "") : "";
                    String issuedBy = etIssuedBy.getText() != null ? etIssuedBy.getText().toString().trim() : "";

                    Log.d(TAG, "Полученные данные: " +
                            "name=" + name + ", " +
                            "birthDate=" + birthDate + ", " +
                            "series=" + passportSeries + ", " +
                            "number=" + passportNumber + ", " +
                            "issueDate=" + issueDate + ", " +
                            "departmentCode=" + departmentCode + ", " +
                            "issuedBy=" + issuedBy);

                    if (validatePassportData(name, birthDate, passportSeries, passportNumber, issueDate, departmentCode, issuedBy)) {
                        // Форматируем данные перед сохранением
                        String formattedSerialNumber = passportSeries + " " + passportNumber;
                        String formattedDepartmentCode = departmentCode.length() > 3 ?
                                departmentCode.substring(0, 3) + "-" + departmentCode.substring(3) : departmentCode;

                        Log.d(TAG, "Форматированные данные: " +
                                "serialNumber=" + formattedSerialNumber + ", " +
                                "departmentCode=" + formattedDepartmentCode);

                        if (!isConnectionActive) {
                            showToast("Нет соединения с сервером. Попробуйте позже.");
                            return;
                        }

                        // Проверяем наличие ключа шифрования
                        if (encryptionKey == null || encryptionKey.isEmpty()) {
                            Log.e(TAG, "Ключ шифрования не установлен перед сохранением");
                            // Пытаемся получить ключ еще раз
                            ensureEncryptionKeyAvailable(() -> {
                                if (encryptionKey != null && !encryptionKey.isEmpty()) {
                                    addNewPassport(name, birthDate, formattedSerialNumber, issueDate, formattedDepartmentCode, issuedBy);
                                } else {
                                    runOnUiThread(() -> showToast("Ошибка: не удалось получить ключ шифрования"));
                                }
                            });
                        } else {
                            Log.d(TAG, "Ключ шифрования доступен: " + (encryptionKey != null ? "ДА" : "НЕТ"));
                            addNewPassport(name, birthDate, formattedSerialNumber, issueDate, formattedDepartmentCode, issuedBy);
                        }

                        dialog.dismiss();
                    } else {
                        showToast("Проверьте правильность заполнения всех полей");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при сохранении паспорта", e);
                    showToast("Ошибка при сохранении данных: " + e.getMessage());
                }
            });

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании диалога", e);
            showToast("Ошибка при создании формы: " + e.getMessage());
        }
    }

    private boolean validatePassportData(String... fields) {
        try {
            String name = fields[0];
            String birthDate = fields[1];
            String passportSeries = fields[2];
            String passportNumber = fields[3];
            String issueDate = fields[4];
            String departmentCode = fields[5];
            String issuedBy = fields[6];

            Log.d(TAG, "Валидация данных: " +
                    "name=" + name + ", " +
                    "birthDate=" + birthDate + ", " +
                    "series=" + passportSeries + ", " +
                    "number=" + passportNumber + ", " +
                    "issueDate=" + issueDate + ", " +
                    "departmentCode=" + departmentCode + ", " +
                    "issuedBy=" + issuedBy);

            // Проверка на пустые поля
            if (name.isEmpty() || birthDate.isEmpty() || passportSeries.isEmpty() ||
                    passportNumber.isEmpty() || issueDate.isEmpty() || departmentCode.isEmpty() ||
                    issuedBy.isEmpty()) {
                Log.w(TAG, "Одно или несколько полей пустые");
                return false;
            }

            // Проверка ФИО (должно быть не менее 2 слов)
            String[] nameParts = name.split("\\s+");
            if (nameParts.length < 2) {
                Log.w(TAG, "ФИО должно содержать минимум 2 слова");
                return false;
            }

            // Проверка на русские буквы в ФИО
            if (!name.matches("^[а-яА-ЯёЁ\\s\\-]+$")) {
                Log.w(TAG, "ФИО должно содержать только русские буквы");
                return false;
            }

            // Проверка серии паспорта (4 цифры)
            if (passportSeries.length() != 4 || !passportSeries.matches("\\d{4}")) {
                Log.w(TAG, "Серия паспорта должна содержать 4 цифры");
                return false;
            }

            // Проверка номера паспорта (6 цифр)
            if (passportNumber.length() != 6 || !passportNumber.matches("\\d{6}")) {
                Log.w(TAG, "Номер паспорта должен содержать 6 цифр");
                return false;
            }

            // Проверка формата дат (должны быть в формате dd.MM.yyyy)
            if (!birthDate.matches("\\d{2}\\.\\d{2}\\.\\d{4}") ||
                    !issueDate.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                Log.w(TAG, "Неверный формат даты. Должен быть dd.MM.yyyy");
                return false;
            }

            // Проверка кода подразделения (6 цифр)
            if (departmentCode.length() != 6 || !departmentCode.matches("\\d{6}")) {
                Log.w(TAG, "Код подразделения должен содержать 6 цифр");
                return false;
            }

            // Проверка даты рождения (не младше 14 лет)
            if (!isValidBirthDate(birthDate)) {
                Log.w(TAG, "Неверная дата рождения");
                return false;
            }

            // Проверка даты выдачи (не позже сегодняшней даты)
            if (!isValidIssueDate(issueDate)) {
                Log.w(TAG, "Неверная дата выдачи");
                return false;
            }

            // Проверка кем выдан (только русские буквы, пробелы, дефисы)
            if (!issuedBy.matches("^[А-ЯЁ\\s\\-]+$")) {
                Log.w(TAG, "Поле 'Кем выдан' должно содержать только русские буквы");
                return false;
            }

            // Проверка на реалистичность кем выдано
            if (!isValidIssuedBy(issuedBy)) {
                Log.w(TAG, "Нереалистичное значение в поле 'Кем выдан'");
                return false;
            }

            Log.d(TAG, "Все проверки пройдены успешно");
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка валидации данных паспорта", e);
            return false;
        }
    }

    private boolean isValidBirthDate(String birthDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            sdf.setLenient(false);

            Date date = sdf.parse(birthDate);
            Calendar birthCalendar = Calendar.getInstance();
            birthCalendar.setTime(date);

            Calendar minCalendar = Calendar.getInstance();
            minCalendar.add(Calendar.YEAR, -14);

            boolean result = !birthCalendar.after(minCalendar);
            Log.d(TAG, "Проверка даты рождения: " + birthDate + ", результат: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка проверки даты рождения", e);
            return false;
        }
    }

    private boolean isValidIssueDate(String issueDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
            sdf.setLenient(false);

            Date date = sdf.parse(issueDate);
            Calendar issueCalendar = Calendar.getInstance();
            issueCalendar.setTime(date);

            Calendar today = Calendar.getInstance();

            boolean result = !issueCalendar.after(today);
            Log.d(TAG, "Проверка даты выдачи: " + issueDate + ", результат: " + result);
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка проверки даты выдачи", e);
            return false;
        }
    }

    private boolean isValidIssuedBy(String issuedBy) {
        // Список реальных органов выдачи паспортов
        String[] validPatterns = {
                "ОВД", "УВД", "МВД", "РОВД", "ГУВД", "ФМС", "УФМС", "ГУФМС",
                "ОТДЕЛЕНИЕ ПО ВОПРОСАМ МИГРАЦИИ", "ОТДЕЛ ПО ВОПРОСАМ МИГРАЦИИ",
                "МРО УФМС", "ОТДЕЛЕНИЕ УФМС", "ОТДЕЛ УФМС"
        };

        String upperIssuedBy = issuedBy.toUpperCase();
        for (String pattern : validPatterns) {
            if (upperIssuedBy.contains(pattern)) {
                return true;
            }
        }

        // Проверка на наличие региональных подразделений
        if (upperIssuedBy.contains("РОССИИ") || upperIssuedBy.contains("СУБЪЕКТ")) {
            return true;
        }

        return false;
    }

    private void addNewPassport(String... data) {
        if (!isConnectionActive) {
            showToast("Нет соединения с сервером");
            return;
        }

        // Проверяем наличие ключа шифрования
        if (encryptionKey == null || encryptionKey.isEmpty()) {
            Log.e(TAG, "Ключ шифрования не установлен перед сохранением паспорта");
            showToast("Ошибка: ключ шифрования не установлен");
            return;
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "Начало сохранения паспорта для пользователя: " + phoneNumber);
                Log.d(TAG, "Ключ шифрования: " + encryptionKey);

                // 1. Шифруем данные
                String[] encryptedData = new String[6];
                for (int i = 0; i < data.length; i++) {
                    Log.d(TAG, "Шифрование поля " + i + ": " + data[i]);
                    encryptedData[i] = encrypt(data[i]);
                    if (encryptedData[i] == null) {
                        Log.e(TAG, "Ошибка шифрования данных для поля " + i + ": " + data[i]);
                        throw new SQLException("Ошибка шифрования данных для поля " + i);
                    }
                    Log.d(TAG, "Зашифровано поле " + i);
                }

                // 2. Вставляем данные паспорта
                String insertQuery = "INSERT INTO passports (user_passport_phone_number, user_passport_name, " +
                        "user_passport_birthday, user_passport_serial_number, user_passport_date_of_issue, " +
                        "user_passport_code, user_passport_issued) VALUES (?, ?, ?, ?, ?, ?, ?)";

                Log.d(TAG, "Выполнение запроса вставки паспорта");

                PreparedStatement insertStmt = dbConnection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS);
                insertStmt.setString(1, phoneNumber);
                for (int i = 0; i < encryptedData.length; i++) {
                    insertStmt.setString(i + 2, encryptedData[i]);
                    Log.d(TAG, "Установлен параметр " + (i + 2) + ": " + (encryptedData[i] != null ? "Зашифрованные данные" : "NULL"));
                }
                int affectedRows = insertStmt.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Не удалось вставить данные паспорта");
                }

                // 3. Получаем ID вставленного паспорта
                ResultSet generatedKeys = insertStmt.getGeneratedKeys();
                if (!generatedKeys.next()) {
                    throw new SQLException("Не удалось получить ID паспорта");
                }

                String passportId = generatedKeys.getString(1);
                Log.d(TAG, "Паспорт успешно вставлен с ID: " + passportId);

                // 4. Обновляем запись пользователя
                String updateQuery = "UPDATE users SET have_a_passport = ? WHERE phone_number = ?";
                Log.d(TAG, "Обновление записи пользователя с ID паспорта: " + passportId);

                PreparedStatement updateStmt = dbConnection.prepareStatement(updateQuery);
                updateStmt.setString(1, passportId);
                updateStmt.setString(2, phoneNumber);
                affectedRows = updateStmt.executeUpdate();

                if (affectedRows == 0) {
                    throw new SQLException("Не удалось обновить данные пользователя");
                }

                Log.d(TAG, "Пользователь успешно обновлен");

                runOnUiThread(() -> {
                    showToast("Паспорт успешно добавлен");
                    checkUserPassport();
                });
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка при сохранении паспорта", e);
                runOnUiThread(() -> {
                    if (e.getMessage().contains("Duplicate entry")) {
                        showToast("Паспорт уже существует для этого пользователя");
                    } else {
                        showToast("Ошибка при сохранении паспорта: " + e.getMessage());
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Неожиданная ошибка при сохранении паспорта", e);
                runOnUiThread(() -> showToast("Ошибка при сохранении данных: " + e.getMessage()));
            }
        }).start();
    }

    private String generateNewKey() {
        try {
            String seed = phoneNumber + System.currentTimeMillis();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(seed.getBytes("UTF-8"));
            String key = Base64.encodeToString(hash, Base64.NO_WRAP).substring(0, 32);
            Log.d(TAG, "Сгенерирован новый ключ: " + key);
            return key;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка генерации ключа", e);
            return null;
        }
    }

    private String encrypt(String data) {
        if (data == null || data.trim().isEmpty()) {
            Log.w(TAG, "Попытка зашифровать пустые данные");
            return null;
        }

        if (encryptionKey == null || encryptionKey.isEmpty()) {
            Log.e(TAG, "Ключ шифрования не установлен при попытке шифрования");
            return null;
        }

        try {
            Log.d(TAG, "Начало шифрования данных. Длина ключа: " + encryptionKey.length());
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes("UTF-8"));
            String result = Base64.encodeToString(encrypted, Base64.NO_WRAP);
            Log.d(TAG, "Шифрование успешно завершено");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка шифрования данных: " + data, e);
            return null;
        }
    }

    private String decrypt(String encryptedData) {
        if (encryptedData == null || encryptedData.trim().isEmpty()) {
            Log.w(TAG, "Попытка расшифровать пустые данные");
            return null;
        }

        if (encryptionKey == null || encryptionKey.isEmpty()) {
            Log.e(TAG, "Ключ шифрования не установлен при попытке расшифрования");
            return null;
        }

        try {
            Log.d(TAG, "Начало расшифрования данных");
            SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes("UTF-8"), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.decode(encryptedData, Base64.NO_WRAP);
            byte[] decrypted = cipher.doFinal(decoded);
            String result = new String(decrypted, "UTF-8");
            Log.d(TAG, "Расшифрование успешно завершено");
            return result;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка расшифрования данных", e);
            return null;
        }
    }

    private void showPassportCard(String[] data) {
        try {
            View passportCard = getLayoutInflater().inflate(R.layout.passport_card, null);
            if (passportCard == null) {
                throw new RuntimeException("Не удалось загрузить макет карточки паспорта");
            }

            String[] nameParts = new String[3];
            if (data[0] != null) {
                String[] tempParts = data[0].split(" ");
                System.arraycopy(tempParts, 0, nameParts, 0, Math.min(tempParts.length, 3));
            }

            TextView lastNameTextView = passportCard.findViewById(R.id.lastNameTextView);
            TextView firstNameTextView = passportCard.findViewById(R.id.firstNameTextView);
            TextView middleNameTextView = passportCard.findViewById(R.id.middleNameTextView);
            TextView birthDateTextView = passportCard.findViewById(R.id.birthDateTextView);
            TextView serialNumberTextView = passportCard.findViewById(R.id.serialNumberTextView);
            TextView issueDateTextView = passportCard.findViewById(R.id.issueDateTextView);
            TextView departmentCodeTextView = passportCard.findViewById(R.id.departmentCodeTextView);
            TextView issuedByTextView = passportCard.findViewById(R.id.issuedByTextView);
            ImageView deleteCardButton = passportCard.findViewById(R.id.deleteCardButton);

            if (lastNameTextView != null) lastNameTextView.setText(nameParts[0] != null ? nameParts[0] : "");
            if (firstNameTextView != null) firstNameTextView.setText(nameParts[1] != null ? nameParts[1] : "");
            if (middleNameTextView != null) middleNameTextView.setText(nameParts[2] != null ? nameParts[2] : "");
            if (birthDateTextView != null) birthDateTextView.setText(data[1] != null ? data[1] : "");
            if (serialNumberTextView != null) serialNumberTextView.setText(data[2] != null ? data[2] : "");
            if (issueDateTextView != null) issueDateTextView.setText(data[3] != null ? data[3] : "");
            if (departmentCodeTextView != null) departmentCodeTextView.setText(data[4] != null ? data[4] : "");
            if (issuedByTextView != null) issuedByTextView.setText(data[5] != null ? data[5] : "");

            if (deleteCardButton != null) {
                deleteCardButton.setOnClickListener(v -> {
                    showDeletePassportDialog();
                });
            }

            cardContainer.removeAllViews();
            cardContainer.addView(passportCard);
            addNewPassportButton.setVisibility(View.INVISIBLE);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка отображения карточки паспорта", e);
            showToast("Ошибка отображения данных паспорта");
        }
    }

    private void showDeletePassportDialog() {
        try {
            final Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.dialog_delete_passport_confirm);

            // Настройка диалога на полную ширину
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

            dialog.setCancelable(true);

            TextView titleTextView = dialog.findViewById(R.id.titleTextView);
            TextView privacyPolicyLink = dialog.findViewById(R.id.privacyPolicyLink);
            Button declineButton = dialog.findViewById(R.id.declineButton);
            Button acceptButton = dialog.findViewById(R.id.acceptButton);

            if (titleTextView != null) {
                titleTextView.setText("Удаление паспорта");
            }

            if (privacyPolicyLink != null) {
                privacyPolicyLink.setText("Вы удаляете паспорт, это действие\nнельзя отменить.");
            }

            if (declineButton != null) {
                declineButton.setEnabled(true);
                declineButton.setOnClickListener(v -> dialog.dismiss());
            }

            if (acceptButton != null) {
                acceptButton.setEnabled(true);
                acceptButton.setOnClickListener(v -> {
                    deletePassportData();
                    dialog.dismiss();
                });
            }

            dialog.show();
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при создании диалога удаления", e);
            showToast("Ошибка при создании диалога удаления");
        }
    }

    private void deletePassportData() {
        if (!isConnectionActive) {
            showToast("Нет соединения с сервером");
            return;
        }

        new Thread(() -> {
            try {
                Log.d(TAG, "Начало удаления паспорта для пользователя: " + phoneNumber);

                // 1. Удаляем ключ шифрования
                String deleteKeyQuery = "DELETE FROM server_passport_keys WHERE user_number = ?";
                Log.d(TAG, "Удаление ключа шифрования");
                PreparedStatement deleteKeyStmt = dbConnection.prepareStatement(deleteKeyQuery);
                deleteKeyStmt.setString(1, phoneNumber);
                int affectedRows = deleteKeyStmt.executeUpdate();

                // 2. Удаляем данные паспорта
                String deletePassportQuery = "DELETE FROM passports WHERE user_passport_phone_number = ?";
                Log.d(TAG, "Удаление данных паспорта");
                PreparedStatement deletePassportStmt = dbConnection.prepareStatement(deletePassportQuery);
                deletePassportStmt.setString(1, phoneNumber);
                affectedRows += deletePassportStmt.executeUpdate();

                // 3. Обнуляем поле have_a_passport у пользователя
                String updateUserQuery = "UPDATE users SET have_a_passport = NULL WHERE phone_number = ?";
                Log.d(TAG, "Обновление записи пользователя");
                PreparedStatement updateUserStmt = dbConnection.prepareStatement(updateUserQuery);
                updateUserStmt.setString(1, phoneNumber);
                affectedRows += updateUserStmt.executeUpdate();

                if (affectedRows > 0) {
                    Log.d(TAG, "Паспорт успешно удален");
                    runOnUiThread(() -> {
                        showToast("Данные паспорта успешно удалены");
                        cardContainer.removeAllViews();
                        addNewPassportButton.setVisibility(View.VISIBLE);
                        encryptionKey = null; // Очищаем ключ после удаления
                    });
                } else {
                    throw new SQLException("Не удалось удалить данные паспорта");
                }
            } catch (SQLException e) {
                Log.e(TAG, "Ошибка при удалении паспорта", e);
                runOnUiThread(() -> showToast("Ошибка при удалении паспорта: " + e.getMessage()));
            }
        }).start();
    }

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(PassportActivity.this, message, Toast.LENGTH_LONG).show());
    }

    interface OnKeyFetchedListener {
        void onKeyFetched(String key);
    }
}