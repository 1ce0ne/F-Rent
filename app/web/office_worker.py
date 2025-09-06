from fastapi import APIRouter, Request

from app.utils.templates import templates
from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import Office, Product, UnifiedOrders, OrderTypes, User, WorkerOfficeAssignment
from data.Scripts import get_free_products

router = APIRouter()


@router.get('/worker/products')
@access_required('office_worker')
async def worker_products(request: Request):
    session = create_session()
    try:
        # Получаем текущего работника
        current_user = getattr(request.state, 'user', None)
        worker_id = current_user.worker_id if current_user else None

        if not worker_id:
            # Если нет worker_id, показываем все продукты (для совместимости)
            products = get_free_products(session)
        else:
            # Получаем офисы, к которым привязан работник
            assigned_offices = session.query(WorkerOfficeAssignment.office_id).filter_by(
                worker_id=worker_id,
                is_active=True
            ).all()

            if not assigned_offices:
                # Если работник не привязан ни к одному офису, показываем пустой список
                products = []
            else:
                office_ids = [office[0] for office in assigned_offices]
                # Получаем продукты только из назначенных офисов
                products = session.query(Product).filter(
                    Product.office_id.in_(office_ids),
                    Product.who_is_reserved.is_(None)
                ).all()

        return templates.TemplateResponse('worker/office/office_products.html',
                                          {'request': request, 'products': products})
    finally:
        session.close()


@router.get('/worker/orders')
@access_required('office_worker')
async def worker_orders(request: Request):
    session = create_session()
    try:
        # Получаем текущего работника
        current_user = getattr(request.state, 'user', None)
        worker_id = current_user.worker_id if current_user else None

        # Получаем офисные заказы
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        orders = []

        if office_type:
            query = session.query(UnifiedOrders).filter_by(
                order_type_id=office_type.type_id,
                returned=0
            )

            # Фильтруем заказы по офисам работника
            if worker_id:
                assigned_offices = session.query(WorkerOfficeAssignment.office_id).filter_by(
                    worker_id=worker_id,
                    is_active=True
                ).all()

                if assigned_offices:
                    office_ids = [office[0] for office in assigned_offices]
                    # Получаем продукты из назначенных офисов
                    product_ids = session.query(Product.id).filter(
                        Product.office_id.in_(office_ids)
                    ).all()
                    product_ids = [str(pid[0]) for pid in product_ids]

                    query = query.filter(UnifiedOrders.product_id.in_(product_ids))
                else:
                    # Если работник не привязан ни к одному офису, показываем пустой список
                    query = query.filter(False)

            unified_orders = query.all()

            # Создаем кортежи (order, user, product) для шаблона
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

        return templates.TemplateResponse('worker/office/office_orders.html',
                                          {'request': request, 'orders': orders})
    finally:
        session.close()


@router.get('/worker/returns')
@access_required('office_worker')
async def worker_returns(request: Request):
    session = create_session()
    try:
        # Получаем текущего работника
        current_user = getattr(request.state, 'user', None)
        worker_id = current_user.worker_id if current_user else None

        # Получаем офисные возвраты
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        returns = []

        if office_type:
            query = session.query(UnifiedOrders).filter_by(
                order_type_id=office_type.type_id,
                ready_for_return=1,
                returned=0
            )

            # Фильтруем возвраты по офисам работника
            if worker_id:
                assigned_offices = session.query(WorkerOfficeAssignment.office_id).filter_by(
                    worker_id=worker_id,
                    is_active=True
                ).all()

                if assigned_offices:
                    office_ids = [office[0] for office in assigned_offices]
                    # Получаем продукты из назначенных офисов
                    product_ids = session.query(Product.id).filter(
                        Product.office_id.in_(office_ids)
                    ).all()
                    product_ids = [str(pid[0]) for pid in product_ids]

                    query = query.filter(UnifiedOrders.product_id.in_(product_ids))
                else:
                    # Если работник не привязан ни к одному офису, показываем пустой список
                    query = query.filter(False)

            unified_returns = query.all()

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

        return templates.TemplateResponse('worker/office/office_returns.html',
                                          {'request': request, 'returns': returns})
    finally:
        session.close()


@router.get('/worker/offices')
@access_required('office_worker')
async def worker_offices(request: Request):
    session = create_session()
    try:
        # Получаем текущего работника
        current_user = getattr(request.state, 'user', None)
        worker_id = current_user.worker_id if current_user else None

        if not worker_id:
            # Если нет worker_id, показываем все офисы (для совместимости)
            offices = session.query(Office).all()
        else:
            # Получаем только офисы, к которым привязан работник
            offices = session.query(Office).join(
                WorkerOfficeAssignment, Office.id == WorkerOfficeAssignment.office_id
            ).filter(
                WorkerOfficeAssignment.worker_id == worker_id,
                WorkerOfficeAssignment.is_active == True
            ).all()

        return templates.TemplateResponse('worker/office/office_offices.html',
                                          {'request': request, 'offices': offices})
    finally:
        session.close()
