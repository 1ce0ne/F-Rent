from datetime import datetime, timedelta

from fastapi import APIRouter, Request
from sqlalchemy import func

from app.utils.templates import templates
from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import Product, Office, ParcelAutomat, UnifiedOrders, OrderTypes, User, Workers, WorkerRoles
from data.Scripts import get_free_products

router = APIRouter()

# Маппинг ролей для отображения на русском языке
ROLE_DISPLAY_MAP = {
    'postamat_worker': 'Работник постамата',
    'office_worker': 'Офисный работник',
    'junior_admin': 'Младший администратор',
    'senior_admin': 'Старший администратор',
    'owner': 'Владелец'
}


@router.get('/owner/offices')
@access_required('owner')
async def owner_offices(request: Request):
    session = create_session()
    try:
        offices = session.query(Office).all()
        return templates.TemplateResponse('owner/owner_offices.html',
                                          {'request': request, 'offices': offices})
    finally:
        session.close()


@router.get('/owner/products')
@access_required('owner')
async def owner_products(request: Request):
    session = create_session()
    try:
        products = get_free_products(session)  # Используем функцию для получения ����олько свободных товаров
        return templates.TemplateResponse('owner/owner_products.html',
                                          {'request': request, 'products': products})
    finally:
        session.close()


@router.get('/owner/postamats')
@access_required('owner')
async def owner_postamats(request: Request):
    session = create_session()
    try:
        postamats = session.query(ParcelAutomat).all()
        return templates.TemplateResponse('owner/owner_postamats.html',
                                          {'request': request, 'postamats': postamats})
    finally:
        session.close()


@router.get('/owner/orders')
@access_required('owner')
async def owner_orders(request: Request):
    session = create_session()
    try:
        # Получаем только офисные активные заказы (как в API)
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        if not office_type:
            return templates.TemplateResponse('owner/owner_orders.html',
                                              {'request': request, 'orders': []})

        # Фильтруем заказы так же, как в API - только активные офисные заказы
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

            # Добавляем заказ в список только если найден пользователь или разрешаем N/A
            orders.append((order, user, product))

        return templates.TemplateResponse('owner/owner_orders.html',
                                          {'request': request, 'orders': orders})
    finally:
        session.close()


@router.get('/owner/users')
@access_required('owner')
async def owner_users(request: Request):
    session = create_session()
    try:
        users = session.query(User).all()
        return templates.TemplateResponse('owner/owner_users.html',
                                          {'request': request, 'users': users})
    finally:
        session.close()


@router.get('/owner/returns')
@access_required('owner')
async def owner_returns(request: Request):
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

        return templates.TemplateResponse('owner/owner_returns.html',
                                          {'request': request, 'returns': returns})
    finally:
        session.close()


@router.get('/owner/workers')
@access_required('owner')
async def owner_workers(request: Request):
    session = create_session()
    try:
        # Получаем всех работников с их ролями
        workers_query = session.query(Workers, WorkerRoles).join(
            WorkerRoles, Workers.role_id == WorkerRoles.role_id
        ).all()

        # Формируем список работников с удобочитаемыми ролями
        workers = []
        for worker, role in workers_query:
            display_role = ROLE_DISPLAY_MAP.get(role.role_name, role.role_name)
            workers.append({
                'worker': worker,
                'role': role,
                'display_role': display_role
            })

        return templates.TemplateResponse('owner/owner_workers.html',
                                          {'request': request, 'workers': workers})
    finally:
        session.close()


@router.get('/owner/statistics')
@access_required('owner')
async def owner_statistics(request: Request):
    session = create_session()
    try:
        # Получаем статистику
        total_products = session.query(Product).count()
        total_offices = session.query(Office).count()
        total_users = session.query(User).count()
        total_workers = session.query(Workers).filter_by(is_active=1).count()

        # Получаем доходы за последний месяц
        month_ago = datetime.now() - timedelta(days=30)
        month_income = session.query(func.sum(UnifiedOrders.rental_amount)).filter(
            UnifiedOrders.issued == 1,
            UnifiedOrders.start_time >= month_ago.strftime('%Y-%m-%d %H:%M:%S')
        ).scalar() or 0

        # Получаем количество заказов за месяц
        month_orders = session.query(UnifiedOrders).filter(
            UnifiedOrders.issued == 1,
            UnifiedOrders.start_time >= month_ago.strftime('%Y-%m-%d %H:%M:%S')
        ).count()

        statistics = {
            'total_products': total_products,
            'total_offices': total_offices,
            'total_users': total_users,
            'total_workers': total_workers,
            'month_income': month_income,
            'month_orders': month_orders
        }

        return templates.TemplateResponse('owner/owner_statistics.html',
                                          {'request': request, 'statistics': statistics})
    finally:
        session.close()


@router.get('/owner/troubleshooting')
@access_required('owner')
async def owner_troubleshooting(request: Request):
    return templates.TemplateResponse('owner/owner_troubleshooting.html', {'request': request})


@router.get('/owner/fines')
@access_required('owner')
async def owner_fines(request: Request):
    """Заглушечный роут для управления штрафами владельцем"""
    # TODO: Реализовать получение штрафов из базы данных
    # Временные тестовые данные
    fines_data = [
        {
            'id': 1,
            'user_phone': '+79991234567',
            'user_name': 'Иванов Иван',
            'amount': 500.0,
            'reason': 'Повреждение оборудования',
            'status': 'unpaid',
            'created_at': '2024-01-15 10:30:00',
            'paid_at': None
        },
        {
            'id': 2,
            'user_phone': '+79997654321',
            'user_name': 'Петров Петр',
            'amount': 1000.0,
            'reason': 'Утеря устройства',
            'status': 'paid',
            'created_at': '2024-01-10 14:20:00',
            'paid_at': '2024-01-12 16:45:00'
        }
    ]

    return templates.TemplateResponse('owner/owner_fines.html',
                                      {'request': request, 'fines': fines_data})
