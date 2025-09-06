from fastapi import APIRouter, Depends, HTTPException, UploadFile, File, Form, Header
from fastapi.responses import JSONResponse
from pydantic import BaseModel
from typing import Optional
import jwt
import os
from dotenv import load_dotenv

from app.mobile_api.auth import verify_api_key
from app.datac.db_session import create_session
from app.datac.__all_models import User, Passport, PassportPhoto

# Загружаем переменные окружения
load_dotenv()

router = APIRouter(prefix="/mobile/passport", tags=["Mobile Passport"])

# JWT настройки
JWT_SECRET = os.getenv("SECRET_KEY", "CoJoaWaCmZ25mw{PoY%*f~7O9Eet")
JWT_ALGORITHM = "HS256"

# Максимальный размер изображения паспорта (64MB)
MAX_PASSPORT_SIZE = 64 * 1024 * 1024

class PassportUploadResponse(BaseModel):
    success: bool
    message: str
    photo_id: Optional[int] = None
    photo_type: Optional[int] = None
    file_size: Optional[int] = None

class PassportStatusResponse(BaseModel):
    success: bool
    passport_status: int  # 0 = не добавлен, 1 = на модерации, 2 = отклонен, 3 = одобрен
    passport_status_name: str
    photos_uploaded: bool = False
    main_page_uploaded: bool = False
    registration_page_uploaded: bool = False
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

@router.post("/upload-photo", response_model=PassportUploadResponse)
async def upload_passport_photo(
    photo: UploadFile = File(...),
    photo_type: int = Form(...),
    phone_number: str = Form(...),
    api_key: str = Depends(verify_api_key),
    authorization: str = Header(None)
):
    """
    Загрузка фотографии паспорта пользователя

    Args:
        photo: Файл изображения (максимум 64MB)
        photo_type: Тип фото (0 = основная страница, 1 = страница с пропиской)
        phone_number: Номер телефона пользователя
    """
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header required")

    user_data = get_user_from_token(authorization)

    session = create_session()
    try:
        # Валидация типа фото
        if photo_type not in [0, 1]:
            return PassportUploadResponse(
                success=False,
                message="Invalid photo_type. Must be 0 (main page) or 1 (registration page)"
            )

        # Проверяем размер файла
        photo_data = await photo.read()
        if len(photo_data) > MAX_PASSPORT_SIZE:
            return PassportUploadResponse(
                success=False,
                message="File too large. Maximum size is 64MB"
            )

        # Проверяем тип файла
        if not photo.content_type or not photo.content_type.startswith('image/'):
            return PassportUploadResponse(
                success=False,
                message="Invalid file type. Only images are allowed"
            )

        # Находим или создаем паспорт пользователя
        passport = session.query(Passport).filter_by(user_passport_phone_number=phone_number).first()
        if not passport:
            # Создаем новый паспорт
            passport = Passport(
                user_passport_phone_number=phone_number,
                user_passport_name="",
                user_passport_birthday="",
                user_passport_serial_number="",
                user_passport_date_of_issue="",
                user_passport_code="",
                user_passport_issued="",
                passport_status=1  # Статус "на модерации"
            )
            session.add(passport)
            session.flush()

        # Проверяем, есть ли уже фото этого типа
        existing_photo = session.query(PassportPhoto).filter_by(
            passport_id=passport.user_passport_id,
            photo_type=photo_type
        ).first()

        if existing_photo:
            # Обновляем существующее фото
            existing_photo.photo_data = photo_data
            existing_photo.file_name = photo.filename
            existing_photo.file_size = len(photo_data)
            photo_id = existing_photo.id
        else:
            # Создаем новую запись
            passport_photo = PassportPhoto(
                passport_id=passport.user_passport_id,
                photo_data=photo_data,
                photo_type=photo_type,
                file_name=photo.filename,
                file_size=len(photo_data)
            )
            session.add(passport_photo)
            session.flush()
            photo_id = passport_photo.id

        # Обновляем статус паспорта на "на модерации" при загрузке новых фото
        passport.passport_status = 1
        session.commit()

        return PassportUploadResponse(
            success=True,
            message="Passport photo uploaded successfully",
            photo_id=photo_id,
            photo_type=photo_type,
            file_size=len(photo_data)
        )

    except Exception as e:
        session.rollback()
        return PassportUploadResponse(success=False, message=f"Error uploading photo: {str(e)}")
    finally:
        session.close()

@router.get("/status", response_model=PassportStatusResponse)
async def get_passport_status(
    api_key: str = Depends(verify_api_key),
    authorization: str = Header(None)
):
    """Получение статуса паспорта пользователя"""
    if not authorization:
        raise HTTPException(status_code=401, detail="Authorization header required")

    user_data = get_user_from_token(authorization)

    session = create_session()
    try:
        # Получаем пользователя
        user = session.query(User).filter_by(id=user_data["user_id"]).first()
        if not user:
            raise HTTPException(status_code=404, detail="User not found")

        # Находим паспорт пользователя
        passport = session.query(Passport).filter_by(user_passport_phone_number=user.phone_number).first()

        if not passport:
            return PassportStatusResponse(
                success=True,
                passport_status=0,
                passport_status_name="not_added",
                message="Passport not added"
            )

        # Проверяем загруженные фотографии
        photos = session.query(PassportPhoto).filter_by(passport_id=passport.user_passport_id).all()
        main_page_uploaded = any(photo.photo_type == 0 for photo in photos)
        registration_page_uploaded = any(photo.photo_type == 1 for photo in photos)
        photos_uploaded = len(photos) > 0

        # Определяем название статуса
        status_names = {
            0: "not_added",
            1: "under_review",
            2: "rejected",
            3: "approved"
        }

        return PassportStatusResponse(
            success=True,
            passport_status=passport.passport_status,
            passport_status_name=status_names.get(passport.passport_status, "unknown"),
            photos_uploaded=photos_uploaded,
            main_page_uploaded=main_page_uploaded,
            registration_page_uploaded=registration_page_uploaded,
            message="Passport status retrieved successfully"
        )

    except Exception as e:
        return PassportStatusResponse(
            success=False,
            passport_status=0,
            passport_status_name="error",
            message=f"Error retrieving passport status: {str(e)}"
        )
    finally:
        session.close()
