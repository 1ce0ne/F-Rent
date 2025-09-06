from datetime import datetime
from fastapi import APIRouter, Request, File, UploadFile, Form, Query
from fastapi.responses import JSONResponse, StreamingResponse
from typing import Optional
import zipfile
import io

from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import UnifiedOrders, OrderPhotos

router = APIRouter()


@router.post('/api/orders/{order_id}/photos')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def upload_order_photo(
    order_id: int,
    request: Request,
    photo: UploadFile = File(...),
    photo_type: int = Form(...)
):
    """Загрузка фотографии для заказа"""
    session = create_session()
    try:
        # Проверяем существование заказа
        order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
        if not order:
            return JSONResponse({'success': False, 'error': 'Order not found'}, status_code=404)

        # Валидация типа фото
        if photo_type not in [0, 1]:
            return JSONResponse({'success': False, 'error': 'Invalid photo_type. Must be 0 or 1'}, status_code=400)

        # Проверяем размер файла (максимум 16MB)
        photo_data = await photo.read()
        if len(photo_data) > 16777215:  # 16MB
            return JSONResponse({'success': False, 'error': 'File too large. Maximum size is 16MB'}, status_code=400)

        # Проверяем тип файла
        if not photo.content_type or not photo.content_type.startswith('image/'):
            return JSONResponse({'success': False, 'error': 'Invalid file type. Only images are allowed'}, status_code=400)

        # Создаем запись в базе данных
        order_photo = OrderPhotos(
            order_id=order_id,
            photo_data=photo_data,
            photo_type=photo_type,
            created_at=datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            file_name=photo.filename,
            file_size=len(photo_data),
            content_type=photo.content_type
        )

        session.add(order_photo)
        session.commit()

        return JSONResponse({
            'success': True,
            'photo_id': order_photo.photo_id,
            'message': 'Photo uploaded successfully',
            'photo_info': {
                'file_name': photo.filename,
                'file_size': len(photo_data),
                'content_type': photo.content_type,
                'photo_type': photo_type,
                'created_at': order_photo.created_at
            }
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'success': False, 'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/orders/{order_id}/photos')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def get_order_photos(
    order_id: int,
    request: Request,
    photo_type: Optional[int] = Query(None)
):
    """Получение списка всех фотографий заказа"""
    session = create_session()
    try:
        # Проверяем существование заказа
        order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
        if not order:
            return JSONResponse({'error': 'Order not found'}, status_code=404)

        # Строим запрос с фильтрацией по типу фото
        query = session.query(OrderPhotos).filter_by(order_id=order_id)
        if photo_type is not None:
            if photo_type not in [0, 1]:
                return JSONResponse({'error': 'Invalid photo_type. Must be 0 or 1'}, status_code=400)
            query = query.filter_by(photo_type=photo_type)

        photos = query.order_by(OrderPhotos.created_at.desc()).all()

        # Формируем ответ
        photo_list = []
        photos_before = 0
        photos_after = 0

        for photo in photos:
            photo_list.append({
                'photo_id': photo.photo_id,
                'photo_type': photo.photo_type,
                'file_name': photo.file_name,
                'file_size': photo.file_size,
                'content_type': photo.content_type,
                'created_at': photo.created_at,
                'photo_url': f'/api/photos/{photo.photo_id}'
            })

            if photo.photo_type == 0:
                photos_before += 1
            else:
                photos_after += 1

        return JSONResponse({
            'order_id': order_id,
            'photos': photo_list,
            'total_photos': len(photos),
            'photos_before': photos_before,
            'photos_after': photos_after
        })

    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/photos/{photo_id}')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def get_photo_image(photo_id: int, request: Request):
    """Получение изображения по ID фотографии"""
    session = create_session()
    try:
        photo = session.query(OrderPhotos).filter_by(photo_id=photo_id).first()
        if not photo:
            return JSONResponse({'error': 'Photo not found'}, status_code=404)

        # Определяем Content-Type
        content_type = photo.content_type or 'image/jpeg'

        return StreamingResponse(
            io.BytesIO(photo.photo_data),
            media_type=content_type,
            headers={
                'Content-Disposition': f'inline; filename="{photo.file_name or f"photo_{photo_id}.jpg"}"'
            }
        )

    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.delete('/api/photos/{photo_id}')
@access_required({'junior_admin', 'senior_admin', 'owner'})
async def delete_photo(photo_id: int, request: Request):
    """Удаление фотографии по ID"""
    session = create_session()
    try:
        photo = session.query(OrderPhotos).filter_by(photo_id=photo_id).first()
        if not photo:
            return JSONResponse({'success': False, 'error': 'Photo not found'}, status_code=404)

        session.delete(photo)
        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Photo deleted successfully',
            'deleted_photo_id': photo_id
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'success': False, 'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/orders/{order_id}/photos/archive')
@access_required({'junior_admin', 'senior_admin', 'owner'})
async def download_order_photos_archive(order_id: int, request: Request):
    """Скачивание архива всех фотографий заказа"""
    session = create_session()
    try:
        # Проверяем существование заказа
        order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
        if not order:
            return JSONResponse({'error': 'Order not found'}, status_code=404)

        # Получаем все фотографии заказа
        photos = session.query(OrderPhotos).filter_by(order_id=order_id).order_by(OrderPhotos.created_at).all()

        if not photos:
            return JSONResponse({'error': 'No photos found for this order'}, status_code=404)

        # Создаем ZIP архив в памяти
        zip_buffer = io.BytesIO()
        with zipfile.ZipFile(zip_buffer, 'w', zipfile.ZIP_DEFLATED) as zip_file:
            before_count = 1
            after_count = 1

            for photo in photos:
                # Определяем расширение файла
                extension = '.jpg'
                if photo.content_type:
                    if 'png' in photo.content_type:
                        extension = '.png'
                    elif 'gif' in photo.content_type:
                        extension = '.gif'

                # Формируем имя файла в архиве
                if photo.photo_type == 0:
                    filename = f'before_rental_{before_count}{extension}'
                    before_count += 1
                else:
                    filename = f'after_return_{after_count}{extension}'
                    after_count += 1

                # Добавляем файл в архив
                zip_file.writestr(filename, photo.photo_data)

        zip_buffer.seek(0)

        return StreamingResponse(
            io.BytesIO(zip_buffer.read()),
            media_type='application/zip',
            headers={
                'Content-Disposition': f'attachment; filename="order_{order_id}_photos.zip"'
            }
        )

    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()
