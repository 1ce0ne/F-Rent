from fastapi import APIRouter, Request
from app.utils.templates import templates
from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import Product, Office, ParcelAutomat, UnifiedOrders, OrderTypes, User, Workers, WorkerRoles

router = APIRouter()

# Маппинг ролей для отображения на русском языке
ROLE_DISPLAY_MAP = {
    'postamat_worker': 'Работник постамата',
    'office_worker': 'Офисный работник',
    'junior_admin': 'Младший администратор',
    'senior_admin': 'Старший администратор',
    'owner': 'Владелец'
}


@router.get('/admin-senior/offices')
@access_required('senior_admin')
async def admin_senior_offices(request: Request):
    session = create_session()
    try:
        offices = session.query(Office).all()
        return templates.TemplateResponse('admin/senior/senior_admin_offices.html',
                                          {'request': request, 'offices': offices})
    finally:
        session.close()


@router.get('/admin-senior/products')
@access_required('senior_admin')
async def admin_senior_products(request: Request):
    session = create_session()
    try:
        products = session.query(Product).all()
        return templates.TemplateResponse('admin/senior/senior_admin_products.html',
                                          {'request': request, 'products': products})
    finally:
        session.close()


@router.get('/admin-senior/postamats')
@access_required('senior_admin')
async def admin_senior_postamats(request: Request):
    session = create_session()
    try:
        postamats = session.query(ParcelAutomat).all()
        return templates.TemplateResponse('admin/senior/senior_admin_postamats.html',
                                          {'request': request, 'postamats': postamats})
    finally:
        session.close()


@router.get('/admin-senior/orders')
@access_required('senior_admin')
async def admin_senior_orders(request: Request):
    session = create_session()
    try:
        # Получаем только офисные активные заказы (как в owner.py)
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        if not office_type:
            return templates.TemplateResponse('admin/senior/senior_admin_orders.html',
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

        return templates.TemplateResponse('admin/senior/senior_admin_orders.html',
                                          {'request': request, 'orders': orders})
    finally:
        session.close()


@router.get('/admin-senior/users')
@access_required('senior_admin')
async def admin_senior_users(request: Request):
    session = create_session()
    try:
        users = session.query(User).all()
        return templates.TemplateResponse('admin/senior/senior_admin_users.html',
                                          {'request': request, 'users': users})
    finally:
        session.close()


@router.get('/admin-senior/returns')
@access_required('senior_admin')
async def admin_senior_returns(request: Request):
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

        return templates.TemplateResponse('admin/senior/senior_admin_returns.html',
                                          {'request': request, 'returns': returns})
    finally:
        session.close()


@router.get('/admin-senior/workers')
@access_required('senior_admin')
async def admin_senior_workers(request: Request):
    session = create_session()
    try:
        # Получаем работников кроме владельцев
        workers_query = session.query(Workers, WorkerRoles).join(
            WorkerRoles, Workers.role_id == WorkerRoles.role_id
        ).filter(
            Workers.is_active == 1,
            WorkerRoles.role_name != 'owner'
        ).all()

        workers = []
        for worker, role in workers_query:
            workers.append({
                'id': worker.worker_id,
                'name': worker.worker_uid,  # Используем worker_uid как имя
                'position': ROLE_DISPLAY_MAP.get(role.role_name, role.role_description),  # Русское название роли
                'office': 'Не назначен',  # Пока нет связи с офисами
                'worker': worker,
                'role': role
            })

        return templates.TemplateResponse('admin/senior/senior_admin_workers.html',
                                          {'request': request, 'workers': workers})
    finally:
        session.close()


@router.get('/admin-senior/troubleshooting')
@access_required('admin_senior')
async def owner_troubleshooting(request: Request):
    return templates.TemplateResponse('admin-senior/admin_senior_troubleshooting.html', {'request': request})


@router.get('/admin-senior/fines')
@access_required('senior_admin')
async def admin_senior_fines(request: Request):
    """Заглушечный роут для управления штрафами старшим администратором"""
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
        },
        {
            'id': 3,
            'user_phone': '+79995556677',
            'user_name': 'Сидоров Сидор',
            'amount': 300.0,
            'reason': 'Просрочка возврата',
            'status': 'pending',
            'created_at': '2024-01-20 09:15:00',
            'paid_at': None
        }
    ]

    return templates.TemplateResponse('admin/senior/senior_admin_fines.html',
                                      {'request': request, 'fines': fines_data})
