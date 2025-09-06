# Интеграция авторизации через Telegram в Android-приложение

## Обзор API

API авторизации через Telegram предоставляет три основных эндпоинта:

| Эндпоинт | Метод | Описание |
|----------|-------|----------|
| `/mobile/auth/telegram-login` | GET | Страница с Telegram Login Widget |
| `/mobile/auth/telegram-login` | POST | Обработка данных авторизации от Telegram |
| `/mobile/auth/check-ban` | GET | Проверка статуса блокировки пользователя |

## Детальное описание API-эндпоинтов

### 1. Получение страницы авторизации Telegram

**Эндпоинт:** `GET /mobile/auth/telegram-login`

**GET-параметры:**
- `api_key` (обязательный) - API-ключ для авторизации запроса
- `redirect_url` (опциональный) - URL для редиректа после успешной авторизации
- `app_name` (опциональный) - название приложения для отображения на странице авторизации
- `theme` (опциональный) - тема оформления: "light" или "dark" (по умолчанию "light")

**Пример запроса:**
```
GET /mobile/auth/telegram-login?api_key=your_api_key&theme=dark&app_name=AccuBatt
```

**Ответ:** HTML-страница с Telegram Login Widget

**Особенности:**
- Страница содержит JavaScript, который обрабатывает данные от Telegram
- После успешной авторизации данные передаются в приложение через JavaScript-интерфейс

### 2. Обработка данных авторизации от Telegram

**Эндпоинт:** `POST /mobile/auth/telegram-login`

**Заголовки:**
- `X-API-Key` - API-ключ для авторизации запроса
- `Content-Type: application/json`

**Тело запроса (пример):**
```json
{
  "id": 123456789,
  "username": "user_name",
  "first_name": "Иван",
  "last_name": "Иванов",
  "phone_number": "+79991234567",
  "auth_date": 1672531200,
  "hash": "abc123def456..."
}
```

**Ответ (успешная авторизация):**
```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user_id": 123,
  "session_id": "random_session_string",
  "banned": false,
  "ban_reason": null,
  "message": "Успешная авторизация через Telegram"
}
```

### 3. Проверка статуса блокировки пользователя

**Эндпоинт:** `GET /mobile/auth/check-ban`

**GET-параметры:**
- `api_key` (обязательный) - API-ключ для авторизации запроса (альтернатива заголовку X-API-Key)
- `user_id` (опциональный) - ID пользователя для проверки (если не указан, используется ID из токена)

**Заголовки:**
- `X-API-Key` - API-ключ для авторизации запроса (альтернатива GET-параметру api_key)
- `Authorization: Bearer <token>` - JWT-токен пользователя

**Пример запроса:**
```
GET /mobile/auth/check-ban?api_key=your_api_key
```

**Ответ:**
```json
{
  "banned": false,
  "ban_reason": null,
  "user_id": 123
}
```

## Процесс авторизации

1. Приложение открывает WebView с Telegram Login Widget
2. Пользователь авторизуется через официальный OAuth Telegram
3. Telegram возвращает данные пользователя на страницу
4. JavaScript отправляет данные в приложение через интерфейс
5. Приложение получает JWT токен для дальнейших запросов

## Интеграция на Android

### 1. Создание Activity с WebView

```java
public class TelegramAuthActivity extends AppCompatActivity {

    private WebView webView;
    private static final String BASE_URL = "https://yourserver.com";
    private static final String API_KEY = "ваш_api_ключ";
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_telegram_auth);
        
        webView = findViewById(R.id.webview);
        setupWebView();
        loadAuthPage();
    }
    
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        
        // Разрешаем отладку WebView в debug-сборках
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true);
        }
        
        // Обрабатываем ссылки внутри WebView
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
        });
        
        // Интерфейс для получения данных от JavaScript
        webView.addJavascriptInterface(new TelegramAuthInterface(), "AndroidInterface");
    }
    
    private void loadAuthPage() {
        String url = BASE_URL + "/mobile/auth/telegram-login";
        webView.loadUrl(url);
    }
    
    // Интерфейс для получения данных от JavaScript
    private class TelegramAuthInterface {
        
        @JavascriptInterface
        public void onTelegramAuth(String authDataJson) {
            Log.d("TelegramAuth", "Received auth data: " + authDataJson);
            
            try {
                JSONObject jsonData = new JSONObject(authDataJson);
                boolean success = jsonData.getBoolean("success");
                
                if (success) {
                    // Получаем данные авторизации
                    String token = jsonData.getString("token");
                    int userId = jsonData.getInt("user_id");
                    String sessionId = jsonData.getString("session_id");
                    boolean banned = jsonData.getBoolean("banned");
                    String banReason = jsonData.optString("ban_reason", null);
                    
                    // Сохраняем данные
                    saveAuthData(token, userId, sessionId, banned, banReason);
                    
                    // Переходим на главный экран или показываем сообщение о бане
                    runOnUiThread(() -> {
                        if (banned) {
                            showBanMessage(banReason);
                        } else {
                            navigateToMainScreen();
                        }
                    });
                } else {
                    String errorMessage = jsonData.optString("message", "Ошибка авторизации");
                    runOnUiThread(() -> showError(errorMessage));
                }
                
            } catch (JSONException e) {
                Log.e("TelegramAuth", "Error parsing auth data", e);
                runOnUiThread(() -> showError("Ошибка обработки данных"));
            }
        }
    }
    
    private void saveAuthData(String token, int userId, String sessionId, boolean banned, String banReason) {
        SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        editor.putString("token", token);
        editor.putInt("user_id", userId);
        editor.putString("session_id", sessionId);
        editor.putBoolean("banned", banned);
        
        if (banReason != null) {
            editor.putString("ban_reason", banReason);
        }
        
        editor.apply();
    }
    
    private void navigateToMainScreen() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
    
    private void showBanMessage(String reason) {
        new AlertDialog.Builder(this)
            .setTitle("Аккаунт заблокирован")
            .setMessage("Ваш аккаунт заблокирован: " + (reason != null ? reason : "причина не указана"))
            .setPositiveButton("ОК", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }
    
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
```

### 2. Layout для WebView

```xml
<!-- activity_telegram_auth.xml -->
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical">

    <androidx.appcompat.widget.Toolbar
        android:id="@+id/toolbar"
        android:layout_width="match_parent"
        android:layout_height="?attr/actionBarSize"
        android:background="?attr/colorPrimary"
        android:elevation="4dp" />

    <WebView
        android:id="@+id/webview"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />

</LinearLayout>
```

### 3. Класс для API запросов с токеном

```java
public class ApiClient {
    
    private static final String BASE_URL = "https://yourserver.com";
    private static final String API_KEY = "ваш_api_ключ";
    private static OkHttpClient client;
    
    static {
        client = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build();
    }
    
    /**
     * Выполняет запрос к API с JWT токеном
     */
    public static void request(Context context, String endpoint, String method, JSONObject requestBody, ApiCallback callback) {
        String token = getAuthToken(context);
        if (token == null) {
            callback.onError("Отсутствует токен авторизации");
            return;
        }
        
        Request.Builder requestBuilder = new Request.Builder()
            .url(BASE_URL + endpoint)
            .addHeader("Authorization", "Bearer " + token)
            .addHeader("X-API-Key", API_KEY);
        
        if (requestBody != null && !method.equals("GET")) {
            RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
            );
            
            switch (method) {
                case "POST":
                    requestBuilder.post(body);
                    break;
                case "PUT":
                    requestBuilder.put(body);
                    break;
                case "DELETE":
                    requestBuilder.delete(body);
                    break;
            }
        } else if (method.equals("DELETE")) {
            requestBuilder.delete();
        }
        
        client.newCall(requestBuilder.build()).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e.getMessage());
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseBody = response.body().string();
                if (response.isSuccessful()) {
                    callback.onSuccess(responseBody);
                } else {
                    callback.onError("HTTP Error: " + response.code() + " - " + responseBody);
                }
            }
        });
    }
    
    /**
     * Проверяет, забанен ли пользователь
     */
    public static void checkUserBan(Context context, BanCallback callback) {
        request(context, "/mobile/auth/check-ban", "GET", null, new ApiCallback() {
            @Override
            public void onSuccess(String response) {
                try {
                    JSONObject data = new JSONObject(response);
                    boolean banned = data.getBoolean("banned");
                    String banReason = data.optString("ban_reason", null);
                    int userId = data.getInt("user_id");
                    
                    callback.onResult(banned, banReason, userId);
                } catch (JSONException e) {
                    callback.onError("Ошибка обработки ответа");
                }
            }
            
            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }
    
    /**
     * Получает токен из SharedPreferences
     */
    private static String getAuthToken(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("auth", Context.MODE_PRIVATE);
        return prefs.getString("token", null);
    }
    
    /**
     * Проверяет, авторизован ли пользователь
     */
    public static boolean isUserAuthorized(Context context) {
        return getAuthToken(context) != null;
    }
    
    /**
     * Выход из аккаунта - очистка данных авторизации
     */
    public static void logout(Context context) {
        context.getSharedPreferences("auth", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply();
    }
    
    public interface ApiCallback {
        void onSuccess(String response);
        void onError(String error);
    }
    
    public interface BanCallback {
        void onResult(boolean banned, String reason, int userId);
        void onError(String error);
    }
}
```

### 4. Манифест и разрешения

```xml
<!-- В AndroidManifest.xml -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<application>
    <!-- ... -->
    <activity 
        android:name=".auth.TelegramAuthActivity"
        android:label="Вход через Telegram"
        android:theme="@style/Theme.AppCompat.Light.DarkActionBar" />
    <!-- ... -->
</application>
```

## Проверка и обработка статуса бана

Для регулярной проверки, не был ли пользователь забанен, используйте такой подход:

```java
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onResume() {
        super.onResume();
        checkUserBanStatus();
    }
    
    private void checkUserBanStatus() {
        ApiClient.checkUserBan(this, new ApiClient.BanCallback() {
            @Override
            public void onResult(boolean banned, String reason, int userId) {
                if (banned) {
                    runOnUiThread(() -> {
                        new AlertDialog.Builder(MainActivity.this)
                            .setTitle("Аккаунт заблокирован")
                            .setMessage("Ваш аккаунт был заблокирован: " + 
                                       (reason != null ? reason : "причина не указана"))
                            .setPositiveButton("Выйти", (dialog, which) -> {
                                ApiClient.logout(MainActivity.this);
                                // Перенаправляем на экран авторизации
                                Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                                startActivity(intent);
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                    });
                }
            }
            
            @Override
            public void onError(String error) {
                Log.e("BanCheck", "Error checking ban status: " + error);
                // Можно решить, нужно ли показывать ошибку пользователю
            }
        });
    }
}
```

## Данные, возвращаемые API

### 1. Ответ при успешной авторизации

```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user_id": 123,
  "session_id": "random_session_string",
  "banned": false,
  "ban_reason": null,
  "message": "Успешная авторизация через Telegram"
}
```

### 2. Ответ при блокировке пользователя

```json
{
  "success": true,
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user_id": 123,
  "session_id": "random_session_string",
  "banned": true,
  "ban_reason": "Нарушение правил использования сервиса",
  "message": "Пользователь заблокирован"
}
```

### 3. Ответ API проверки бана

```json
{
  "banned": true,
  "ban_reason": "Нарушение правил использования сервиса",
  "user_id": 123
}
```

## Обработка ошибок

| Код ошибки | Описание | Действие |
|------------|----------|----------|
| 400 | Неверная подпись от Telegram | Перезапустить авторизацию |
| 401 | Неверный API ключ или токен | Проверить API ключ или перелогиниться |
| 500 | Внутренняя ошибка сервера | Попробовать позже |

## Советы по отладке

1. Включите отладку WebView:
```java
if (BuildConfig.DEBUG) {
    WebView.setWebContentsDebuggingEnabled(true);
}
```

2. Добавьте логирование всех этапов авторизации:
```java
Log.d("TelegramAuth", "Loading auth page: " + url);
Log.d("TelegramAuth", "Received data: " + authDataJson);
```

3. Проверяйте данные авторизации в SharedPreferences:
```java
SharedPreferences prefs = getSharedPreferences("auth", MODE_PRIVATE);
String token = prefs.getString("token", null);
Log.d("Auth", "Token: " + (token != null ? "exists" : "missing"));
```

## Настройки безопасности

1. Всегда проверяйте SSL сертификаты при запросах
2. Не сохраняйте токен в публичных местах
3. Используйте `android:usesCleartextTraffic="false"` в манифесте
4. Проверяйте подлинность ответов от сервера
