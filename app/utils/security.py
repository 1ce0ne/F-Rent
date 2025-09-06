import os
import jwt
import secrets
from datetime import datetime, timedelta
from fastapi import HTTPException, Header
from typing import Optional
from dotenv import load_dotenv
from itsdangerous import Signer

load_dotenv()

# Конфигурация
SECRET_KEY = os.getenv("SECRET_KEY", "your-secret-key-here")
MOBILE_API_KEY = os.getenv("MOBILE_API_KEY", "your-mobile-api-key")
ALGORITHM = "HS256"
ACCESS_TOKEN_EXPIRE_HOURS = 24

# Signer для подписи данных
signer = Signer(SECRET_KEY)

def create_access_token(data: dict, expires_delta: Optional[timedelta] = None):
    """Создание JWT токена"""
    to_encode = data.copy()
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(hours=ACCESS_TOKEN_EXPIRE_HOURS)

    to_encode.update({"exp": expire})
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, algorithm=ALGORITHM)
    return encoded_jwt

def verify_token(token: str):
    """Проверка JWT токена"""
    try:
        payload = jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except jwt.PyJWTError:
        raise HTTPException(status_code=401, detail="Неверный токен")

def verify_api_key(x_api_key: str = Header(...)):
    """Проверка API ключа для мобильного приложения"""
    if x_api_key != MOBILE_API_KEY:
        raise HTTPException(status_code=401, detail="Неверный API ключ")
    return x_api_key

async def get_current_user(request):
    """Получение текущего пользователя из JWT токена"""
    from app.datac.db_session import create_session
    from app.datac.__all_models import User

    # Получаем токен из заголовка Authorization
    authorization = request.headers.get("Authorization")
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Токен не найден")

    token = authorization.split(" ")[1]
    payload = verify_token(token)
    user_id = int(payload.get("sub"))

    session = create_session()
    try:
        user = session.query(User).filter_by(id=user_id).first()
        if not user:
            raise HTTPException(status_code=404, detail="Пользователь не найден")
        return user
    finally:
        session.close()
