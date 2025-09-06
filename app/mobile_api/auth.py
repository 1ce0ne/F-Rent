from functools import wraps
from fastapi import HTTPException, Request, Header
from typing import Optional
import os
from dotenv import load_dotenv

# Загружаем переменные окружения
load_dotenv()

# API ключ для мобильного приложения из .env файла
MOBILE_API_KEY = os.getenv("API_KEY")

def verify_api_key(x_api_key: Optional[str] = Header(None)):
    """
    Проверка API ключа для мобильного приложения
    Ключ должен передаваться в заголовке X-API-Key
    """
    if not x_api_key or x_api_key != MOBILE_API_KEY:
        raise HTTPException(
            status_code=401,
            detail="Invalid or missing API key"
        )
    return True

def mobile_api_key_required(func):
    """
    Декоратор для проверки API ключа в мобильных API
    """
    @wraps(func)
    async def wrapper(*args, **kwargs):
        # API ключ проверяется через Depends в параметрах функции
        return await func(*args, **kwargs)
    return wrapper
