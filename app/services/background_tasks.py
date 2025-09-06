import asyncio
import logging
from datetime import datetime, timedelta
import pytz
from app.datac.db_session import create_session
from app.datac.__all_models import Product, UnifiedOrders, OrderTypes, User

# Импорт функций для работы с платежами
try:
    from app.utils.cloudpayments import charge_token_payment
except ImportError:
    # Если импорт не удался, создаем заглушку
    async def charge_token_payment(*args, **kwargs):
        return {'success': False, 'message': 'Payment function not available'}

# Настройка логирования
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# Установка часового пояса Екатеринбурга
YEKT = pytz.timezone('Asia/Yekaterinburg')


def get_user_by_id(user_id):
    """Получить данные пользователя по ID"""
    session = create_session()
    try:
        user = session.query(User).filter_by(id=int(user_id)).first()
        if user:
            return {
                'id': user.id,
                'name': user.name,
                'phone_number': user.phone_number,
                'card_token': None  # TODO: Добавить поддержку токенов карт если нужно
            }
        return None
    except Exception as e:
        logger.error(f"Ошибка при получении пользователя {user_id}: {e}")
        return None
    finally:
        session.close()


async def check_and_update_reservations():
    """Проверяет и обновляет резервации продуктов"""
    session = create_session()
    try:
        now = datetime.now(YEKT)
        threshold = now - timedelta(minutes=30)

        # Находим все продукты с истекшим временем резервации
        expired_reservations = session.query(Product).filter(
            Product.start_of_reservation.isnot(None),
            Product.start_of_reservation <= threshold.strftime('%Y-%m-%d %H:%M:%S')
        ).all()

        # Сбрасываем резервацию для истекших
        for product in expired_reservations:
            product.who_is_reserved = None
            product.start_of_reservation = None

        session.commit()
        logger.info(f"Обновлено {len(expired_reservations)} резерваций продуктов")
    except Exception as e:
        session.rollback()
        logger.error(f"Ошибка при обновлении резерваций: {e}")
    finally:
        session.close()


async def check_and_extend_office_orders():
    """Проверяет и продлевает офисные заказы с автоматическим списанием"""
    session = create_session()
    try:
        logger.info("Начинаем проверку офисных заказов для продления")
        now = datetime.now(YEKT)

        logger.debug(f"Текущее время: {now.strftime('%Y-%m-%d %H:%M:%S')}")

        # Получаем тип офисных заказов
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        if not office_type:
            logger.warning("Тип заказа 'office' не найден в базе данных")
            return

        # Находим все офисные заказы, которые нужно продлить через UnifiedOrders
        orders_to_extend = session.query(UnifiedOrders).filter(
            UnifiedOrders.order_type_id == office_type.type_id,
            UnifiedOrders.end_time < now.strftime('%Y-%m-%d %H:%M:%S'),
            UnifiedOrders.returned == 0
        ).all()

        logger.info(f"Найдено {len(orders_to_extend)} заказов для продления")

        for order in orders_to_extend:
            logger.debug(f"Обрабатываем заказ ID: {order.order_id}, продукт ID: {order.product_id}, "
                        f"время окончания: {order.end_time}")

            # Находим соответствующий продукт по UUID
            product = session.query(Product).filter(
                Product.product_uuid == order.product_id
            ).first()

            if product:
                # Рассчитываем новое время и стоимость (добавляем 45 минут)
                old_end_time = order.end_time
                new_end_time = (datetime.strptime(order.end_time, '%Y-%m-%d %H:%M:%S') +
                                timedelta(minutes=45)).strftime('%Y-%m-%d %H:%M:%S')

                if order.renewal_time:
                    new_renewal_time = (datetime.strptime(order.renewal_time, '%Y-%m-%d %H:%M:%S') +
                                        timedelta(minutes=45)).strftime('%Y-%m-%d %H:%M:%S')
                else:
                    new_renewal_time = (now + timedelta(minutes=45)).strftime('%Y-%m-%d %H:%M:%S')

                # Рассчитываем стоимость за 45 минут
                additional_amount = product.price_per_hour
                old_rental_amount = order.rental_amount

                # Получаем данные пользователя для списания
                user_data = get_user_by_id(order.client_id)

                if user_data and user_data.get('card_token'):
                    # Попытка списать деньги за продление
                    logger.info(f"Попытка списания {additional_amount} руб. за продление заказа {order.order_id}")

                    # Формируем данные для платежа
                    invoice_id = f'auto_extend_office_{order.order_id}_{int(now.timestamp())}'
                    description = f'Автопродление офисного заказа #{order.order_id} на 45 минут'

                    try:
                        payment_result = await charge_token_payment(
                            token=user_data['card_token'],
                            amount=float(additional_amount),
                            account_id=str(user_data['id']),
                            phone_number=user_data['phone_number'],
                            invoice_id=invoice_id,
                            description=description
                        )

                        if payment_result['success']:
                            # Успешная оплата - продлеваем заказ
                            new_rental_amount = order.rental_amount + additional_amount

                            order.end_time = new_end_time
                            order.renewal_time = new_renewal_time
                            order.rental_amount = new_rental_amount

                            logger.info(f"✅ Заказ ID: {order.order_id} успешно продлен и оплачен")
                            logger.debug(f"  - Время окончания: {old_end_time} -> {new_end_time}")
                            logger.debug(f"  - Стоимость аренды: {old_rental_amount} -> {new_rental_amount}")
                            logger.debug(f"  - ID транзакции: {payment_result.get('transaction_id')}")
                        else:
                            # Неудачная оплата - не продлеваем заказ
                            logger.warning(f"❌ Не удалось списать средства за продление заказа {order.order_id}: {payment_result.get('message')}")
                            logger.warning(f"  - Код ошибки: {payment_result.get('reason_code')}")
                            logger.warning(f"  - Заказ не будет продлен из-за неудачной оплаты")

                    except Exception as payment_error:
                        logger.error(f"❌ Ошибка при попытке оплаты продления заказа {order.order_id}: {payment_error}")
                        logger.warning(f"  - Заказ не будет продлен из-за ошибки оплаты")
                else:
                    # Нет данных карты - продлеваем без оплаты (как было раньше)
                    logger.warning(f"⚠️ У пользователя {order.client_id} нет привязанной карты для заказа {order.order_id}")
                    logger.info(f"  - Продлеваем заказ без автоматической оплаты")

                    new_rental_amount = order.rental_amount + additional_amount

                    order.end_time = new_end_time
                    order.renewal_time = new_renewal_time
                    order.rental_amount = new_rental_amount

                    logger.info(f"📝 Заказ ID: {order.order_id} продлен без оплаты")
                    logger.debug(f"  - Время окончания: {old_end_time} -> {new_end_time}")
                    logger.debug(f"  - Стоимость аренды: {old_rental_amount} -> {new_rental_amount}")
            else:
                logger.warning(f"⚠️ Продукт с ID {order.product_id} не найден для заказа {order.order_id}")

        session.commit()
        logger.info(f"✅ Обработка продления завершена для {len(orders_to_extend)} заказов")

    except Exception as e:
        session.rollback()
        logger.error(f"❌ Ошибка при продлении офисных заказов: {e}", exc_info=True)
    finally:
        session.close()
        logger.debug("Сессия БД для заказов закрыта")


async def background_task_runner():
    """Основная функция для запуска фоновых задач"""
    logger.info("Запуск фоновых задач")

    while True:
        try:
            logger.info(f"Выполнение проверки в {datetime.now(YEKT).strftime('%Y-%m-%d %H:%M:%S')}")

            # Выполняем обе задачи параллельно
            await asyncio.gather(
                check_and_update_reservations(),
                check_and_extend_office_orders()
            )

            # Ждем 2 минуты перед следующей проверкой
            await asyncio.sleep(120)

        except Exception as e:
            logger.error(f"Ошибка в фоновых задачах: {e}")
            await asyncio.sleep(60)  # При ошибке ждем 1 минуту перед повтором


def start_background_tasks():
    """Запускает фоновые задачи в отдельной задаче asyncio"""
    return asyncio.create_task(background_task_runner())
