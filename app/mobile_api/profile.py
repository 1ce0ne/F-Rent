from fastapi import APIRouter, Depends, HTTPException, Header
from pydantic import BaseModel
from typing import Optional
import jwt
import os
from dotenv import load_dotenv

from app.mobile_api.auth import verify_api_key
from app.datac.db_session import create_session
from app.datac.__all_models import User, Passport

# Загружаем переменные окружения
load_dotenv()

router = APIRouter(prefix="/mobile/profile", tags=["Mobile Profile"])

# JWT настройки
JWT_SECRET = os.getenv("SECRET_KEY", "CoJoaWaCmZ25mw{PoY%*f~7O9Eet")
JWT_ALGORITHM = "HS256"

class ProfileResponse(BaseModel):
    success: bool
    user_id: Optional[int] = None
    name: Optional[str] = None
    phone_number: Optional[str] = None
    has_passport: bool = False
    banned: bool = False
    message: str

class UpdateProfileRequest(BaseModel):
    name: Optional[str] = None
    phone_number: Optional[str] = None
    password: Optional[str] = None

class StandardResponse(BaseModel):
    success: bool
    message: str

def get_user_from_token(authorization: str) -> Optional[dict]:
    """Извлечение данных пользователя из JWT токена"""
    try:
        if not authorization.startswith("Bearer "):
            raise HTTPException(status_code=401, detail="Invalid authorization header")

        token = authorization.replace("Bearer ", "")
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM], options={"verify_exp": False})
        return payload
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")

@router.get("/me", response_model=ProfileResponse)
async def get_profile(
    api_key: str = Depends(verify_api_key),
    authorization: str = Header(None)
):
    """Получение информации о текущем пользователе"""
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header required")

    user_data = get_user_from_token(authorization)

    session = create_session()
    try:
        user = session.query(User).filter_by(id=user_data["user_id"]).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")

        # Проверяем наличие паспорта
        passport = session.query(Passport).filter_by(user_passport_phone_number=user.phone_number).first()
        has_passport = passport is not None

        return ProfileResponse(
            success=True,
            user_id=user.id,
            name=user.name,
            phone_number=user.phone_number,
            has_passport=has_passport,
            banned=bool(user.banned),
            message="Профиль получен успешно"
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Error retrieving profile: {str(e)}")
    finally:
        session.close()

@router.put("/update", response_model=StandardResponse)
async def update_profile(
    profile_data: UpdateProfileRequest,
    api_key: str = Depends(verify_api_key),
    authorization: str = Header(None)
):
    """Обновление профиля пользователя"""
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header required")

    user_data = get_user_from_token(authorization)

    session = create_session()
    try:
        user = session.query(User).filter_by(id=user_data["user_id"]).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")

        # Обновляем только переданные поля
        if profile_data.name is not None:
            user.name = profile_data.name

        if profile_data.phone_number is not None:
            # Проверяем, что новый номер телефона не занят
            existing_user = session.query(User).filter_by(phone_number=profile_data.phone_number).first()
            if existing_user and existing_user.id != user.id:
                return StandardResponse(
                    success=False,
                    message="Этот номер телефона уже используется другим пользователем"
                )
            user.phone_number = profile_data.phone_number

        if profile_data.password is not None:
            # Хэшируем новый пароль
            import bcrypt
            hashed_password = bcrypt.hashpw(profile_data.password.encode('utf-8'), bcrypt.gensalt())
            user.password = hashed_password.decode('utf-8')

        session.commit()

        return StandardResponse(
            success=True,
            message="Профиль обновлен успешно"
        )

    except Exception as e:
        session.rollback()
        return StandardResponse(
            success=False,
            message=f"Ошибка обновления профиля: {str(e)}"
        )
    finally:
        session.close()
