from fastapi import APIRouter, Depends, HTTPException, Header
from pydantic import BaseModel
from typing import Optional, List
import jwt
from datetime import datetime, timedelta
import os

from app.mobile_api.auth import verify_api_key
from app.datac.db_session import create_session
from app.datac.__all_models import Product, Office, Orders, OfficeOrders, User

router = APIRouter(prefix="/mobile/rental", tags=["Mobile Rental"])

# JWT настройки
JWT_SECRET = os.getenv("SECRET_KEY", "CoJoaWaCmZ25mw{PoY%*f~7O9Eet")
JWT_ALGORITHM = "HS256"

class RentalRequest(BaseModel):
    product_id: int
    rental_hours: int  # количество часов аренды
    rental_type: str  # "office" или "postamat"

class RentalResponse(BaseModel):
    success: bool
    order_id: Optional[int] = None
    product_name: Optional[str] = None
    start_time: Optional[str] = None
    end_time: Optional[str] = None
    total_amount: Optional[float] = None
    pickup_address: Optional[str] = None
    message: str = None

class OrderResponse(BaseModel):
    order_id: int
    product_id: str
    product_name: str
    start_time: str
    end_time: str
    rental_amount: float
    returned: bool
    pickup_address: str
    order_type: str  # "office" или "postamat"

class OrdersListResponse(BaseModel):
    success: bool
    orders: List[OrderResponse] = []
    message: str = None

def get_user_from_token(authorization: str) -> Optional[dict]:
    """Извлечение данных пользователя из JWT токена (анлимитный токен)"""
    try:
        if not authorization.startswith("Bearer "):
            raise HTTPException(status_code=401, detail="Invalid authorization header")

        token = authorization.replace("Bearer ", "")
        # Убираем проверку времени истечения для анлимитного токена
        payload = jwt.decode(token, JWT_SECRET, algorithms=[JWT_ALGORITHM], options={"verify_exp": False})
        return payload
    except jwt.InvalidTokenError:
        raise HTTPException(status_code=401, detail="Invalid token")

@router.post("/create", response_model=RentalResponse)
async def create_rental(
    request: RentalRequest,
    authorization: str = Header(...),
    api_key_valid: bool = Depends(verify_api_key)
):
    """
    Создание заказа на аренду товара
    """
    try:
        user_data = get_user_from_token(authorization)

        session = create_session()
        try:
            # Проверяем существование пользователя
            user = session.query(User).filter_by(id=user_data["user_id"]).first()
            if not user:
                return RentalResponse(
                    success=False,
                    message="Пользователь не найден"
                )

            # Проверяем, не заблокирован ли пользователь
            if user.banned == 1:
                return RentalResponse(
                    success=False,
                    message="Аккаунт заблокирован"
                )

            # Проверяем наличие паспорта для аренды
            if not user.have_a_passport:
                return RentalResponse(
                    success=False,
                    message="Для аренды необходимо загрузить паспорт в профиле"
                )

            # Получаем товар
            product = session.query(Product).filter_by(id=request.product_id).first()
            if not product:
                return RentalResponse(
                    success=False,
                    message="Товар не найден"
                )

            # Проверяем доступность товара
            if product.who_is_reserved is not None:
                return RentalResponse(
                    success=False,
                    message="Товар уже зарезервирован"
                )

            if product.have_problem == 1:
                return RentalResponse(
                    success=False,
                    message="Товар временно недоступен (неисправен)"
                )

            # Валидация времени аренды
            if request.rental_hours < 1 or request.rental_hours > 24 * 30:  # максимум месяц
                return RentalResponse(
                    success=False,
                    message="Время аренды должно быть от 1 часа до 30 дней"
                )

            # Рассчитываем стоимость
            total_amount = 0
            if request.rental_hours <= 24:
                # Почасовая оплата
                total_amount = product.price_per_hour * request.rental_hours
            elif request.rental_hours <= 24 * 7:
                # Подневная оплата
                days = (request.rental_hours + 23) // 24  # округляем вверх
                total_amount = product.price_per_day * days
            else:
                # Помесячная оплата
                months = (request.rental_hours + 24 * 30 - 1) // (24 * 30)  # округляем вверх
                total_amount = product.price_per_month * months

            # Определяем время начала и окончания
            start_time = datetime.now()
            end_time = start_time + timedelta(hours=request.rental_hours)

            start_time_str = start_time.strftime("%Y-%m-%d %H:%M:%S")
            end_time_str = end_time.strftime("%Y-%m-%d %H:%M:%S")

            # Резервируем товар
            product.who_is_reserved = user.phone_number
            product.start_of_reservation = start_time_str

            # Создаем заказ в зависимости от типа
            if request.rental_type == "office" and product.office_id:
                # Офисный заказ
                office = session.query(Office).filter_by(id=product.office_id).first()
                pickup_address = office.address if office else "Адрес офиса не указан"

                new_order = OfficeOrders(
                    product_id=str(product.id),
                    client_id=str(user.id),
                    start_time=start_time_str,
                    end_time=end_time_str,
                    rental_time=request.rental_hours,
                    rental_amount=int(total_amount),
                    returned=0,
                    ready_for_return=0,
                    issued=0,
                    not_issued=1,
                    accepted=1,
                    address_office=pickup_address
                )
                session.add(new_order)
            else:
                # Постаматный заказ
                pickup_address = product.address_of_postamat or "Адрес постамата не указан"

                new_order = Orders(
                    product_id=str(product.id),
                    client_id=str(user.id),
                    start_time=start_time_str,
                    end_time=end_time_str,
                    rental_time=request.rental_hours,
                    rental_amount=int(total_amount),
                    returned=0
                )
                session.add(new_order)

            session.commit()

            return RentalResponse(
                success=True,
                order_id=new_order.order_id,
                product_name=product.name,
                start_time=start_time_str,
                end_time=end_time_str,
                total_amount=total_amount,
                pickup_address=pickup_address,
                message="Заказ создан успешно"
            )

        except Exception as e:
            session.rollback()
            return RentalResponse(
                success=False,
                message=f"Ошибка создания заказа: {str(e)}"
            )
        finally:
            session.close()

    except Exception as e:
        return RentalResponse(
            success=False,
            message=f"Ошибка: {str(e)}"
        )

@router.get("/my-orders", response_model=OrdersListResponse)
async def get_my_orders(
    authorization: str = Header(...),
    api_key_valid: bool = Depends(verify_api_key)
):
    """
    Получение списка заказов пользователя
    """
    try:
        user_data = get_user_from_token(authorization)

        session = create_session()
        try:
            user_id_str = str(user_data["user_id"])

            # Получаем офисные заказы
            office_orders = session.query(OfficeOrders).filter_by(client_id=user_id_str).all()

            # Получаем постаматные заказы
            postamat_orders = session.query(Orders).filter_by(client_id=user_id_str).all()

            orders_list = []

            # Обрабатываем офисные заказы
            for order in office_orders:
                product = session.query(Product).filter_by(id=int(order.product_id)).first()
                product_name = product.name if product else "Товар не найден"

                orders_list.append(OrderResponse(
                    order_id=order.order_id,
                    product_id=order.product_id,
                    product_name=product_name,
                    start_time=order.start_time,
                    end_time=order.end_time,
                    rental_amount=float(order.rental_amount),
                    returned=bool(order.returned),
                    pickup_address=order.address_office,
                    order_type="office"
                ))

            # Обрабатываем постаматные заказы
            for order in postamat_orders:
                product = session.query(Product).filter_by(id=int(order.product_id)).first()
                product_name = product.name if product else "Товар не найден"
                pickup_address = product.address_of_postamat if product else "Адрес не указан"

                orders_list.append(OrderResponse(
                    order_id=order.order_id,
                    product_id=order.product_id,
                    product_name=product_name,
                    start_time=order.start_time,
                    end_time=order.end_time,
                    rental_amount=float(order.rental_amount),
                    returned=bool(order.returned),
                    pickup_address=pickup_address,
                    order_type="postamat"
                ))

            # Сортируем по дате создания (по убыванию)
            orders_list.sort(key=lambda x: x.start_time, reverse=True)

            return OrdersListResponse(
                success=True,
                orders=orders_list,
                message=f"Найдено {len(orders_list)} заказов"
            )

        finally:
            session.close()

    except Exception as e:
        return OrdersListResponse(
            success=False,
            message=f"Ошибка получения заказов: {str(e)}"
        )

@router.post("/cancel/{order_id}")
async def cancel_order(
    order_id: int,
    authorization: str = Header(...),
    api_key_valid: bool = Depends(verify_api_key)
):
    """
    Отмена заказа (только если товар еще не выдан)
    """
    try:
        user_data = get_user_from_token(authorization)

        session = create_session()
        try:
            user_id_str = str(user_data["user_id"])

            # Ищем заказ в офисных заказах
            office_order = session.query(OfficeOrders).filter_by(
                order_id=order_id,
                client_id=user_id_str
            ).first()

            if office_order:
                if office_order.issued == 1:
                    return {
                        "success": False,
                        "message": "Нельзя отменить заказ - товар уже выдан"
                    }

                # Освобождаем товар
                product = session.query(Product).filter_by(id=int(office_order.product_id)).first()
                if product:
                    product.who_is_reserved = None
                    product.start_of_reservation = None

                # Удаляем заказ
                session.delete(office_order)
                session.commit()

                return {
                    "success": True,
                    "message": "Заказ отменен успешно"
                }

            # Ищем заказ в постаматных заказах
            postamat_order = session.query(Orders).filter_by(
                order_id=order_id,
                client_id=user_id_str
            ).first()

            if postamat_order:
                # Освобождаем товар
                product = session.query(Product).filter_by(id=int(postamat_order.product_id)).first()
                if product:
                    product.who_is_reserved = None
                    product.start_of_reservation = None

                # Удаляем заказ
                session.delete(postamat_order)
                session.commit()

                return {
                    "success": True,
                    "message": "Заказ отменен успешно"
                }

            return {
                "success": False,
                "message": "Заказ не найден"
            }

        except Exception as e:
            session.rollback()
            return {
                "success": False,
                "message": f"Ошибка отмены заказа: {str(e)}"
            }
        finally:
            session.close()

    except Exception as e:
        return {
            "success": False,
            "message": f"Ошибка: {str(e)}"
        }
