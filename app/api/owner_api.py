from datetime import datetime, timedelta

from fastapi import APIRouter, Request
from fastapi import Query
from fastapi.responses import JSONResponse

from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import UnifiedOrders, OrderTypes, Product

router = APIRouter()

@router.get("/api/owner/statistics")
@access_required('owner')
async def get_owner_statistics(request: Request, period: str = Query(..., pattern="^(day|week|month)$")):
    # остальной код функции
    session = create_session()
    try:
        now = datetime.now()
        if period == "day":
            start_date = now - timedelta(days=1)
        elif period == "week":
            start_date = now - timedelta(days=7)
        else:
            start_date = now - timedelta(days=30)

        # Получаем все заказы (офисные и обычные) через объединенную таблицу
        orders = session.query(UnifiedOrders).filter(
            UnifiedOrders.issued == 1,
            UnifiedOrders.start_time >= start_date.strftime('%Y-%m-%d %H:%M:%S')
        ).all()

        income = sum(order.rental_amount for order in orders)
        orders_count = len(orders)
        client_ids = set(order.client_id for order in orders if order.client_id)
        clients_count = len(client_ids)

        from collections import defaultdict

        orders_by_period = defaultdict(int)
        for order in orders:
            dt = datetime.strptime(order.start_time, '%Y-%m-%d %H:%M:%S')
            if period == "day":
                key = dt.strftime('%H:00')
            else:
                key = dt.strftime('%d.%m')
            orders_by_period[key] += order.rental_amount

        labels = sorted(orders_by_period.keys())
        chart = [orders_by_period[label] for label in labels]

        result = {
            "income": income,
            "orders": orders_count,
            "clients": clients_count,
            "labels": labels,
            "chart": chart
        }
        return JSONResponse(content=result)
    except Exception as e:
        return JSONResponse(content={"error": str(e)}, status_code=500)
    finally:
        session.close()

@router.get("/api/get_owner_products_statistics")
@access_required('owner')
async def get_owner_product_statistics(request: Request):
    session = create_session()
    try:
        # Получаем все товары
        products = session.query(Product).all()

        # Получаем все заказы через объединенную таблицу с типами
        orders_query = session.query(UnifiedOrders, OrderTypes).join(
            OrderTypes, UnifiedOrders.order_type_id == OrderTypes.type_id
        ).filter(UnifiedOrders.issued == 1).all()

        # Создаем словарь для быстрого доступа к товарам
        products_dict = {str(p.id): p for p in products}

        # Инициализируем статистику для каждого товара
        product_stats = {}

        for product in products:
            product_stats[str(product.id)] = {
                "product_id": product.id,                           # ID товара в базе данных
                "product_name": product.name,                       # Название товара
                "product_category": product.category or "Без категории",  # Категория товара
                "price_per_hour": float(product.price_per_hour),    # Цена аренды за час
                "price_per_day": float(product.price_per_day),      # Цена аренды за день
                "price_per_month": float(product.price_per_month),  # Цена аренды за месяц
                "total_income": 0,                                  # Общий доход от товара (руб.)
                "office_orders_count": 0,                          # Количество офисных заказов
                "office_orders_income": 0,                         # Доход от офисных заказов (руб.)
                "postamat_orders_count": 0,                        # Количество заказов через постаматы
                "postamat_orders_income": 0,                       # Доход от заказов через постаматы (руб.)
                "total_orders_count": 0,                           # Общее количество всех заказов
                "returned_orders": 0,                              # Количество возвращенных заказов
                "active_orders": 0,                                # Количество активных заказов
                "average_rental_time": 0,                          # Среднее время аренды (часы)
                "total_rental_time": 0,                            # Общее время аренды (часы)
                "popularity_rank": 0,                              # Рейтинг популярности по доходу (1 = самый прибыльный)
                "revenue_per_hour": 0                              # Доход за час использования (руб./час)
            }

        # Обрабатываем все заказы
        total_rental_time = {}
        for order, order_type in orders_query:
            product_id = str(order.product_id)
            if product_id in product_stats:
                stats = product_stats[product_id]

                # Общая статистика
                stats["total_income"] += order.rental_amount
                stats["total_orders_count"] += 1

                if order.returned == 1:
                    stats["returned_orders"] += 1
                else:
                    stats["active_orders"] += 1

                # Статистика по типам заказов
                if order_type.type_name == 'office':
                    stats["office_orders_count"] += 1
                    stats["office_orders_income"] += order.rental_amount
                elif order_type.type_name == 'postamat':
                    stats["postamat_orders_count"] += 1
                    stats["postamat_orders_income"] += order.rental_amount

                # Подсчет времени аренды
                rental_time = order.rental_time
                stats["total_rental_time"] += rental_time
                if product_id not in total_rental_time:
                    total_rental_time[product_id] = []
                total_rental_time[product_id].append(rental_time)

        # Вычисляем дополнительные метрики
        for product_id, stats in product_stats.items():
            # Средняя продолжительность аренды
            if stats["total_orders_count"] > 0:
                stats["average_rental_time"] = stats["total_rental_time"] / stats["total_orders_count"]

            # Доход за час использования
            if stats["total_rental_time"] > 0:
                stats["revenue_per_hour"] = stats["total_income"] / stats["total_rental_time"]

        # Сортируем по общему доходу для определения рейтинга популярности
        sorted_products = sorted(product_stats.values(), key=lambda x: x["total_income"], reverse=True)
        for rank, product in enumerate(sorted_products, 1):
            product["popularity_rank"] = rank

        # Добавляем общую статистику
        total_stats = {
            "total_products": len(products),                        # Общее количество товаров в системе
            "total_income_all_products": sum(stats["total_income"] for stats in product_stats.values()),  # Суммарный доход от всех товаров (руб.)
            "total_orders_all_products": sum(stats["total_orders_count"] for stats in product_stats.values()),  # Общее количество заказов по всем товарам
            "most_profitable_product": max(product_stats.values(), key=lambda x: x["total_income"])["product_name"] if product_stats else None,  # Название самого прибыльного товара
            "most_popular_product": max(product_stats.values(), key=lambda x: x["total_orders_count"])["product_name"] if product_stats else None  # Название самого популярного товара (по количеству заказов)
        }

        result = {
            "products": list(product_stats.values()),
            "summary": total_stats
        }

        return JSONResponse(content=result)

    except Exception as e:
        return JSONResponse(content={"error": str(e)}, status_code=500)
    finally:
        session.close()
