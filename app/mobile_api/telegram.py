from fastapi import APIRouter, HTTPException, Depends, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import Dict, Any, Optional
import logging

from ..utils.telegram_auth import (
    verify_telegram_auth, save_telegram_user, get_telegram_user_by_user_id,
    unlink_telegram_user
)
from ..utils.auth import get_current_user

# Настройка логирования
logger = logging.getLogger(__name__)

# Создаем роутер для мобильной Telegram аутентификации
router = APIRouter(prefix="/mobile/telegram", tags=["mobile-telegram"])


class TelegramAuthRequest(BaseModel):
    telegram_data: Dict[str, Any] = Field(..., description='Данные от Telegram Login Widget')


class TelegramUserInfo(BaseModel):
    telegram_id: int
    username: Optional[str]
    first_name: Optional[str]
    last_name: Optional[str]
    verified_at: Optional[str]


@router.post('/link')
async def link_telegram_account(
    auth_request: TelegramAuthRequest,
    current_user: dict = Depends(get_current_user)
):
    """Привязать Telegram аккаунт к пользователю (мобильное API)"""
    try:
        user_id = str(current_user.get('id'))

        # Проверяем подлинность данных от Telegram
        telegram_data = auth_request.telegram_data.copy()

        if not verify_telegram_auth(telegram_data):
            raise HTTPException(status_code=400, detail="Неверные данные аутентификации Telegram")

        # Сохраняем данные пользователя
        success = save_telegram_user(user_id, auth_request.telegram_data)

        if success:
            return JSONResponse(content={
                'success': True,
                'message': 'Telegram аккаунт успешно привязан',
                'telegram_data': {
                    'telegram_id': auth_request.telegram_data.get('id'),
                    'username': auth_request.telegram_data.get('username'),
                    'first_name': auth_request.telegram_data.get('first_name'),
                    'last_name': auth_request.telegram_data.get('last_name')
                }
            })
        else:
            raise HTTPException(status_code=500, detail="Ошибка при сохранении данных Telegram")

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f'Ошибка при привязке Telegram аккаунта: {str(e)}')
        raise HTTPException(status_code=500, detail="Внутренняя ошибка сервера")


@router.get('/info')
async def get_telegram_info(current_user: dict = Depends(get_current_user)):
    """Получить информацию о привязанном Telegram аккаунте (мобильное API)"""
    try:
        user_id = str(current_user.get('id'))
        telegram_user = get_telegram_user_by_user_id(user_id)

        if telegram_user:
            return JSONResponse(content={
                'success': True,
                'is_linked': True,
                'telegram_user': {
                    'telegram_id': telegram_user.telegram_id,
                    'username': telegram_user.telegram_username,
                    'first_name': telegram_user.first_name,
                    'last_name': telegram_user.last_name,
                    'phone_number': telegram_user.phone_number,
                    'verified_at': telegram_user.verified_at.isoformat() if telegram_user.verified_at else None
                }
            })
        else:
            return JSONResponse(content={
                'success': True,
                'is_linked': False,
                'message': 'Telegram аккаунт не привязан'
            })

    except Exception as e:
        logger.error(f'Ошибка при получении информации о Telegram пользователе: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при получении данных")


@router.delete('/unlink')
async def unlink_telegram_account_mobile(current_user: dict = Depends(get_current_user)):
    """Отвязать Telegram аккаунт от пользователя (мобильное API)"""
    try:
        user_id = str(current_user.get('id'))
        success = unlink_telegram_user(user_id)

        if success:
            return JSONResponse(content={
                'success': True,
                'message': 'Telegram аккаунт успешно отвязан'
            })
        else:
            return JSONResponse(content={
                'success': False,
                'message': 'Telegram аккаунт не был привязан'
            })

    except Exception as e:
        logger.error(f'Ошибка при отвязке Telegram аккаунта: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при отвязке аккаунта")


@router.get('/auth-widget-config')
async def get_auth_widget_config():
    """Получить конфигурацию для Telegram Login Widget (мобильное API)"""
    try:
        import os
        bot_username = os.getenv('BOT_USERNAME', 'YOUR_BOT_USERNAME')

        return JSONResponse(content={
            'success': True,
            'config': {
                'bot_username': bot_username,
                'auth_url': f'https://oauth.telegram.org/auth?bot_id={bot_username}&origin=akkubatt-work.ru&embed=1',
                'redirect_url': '/api/telegram/auth-callback'
            },
            'instructions': {
                'step1': 'Используйте Telegram Login Widget в мобильном приложении',
                'step2': 'После авторизации отправьте полученные данные на /mobile/telegram/link',
                'step3': 'Проверьте статус привязки через /mobile/telegram/info'
            }
        })

    except Exception as e:
        logger.error(f'Ошибка при получении конфигурации виджета: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при получении конфигурации")
