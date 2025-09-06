from fastapi import APIRouter, Request

from app.utils.templates import templates
from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import Office, ParcelAutomat, Product, UnifiedOrders, OrderTypes, User
from data.Scripts import get_free_products

router = APIRouter()


@router.get('/admin-junior/offices')
@access_required('junior_admin')
async def admin_junior_offices(request: Request):
    session = create_session()
    try:
        offices = session.query(Office).all()
        return templates.TemplateResponse('admin/junior/junior_admin_offices.html',
                                          {'request': request, 'offices': offices})
    finally:
        session.close()


@router.get('/admin-junior/products')
@access_required('junior_admin')
async def admin_junior_products(request: Request):
    session = create_session()
    try:
        products = get_free_products(session)
        return templates.TemplateResponse('admin/junior/junior_admin_products.html',
                                          {'request': request, 'products': products})
    finally:
        session.close()


@router.get('/admin-junior/postamats')
@access_required('junior_admin')
async def admin_junior_postamats(request: Request):
    session = create_session()
    try:
        postamats = session.query(ParcelAutomat).all()
        return templates.TemplateResponse('admin/junior/junior_admin_postamats.html',
                                          {'request': request, 'postamats': postamats})
    finally:
        session.close()


@router.get('/admin-junior/orders')
@access_required('junior_admin')
async def admin_junior_orders(request: Request):
    session = create_session()
    try:
        # Получаем только офисные активные заказы (как в owner.py)
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        if not office_type:
            return templates.TemplateResponse('admin/junior/junior_admin_orders.html',
                                              {'request': request, 'orders': []})

        # Фильтруем заказы так же, как в owner.py - только активные офисные заказы
        unified_orders = session.query(UnifiedOrders).filter(
            UnifiedOrders.order_type_id == office_type.type_id,
            UnifiedOrders.not_issued != 1,  # Не отклоненные
            UnifiedOrders.returned != 1     # Не возвращенные
        ).all()

        # Создаем кортежи (order, user, product) для шаблона
        orders = []
        for order in unified_orders:
            user = None
            product = None

            # Безопасный поиск пользователя
            if order.client_id:
                try:
                    # Сначала пробуем найти по ID (если client_id это числовой ID)
                    if order.client_id.isdigit():
                        user = session.query(User).filter_by(id=int(order.client_id)).first()

                    # Если не найден по ID, пробуем найти по номеру телефона
                    if not user:
                        user = session.query(User).filter_by(phone_number=order.client_id).first()

                except Exception as e:
                    print(f"Ошибка поиска пользователя для client_id {order.client_id}: {e}")
                    user = None

            # Безопасный поиск продукта
            if order.product_id:
                try:
                    if order.product_id.isdigit():
                        product = session.query(Product).filter_by(id=int(order.product_id)).first()
                except Exception as e:
                    print(f"Ошибка поиска продукта для product_id {order.product_id}: {e}")
                    product = None

            orders.append((order, user, product))

        return templates.TemplateResponse('admin/junior/junior_admin_orders.html',
                                          {'request': request, 'orders': orders})
    finally:
        session.close()


@router.get('/admin-junior/users')
@access_required('junior_admin')
async def admin_junior_users(request: Request):
    session = create_session()
    try:
        users = session.query(User).all()
        return templates.TemplateResponse('admin/junior/junior_admin_users.html',
                                          {'request': request, 'users': users})
    finally:
        session.close()


@router.get('/admin-junior/returns')
@access_required('junior_admin')
async def admin_junior_returns(request: Request):
    session = create_session()
    try:
        # Получаем офисные возвраты
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        returns = []
        if office_type:
            unified_returns = session.query(UnifiedOrders).filter_by(
                order_type_id=office_type.type_id,
                ready_for_return=1
            ).all()

            # Создаем кортежи (order, user, product) для шаблона
            for order in unified_returns:
                user = None
                product = None

                # Безопасный поиск пользователя
                if order.client_id:
                    try:
                        # Сначала пробуем найти по ID (если client_id это числовой ID)
                        if order.client_id.isdigit():
                            user = session.query(User).filter_by(id=int(order.client_id)).first()

                        # Если не найден по ID, пробуем найти по номеру телефона
                        if not user:
                            user = session.query(User).filter_by(phone_number=order.client_id).first()

                    except Exception as e:
                        print(f"Ошибка поиска пользователя для client_id {order.client_id}: {e}")
                        user = None

                # Безопасный поиск продукта
                if order.product_id:
                    try:
                        if order.product_id.isdigit():
                            product = session.query(Product).filter_by(id=int(order.product_id)).first()
                    except Exception as e:
                        print(f"Ошибка поиска продукта для product_id {order.product_id}: {e}")
                        product = None

                returns.append((order, user, product))

        return templates.TemplateResponse('admin/junior/junior_admin_returns.html',
                                          {'request': request, 'returns': returns})
    finally:
        session.close()
