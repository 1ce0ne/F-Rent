from fastapi import APIRouter, HTTPException, Depends, Request
from fastapi.responses import JSONResponse
from pydantic import BaseModel, Field
from typing import Dict, Any, Optional, List
import logging

from ..utils.cloudpayments import (
    add_user_card, remove_user_card, get_user_cards,
    charge_token_payment, refund_payment
)
from ..utils.auth import get_current_user

# Настройка логирования
logger = logging.getLogger(__name__)

# Создаем роутер для мобильных платежных операций
router = APIRouter(prefix="/mobile/payments", tags=["mobile-payments"])


class CardInfo(BaseModel):
    id: int
    last4_digits: str
    card_type: Optional[str]
    created_at: Optional[str]


class PaymentRequest(BaseModel):
    card_id: int = Field(..., description='ID карты для списания')
    amount: float = Field(..., gt=0, description='Сумма платежа')
    order_id: Optional[str] = Field(None, description='ID заказа')
    description: Optional[str] = Field(None, description='Описание платежа')


class RefundRequest(BaseModel):
    transaction_id: str = Field(..., description='ID транзакции для возврата')
    amount: float = Field(..., gt=0, description='Сумма возврата')


@router.get('/cards', response_model=List[CardInfo])
async def get_user_cards_mobile(current_user: dict = Depends(get_current_user)):
    """Получить все карты пользователя (мобильное API)"""
    try:
        user_id = str(current_user.get('id'))
        cards = get_user_cards(user_id)

        return JSONResponse(content={
            'success': True,
            'cards': cards
        })

    except Exception as e:
        logger.error(f'Ошибка при получении карт пользователя: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при получении карт")


@router.delete('/cards/{card_id}')
async def remove_card_mobile(
    card_id: int,
    current_user: dict = Depends(get_current_user)
):
    """Удалить карту пользователя (мобильное API)"""
    try:
        user_id = str(current_user.get('id'))
        success = remove_user_card(user_id, card_id)

        if success:
            return JSONResponse(content={
                'success': True,
                'message': 'Карта успешно удалена'
            })
        else:
            raise HTTPException(status_code=404, detail="Карта не найдена")

    except Exception as e:
        logger.error(f'Ошибка при удалении карты: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при удалении карты")


@router.post('/charge')
async def charge_payment_mobile(
    payment_request: PaymentRequest,
    current_user: dict = Depends(get_current_user)
):
    """Списание с привязанной карты (мобильное API)"""
    try:
        user_id = str(current_user.get('id'))

        # Получаем карты пользователя
        user_cards = get_user_cards(user_id)

        # Находим нужную карту
        user_card = next((card for card in user_cards if card['id'] == payment_request.card_id), None)

        if not user_card:
            raise HTTPException(status_code=404, detail="Карта не найдена")

        # TODO: Получить реальный card_token из базы данных
        # Это временная заглушка
        card_token = "temp_token"

        result = await charge_token_payment(
            card_token=card_token,
            amount=payment_request.amount,
            user_id=user_id,
            order_id=payment_request.order_id
        )

        return JSONResponse(content=result)

    except HTTPException:
        raise
    except Exception as e:
        logger.error(f'Ошибка при списании: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при проведении платежа")


@router.get('/card-binding-url')
async def get_card_binding_url(current_user: dict = Depends(get_current_user)):
    """Получить URL для привязки карты (мобильное API)"""
    try:
        user_id = str(current_user.get('id'))

        # Генерируем URL для привязки карты
        binding_url = f"/api/payments/card-binding-form?user_id={user_id}"

        return JSONResponse(content={
            'success': True,
            'binding_url': binding_url,
            'instructions': 'Откройте этот URL в браузере для привязки карты'
        })

    except Exception as e:
        logger.error(f'Ошибка при генерации URL привязки карты: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при генерации URL")


@router.post('/refund')
async def refund_payment_mobile(
    refund_request: RefundRequest,
    current_user: dict = Depends(get_current_user)
):
    """Возврат платежа (мобильное API)"""
    try:
        # Проверяем права пользователя на возврат
        # TODO: Добавить проверку прав

        result = await refund_payment(
            transaction_id=refund_request.transaction_id,
            amount=str(refund_request.amount)
        )

        return JSONResponse(content=result)

    except Exception as e:
        logger.error(f'Ошибка при возврате: {str(e)}')
        raise HTTPException(status_code=500, detail="Ошибка при возврате платежа")
