# Мобильное API документация (обновленная версия)

## Базовая информация

**Base URL:** `https://akkubatt-work.ru`  
**API Key:** Требуется для всех запросов в заголовке `X-API-Key`  
**Authorization:** JWT токен в заголовке `Authorization: Bearer <token>`

## Аутентификация

### POST `/mobile/auth/login`
Авторизация пользователя

**Заголовки:**
- `X-API-Key: <api_key>`

**Параметры:**
```json
{
  "phone_number": "string",
  "password": "string"
}
```

**Успешный ответ:**
```json
{
  "success": true,
  "token": "jwt_token_string",
  "user_id": 123,
  "name": "Имя пользователя",
  "banned": false,
  "ban_reason": null,
  "message": "Успешная авторизация"
}
```

**Ответ при заблокированном пользователе:**
```json
{
  "success": false,
  "token": null,
  "user_id": null,
  "name": null,
  "banned": true,
  "ban_reason": "Нарушение правил использования",
  "message": "Аккаунт заблокирован"
}
```

**Ответ при неверных данных:**
```json
{
  "success": false,
  "token": null,
  "user_id": null,
  "name": null,
  "banned": null,
  "ban_reason": null,
  "message": "Пользователь с таким номером телефона не найден"
}
```

### POST `/mobile/auth/register`
Регистрация нового пользователя

**Заголовки:**
- `X-API-Key: <api_key>`

**Параметры:**
```json
{
  "name": "string",
  "phone_number": "string", 
  "password": "string"
}
```

**Успешный ответ:**
```json
{
  "success": true,
  "token": "jwt_token_string",
  "user_id": 123,
  "name": "Имя пользователя",
  "banned": false,
  "ban_reason": null,
  "message": "Регистрация прошла успешно"
}
```

**Ответ при существующем пользователе:**
```json
{
  "success": false,
  "token": null,
  "user_id": null,
  "name": null,
  "banned": null,
  "ban_reason": null,
  "message": "Пользователь с таким номером телефона уже существует"
}
```

### GET `/mobile/auth/ban-status`
Проверка статуса бана пользователя по JWT токену

**Заголовки:**
- `X-API-Key: <api_key>`
- `Authorization: Bearer <jwt_token>`

**Ответ для незаблокированного пользователя:**
```json
{
  "success": true,
  "user_id": 123,
  "banned": false,
  "ban_reason": null,
  "message": "Статус бана получен успешно"
}
```

**Ответ для заблокированного пользователя:**
```json
{
  "success": true,
  "user_id": 123,
  "banned": true,
  "ban_reason": "Нарушение правил использования",
  "message": "Статус бана получен успешно"
}
```

**Ответ при невалидном токене:**
```json
{
  "detail": "Invalid token"
}
```

## Профиль пользователя

### GET `/mobile/profile/me`
Получение информации о текущем пользователе

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Ответ:**
```json
{
  "success": true,
  "user_id": 123,
  "name": "Имя пользователя",
  "phone_number": "79001234567",
  "has_passport": false,
  "banned": false,
  "message": "Профиль получен успешно"
}
```

### PUT `/mobile/profile/update`
Обновление профиля пользователя

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Параметры (все опциональные):**
```json
{
  "name": "string",
  "phone_number": "string",
  "password": "string"
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Профиль обновлен успешно"
}
```

## Паспорта

### POST `/mobile/passport/upload-photo`
Загрузка фотографии паспорта (максимум 64MB)

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Параметры (form-data):**
- `photo`: файл изображения
- `photo_type`: integer (0 = основная страница, 1 = страница с пропиской)
- `phone_number`: string

**Ответ:**
```json
{
  "success": true,
  "message": "Passport photo uploaded successfully",
  "photo_id": 123,
  "photo_type": 0,
  "file_size": 1024000
}
```

### GET `/mobile/passport/status`
Получение статуса паспорта пользователя

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Ответ:**
```json
{
  "success": true,
  "passport_status": 1,
  "passport_status_name": "under_review",
  "photos_uploaded": true,
  "main_page_uploaded": true,
  "registration_page_uploaded": false,
  "message": "Passport status retrieved successfully"
}
```

**Статусы паспорта:**
- `0` / `"not_added"` - паспорт не добавлен
- `1` / `"under_review"` - на модерации
- `2` / `"rejected"` - отклонен
- `3` / `"approved"` - одобрен

## Товары

### GET `/mobile/products/by-category/{category_name}`
Получение товаров по категории

**Заголовки:**
- `X-API-Key: <api_key>`

**Ответ:**
```json
{
  "success": true,
  "products": [
    {
      "id": 123,
      "name": "Товар",
      "description": "Описание",
      "price_per_hour": 100.0,
      "price_per_day": 500.0,
      "price_per_month": 3000.0,
      "category": "electronics",
      "size": "medium",
      "image_base64": "base64_string",
      "office_id": 1,
      "address_of_postamat": "Адрес",
      "is_available": true,
      "have_problem": false
    }
  ],
  "message": "Найдено 5 товаров в категории 'electronics'"
}
```

### GET `/mobile/products/{product_id}`
Получение детальной информации о товаре

**Заголовки:**
- `X-API-Key: <api_key>`

**Ответ:**
```json
{
  "id": 123,
  "name": "Товар",
  "description": "Описание",
  "price_per_hour": 100.0,
  "price_per_day": 500.0,
  "price_per_month": 3000.0,
  "category": "electronics",
  "size": "medium",
  "image_base64": "base64_string",
  "office_id": 1,
  "address_of_postamat": "Адрес",
  "is_available": true,
  "have_problem": false
}
```

## Постаматы

### GET `/mobile/postamats/list`
Получение списка всех постаматов

**Заголовки:**
- `X-API-Key: <api_key>`

**Ответ:**
```json
{
  "success": true,
  "postamats": [
    {
      "id": 1,
      "address": "ул. Примерная, 1",
      "name": "Постамат №1",
      "is_active": true,
      "total_cells": 50,
      "available_cells": 25,
      "coordinates": {
        "latitude": 55.7558,
        "longitude": 37.6176
      }
    }
  ],
  "message": "Список постаматов получен успешно"
}
```

### GET `/mobile/postamats/{postamat_id}/products`
Получение товаров в конкретном постамате

**Заголовки:**
- `X-API-Key: <api_key>`

**Ответ:**
```json
{
  "success": true,
  "postamat_info": {
    "id": 1,
    "address": "ул. Примерная, 1",
    "name": "Постамат №1"
  },
  "products": [
    {
      "id": 123,
      "name": "Товар",
      "price_per_hour": 100.0,
      "price_per_day": 500.0,
      "price_per_month": 3000.0,
      "category": "electronics",
      "size": "medium",
      "is_available": true
    }
  ],
  "message": "Найдено 10 товаров в постамате"
}
```

## Аренда

### POST `/mobile/rental/create`
Создание нового заказа на аренду

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Параметры:**
```json
{
  "product_id": 123,
  "rental_period": "hour|day|month",
  "rental_duration": 1,
  "pickup_address": "string"
}
```

**Ответ:**
```json
{
  "success": true,
  "order_id": 456,
  "total_cost": 500.0,
  "pickup_code": "ABC123",
  "estimated_pickup_time": "2023-12-01T15:00:00",
  "message": "Заказ создан успешно"
}
```

### GET `/mobile/rental/my-orders`
Получение списка заказов пользователя

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Ответ:**
```json
{
  "success": true,
  "orders": [
    {
      "order_id": 456,
      "product_name": "Товар",
      "status": "active",
      "rental_start": "2023-12-01T10:00:00",
      "rental_end": "2023-12-02T10:00:00",
      "total_cost": 500.0,
      "pickup_code": "ABC123",
      "return_code": "DEF456"
    }
  ],
  "message": "Список заказов получен успешно"
}
```

### POST `/mobile/rental/{order_id}/return`
Возврат товара

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Параметры:**
```json
{
  "return_address": "string",
  "condition_notes": "string"
}
```

**Ответ:**
```json
{
  "success": true,
  "return_code": "DEF456",
  "final_cost": 450.0,
  "message": "Товар возвращен успешно"
}
```

## Платежи

### POST `/mobile/payments/create`
Создание платежа

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Параметры:**
```json
{
  "order_id": 456,
  "amount": 500.0,
  "payment_method": "card"
}
```

**Ответ:**
```json
{
  "success": true,
  "payment_id": 789,
  "payment_url": "https://payment.url",
  "amount": 500.0,
  "message": "Платеж создан успешно"
}
```

### GET `/mobile/payments/history`
История платежей пользователя

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Ответ:**
```json
{
  "success": true,
  "payments": [
    {
      "payment_id": 789,
      "order_id": 456,
      "amount": 500.0,
      "status": "completed",
      "payment_date": "2023-12-01T10:30:00",
      "payment_method": "card"
    }
  ],
  "message": "История платежей получена успешно"
}
```

## Telegram интеграция

### POST `/mobile/telegram/link`
Привязка Telegram аккаунта для уведомлений

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Параметры:**
```json
{
  "telegram_username": "username"
}
```

**Ответ:**
```json
{
  "success": true,
  "verification_code": "ABC123",
  "message": "Отправьте код боту @YourBot"
}
```

### POST `/mobile/telegram/verify`
Подтверждение Telegram аккаунта

**Заголовки:** 
- `X-API-Key: <api_key>`
- `Authorization: Bearer <token>`

**Параметры:**
```json
{
  "verification_code": "ABC123"
}
```

**Ответ:**
```json
{
  "success": true,
  "message": "Telegram аккаунт успешно привязан"
}
```

## Коды ошибок

- `400` - Неверные параметры запроса
- `401` - Неавторизованный доступ (неверный/отсутствующий API ключ или JWT токен)
- `403` - Доступ запрещен
- `404` - Ресурс не найден
- `500` - Внутренняя ошибка сервера

## Примечания по интеграции

1. **API Key** обязателен для всех запросов в заголовке `X-API-Key`
2. **JWT токен** требуется для операций, требующих авторизации, передается в заголовке `Authorization: Bearer <token>`
3. **JWT токены** не имеют срока истечения (unlimited lifetime)
4. **Размер изображений паспорта** - максимум 64MB
5. **Статус бана** проверяется при каждом логине и доступен через отдельный endpoint
6. **Все даты** в формате ISO 8601 (YYYY-MM-DDTHH:MM:SS)
7. **Номера телефонов** передаются в строковом формате (только цифры)
8. **Координаты** в формате WGS84 (latitude, longitude)
9. **Поля banned и ban_reason** возвращаются во всех операциях аутентификации
10. **При блокировке пользователя** логин возвращает success: false с указанием причины бана

## Изменения в текущей версии

- ✅ Добавлен новый endpoint `/mobile/auth/ban-status` для проверки статуса бана по токену
- ✅ В ответе логина добавлены поля `banned` и `ban_reason`
- ✅ При блокировке пользователя логин возвращает детальную информацию о бане
- ✅ Исправлена типизация всех полей ответа (добавлен Optional для корректной работы с null)
- ✅ Обновлены примеры ответов с корректными структурами данных
- ✅ Добавлено указание обязательности API ключа для всех запросов
- ✅ Уточнена информация о JWT токенах (unlimited lifetime)
