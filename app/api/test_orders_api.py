from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse
from datetime import datetime, timedelta
import random
from typing import List, Dict, Any

from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import UnifiedOrders, OrderTypes, Product, User, Office

router = APIRouter()

class TestOrderAPI:
    """API для создания тестовых заказов"""

    @staticmethod
    def get_test_users_data():
        """Возвращает данные тестовых пользователей"""
        return [
            {"name": "Иван Петров", "phone": "79101234567"},
            {"name": "Мария Сидорова", "phone": "79102345678"},
            {"name": "Алексей Козлов", "phone": "79103456789"},
            {"name": "Елена Морозова", "phone": "79104567890"},
            {"name": "Дмитрий Волков", "phone": "79105678901"},
            {"name": "Анна Белова", "phone": "79106789012"},
            {"name": "Сергей Новиков", "phone": "79107890123"},
            {"name": "Ольга Смирнова", "phone": "79108901234"},
            {"name": "Николай Попов", "phone": "79109012345"},
            {"name": "Татьяна Кузнецова", "phone": "79100123456"}
        ]

    @staticmethod
    def get_rental_durations():
        """Возвращает варианты длительности аренды"""
        return [
            {"hours": 2, "name": "2 часа"},
            {"hours": 4, "name": "4 часа"},
            {"hours": 8, "name": "8 часов"},
            {"hours": 24, "name": "1 день"},
            {"hours": 48, "name": "2 дня"},
            {"hours": 72, "name": "3 дня"},
            {"hours": 168, "name": "1 неделя"},
            {"hours": 720, "name": "1 месяц"}
        ]


@router.post('/api/create-test-orders')
@access_required({'senior_admin', 'owner'})
async def api_create_test_orders(request: Request):
    """Создание тестовых заказов"""
    data = await request.json()
    count = data.get('count', 10)

    if count < 1 or count > 100:
        return JSONResponse({'error': 'Количество заказов должно быть от 1 до 100'}, status_code=400)

    session = create_session()
    try:
        # Создаем или получаем тестовых пользователей
        test_users_data = TestOrderAPI.get_test_users_data()
        user_ids = []

        for user_data in test_users_data:
            existing_user = session.query(User).filter_by(phone_number=user_data["phone"]).first()

            if not existing_user:
                new_user = User(
                    name=user_data["name"],
                    password="$2b$12$test_password_hash",
                    phone_number=user_data["phone"],
                    banned=0
                )
                session.add(new_user)
                session.flush()
                user_ids.append(str(new_user.id))
            else:
                user_ids.append(str(existing_user.id))

        # Получаем доступные продукты
        products = session.query(Product).all()

        if not products:
            return JSONResponse({'error': 'Нет доступных продуктов для создания заказов'}, status_code=400)

        # Улучшаем данные продуктов
        improved_products = []
        for product in products:
            # Проверяем и исправляем данные продуктов
            product_name = product.name if product.name and product.name.strip() and product.name != "na" else f"Товар-{product.id}"

            # Если нет адреса постамата, создаем тестовый
            address = product.address_of_postamat
            if not address or address.strip() == "" or address == "na":
                test_addresses = [
                    "ул. Ленина, д. 15, постамат №1",
                    "пр. Мира, д. 42, ТЦ 'Радуга'",
                    "ул. Советская, д. 28, вход со двора",
                    "пр. Победы, д. 71, около метро",
                    "ул. Гагарина, д. 33, 1 этаж"
                ]
                address = random.choice(test_addresses)

            improved_products.append({
                "id": product.id,  # Используем числовой ID вместо UUID
                "uuid": product.product_uuid,
                "name": product_name,
                "price_per_hour": float(product.price_per_hour) if product.price_per_hour else 100.0,
                "price_per_day": float(product.price_per_day) if product.price_per_day else 500.0,
                "office_id": product.office_id,
                "address": address
            })

        # Получаем типы заказов
        order_types = session.query(OrderTypes).all()
        if not order_types:
            return JSONResponse({'error': 'Нет типов заказов в базе данных'}, status_code=400)

        # Создаем тестовые заказы
        created_orders = []
        rental_durations = TestOrderAPI.get_rental_durations()

        for i in range(count):
            try:
                # Случайные данные
                user_id = random.choice(user_ids)
                product = random.choice(improved_products)
                order_type = random.choice(order_types)
                duration = random.choice(rental_durations)

                # Генерируем даты
                start_time = datetime.now() - timedelta(days=random.randint(0, 30))
                end_time = start_time + timedelta(hours=duration["hours"])

                # Рассчитываем стоимость
                if duration["hours"] <= 8:
                    rental_amount = int(product["price_per_hour"] * duration["hours"])
                elif duration["hours"] <= 72:
                    rental_amount = int(product["price_per_day"] * (duration["hours"] / 24))
                else:
                    rental_amount = int(product["price_per_day"] * 30)

                # Статус заказа
                returned = random.choice([0, 0, 0, 1])  # 75% активных

                # Создаем заказ
                new_order = UnifiedOrders(
                    product_id=product["id"],  # Используем числовой ID вместо UUID
                    client_id=user_id,
                    order_type_id=order_type.type_id,
                    start_time=start_time.strftime('%Y-%m-%d %H:%M:%S'),
                    end_time=end_time.strftime('%Y-%m-%d %H:%M:%S'),
                    rental_time=duration["hours"],
                    rental_amount=rental_amount,
                    returned=returned,
                    created_at=datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                    updated_at=datetime.now().strftime('%Y-%m-%d %H:%M:%S')
                )

                # Дополнительные поля для офисных заказов
                if order_type.type_name == "office" and product["office_id"]:
                    office = session.query(Office).filter_by(id=product["office_id"]).first()
                    if office:
                        new_order.address_office = office.address
                        new_order.issued = random.choice([0, 1])
                        new_order.accepted = random.choice([0, 1])
                        new_order.ready_for_return = random.choice([0, 1]) if returned else 0

                session.add(new_order)
                session.flush()

                created_orders.append({
                    "order_id": new_order.order_id,
                    "product_name": product["name"],
                    "client_id": user_id,
                    "order_type": order_type.type_name,
                    "duration": duration["name"],
                    "amount": rental_amount,
                    "status": "Возвращен" if returned else "Активен"
                })

            except Exception as e:
                continue

        session.commit()

        return JSONResponse({
            'success': True,
            'message': f'Успешно создано {len(created_orders)} тестовых заказов',
            'orders': created_orders[:10],  # Возвращаем первые 10 для отображения
            'total_created': len(created_orders)
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': f'Ошибка при создании заказов: {str(e)}'}, status_code=500)
    finally:
        session.close()


@router.delete('/api/cleanup-test-orders')
@access_required({'senior_admin', 'owner'})
async def api_cleanup_test_orders(request: Request):
    """Удаление всех тестовых заказов"""
    session = create_session()
    try:
        # Получаем номера тестовых пользователей
        test_phones = [user["phone"] for user in TestOrderAPI.get_test_users_data()]

        # Находим тестовых пользователей
        test_users = session.query(User).filter(
            User.phone_number.in_(test_phones)
        ).all()

        test_user_ids = [str(user.id) for user in test_users]

        # Удаляем заказы тестовых пользователей
        deleted_orders = session.query(UnifiedOrders).filter(
            UnifiedOrders.client_id.in_(test_user_ids)
        ).count()

        session.query(UnifiedOrders).filter(
            UnifiedOrders.client_id.in_(test_user_ids)
        ).delete(synchronize_session=False)

        # Удаляем тестовых пользователей
        deleted_users = len(test_users)
        for user in test_users:
            session.delete(user)

        session.commit()

        return JSONResponse({
            'success': True,
            'message': f'Удалено {deleted_orders} тестовых заказов и {deleted_users} тестовых пользователей'
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': f'Ошибка при удалении данных: {str(e)}'}, status_code=500)
    finally:
        session.close()


@router.get('/api/test-orders-stats')
@access_required({'senior_admin', 'owner'})
async def api_test_orders_stats(request: Request):
    """Получение статистики по тестовым заказам"""
    session = create_session()
    try:
        # Получаем номера тестовых пользователей
        test_phones = [user["phone"] for user in TestOrderAPI.get_test_users_data()]

        # Находим тестовых пользователей
        test_users = session.query(User).filter(
            User.phone_number.in_(test_phones)
        ).all()

        test_user_ids = [str(user.id) for user in test_users]

        # Считаем статистику заказов
        total_orders = session.query(UnifiedOrders).filter(
            UnifiedOrders.client_id.in_(test_user_ids)
        ).count()

        active_orders = session.query(UnifiedOrders).filter(
            UnifiedOrders.client_id.in_(test_user_ids),
            UnifiedOrders.returned == 0
        ).count()

        returned_orders = session.query(UnifiedOrders).filter(
            UnifiedOrders.client_id.in_(test_user_ids),
            UnifiedOrders.returned == 1
        ).count()

        return JSONResponse({
            'test_users': len(test_users),
            'total_orders': total_orders,
            'active_orders': active_orders,
            'returned_orders': returned_orders
        })

    except Exception as e:
        return JSONResponse({'error': f'Ошибка при получении статистики: {str(e)}'}, status_code=500)
    finally:
        session.close()
