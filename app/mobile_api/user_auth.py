import hashlib
import hmac
import time
import secrets
import os
from typing import Optional

from fastapi import APIRouter, HTTPException, Request, Depends
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel

from app.datac.db_session import create_session
from app.datac.__all_models import User, TelegramUser, BannedUsers, ReasonsForBan
from app.utils.security import create_access_token, verify_api_key, get_current_user

router = APIRouter(prefix="/mobile/auth", tags=["Mobile Auth"])
templates = Jinja2Templates(directory="app/templates")


class TelegramAuthData(BaseModel):
    id: int
    username: Optional[str] = None
    first_name: Optional[str] = None
    last_name: Optional[str] = None
    phone_number: Optional[str] = None
    auth_date: int
    hash: str


class AuthResponse(BaseModel):
    success: bool
    token: Optional[str] = None
    user_id: Optional[int] = None
    session_id: Optional[str] = None
    banned: bool = False
    ban_reason: Optional[str] = None
    message: Optional[str] = None


class BanCheckResponse(BaseModel):
    banned: bool
    ban_reason: Optional[str] = None
    user_id: int


BOT_TOKEN = os.getenv("BOT_TOKEN", "your_bot_token")
BOT_USERNAME = os.getenv("BOT_USERNAME", "your_bot_username")


def verify_telegram_auth(data: TelegramAuthData) -> bool:
    secret_key = hashlib.sha256(BOT_TOKEN.encode()).digest()
    check_hash = data.hash
    # Используем model_dump вместо устаревшего dict()
    data_dict = data.model_dump(exclude={'hash'})

    check_string_parts = []
    for key in sorted(data_dict.keys()):
        value = data_dict[key]
        if value is not None:
            check_string_parts.append(f"{key}={value}")

    check_string = '\n'.join(check_string_parts)
    expected_hash = hmac.new(secret_key, check_string.encode(), hashlib.sha256).hexdigest()

    is_valid = hmac.compare_digest(expected_hash, check_hash)
    is_recent = (time.time() - data.auth_date) < 86400

    return is_valid and is_recent


@router.get("/telegram-login", response_class=HTMLResponse)
async def telegram_login_page(request: Request, api_key: str = Depends(verify_api_key)):
    return templates.TemplateResponse("mobile_telegram_login.html", {
        "request": request,
        "bot_username": BOT_USERNAME,
        "api_key": api_key
    })


@router.post("/telegram-login", response_model=AuthResponse)
async def telegram_auth(telegram_data: TelegramAuthData, api_key: str = Depends(verify_api_key)):
    if not verify_telegram_auth(telegram_data):
        raise HTTPException(status_code=400, detail="Неверная подпись Telegram")

    session = create_session()
    try:
        # Ищем пользователя по Telegram ID
        telegram_user = None
        try:
            telegram_user = session.query(TelegramUser).filter_by(telegram_id=telegram_data.id).first()
        except:
            # TelegramUser модель может не существовать
            pass

        if telegram_user:
            # Обновляем существующего пользователя
            user = session.query(User).filter_by(id=telegram_user.user_id).first()
            if telegram_data.phone_number and not user.phone_number:
                user.phone_number = telegram_data.phone_number
        else:
            # Создаем нового пользователя
            user = User(
                name=telegram_data.first_name or "Telegram User",
                password="telegram_auth",  # Заглушка для пароля
                phone_number=telegram_data.phone_number or ""
            )
            session.add(user)
            session.flush()

            # Создаем связь с Telegram если модель существует
            try:
                telegram_user = TelegramUser(
                    user_id=user.id,
                    telegram_id=telegram_data.id,
                    # Убираем поле username если его нет в модели
                    first_name=telegram_data.first_name,
                    last_name=telegram_data.last_name,
                    phone_number=telegram_data.phone_number
                )
                session.add(telegram_user)
            except:
                # TelegramUser модель не существует
                pass

        session.commit()

        # Проверяем бан по номеру телефона
        banned_user = session.query(BannedUsers).filter_by(banned_user_number=user.phone_number).first()
        ban_reason = None
        if banned_user:
            reason_obj = session.query(ReasonsForBan).filter_by(reason_id=banned_user.reason_id).first()
            ban_reason = reason_obj.reason_ban_name if reason_obj else "Причина не указана"

        # Создаем токен
        token_data = {"sub": str(user.id)}
        if telegram_user:
            # Преобразуем telegram_id в строку
            token_data["telegram_id"] = str(telegram_user.telegram_id)

        token = create_access_token(data=token_data)
        session_id = secrets.token_urlsafe(32)

        return AuthResponse(
            success=True,
            token=token,
            user_id=int(user.id),  # Явное преобразование в int
            session_id=session_id,
            banned=banned_user is not None,
            ban_reason=ban_reason,
            message="Успешная авторизация через Telegram"
        )

    except Exception as e:
        session.rollback()
        raise HTTPException(status_code=500, detail=f"Ошибка авторизации: {str(e)}")
    finally:
        session.close()


@router.get("/check-ban", response_model=BanCheckResponse)
async def check_user_ban(request: Request, api_key: str = Depends(verify_api_key)):
    try:
        current_user = await get_current_user(request)
        session = create_session()
        try:
            # Проверяем бан по номеру телефона
            banned_user = session.query(BannedUsers).filter_by(banned_user_number=current_user.phone_number).first()
            ban_reason = None

            if banned_user:
                reason_obj = session.query(ReasonsForBan).filter_by(reason_id=banned_user.reason_id).first()
                ban_reason = reason_obj.reason_ban_name if reason_obj else "Причина не указана"

            return BanCheckResponse(
                banned=banned_user is not None,
                ban_reason=ban_reason,
                user_id=current_user.id
            )
        finally:
            session.close()
    except Exception as e:
        raise HTTPException(status_code=401, detail="Неверный токен")
