from fastapi import APIRouter, HTTPException, Request, Query, Depends
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel, Field
from typing import Dict, Any, Optional
import logging
from urllib.parse import unquote

from ..utils.telegram_auth import (
    verify_telegram_auth, save_telegram_user, get_telegram_user_by_id,
    get_telegram_user_by_user_id, unlink_telegram_user
)
from ..utils.cloudpayments import verify_api_key

# Настройка логирования
logger = logging.getLogger(__name__)

# Создаем роутер для Telegram аутентификации
router = APIRouter(prefix="/api/telegram", tags=["telegram"])

# Настройка шаблонов
templates = Jinja2Templates(directory="app/templates")


class TelegramAuthRequest(BaseModel):
    user_id: str = Field(..., description='ID пользователя в системе')
    telegram_data: Dict[str, Any] = Field(..., description='Данные от Telegram Login Widget')


class PhoneVerifyRequest(BaseModel):
    app_user_id: str = Field(..., description='ID пользователя из приложения')
    telegram_id: int = Field(..., description='Telegram ID пользователя')
    phone: str = Field(..., description='Номер телефона пользователя')


@router.get('/login-widget')
async def telegram_login_widget(request: Request):
    """Страница с Telegram Login Widget"""
    context = {
        'request': request,
        'bot_username': 'YOUR_BOT_USERNAME'  # TODO: Получать из настроек
    }
    return templates.TemplateResponse('telegram_login.html', context)


@router.post('/verify-auth')
async def verify_telegram_auth_endpoint(
    auth_request: TelegramAuthRequest,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Проверка и сохранение данных Telegram аутентификации"""
    try:
        # Проверяем подлинность данных от Telegram
        telegram_data = auth_request.telegram_data.copy()

        if not verify_telegram_auth(telegram_data):
            raise HTTPException(status_code=400, detail="Неверные данные аутентификации Telegram")

        # Сохраняем данные пользователя
        success = save_telegram_user(auth_request.user_id, auth_request.telegram_data)

        if success:
            return JSONResponse(content={
                'success': True,
                'message': 'Telegram успешно привязан к аккаунту',
                'telegram_id': auth_request.telegram_data.get('id'),
                'username': auth_request.telegram_data.get('username'),
                'first_name': auth_request.telegram_data.get('first_name')
            })
        else:
            raise HTTPException(status_code=500, detail="Ошибка при сохранении данных Telegram")

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f'Ошибка при проверке Telegram аутентификации: {str(e)}')
        raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера")


@router.get('/auth-callback')
async def telegram_auth_callback(
    request: Request,
    id: Optional[str] = Query(None),
    first_name: Optional[str] = Query(None),
    last_name: Optional[str] = Query(None),
    username: Optional[str] = Query(None),
    photo_url: Optional[str] = Query(None),
    auth_date: Optional[str] = Query(None),
    hash: Optional[str] = Query(None)
):
    """Callback для Telegram Login Widget"""
    try:
        # Собираем данные аутентификации
        auth_data = {}

        if id:
            auth_data['id'] = unquote(id)
        if first_name:
            auth_data['first_name'] = unquote(first_name)
        if last_name:
            auth_data['last_name'] = unquote(last_name)
        if username:
            auth_data['username'] = unquote(username)
        if photo_url:
            auth_data['photo_url'] = unquote(photo_url)
        if auth_date:
            auth_data['auth_date'] = unquote(auth_date)
        if hash:
            auth_data['hash'] = unquote(hash)

        # Проверяем подлинность данных
        if verify_telegram_auth(auth_data.copy()):
            # Возвращаем страницу успешной аутентификации
            context = {
                'request': request,
                'telegram_data': auth_data,
                'success': True
            }
            return templates.TemplateResponse('telegram_success.html', context)
        else:
            # Возвращаем страницу с ошибкой
            context = {
                'request': request,
                'error': 'Неверные данные аутентификации',
                'success': False
            }
            return templates.TemplateResponse('telegram_success.html', context)

    except Exception as e:
        logger.error(f'Ошибка в callback Telegram аутентификации: {str(e)}')
        context = {
            'request': request,
            'error': 'Внутренняя ошибка сервера',
            'success': False
        }
        return templates.TemplateResponse('telegram_success.html', context)


@router.get('/user/{user_id}')
async def get_telegram_user_info(
    user_id: str,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Получить информацию о привязанном Telegram аккаунте"""
    try:
        telegram_user = get_telegram_user_by_user_id(user_id)

        if telegram_user:
            return JSONResponse(content={
                'success': True,
                'telegram_user': {
                    'telegram_id': telegram_user.telegram_id,
                    'username': telegram_user.telegram_username,
                    'first_name': telegram_user.first_name,
                    'last_name': telegram_user.last_name,
                    'verified_at': telegram_user.verified_at.isoformat() if telegram_user.verified_at else None
                }
            })
        else:
            return JSONResponse(content={
                'success': False,
                'message': 'Telegram аккаунт не привязан'
            })

    except Exception as e:
        logger.error(f'Ошибка при получении информации о Telegram пользователе: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при получении данных")


@router.delete('/user/{user_id}/unlink')
async def unlink_telegram_account(
    user_id: str,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Отвязать Telegram аккаунт от пользователя"""
    try:
        success = unlink_telegram_user(user_id)

        if success:
            return JSONResponse(content={
                'success': True,
                'message': 'Telegram аккаунт успешно отвязан'
            })
        else:
            raise HTTPException(status_code=404, detail="Telegram аккаунт не найден")

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f'Ошибка при отвязке Telegram аккаунта: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при отвязке аккаунта")


@router.post('/verify-phone')
async def verify_phone_endpoint(
    phone_request: PhoneVerifyRequest,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Верификация телефона через Telegram"""
    try:
        # Проверяем, существует ли Telegram пользователь
        telegram_user = get_telegram_user_by_id(phone_request.telegram_id)

        if not telegram_user:
            raise HTTPException(status_code=404, detail="Telegram пользователь не найден")

        # Обновляем номер телефона
        telegram_user.phone_number = phone_request.phone

        # Здесь можно добавить дополнительную логику верификации телефона

        return JSONResponse(content={
            'success': True,
            'message': 'Номер телефона успешно привязан',
            'phone': phone_request.phone
        })

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f'Ошибка при верификации телефона: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при верификации телефона")
