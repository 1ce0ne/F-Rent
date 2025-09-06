from datetime import datetime
from fastapi import APIRouter, Request, File, UploadFile, Form, HTTPException
from fastapi.responses import JSONResponse
from typing import Optional

from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import Passport, PassportPhoto, User

router = APIRouter()


@router.post('/api/passport/{phone_number}/photos')
async def upload_passport_photo(
    phone_number: str,
    request: Request,
    photo: UploadFile = File(...),
    photo_type: int = Form(...)
):
    """
    Загрузка фотографии паспорта для пользователя

    Args:
        phone_number: Номер телефона пользователя
        photo: Файл изображения (максимум 64MB)
        photo_type: Тип фото (0 = первая страница, 1 = страница с пропиской)
    """
    session = create_session()
    try:
        # Валидация типа фото
        if photo_type not in [0, 1]:
            return JSONResponse({'success': False, 'error': 'Invalid photo_type. Must be 0 (main page) or 1 (registration page)'}, status_code=400)

        # Проверяем размер файла (максимум 64MB для паспортов)
        photo_data = await photo.read()
        if len(photo_data) > 67108864:  # 64MB
            return JSONResponse({'success': False, 'error': 'File too large. Maximum size is 64MB'}, status_code=400)

        # Проверяем тип файла
        if not photo.content_type or not photo.content_type.startswith('image/'):
            return JSONResponse({'success': False, 'error': 'Invalid file type. Only images are allowed'}, status_code=400)

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
            existing_photo.created_at = datetime.utcnow()
            photo_id = existing_photo.id
        else:
            # Создаем новую запись
            passport_photo = PassportPhoto(
                passport_id=passport.user_passport_id,
                photo_data=photo_data,
                photo_type=photo_type,
                file_name=photo.filename,
                file_size=len(photo_data),
                created_at=datetime.utcnow()
            )
            session.add(passport_photo)
            session.flush()
            photo_id = passport_photo.id

        # Обновляем статус паспорта на "на модерации"
        passport.passport_status = 1
        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Passport photo uploaded successfully',
            'photo_id': photo_id,
            'photo_type': photo_type,
            'file_size': len(photo_data)
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'success': False, 'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/passport/{phone_number}/status')
async def get_passport_status(phone_number: str, request: Request):
    """Получение статуса паспорта пользователя"""
    session = create_session()
    try:
        # Находим паспорт пользователя
        passport = session.query(Passport).filter_by(user_passport_phone_number=phone_number).first()

        if not passport:
            return JSONResponse({
                'success': True,
                'passport_status': 0,
                'passport_status_name': 'not_added',
                'photos_uploaded': False,
                'main_page_uploaded': False,
                'registration_page_uploaded': False,
                'passport_data_complete': False
            })

        # Проверяем загруженные фотографии
        photos = session.query(PassportPhoto).filter_by(passport_id=passport.user_passport_id).all()
        main_page_uploaded = any(photo.photo_type == 0 for photo in photos)
        registration_page_uploaded = any(photo.photo_type == 1 for photo in photos)
        photos_uploaded = len(photos) > 0
        passport_data_complete = main_page_uploaded and registration_page_uploaded

        # Определяем название статуса
        status_names = {
            0: 'not_added',
            1: 'under_review',
            2: 'rejected',
            3: 'approved'
        }

        return JSONResponse({
            'success': True,
            'passport_status': passport.passport_status,
            'passport_status_name': status_names.get(passport.passport_status, 'unknown'),
            'photos_uploaded': photos_uploaded,
            'main_page_uploaded': main_page_uploaded,
            'registration_page_uploaded': registration_page_uploaded,
            'passport_data_complete': passport_data_complete
        })

    except Exception as e:
        return JSONResponse({'success': False, 'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.delete('/api/passport/photos/{photo_id}')
@access_required({'senior_admin', 'owner'})
async def delete_passport_photo(photo_id: int, request: Request):
    """Удаление фотографии паспорта (только для администраторов)"""
    session = create_session()
    try:
        # Получаем фотографию
        photo = session.query(PassportPhoto).filter_by(id=photo_id).first()
        if not photo:
            return JSONResponse({'success': False, 'error': 'Photo not found'}, status_code=404)

        session.delete(photo)
        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Passport photo deleted successfully'
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'success': False, 'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.put('/api/passport/{phone_number}/status')
@access_required({'senior_admin', 'owner'})
async def update_passport_status(phone_number: str, request: Request):
    """Обновление статуса паспорта (только для администраторов)"""
    session = create_session()
    try:
        data = await request.json()
        new_status = data.get('passport_status')

        if new_status not in [0, 1, 2, 3]:
            return JSONResponse({'success': False, 'error': 'Invalid status. Must be 0-3'}, status_code=400)

        passport = session.query(Passport).filter_by(user_passport_phone_number=phone_number).first()
        if not passport:
            return JSONResponse({'success': False, 'error': 'Passport not found'}, status_code=404)

        passport.passport_status = new_status
        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Passport status updated successfully',
            'new_status': new_status
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'success': False, 'error': str(e)}, status_code=500)
    finally:
        session.close()
