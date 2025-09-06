from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import UnifiedOrders, OrderTypes, User, Product

router = APIRouter()


@router.get('/api/get-active-orders')
@access_required()
async def api_get_active_orders(request: Request):
    session = create_session()
    try:
        # Получаем офисные заказы через объединенную таблицу
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        if not office_type:
            return JSONResponse({'error': 'Office order type not found'}, status_code=404)

        # Получаем ВСЕ офисные заказы сначала, потом отфильтруем в Python
        orders = session.query(UnifiedOrders).filter(
            UnifiedOrders.order_type_id == office_type.type_id
        ).all()

        print(f"Найдено всего офисных заказов: {len(orders)}")

        result = []
        for order in orders:
            # Логируем статусы каждого заказа
            print(f"Заказ {order.order_id}: issued={order.issued}, returned={order.returned}, not_issued={order.not_issued}")

            # Фильтруем в Python для большей надежности
            if order.not_issued == 1:
                print(f"Заказ {order.order_id} отклонен, пропускаем")
                continue

            if order.returned == 1:
                print(f"Заказ {order.order_id} возвращен, пропускаем")
                continue

            user = None
            product = None

            # Простой поиск пользователя
            if order.client_id:
                try:
                    # Пробуем найти по ID если это число
                    if str(order.client_id).isdigit():
                        user = session.query(User).filter_by(id=int(order.client_id)).first()

                    # Если не найден по ID, пробуем найти по номеру телефона
                    if not user:
                        user = session.query(User).filter_by(phone_number=str(order.client_id)).first()
                except Exception as e:
                    print(f"Ошибка поиска пользователя для client_id {order.client_id}: {e}")

            # Простой поиск продукта
            if order.product_id:
                try:
                    # Сначала пробуем найти по числовому ID
                    if str(order.product_id).isdigit():
                        product = session.query(Product).filter_by(id=int(order.product_id)).first()

                    # Если не найден по ID, пробуем найти по UUID
                    if not product:
                        product = session.query(Product).filter_by(product_uuid=str(order.product_id)).first()
                except Exception as e:
                    print(f"Ошибка поиска продукта для product_id {order.product_id}: {e}")

            # Возвращаем данные в формате, который ожидает JavaScript
            order_data = {
                'order_id': order.order_id,
                'client_name': user.name if user else 'N/A',
                'phone_number': user.phone_number if user else 'N/A',
                'product_name': product.name if product else 'N/A',
                'address_office': order.address_office or 'N/A',
                'issued': order.issued or 0,
                'returned': order.returned or 0,
                'ready_for_return': order.ready_for_return or 0,
                'not_issued': order.not_issued or 0,
                'accepted': order.accepted or 0,
                'order_type': 'office',
                'start_time': order.start_time,
                'end_time': order.end_time,
                'rental_amount': order.rental_amount,
                'rental_time': order.rental_time
            }
            result.append(order_data)
            print(f"Добавлен заказ {order.order_id} в результат")

        print(f"Возвращаем {len(result)} активных заказов")
        return JSONResponse(result)
    except Exception as e:
        print(f"Ошибка в API: {e}")
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-order-details/{order_id}')
@access_required()
async def api_get_order_details(order_id: int, request: Request):
    session = create_session()
    try:
        # Получаем заказ через объединенную таблицу
        order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
        if not order:
            return JSONResponse({'error': 'Order not found'}, status_code=404)

        user = None
        product = None

        # Простой поиск пользователя
        if order.client_id:
            try:
                if str(order.client_id).isdigit():
                    user = session.query(User).filter_by(id=int(order.client_id)).first()
                if not user:
                    user = session.query(User).filter_by(phone_number=str(order.client_id)).first()
            except Exception as e:
                print(f"Ошибка поиска пользователя для client_id {order.client_id}: {e}")

        # Простой поиск продукта
        if order.product_id:
            try:
                # Сначала пробуем найти по числовому ID
                if str(order.product_id).isdigit():
                    product = session.query(Product).filter_by(id=int(order.product_id)).first()

                # Если не найден по ID, пробуем найти по UUID
                if not product:
                    product = session.query(Product).filter_by(product_uuid=str(order.product_id)).first()
            except Exception as e:
                print(f"Ошибка поиска продукта для product_id {order.product_id}: {e}")

        # Возвращаем детальную информацию о заказе
        result = {
            'order_id': order.order_id,
            'client_name': user.name if user else 'N/A',
            'client_phone': user.phone_number if user else 'N/A',
            'product_name': product.name if product else 'N/A',
            'address_office': order.address_office or 'N/A',
            'issued': order.issued or 0,
            'returned': order.returned or 0,
            'ready_for_return': order.ready_for_return or 0,
            'not_issued': order.not_issued or 0,
            'accepted': order.accepted or 0,
            'start_time': order.start_time,
            'end_time': order.end_time,
            'rental_time': order.rental_time,
            'rental_amount': order.rental_amount
        }

        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/issue-order/{order_id}')
@access_required()
async def api_issue_order(order_id: int, request: Request):
    session = create_session()
    try:
        # Получаем заказ через объединенную таблицу
        order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
        if not order:
            return JSONResponse({'error': 'Order not found'}, status_code=404)

        # Проверяем статус заказа
        if order.issued == 1:
            return JSONResponse({'error': 'Order already issued'}, status_code=400)

        # Выдаем заказ
        order.issued = 1
        order.not_issued = 0
        session.commit()

        return JSONResponse({'success': True, 'message': 'Order issued successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/ready-for-return/{order_id}')
@access_required()
async def api_ready_for_return(order_id: int, request: Request):
    session = create_session()
    try:
        # Получаем заказ через объединенную таблицу
        order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
        if not order:
            return JSONResponse({'error': 'Order not found'}, status_code=404)

        # Помечаем заказ как готовый к возврату
        order.ready_for_return = 1
        session.commit()

        return JSONResponse({'success': True, 'message': 'Order marked as ready for return'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/reject-order/{order_id}')
@access_required()
async def api_reject_order(order_id: int, request: Request):
    session = create_session()
    try:
        # Получаем заказ через объединенную таблицу
        order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
        if not order:
            return JSONResponse({'error': 'Order not found'}, status_code=404)

        # Отклоняем заказ
        order.not_issued = 1
        session.commit()

        return JSONResponse({'success': True, 'message': 'Order rejected successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/decline-order/{order_id}')
@access_required()
async def api_decline_order(order_id: int, request: Request):
    session = create_session()
    try:
        # Получаем заказ через объединенную таблицу
        order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
        if not order:
            return JSONResponse({'error': 'Order not found'}, status_code=404)

        # Проверяем статус заказа
        if order.issued == 1:
            return JSONResponse({'error': 'Cannot decline already issued order'}, status_code=400)

        # Отклоняем заказ
        order.not_issued = 1
        session.commit()

        return JSONResponse({'success': True, 'message': 'Order declined successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/accept-return/{order_id}')
@access_required()
async def api_accept_return(order_id: int, request: Request):
    session = create_session()
    try:
        # Получаем заказ через объединенную таблицу
        order = session.query(UnifiedOrders).filter_by(order_id=order_id).first()
        if not order:
            return JSONResponse({'error': 'Order not found'}, status_code=404)

        # Проверяем статус заказа
        if order.returned == 1:
            return JSONResponse({'error': 'Order already returned'}, status_code=400)

        if order.issued != 1:
            return JSONResponse({'error': 'Order was not issued yet'}, status_code=400)

        # Принимаем возврат
        order.returned = 1
        order.ready_for_return = 0
        session.commit()

        return JSONResponse({'success': True, 'message': 'Return accepted successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()
