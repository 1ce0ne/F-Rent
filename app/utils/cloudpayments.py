import os
import base64
import logging
import json
import time
from typing import Optional, Dict, Any

import httpx
from fastapi import HTTPException, Header, Depends
from sqlalchemy.orm import Session

from ..datac.db_session import create_session
from ..datac.__all_models import UserCard, PaymentTransaction

# Настройка логирования
logger = logging.getLogger(__name__)

# CloudPayments API настройки
CLOUDPAYMENTS_PUBLIC_ID = os.getenv('CLOUDPAYMENTS_PUBLIC_ID', '')
CLOUDPAYMENTS_API_SECRET = os.getenv('CLOUDPAYMENTS_API_SECRET', '')
CLOUDPAYMENTS_API_URL = 'https://api.cloudpayments.ru'

# API ключ для защиты внутренних endpoints
API_KEY = os.getenv('API_KEY', 'fallback-api-key-change-me')


def verify_api_key(x_api_key: str = Header(None)):
    """Проверка API ключа для защищенных эндпоинтов"""
    if x_api_key != API_KEY:
        raise HTTPException(status_code=401, detail="Неверный API ключ")
    return True


async def refund_payment(transaction_id: str, amount: str) -> Dict[str, Any]:
    """Возврат денег через CloudPayments API"""
    try:
        # Формируем данные для возврата
        refund_data = {
            'TransactionId': int(transaction_id),
            'Amount': float(amount)
        }

        # Создаем Basic Auth заголовок
        credentials = f'{CLOUDPAYMENTS_PUBLIC_ID}:{CLOUDPAYMENTS_API_SECRET}'
        encoded_credentials = base64.b64encode(credentials.encode()).decode()

        headers = {
            'Authorization': f'Basic {encoded_credentials}',
            'Content-Type': 'application/json'
        }

        logger.info(f'Отправляем запрос на возврат: TransactionId={transaction_id}, Amount={amount}')

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f'{CLOUDPAYMENTS_API_URL}/payments/refund',
                json=refund_data,
                headers=headers,
                timeout=30.0
            )

            result = response.json()
            logger.info(f'Ответ от CloudPayments API: {result}')

            if response.status_code == 200 and result.get('Success'):
                refund_transaction_id = result.get('Model', {}).get('TransactionId')
                logger.info(f'Возврат успешно выполнен. ID транзакции возврата: {refund_transaction_id}')
                return {
                    'success': True,
                    'refund_transaction_id': refund_transaction_id,
                    'message': 'Возврат успешно выполнен'
                }
            else:
                error_message = result.get('Message', 'Неизвестная ошибка')
                logger.error(f'Ошибка при возврате: {error_message}')
                return {
                    'success': False,
                    'message': error_message
                }

    except Exception as e:
        logger.error(f'Исключение при выполнении возврата: {str(e)}')
        return {
            'success': False,
            'message': f'Ошибка при выполнении возврата: {str(e)}'
        }


async def charge_token_payment(card_token: str, amount: float, user_id: str, order_id: str = None) -> Dict[str, Any]:
    """Списание с привязанной карты через CloudPayments API"""
    try:
        # Формируем данные для списания
        charge_data = {
            'Amount': amount,
            'Currency': 'RUB',
            'Token': card_token,
            'AccountId': user_id,
            'Description': f'Оплата заказа {order_id}' if order_id else 'Оплата услуг',
            'InvoiceId': order_id if order_id else f'invoice_{user_id}_{int(time.time())}'
        }

        # Создаем Basic Auth заголовок
        credentials = f'{CLOUDPAYMENTS_PUBLIC_ID}:{CLOUDPAYMENTS_API_SECRET}'
        encoded_credentials = base64.b64encode(credentials.encode()).decode()

        headers = {
            'Authorization': f'Basic {encoded_credentials}',
            'Content-Type': 'application/json'
        }

        logger.info(f'Отправляем запрос на списание: Amount={amount}, Token={card_token[:8]}...')

        async with httpx.AsyncClient() as client:
            response = await client.post(
                f'{CLOUDPAYMENTS_API_URL}/payments/tokens/charge',
                json=charge_data,
                headers=headers,
                timeout=30.0
            )

            result = response.json()
            logger.info(f'Ответ от CloudPayments API: {result}')

            if response.status_code == 200 and result.get('Success'):
                transaction_data = result.get('Model', {})
                transaction_id = transaction_data.get('TransactionId')
                logger.info(f'Списание успешно выполнено. ID транзакции: {transaction_id}')
                return {
                    'success': True,
                    'transaction_id': transaction_id,
                    'message': 'Платеж успешно проведен',
                    'data': transaction_data
                }
            else:
                error_message = result.get('Message', 'Неизвестная ошибка')
                logger.error(f'Ошибка при списании: {error_message}')
                return {
                    'success': False,
                    'message': error_message
                }

    except Exception as e:
        logger.error(f'Исключение при выполнении списания: {str(e)}')
        return {
            'success': False,
            'message': f'Ошибка при выполнении списания: {str(e)}'
        }


def add_user_card(user_id: str, card_token: str, last4_digits: str, card_type: str = None) -> bool:
    """Добавить карту пользователя в базу данных"""
    try:
        with create_session() as db:
            # Проверяем, есть ли уже такая карта
            existing_card = db.query(UserCard).filter(
                UserCard.user_id == user_id,
                UserCard.card_token == card_token
            ).first()

            if existing_card:
                logger.info(f'Карта уже существует для пользователя {user_id}')
                return True

            # Создаем новую запись карты
            new_card = UserCard(
                user_id=user_id,
                card_token=card_token,
                last4_digits=last4_digits,
                card_type=card_type,
                is_active=True
            )

            db.add(new_card)
            db.commit()
            logger.info(f'Карта успешно добавлена для пользователя {user_id}: **** {last4_digits}')
            return True

    except Exception as e:
        logger.error(f'Ошибка при добавлении карты: {str(e)}')
        return False


def remove_user_card(user_id: str, card_id: int) -> bool:
    """Удалить карту пользователя"""
    try:
        with create_session() as db:
            card = db.query(UserCard).filter(
                UserCard.id == card_id,
                UserCard.user_id == user_id
            ).first()

            if card:
                card.is_active = False
                db.commit()
                logger.info(f'Карта {card_id} деактивирована для пользователя {user_id}')
                return True
            else:
                logger.warning(f'Карта {card_id} не найдена для пользователя {user_id}')
                return False

    except Exception as e:
        logger.error(f'Ошибка при удалении карты: {str(e)}')
        return False


def get_user_cards(user_id: str) -> list:
    """Получить все активные карты пользователя"""
    try:
        with create_session() as db:
            cards = db.query(UserCard).filter(
                UserCard.user_id == user_id,
                UserCard.is_active == True
            ).all()

            return [{
                'id': card.id,
                'last4_digits': card.last4_digits,
                'card_type': card.card_type,
                'created_at': card.created_at.isoformat() if card.created_at else None
            } for card in cards]

    except Exception as e:
        logger.error(f'Ошибка при получении карт пользователя: {str(e)}')
        return []


def save_payment_transaction(transaction_data: Dict[str, Any]) -> bool:
    """Сохранить данные транзакции в базу данных"""
    try:
        with create_session() as db:
            transaction = PaymentTransaction(
                transaction_id=transaction_data.get('TransactionId'),
                user_id=transaction_data.get('AccountId'),
                order_id=transaction_data.get('InvoiceId'),
                amount=transaction_data.get('Amount'),
                currency=transaction_data.get('Currency', 'RUB'),
                status=transaction_data.get('Status'),
                operation_type=transaction_data.get('OperationType'),
                card_first_six=transaction_data.get('CardFirstSix'),
                card_last_four=transaction_data.get('CardLastFour'),
                card_type=transaction_data.get('CardType'),
                gateway_name=transaction_data.get('GatewayName'),
                test_mode=transaction_data.get('TestMode', '0') == '1',
                raw_data=json.dumps(transaction_data)
            )

            db.add(transaction)
            db.commit()
            logger.info(f'Транзакция {transaction_data.get("TransactionId")} сохранена в базу данных')
            return True

    except Exception as e:
        logger.error(f'Ошибка при сохранении транзакции: {str(e)}')
        return False
