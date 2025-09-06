from datetime import datetime
from fastapi import APIRouter, Request, Form
from fastapi.responses import JSONResponse
from typing import Optional

from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import Product, UnifiedOrders, OrderTypes, User

router = APIRouter()


@router.post('/api/accept-office-return/{return_id}')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def api_accept_office_return(
    return_id: int,
    request: Request,
    have_problem: Optional[bool] = Form(None),  # Заменено service_required на have_problem
    comment: Optional[str] = Form(None)         # Убрал photo параметр
):
    session = create_session()
    try:
        # Получаем офисный заказ через объединенную таблицу
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        if not office_type:
            return JSONResponse({'success': False, 'error': 'Office order type not found'}, status_code=404)

        order = session.query(UnifiedOrders).filter_by(
            order_id=return_id,
            returned=0,
            ready_for_return=1,
            order_type_id=office_type.type_id
        ).first()

        if not order:
            return JSONResponse({'success': False, 'error': 'Order not found or already processed'},
                                status_code=404)

        # Основная логика принятия возврата
        order.returned = 1
        order.accepted = 1
        order.updated_at = datetime.now().strftime('%Y-%m-%d %H:%M:%S')

        # Сохраняем комментарий в заказ
        if comment is not None and comment.strip():
            order.comment = comment.strip()

        # Обрабатываем проблемы с товаром
        additional_data = {}
        if have_problem is not None:
            additional_data['have_problem'] = have_problem

            # Если есть проблема с товаром, обновляем статус товара
            if have_problem:
                try:
                    product_id = int(order.product_id)
                    product = session.query(Product).filter_by(id=product_id).first()
                    if product:
                        product.have_problem = 1
                        additional_data['product_marked_as_problem'] = True
                        print(f"Product {product_id} marked as having problems")
                    else:
                        print(f"Warning: Product {product_id} not found")
                except (ValueError, TypeError):
                    print(f"Warning: Invalid product_id format: {order.product_id}")

        if comment is not None and comment.strip():
            additional_data['comment_saved'] = True

        session.commit()

        response_data = {
            'success': True,
            'message': 'Return accepted successfully'
        }
        if additional_data:
            response_data['processed_data'] = additional_data

        return JSONResponse(response_data)

    except Exception as e:
        session.rollback()
        print(f"Error accepting office return: {e}")
        return JSONResponse({'success': False, 'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-active-office-returns')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def api_get_active_office_returns(request: Request):
    session = create_session()
    try:
        # Получаем офисные заказы через объединенную таблицу
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        if not office_type:
            return JSONResponse({'error': 'Office order type not found'}, status_code=404)

        orders = session.query(UnifiedOrders).filter_by(
            returned=0,
            ready_for_return=1,
            order_type_id=office_type.type_id
        ).all()

        users = {u.id: u for u in session.query(User).all()}
        products = {p.id: p for p in session.query(Product).all()}
        result = []
        for order in orders:
            try:
                user = users.get(int(order.client_id)) if order.client_id else None
                product = products.get(int(order.product_id)) if order.product_id else None
                result.append({
                    'order_id': order.order_id,
                    'client_name': user.name if user else 'N/A',
                    'phone_number': user.phone_number if user else 'N/A',
                    'product_name': product.name if product else 'N/A',
                    'address_office': order.address_office,
                    'accepted': order.accepted
                })
            except Exception as e:
                print(f"Error processing order {order.order_id}: {e}")
        print("Returning office returns:", result)
        return JSONResponse(result)
    except Exception as e:
        print(f"Error in api_get_active_office_returns: {e}")
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()
