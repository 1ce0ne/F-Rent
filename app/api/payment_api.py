from fastapi import APIRouter, HTTPException, Request, Query, Depends
from fastapi.responses import JSONResponse, HTMLResponse
from fastapi.templating import Jinja2Templates
from pydantic import BaseModel, Field
from typing import Dict, Any, Optional
import logging
import json
from urllib.parse import parse_qs

from ..utils.cloudpayments import (
    verify_api_key, refund_payment, charge_token_payment,
    add_user_card, remove_user_card, get_user_cards,
    save_payment_transaction
)

# Настройка логирования
logger = logging.getLogger(__name__)

# Создаем роутер для платежных операций
router = APIRouter(prefix="/api/payments", tags=["payments"])

# Настройка шаблонов
templates = Jinja2Templates(directory="app/templates")


class CardBindingRequest(BaseModel):
    user_id: str = Field(..., description='ID пользователя')


class PaymentRequest(BaseModel):
    user_id: str = Field(..., description='ID пользователя')
    card_id: int = Field(..., description='ID карты пользователя')
    amount: float = Field(..., gt=0, description='Сумма платежа')
    order_id: str = Field(None, description='ID заказа')


class RefundRequest(BaseModel):
    transaction_id: str = Field(..., description='ID транзакции для возврата')
    amount: float = Field(..., gt=0, description='Сумма возврата')


@router.get('/card-binding-form')
async def card_binding_form(
    request: Request,
    user_id: str = Query(..., description='ID пользователя'),
    api_key_valid: bool = Depends(verify_api_key)
):
    """Форма для привязки карты (требует API ключ)"""
    logger.info(f'Запрос формы привязки карты для пользователя {user_id}')

    context = {
        'request': request,
        'user_id': user_id,
        'action': 'bind_card',
        'form_title': 'Привязка банковской карты',
        'amount': 11  # Сумма для привязки карты
    }

    return templates.TemplateResponse('payment_form.html', context)


@router.post('/cloudpayments-webhook')
async def cloudpayments_webhook(request: Request):
    """Обработка Pay уведомления от CloudPayments"""
    try:
        # Получаем данные из тела запроса
        body = await request.body()
        logger.info(f'Получено Pay уведомление от CloudPayments')
        logger.info(f'Размер данных: {len(body)} байт')

        # CloudPayments отправляет данные в формате application/x-www-form-urlencoded
        try:
            # Декодируем URL-encoded данные
            body_str = body.decode('utf-8')
            logger.info(f'Данные тела запроса: {body_str}')

            # Парсим как form data
            parsed_data = parse_qs(body_str)

            # Конвертируем в обычный словарь (parse_qs возвращает списки)
            data = {}
            for key, value_list in parsed_data.items():
                data[key] = value_list[0] if value_list else None

        except Exception as e:
            logger.error(f'Ошибка парсинга form data: {e}')
            return JSONResponse(
                status_code=400,
                content={'code': 13, 'message': 'Неверный формат данных'}
            )

        # Логируем основную информацию о платеже
        transaction_id = data.get('TransactionId')
        amount = data.get('Amount')
        currency = data.get('Currency')
        status = data.get('Status')
        operation_type = data.get('OperationType')
        card_last_four = data.get('CardLastFour')

        logger.info(f'TransactionId: {transaction_id}')
        logger.info(f'Amount: {amount} {currency}')
        logger.info(f'Status: {status}')
        logger.info(f'OperationType: {operation_type}')

        # Проверяем обязательные параметры
        required_fields = ['TransactionId', 'Amount', 'Currency', 'CardFirstSix', 'CardLastFour',
                          'CardType', 'CardExpDate', 'TestMode', 'Status', 'OperationType', 'GatewayName']
        missing_fields = [field for field in required_fields if not data.get(field)]

        if missing_fields:
            logger.error(f'Отсутствуют обязательные поля: {missing_fields}')
            return JSONResponse(
                status_code=400,
                content={'code': 13, 'message': f'Отсутствуют обязательные поля: {missing_fields}'}
            )

        # Сохраняем транзакцию в базу данных
        save_payment_transaction(data)

        # Проверка суммы - обрабатываем только 11 рублей для привязки карты
        try:
            amount_float = float(amount) if amount else 0
            if amount_float == 11.0 and currency == 'RUB':
                # Это привязка карты
                if operation_type == 'Payment' and status in ['Completed', 'Authorized']:
                    account_id = data.get('AccountId')
                    card_token = data.get('Token')

                    if account_id and card_token and card_last_four:
                        logger.info(f'Сохраняем данные карты для пользователя {account_id}')
                        card_saved = add_user_card(
                            user_id=account_id,
                            card_token=card_token,
                            last4_digits=card_last_four,
                            card_type=data.get('CardType')
                        )

                        if card_saved:
                            logger.info(f'Данные карты успешно сохранены для пользователя {account_id}: **** {card_last_four}')
                        else:
                            logger.error(f'Ошибка при сохранении данных карты для пользователя {account_id}')

        except (ValueError, TypeError):
            logger.error(f'Некорректная сумма: {amount}')
            return JSONResponse(
                status_code=400,
                content={'code': 13, 'message': 'Некорректная сумма платежа'}
            )

        # Возвращаем успешный ответ
        return JSONResponse(content={'code': 0})

    except Exception as e:
        logger.error(f'Ошибка при обработке webhook: {str(e)}')
        return JSONResponse(
            status_code=500,
            content={'code': 13, 'message': 'Внутренняя ошибка сервера'}
        )


@router.get('/cards/{user_id}')
async def get_user_cards_endpoint(
    user_id: str,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Получить все карты пользователя"""
    try:
        cards = get_user_cards(user_id)
        return JSONResponse(content={'success': True, 'cards': cards})
    except Exception as e:
        logger.error(f'Ошибка при получении карт пользователя: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при получении карт")


@router.delete('/cards/{user_id}/{card_id}')
async def remove_user_card_endpoint(
    user_id: str,
    card_id: int,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Удалить карту пользователя"""
    try:
        success = remove_user_card(user_id, card_id)
        if success:
            return JSONResponse(content={'success': True, 'message': 'Карта успешно удалена'})
        else:
            raise HTTPException(status_code=404, detail="Карта не найдена")
    except Exception as e:
        logger.error(f'Ошибка при удалении карты: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при удалении карты")


@router.post('/charge')
async def charge_payment(
    payment_request: PaymentRequest,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Списание с привязанной карты"""
    try:
        # Получаем карты пользователя
        user_cards = get_user_cards(payment_request.user_id)

        # Находим нужную карту
        user_card = next((card for card in user_cards if card['id'] == payment_request.card_id), None)

        if not user_card:
            raise HTTPException(status_code=404, detail="Карта не найдена")

        # Здесь нужно получить card_token из базы данных
        # Это временная заглушка - в реальном приложении нужно реализовать получение токена
        card_token = "temp_token"  # TODO: Получить реальный токен из базы

        result = await charge_token_payment(
            card_token=card_token,
            amount=payment_request.amount,
            user_id=payment_request.user_id,
            order_id=payment_request.order_id
        )

        return JSONResponse(content=result)

    except Exception as e:
        logger.error(f'Ошибка при списании: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при проведении платежа")


@router.post('/refund')
async def refund_payment_endpoint(
    refund_request: RefundRequest,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Возврат платежа"""
    try:
        result = await refund_payment(
            transaction_id=refund_request.transaction_id,
            amount=str(refund_request.amount)
        )

        return JSONResponse(content=result)

    except Exception as e:
        logger.error(f'Ошибка при возврате: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при возврате платежа")
