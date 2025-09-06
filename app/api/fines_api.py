from datetime import datetime, timedelta
from fastapi import APIRouter, Request, Form
from fastapi.responses import JSONResponse
from typing import Optional

from app.utils.auth import access_required
from app.utils.secure_cookies import get_verified_cookie
from app.utils.security import signer
from app.datac.db_session import create_session
from app.datac.__all_models import Fines, Product, User, UnifiedOrders

router = APIRouter()


@router.post('/api/create-fine')
@access_required({'junior_admin', 'senior_admin', 'owner'})
async def api_create_fine(request: Request,
                         user_id: int = Form(...),
                         product_id: int = Form(...),
                         fine_amount: float = Form(...),
                         fine_reason: str = Form(...),
                         fine_description: Optional[str] = Form(None),
                         order_id: Optional[int] = Form(None),
                         due_days: int = Form(14)):
    """
    Создание нового штрафа
    """
    session = create_session()
    try:
        # Проверяем пользователя
        user = session.query(User).filter_by(id=user_id).first()
        if not user:
            return JSONResponse({'error': 'Пользователь не найден'}, status_code=404)

        # Проверяем товар
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Товар не найден'}, status_code=404)

        # Проверяем заказ если указан
        if order_id:
            order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
            if not order:
                return JSONResponse({'error': 'Заказ не найден'}, status_code=404)

        # Получаем данные о создателе штрафа
        worker_id = get_verified_cookie(signer, request, 'worker_id')
        worker_role = get_verified_cookie(signer, request, 'user_role')

        # Рассчитываем срок оплаты
        due_date = datetime.utcnow() + timedelta(days=due_days)

        # Создаем новый штраф
        new_fine = Fines(
            user_phone=user.phone_number,
            user_name=user.name,
            amount=fine_amount,
            reason=fine_reason,
            status='unpaid',
            created_by_worker_id=worker_id,
            notes=fine_description
        )

        session.add(new_fine)
        session.commit()

        return JSONResponse({
            'success': True,
            'fine_id': new_fine.fine_id,
            'message': 'Штраф успешно создан'
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': f'Ошибка при создании штрафа: {str(e)}'}, status_code=500)
    finally:
        session.close()


@router.get('/api/fines')
@access_required({'junior_admin', 'senior_admin', 'owner'})
async def api_get_fines(request: Request):
    """
    Получение списка всех штрафов
    """
    session = create_session()
    try:
        fines = session.query(Fines).all()

        fines_list = []
        for fine in fines:
            fines_list.append({
                'fine_id': fine.fine_id,
                'user_phone': fine.user_phone,
                'user_name': fine.user_name,
                'amount': float(fine.amount),
                'reason': fine.reason,
                'status': fine.status,
                'created_at': fine.created_at.isoformat() if fine.created_at else None,
                'paid_at': fine.paid_at.isoformat() if fine.paid_at else None,
                'notes': fine.notes
            })

        return JSONResponse({
            'success': True,
            'fines': fines_list
        })

    except Exception as e:
        return JSONResponse({'error': f'Ошибка при получении штрафов: {str(e)}'}, status_code=500)
    finally:
        session.close()


@router.post('/api/fines/{fine_id}/pay')
@access_required({'junior_admin', 'senior_admin', 'owner'})
async def api_pay_fine(request: Request, fine_id: int):
    """
    Отметить штраф как оплаченный
    """
    session = create_session()
    try:
        fine = session.query(Fines).filter_by(fine_id=fine_id).first()
        if not fine:
            return JSONResponse({'error': 'Штраф не найден'}, status_code=404)

        fine.status = 'paid'
        fine.paid_at = datetime.utcnow()
        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Штраф отмечен как оплаченный'
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': f'Ошибка при обновлении штрафа: {str(e)}'}, status_code=500)
    finally:
        session.close()


@router.delete('/api/fines/{fine_id}')
@access_required({'senior_admin', 'owner'})
async def api_delete_fine(request: Request, fine_id: int):
    """
    Удаление штрафа (только для старших админов и владельцев)
    """
    session = create_session()
    try:
        fine = session.query(Fines).filter_by(fine_id=fine_id).first()
        if not fine:
            return JSONResponse({'error': 'Штраф не найден'}, status_code=404)

        session.delete(fine)
        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Штраф успешно удален'
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': f'Ошибка при удалении штрафа: {str(e)}'}, status_code=500)
    finally:
        session.close()
