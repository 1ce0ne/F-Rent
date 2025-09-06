from datetime import datetime
from typing import Optional

from fastapi import APIRouter, Request, Form
from fastapi.responses import JSONResponse
from sqlalchemy import func, cast, Integer

from app.utils.auth import access_required
from app.utils.secure_cookies import get_verified_cookie
from app.utils.security import signer
from app.datac.db_session import create_session
from app.datac.__all_models import Product, Office, UnifiedOrders, OrderPhotos, User, Fines

router = APIRouter()


@router.get('/api/get-problem-products')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def get_problem_products(request: Request):
    """
    Получение списка всех товаров с проблемами
    """
    session = create_session()
    try:
        # Получаем товары с проблемами
        products = session.query(Product).filter(Product.have_problem == 1).all()

        result = []
        for product in products:
            # Получаем информацию об офисе/постамате
            location_info = None
            if product.office_id:
                office = session.query(Office).filter_by(id=product.office_id).first()
                if office:
                    location_info = {
                        'type': 'office',
                        'id': office.id,
                        'address': office.address
                    }
            elif product.address_of_postamat:
                location_info = {
                    'type': 'postamat',
                    'address': product.address_of_postamat
                }

            # Получаем последний заказ для контекста
            last_order = session.query(UnifiedOrders).filter(
                UnifiedOrders.product_id == str(product.id)
            ).order_by(UnifiedOrders.order_id.desc()).first()

            last_order_info = None
            if last_order:
                # Пытаемся найти пользователя по ID или номеру телефона
                user = None
                try:
                    user = session.query(User).filter_by(id=int(last_order.client_id)).first()
                except (ValueError, TypeError):
                    user = session.query(User).filter_by(phone_number=last_order.client_id).first()

                last_order_info = {
                    'order_id': last_order.order_id,
                    'client_name': user.name if user else "Неизвестный клиент",
                    'client_phone': user.phone_number if user else last_order.client_id,
                    'end_time': last_order.end_time,
                    'returned': bool(last_order.returned),
                    'comment': last_order.comment
                }

            product_data = {
                'id': product.id,
                'name': product.name,
                'description': product.description,
                'category': product.category,
                'size': product.size,
                'location': location_info,
                'last_order': last_order_info,
                'reservation_status': {
                    'is_reserved': bool(product.who_is_reserved),
                    'reserved_by': product.who_is_reserved,
                    'reservation_start': product.start_of_reservation
                }
            }

            result.append(product_data)

        return JSONResponse({
            'success': True,
            'problem_products': result,
            'total_count': len(result)
        })

    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/solve-problem/{product_id}')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def solve_problem(product_id: int, request: Request,
                       solution_comment: str = Form(...),
                       mark_as_solved: bool = Form(True)):
    """
    Решение проблемы с товаром
    """
    session = create_session()
    try:
        # Проверяем существование товара
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Товар не найден'}, status_code=404)

        if product.have_problem != 1:
            return JSONResponse({'error': 'У товара нет активных проблем'}, status_code=400)

        # Получаем информацию о пользователе
        user_id = get_verified_cookie(signer, request, 'user_id')
        user_role = get_verified_cookie(signer, request, 'user_role')

        # Если отмечаем как решенную, убираем флаг проблемы
        if mark_as_solved:
            product.have_problem = 0

        # Добавляем комментарий к последнему заказу, если он есть
        last_order = session.query(UnifiedOrders).filter(
            UnifiedOrders.product_id == str(product_id)
        ).order_by(UnifiedOrders.order_id.desc()).first()

        if last_order:
            current_comment = last_order.comment or ""
            timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            new_comment = f"[{timestamp}] Решение проблемы ({user_role} ID:{user_id}): {solution_comment}"

            if current_comment:
                last_order.comment = f"{current_comment}\n{new_comment}"
            else:
                last_order.comment = new_comment

            last_order.updated_at = timestamp

        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Проблема успешно решена' if mark_as_solved else 'Комментарий добавлен',
            'product_id': product_id,
            'solved': mark_as_solved
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/create-fine/{product_id}')
@access_required({'junior_admin', 'senior_admin', 'owner'})
async def create_fine(product_id: int, request: Request,
                     fine_amount: float = Form(...),
                     fine_reason: str = Form(...),
                     client_id: Optional[str] = Form(None)):
    """
    Создание штрафа за повреждение/потерю товара
    """
    session = create_session()
    try:
        # Проверяем товар
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Товар не найден'}, status_code=404)

        # Если client_id не указан, берем из последнего заказа
        if not client_id:
            last_order = session.query(UnifiedOrders).filter(
                UnifiedOrders.product_id == str(product_id)
            ).order_by(UnifiedOrders.order_id.desc()).first()

            if not last_order:
                return JSONResponse({'error': 'Не найден заказ для создания штрафа'}, status_code=400)

            client_id = last_order.client_id

        # Проверяем клиента
        client = None
        try:
            client = session.query(User).filter_by(id=int(client_id)).first()
        except (ValueError, TypeError):
            client = session.query(User).filter_by(phone_number=client_id).first()

        if not client:
            return JSONResponse({'error': 'Клиент не найден'}, status_code=404)

        # Получаем информацию о создателе штрафа
        admin_id = get_verified_cookie(signer, request, 'user_id')
        admin_role = get_verified_cookie(signer, request, 'user_role')

        # Создаем запись о штрафе (добавляем в комментарий к заказу)
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        fine_comment = f"[{timestamp}] ШТРАФ ({admin_role} ID:{admin_id}): {fine_amount} руб. Причина: {fine_reason}"

        # Находим заказ и добавляем информацию о штрафе
        order = session.query(UnifiedOrders).filter(
            UnifiedOrders.product_id == str(product_id),
            UnifiedOrders.client_id == client_id
        ).order_by(UnifiedOrders.order_id.desc()).first()

        if order:
            current_comment = order.comment or ""
            if current_comment:
                order.comment = f"{current_comment}\n{fine_comment}"
            else:
                order.comment = fine_comment
            order.updated_at = timestamp

        # Отмечаем товар как имеющий проблему
        product.have_problem = 1

        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Штраф успешно создан',
            'fine_details': {
                'product_id': product_id,
                'client_id': client_id,
                'client_name': client.name,
                'client_phone': client.phone_number,
                'amount': fine_amount,
                'reason': fine_reason,
                'created_by': f"{admin_role} ID:{admin_id}",
                'created_at': timestamp
            }
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/mark-problem/{product_id}')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def mark_product_problem(product_id: int, request: Request,
                              problem_description: str = Form(...)):
    """
    Отметить товар как проблемный
    """
    session = create_session()
    try:
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Товар не найден'}, status_code=404)

        # Отмечаем товар как проблемный
        product.have_problem = 1

        # Получаем информацию о сотруднике
        user_id = get_verified_cookie(signer, request, 'user_id')
        user_role = get_verified_cookie(signer, request, 'user_role')

        # Добавляем комментарий к последнему заказу
        last_order = session.query(UnifiedOrders).filter(
            UnifiedOrders.product_id == str(product_id)
        ).order_by(UnifiedOrders.order_id.desc()).first()

        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        problem_comment = f"[{timestamp}] ПРОБЛЕМА ({user_role} ID:{user_id}): {problem_description}"

        if last_order:
            current_comment = last_order.comment or ""
            if current_comment:
                last_order.comment = f"{current_comment}\n{problem_comment}"
            else:
                last_order.comment = problem_comment
            last_order.updated_at = timestamp

        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Товар отмечен как проблемный',
            'product_id': product_id
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/product-problem-history/{product_id}')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def get_product_problem_history(product_id: int, request: Request):
    """
    Получение истории проблем конкретного товара
    """
    session = create_session()
    try:
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Товар не найден'}, status_code=404)

        # Получаем все заказы товара с комментариями
        orders = session.query(UnifiedOrders).filter(
            UnifiedOrders.product_id == str(product_id)
        ).order_by(UnifiedOrders.order_id.desc()).all()

        history = []
        for order in orders:
            if order.comment:
                # Получаем информацию о клиенте
                client = None
                try:
                    client = session.query(User).filter_by(id=int(order.client_id)).first()
                except (ValueError, TypeError):
                    client = session.query(User).filter_by(phone_number=order.client_id).first()

                client_info = None
                if client:
                    client_info = {
                        'name': client.name,
                        'phone': client.phone_number
                    }

                # Получаем фотографии заказа
                photos = session.query(OrderPhotos).filter(
                    OrderPhotos.order_id == order.order_id
                ).all()

                history.append({
                    'order_id': order.order_id,
                    'client': client_info,
                    'start_time': order.start_time,
                    'end_time': order.end_time,
                    'returned': bool(order.returned),
                    'comment': order.comment,
                    'photos_count': len(photos),
                    'rental_amount': order.rental_amount
                })

        return JSONResponse({
            'success': True,
            'product': {
                'id': product.id,
                'name': product.name,
                'description': product.description,
                'have_problem': bool(product.have_problem)
            },
            'problem_history': history
        })

    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/problem-products-stats')
@access_required({'junior_admin', 'senior_admin', 'owner'})
async def get_problem_products_stats(request: Request):
    """
    Получение статистики по проблемным товарам
    """
    session = create_session()
    try:
        # Общая статистика
        total_products = session.query(Product).count()
        problem_products = session.query(Product).filter(Product.have_problem == 1).count()

        # Статистика по категориям
        category_stats_query = session.query(
            Product.category,
            func.count(Product.id).label('total'),
            func.sum(Product.have_problem).label('problems')
        ).filter(
            Product.category.isnot(None)
        ).group_by(Product.category).all()

        # Статистика по офисам
        office_stats_query = session.query(
            Office.address,
            func.count(Product.id).label('total_products'),
            func.sum(Product.have_problem).label('problem_products')
        ).join(
            Product, Product.office_id == Office.id
        ).group_by(Office.id, Office.address).all()

        # Последние проблемы - получаем товары с проблемами и их последние заказы с комментариями
        problem_products_with_orders = session.query(Product, UnifiedOrders).join(
            UnifiedOrders, cast(UnifiedOrders.product_id, Integer) == Product.id
        ).filter(
            Product.have_problem == 1,
            UnifiedOrders.comment.isnot(None)
        ).order_by(UnifiedOrders.updated_at.desc()).limit(10).all()

        return JSONResponse({
            'success': True,
            'stats': {
                'total_products': total_products,
                'problem_products': problem_products,
                'problem_percentage': round((problem_products / total_products * 100) if total_products > 0 else 0, 2)
            },
            'category_breakdown': [
                {
                    'category': row.category,
                    'total': row.total,
                    'problems': row.problems or 0,
                    'problem_rate': round(((row.problems or 0) / row.total * 100) if row.total > 0 else 0, 2)
                } for row in category_stats_query
            ],
            'office_breakdown': [
                {
                    'office_address': row.address,
                    'total_products': row.total_products,
                    'problem_products': row.problem_products or 0,
                    'problem_rate': round(((row.problem_products or 0) / row.total_products * 100) if row.total_products > 0 else 0, 2)
                } for row in office_stats_query
            ],
            'recent_problems': [
                {
                    'product_id': product.id,
                    'product_name': product.name,
                    'last_comment': order.comment[:200] + '...' if len(order.comment) > 200 else order.comment,
                    'updated_at': order.updated_at
                } for product, order in problem_products_with_orders
            ]
        })

    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/bulk-solve-problems')
@access_required({'senior_admin', 'owner'})
async def bulk_solve_problems(request: Request,
                             product_ids: str = Form(...),  # Comma-separated IDs
                             solution_comment: str = Form(...)):
    """
    Массовое решение проблем для нескольких товаров
    """
    session = create_session()
    try:
        # Парсим список ID товаров
        try:
            ids = [int(id.strip()) for id in product_ids.split(',') if id.strip()]
        except ValueError:
            return JSONResponse({'error': 'Неверный формат ID товаров'}, status_code=400)

        if not ids:
            return JSONResponse({'error': 'Не указаны ID товаров'}, status_code=400)

        # Получаем информацию о пользователе
        user_id = get_verified_cookie(signer, request, 'user_id')
        user_role = get_verified_cookie(signer, request, 'user_role')

        solved_count = 0
        errors = []

        for product_id in ids:
            try:
                product = session.query(Product).filter_by(id=product_id).first()
                if not product:
                    errors.append(f"Товар ID {product_id} не найден")
                    continue

                if product.have_problem != 1:
                    errors.append(f"Товар ID {product_id} не имеет активных проблем")
                    continue

                # Решаем проблему
                product.have_problem = 0

                # Добавляем комментарий
                last_order = session.query(UnifiedOrders).filter(
                    UnifiedOrders.product_id == str(product_id)
                ).order_by(UnifiedOrders.order_id.desc()).first()

                if last_order:
                    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                    new_comment = f"[{timestamp}] Массовое решение проблем ({user_role} ID:{user_id}): {solution_comment}"

                    current_comment = last_order.comment or ""
                    if current_comment:
                        last_order.comment = f"{current_comment}\n{new_comment}"
                    else:
                        last_order.comment = new_comment

                    last_order.updated_at = timestamp

                solved_count += 1

            except Exception as e:
                errors.append(f"Ошибка при обработке товара ID {product_id}: {str(e)}")

        session.commit()

        return JSONResponse({
            'success': True,
            'message': f'Решено проблем: {solved_count}',
            'solved_count': solved_count,
            'total_requested': len(ids),
            'errors': errors
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()
