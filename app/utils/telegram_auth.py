import os
import hashlib
import hmac
import time
import logging
from typing import Dict, Any, Optional
from urllib.parse import unquote

from sqlalchemy.orm import Session
from ..datac.db_session import create_session
from ..datac.__all_models import TelegramUser

# Настройка логирования
logger = logging.getLogger(__name__)

# Telegram Bot настройки из переменных окружения
TELEGRAM_BOT_TOKEN = os.getenv('BOT_TOKEN', 'YOUR_BOT_TOKEN')
TELEGRAM_BOT_USERNAME = os.getenv('BOT_USERNAME', 'YOUR_BOT_USERNAME')


def verify_telegram_auth(auth_data: Dict[str, Any]) -> bool:
    """Проверяет подлинность данных от Telegram Login Widget"""
    try:
        # Получаем hash из данных
        received_hash = auth_data.pop('hash', None)
        if not received_hash:
            logger.error('Отсутствует hash в данных аутентификации')
            return False

        # Создаем строку для проверки
        data_check_string = '\n'.join([f'{k}={v}' for k, v in sorted(auth_data.items())])

        # Создаем секретный ключ из токена бота
        secret_key = hashlib.sha256(TELEGRAM_BOT_TOKEN.encode()).digest()

        # Вычисляем HMAC
        calculated_hash = hmac.new(secret_key, data_check_string.encode(), hashlib.sha256).hexdigest()

        # Сравниваем хеши
        is_valid = hmac.compare_digest(received_hash, calculated_hash)

        if not is_valid:
            logger.error('Неверный hash аутентификации Telegram')
            return False

        # Проверяем время (данные должны быть не старше 86400 секунд = 24 часа)
        auth_date = auth_data.get('auth_date')
        if auth_date:
            try:
                auth_timestamp = int(auth_date)
                current_timestamp = int(time.time())
                if current_timestamp - auth_timestamp > 86400:
                    logger.error('Данные аутентификации Telegram устарели')
                    return False
            except (ValueError, TypeError):
                logger.error('Неверный формат auth_date')
                return False

        logger.info('Аутентификация Telegram успешно проверена')
        return True

    except Exception as e:
        logger.error(f'Ошибка при проверке аутентификации Telegram: {str(e)}')
        return False


def save_telegram_user(user_id: str, telegram_data: Dict[str, Any]) -> bool:
    """Сохранить данные пользователя Telegram в базу данных"""
    try:
        with create_session() as db:
            # Проверяем, есть ли уже такой пользователь
            existing_user = db.query(TelegramUser).filter(
                TelegramUser.telegram_id == int(telegram_data.get('id'))
            ).first()

            if existing_user:
                # Обновляем существующие данные
                existing_user.user_id = user_id
                existing_user.telegram_username = telegram_data.get('username')
                existing_user.first_name = telegram_data.get('first_name')
                existing_user.last_name = telegram_data.get('last_name')
                existing_user.is_verified = True
                logger.info(f'Обновлены данные Telegram пользователя {telegram_data.get("id")}')
            else:
                # Создаем новую запись
                new_telegram_user = TelegramUser(
                    user_id=user_id,
                    telegram_id=int(telegram_data.get('id')),
                    telegram_username=telegram_data.get('username'),
                    first_name=telegram_data.get('first_name'),
                    last_name=telegram_data.get('last_name'),
                    is_verified=True
                )
                db.add(new_telegram_user)
                logger.info(f'Создан новый Telegram пользователь {telegram_data.get("id")}')

            db.commit()
            return True

    except Exception as e:
        logger.error(f'Ошибка при сохранении Telegram пользователя: {str(e)}')
        return False


def get_telegram_user_by_id(telegram_id: int) -> Optional[TelegramUser]:
    """Получить пользователя Telegram по ID"""
    try:
        with create_session() as db:
            return db.query(TelegramUser).filter(
                TelegramUser.telegram_id == telegram_id,
                TelegramUser.is_verified == True
            ).first()
    except Exception as e:
        logger.error(f'Ошибка при получении Telegram пользователя: {str(e)}')
        return None


def get_telegram_user_by_user_id(user_id: str) -> Optional[TelegramUser]:
    """Получить пользователя Telegram по ID пользователя в системе"""
    try:
        with create_session() as db:
            return db.query(TelegramUser).filter(
                TelegramUser.user_id == user_id,
                TelegramUser.is_verified == True
            ).first()
    except Exception as e:
        logger.error(f'Ошибка при получении Telegram пользователя: {str(e)}')
        return None


def unlink_telegram_user(user_id: str) -> bool:
    """Отвязать Telegram от пользователя"""
    try:
        with create_session() as db:
            telegram_user = db.query(TelegramUser).filter(
                TelegramUser.user_id == user_id
            ).first()

            if telegram_user:
                telegram_user.is_verified = False
                db.commit()
                logger.info(f'Telegram отвязан от пользователя {user_id}')
                return True
            else:
                logger.warning(f'Telegram пользователь не найден для user_id {user_id}')
                return False

    except Exception as e:
        logger.error(f'Ошибка при отвязке Telegram пользователя: {str(e)}')
        return False
