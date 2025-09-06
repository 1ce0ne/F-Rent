import os
from datetime import datetime, timedelta

from fastapi import APIRouter, HTTPException, Request
from fastapi.responses import JSONResponse, StreamingResponse

from app.utils.auth import access_required
from app.utils.secure_cookies import get_verified_cookie
from app.utils.security import signer
from app.datac.db_session import create_session
from app.datac.__all_models import Product, Office, UnifiedOrders, OrderTypes, OrderPhotos, User
from data.Scripts import get_free_products, add_new_product

router = APIRouter()


@router.get('/api/products')
@access_required()
async def api_get_products(request: Request):
    session = create_session()
    try:
        products = get_free_products(session)
        result = []
        for p in products:
            product_data = {
                'id': p.id,
                'name': p.name,
                'category': p.category or '',
                'price_per_hour': p.price_per_hour,
                'price_per_day': p.price_per_day,
                'price_per_month': p.price_per_month,
                'office_id': p.office_id,
                'office_address': None
            }

            # Получаем адрес офиса, если товар привязан к офису
            if p.office_id:
                office = session.query(Office).filter_by(id=p.office_id).first()
                if office:
                    product_data['office_address'] = office.address

            result.append(product_data)

        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/products/{product_id}')
@access_required()
async def api_get_product(product_id: int, request: Request):
    session = create_session()
    try:
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Product not found'}, status_code=404)

        result = {
            'id': product.id,
            'name': product.name,
            'description': product.description,
            'category': product.category,
            'price_per_hour': product.price_per_hour,
            'price_per_day': product.price_per_day,
            'price_per_month': product.price_per_month,
            'office_id': product.office_id,
            'office_address': None
        }

        # Получаем адрес офиса, если товар привязан к офису
        if product.office_id:
            office = session.query(Office).filter_by(id=product.office_id).first()
            if office:
                result['office_address'] = office.address

        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/get-product-image/{product_id}')
@access_required()
async def get_product_image(product_id: int, request: Request):
    session = create_session()
    try:
        product = session.query(Product).filter_by(id=product_id).first()
        if product and product.image:
            content_type = 'image/jpeg'
            if product.image.startswith(b'\x89PNG\r\n\x1a\n'):
                content_type = 'image/png'
            return StreamingResponse(iter([product.image]), media_type=content_type)
        # Абсолютный путь к placeholder
        static_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'static', 'img')
        default_image_path = os.path.abspath(os.path.join(static_dir, 'placeholder.jpg'))
        if not os.path.exists(default_image_path):
            raise HTTPException(status_code=500, detail="Placeholder image not found")
        with open(default_image_path, 'rb') as f:
            default_image = f.read()
        return StreamingResponse(iter([default_image]), media_type='image/jpeg')
    except Exception as e:
        print(f"Error getting product image: {e}")
        raise HTTPException(status_code=500)
    finally:
        session.close()


@router.get('/api/get-product-image-url/{product_id}')
@access_required()
async def api_get_product_image_url(product_id: int, request: Request):
    return JSONResponse({
        'image_url': f'/get-product-image/{product_id}'
    })


@router.post('/api/add-product-to-office')
@access_required()
async def api_add_product_to_office(request: Request):
    data = await request.json()
    office_id = data.get('office_id')
    product_id = data.get('product_id')
    if not office_id or not product_id:
        return JSONResponse({'success': False, 'error': 'Не указан офис или товар'}, status_code=400)
    session = create_session()
    try:
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'success': False, 'error': 'Товар не найден'}, status_code=404)
        office = session.query(Office).filter_by(id=office_id).first()
        if not office:
            return JSONResponse({'success': False, 'error': 'Офис не найден'}, status_code=404)

        # Получаем тип заказа "office"
        office_type = session.query(OrderTypes).filter_by(type_name='office').first()
        if not office_type:
            return JSONResponse({'success': False, 'error': 'Тип заказа "office" не найден'}, status_code=404)

        current_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        new_order = UnifiedOrders(
            product_id=str(product_id),
            client_id='0',
            order_type_id=office_type.type_id,
            start_time=current_time,
            end_time=(datetime.now() + timedelta(hours=1)).strftime('%Y-%m-%d %H:%M:%S'),
            rental_time=60,
            rental_amount=int(product.price_per_hour),
            address_office=office.address,
            issued=0,
            not_issued=0,
            created_at=current_time,
            updated_at=current_time
        )
        session.add(new_order)
        session.commit()
        return JSONResponse({'success': True})
    except Exception as e:
        session.rollback()
        return JSONResponse({'success': False, 'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/add-product')
@access_required({'senior_admin', 'owner'})
async def api_add_product(request: Request):
    # Проверяем тип контента
    if request.headers.get('content-type', '').startswith('multipart/form-data'):
        form = await request.form()
        name = form.get('name')
        price_per_hour = form.get('price_per_hour')
        price_per_day = form.get('price_per_day')
        price_per_month = form.get('price_per_month')
        description = form.get('description')
        size = form.get('size')
        category = form.get('category')
        office_id = form.get('office_id')  # Добавляем office_id
        if category is not None:
            category = str(category)
        image_file = form.get('image')
        image_bytes = await image_file.read() if image_file else None
    else:
        data = await request.json()
        name = data.get('name')
        price_per_hour = data.get('price_per_hour')
        price_per_day = data.get('price_per_day')
        price_per_month = data.get('price_per_month')
        description = data.get('description')
        size = data.get('size')
        category = data.get('category')
        office_id = data.get('office_id')  # Добавляем office_id
        image_bytes = data.get('image', '').encode() if data.get('image') else None

    if not name or price_per_hour is None or price_per_day is None or price_per_month is None or not size:
        return JSONResponse({'error': 'Не указаны обязательные поля'}, status_code=400)

    session = create_session()
    try:
        # Проверяем существование офиса, если office_id указан
        if office_id:
            office = session.query(Office).filter_by(id=office_id).first()
            if not office:
                return JSONResponse({'error': 'Указанный офис не найден'}, status_code=404)

        # Создаем новый товар с привязкой к офису
        product = add_new_product(session, name, price_per_hour, price_per_day, price_per_month,
                                description, size, category, image_bytes, office_id)
        return JSONResponse({'success': True, 'product_id': product.id})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.put('/api/update-product/{product_id}')
@access_required({'senior_admin', 'owner'})
async def api_update_product(product_id: int, request: Request):
    user_role = get_verified_cookie(signer, request, 'user_role')
    if user_role not in ['senior_admin', 'owner']:
        return JSONResponse({'error': 'Unauthorized'}, status_code=403)
    data = await request.json()
    session = create_session()
    try:
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Product not found'}, status_code=404)

        if 'name' in data:
            product.name = data['name']
        if 'description' in data:
            product.description = data['description']
        if 'price_per_hour' in data:
            product.price_per_hour = data['price_per_hour']
        if 'price_per_day' in data:
            product.price_per_day = data['price_per_day']
        if 'price_per_month' in data:
            product.price_per_month = data['price_per_month']
        if 'category' in data:
            product.category = data['category']
        if 'size' in data:
            product.size = data['size']
        if 'office_id' in data:
            # Проверяем существование офиса
            if data['office_id']:
                office = session.query(Office).filter_by(id=data['office_id']).first()
                if not office:
                    return JSONResponse({'error': 'Указанный офис не найден'}, status_code=404)
            product.office_id = data['office_id']

        session.commit()
        return JSONResponse({'success': True, 'message': 'Product updated successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/assign-product-to-office')
@access_required({'senior_admin', 'owner'})
async def api_assign_product_to_office(request: Request):
    """
    Привязывает товар к офису
    """
    data = await request.json()
    product_id = data.get('product_id')
    office_id = data.get('office_id')

    if not product_id:
        return JSONResponse({'error': 'Не указан ID товара'}, status_code=400)

    session = create_session()
    try:
        # Находим товар
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Товар не найден'}, status_code=404)

        # Проверяем офис, если указан
        if office_id:
            office = session.query(Office).filter_by(id=office_id).first()
            if not office:
                return JSONResponse({'error': 'Офис не найден'}, status_code=404)

        # Привязываем товар к офису (и��и отвязываем, если office_id = None)
        product.office_id = office_id
        session.commit()

        return JSONResponse({
            'success': True,
            'message': f'Товар {"привязан к офису" if office_id else "отвязан от офиса"}'
        })
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.delete('/api/delete-product/{product_id}')
@access_required({'senior_admin', 'owner'})
async def api_delete_product(product_id: int, request: Request):
    user_role = get_verified_cookie(signer, request, 'user_role')
    if user_role not in ['senior_admin', 'owner']:
        return JSONResponse({'error': 'Unauthorized'}, status_code=403)
    session = create_session()
    try:
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Product not found'}, status_code=404)
        session.delete(product)
        session.commit()
        return JSONResponse({'success': True, 'message': 'Product deleted successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/offices')
@access_required()
async def api_get_offices(request: Request):
    """
    П��лучить список всех офисов
    """
    session = create_session()
    try:
        offices = session.query(Office).all()
        result = []
        for office in offices:
            result.append({
                'id': office.id,
                'address': office.address,
                'first_coordinate': office.first_coordinate,
                'second_coordinate': office.second_coordinate
            })
        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-product-history/{product_id}')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def api_get_product_history(product_id: int, request: Request):
    """
    Получение максимальной истории продукта включая:
    - Детальную информацию о продукте
    - Все заказы (офисные и через постаматы)
    - Фотографии для каждого заказа
    - Информацию о клиентах
    - Статистику использования
    - Проблемы и комментарии
    """
    session = create_session()
    try:
        # 1. Получаем основную информацию о продукте
        product = session.query(Product).filter_by(id=product_id).first()
        if not product:
            return JSONResponse({'error': 'Product not found'}, status_code=404)

        # 2. Получаем информацию об офисе (если привязан)
        office_info = None
        if product.office_id:
            office = session.query(Office).filter_by(id=product.office_id).first()
            if office:
                office_info = {
                    'id': office.id,
                    'address': office.address,
                    'coordinates': {
                        'lat': office.first_coordinate,
                        'lng': office.second_coordinate
                    }
                }

        # 3. Получаем все заказы продукта (включая старые таблицы для полноты истории)
        # Сначала из новой унифицированной таблицы
        unified_orders = session.query(UnifiedOrders).filter_by(product_id=str(product_id)).all()

        # Также проверяем старые таблицы для полной истории
        from app.datac.__all_models import Orders, OfficeOrders
        legacy_orders = session.query(Orders).filter_by(product_id=str(product_id)).all()
        legacy_office_orders = session.query(OfficeOrders).filter_by(product_id=str(product_id)).all()

        # 4. Формируем детальную информацию о каждом заказе
        orders_history = []
        total_revenue = 0
        total_rental_hours = 0
        unique_clients = set()
        problems_count = 0

        # Обрабатываем унифицированные заказы
        for order in unified_orders:
            order_type = session.query(OrderTypes).filter_by(type_id=order.order_type_id).first()
            order_type_name = order_type.type_name if order_type else 'unknown'

            # Получаем информацию о клиенте
            client_info = None
            if order.client_id and order.client_id != '0':
                try:
                    # Пробуем найти клиента по ID или номеру телефона
                    if order.client_id.isdigit():
                        user = session.query(User).filter_by(id=int(order.client_id)).first()
                    else:
                        user = session.query(User).filter_by(phone_number=order.client_id).first()

                    if user:
                        client_info = {
                            'id': user.id,
                            'name': user.name,
                            'phone_number': user.phone_number,
                            'banned': bool(user.banned)
                        }
                        unique_clients.add(user.phone_number)
                except:
                    pass

            # Получаем фотографии для этого заказа
            photos = session.query(OrderPhotos).filter_by(order_id=order.order_id).all()
            order_photos = []
            for photo in photos:
                order_photos.append({
                    'photo_id': photo.photo_id,
                    'photo_type': photo.photo_type,  # 0 = до аренды, 1 = после возврата
                    'photo_type_text': 'До аренды' if photo.photo_type == 0 else 'После возврата',
                    'file_name': photo.file_name,
                    'file_size': photo.file_size,
                    'content_type': photo.content_type,
                    'created_at': photo.created_at,
                    'photo_url': f'/api/photos/{photo.photo_id}'
                })

            # Определяем статус заказа
            status = 'unknown'
            status_text = 'Неизвестно'
            if order.not_issued == 1:
                status = 'rejected'
                status_text = 'Отклонен'
            elif order.returned == 1:
                status = 'returned'
                status_text = 'Возвращен'
            elif order.issued == 1:
                if order.ready_for_return == 1:
                    status = 'ready_for_return'
                    status_text = 'Готов к возврату'
                else:
                    status = 'active'
                    status_text = 'Активен'
            else:
                status = 'pending'
                status_text = 'Ожидает выдачи'

            # Проверяем наличие проблем
            has_problems = bool(order.comment and 'проблем' in order.comment.lower())
            if has_problems:
                problems_count += 1

            order_info = {
                'order_id': order.order_id,
                'order_type': order_type_name,
                'order_type_text': 'Офисный заказ' if order_type_name == 'office' else 'Заказ через постамат',
                'client_info': client_info,
                'timeline': {
                    'created_at': order.created_at,
                    'start_time': order.start_time,
                    'end_time': order.end_time,
                    'updated_at': order.updated_at
                },
                'rental_details': {
                    'rental_time_minutes': order.rental_time,
                    'rental_time_hours': round(order.rental_time / 60, 2) if order.rental_time else 0,
                    'rental_amount': order.rental_amount,
                    'renewal_time': order.renewal_time
                },
                'status': {
                    'code': status,
                    'text': status_text,
                    'issued': bool(order.issued),
                    'returned': bool(order.returned),
                    'ready_for_return': bool(order.ready_for_return),
                    'accepted': bool(order.accepted),
                    'not_issued': bool(order.not_issued)
                },
                'location': {
                    'address_office': order.address_office
                },
                'comments': order.comment,
                'has_problems': has_problems,
                'photos': order_photos,
                'photos_count': len(order_photos)
            }

            orders_history.append(order_info)

            # Считаем статистику
            if order.rental_amount:
                total_revenue += order.rental_amount
            if order.rental_time:
                total_rental_hours += order.rental_time / 60

        # 5. Обрабатываем legacy заказы для полноты истории
        for legacy_order in legacy_orders + legacy_office_orders:
            # Проверяем, нет ли уже этого заказа в унифицированной таблице
            duplicate = any(o['order_id'] == legacy_order.order_id for o in orders_history)
            if duplicate:
                continue

            # Получаем информацию о клиенте
            client_info = None
            if legacy_order.client_id and legacy_order.client_id != '0':
                try:
                    if legacy_order.client_id.isdigit():
                        user = session.query(User).filter_by(id=int(legacy_order.client_id)).first()
                    else:
                        user = session.query(User).filter_by(phone_number=legacy_order.client_id).first()

                    if user:
                        client_info = {
                            'id': user.id,
                            'name': user.name,
                            'phone_number': user.phone_number,
                            'banned': bool(user.banned)
                        }
                        unique_clients.add(user.phone_number)
                except:
                    pass

            # Определяем тип заказа
            order_type_name = 'office' if isinstance(legacy_order, OfficeOrders) else 'postamat'

            # Определяем статус
            status = 'returned' if legacy_order.returned else 'unknown'
            status_text = 'Возвращен' if legacy_order.returned else 'Legacy заказ'

            legacy_order_info = {
                'order_id': legacy_order.order_id,
                'order_type': order_type_name,
                'order_type_text': 'Офисный заказ (legacy)' if order_type_name == 'office' else 'Заказ через постамат (legacy)',
                'client_info': client_info,
                'timeline': {
                    'created_at': None,
                    'start_time': legacy_order.start_time,
                    'end_time': legacy_order.end_time,
                    'updated_at': None
                },
                'rental_details': {
                    'rental_time_minutes': legacy_order.rental_time,
                    'rental_time_hours': round(legacy_order.rental_time / 60, 2) if legacy_order.rental_time else 0,
                    'rental_amount': legacy_order.rental_amount,
                    'renewal_time': legacy_order.renewal_time
                },
                'status': {
                    'code': status,
                    'text': status_text,
                    'issued': getattr(legacy_order, 'issued', None),
                    'returned': bool(legacy_order.returned),
                    'ready_for_return': getattr(legacy_order, 'ready_for_return', None),
                    'accepted': getattr(legacy_order, 'accepted', None),
                    'not_issued': getattr(legacy_order, 'not_issued', None)
                },
                'location': {
                    'address_office': getattr(legacy_order, 'address_office', None)
                },
                'comments': None,
                'has_problems': False,
                'photos': [],
                'photos_count': 0,
                'is_legacy': True
            }

            orders_history.append(legacy_order_info)

            # Считаем статистику
            if legacy_order.rental_amount:
                total_revenue += legacy_order.rental_amount
            if legacy_order.rental_time:
                total_rental_hours += legacy_order.rental_time / 60

        # 6. Сортируем заказы по дате создания (новые первыми)
        orders_history.sort(key=lambda x: x['timeline']['created_at'] or '1900-01-01', reverse=True)

        # 7. Вычисляем детальную статистику
        total_orders = len(orders_history)
        active_orders = len([o for o in orders_history if o['status']['code'] == 'active'])
        returned_orders = len([o for o in orders_history if o['status']['code'] == 'returned'])
        rejected_orders = len([o for o in orders_history if o['status']['code'] == 'rejected'])

        # Статистика по типам заказов
        office_orders = len([o for o in orders_history if o['order_type'] == 'office'])
        postamat_orders = len([o for o in orders_history if o['order_type'] == 'postamat'])

        # Статистика по фотографиям
        total_photos = sum(o['photos_count'] for o in orders_history)
        orders_with_photos = len([o for o in orders_history if o['photos_count'] > 0])

        # Последний заказ
        last_order = orders_history[0] if orders_history else None

        # 8. Формируем итоговый ответ
        result = {
            'product_info': {
                'id': product.id,
                'name': product.name,
                'description': product.description,
                'category': product.category,
                'size': product.size,
                'product_uuid': product.product_uuid,
                'pricing': {
                    'price_per_hour': product.price_per_hour,
                    'price_per_day': product.price_per_day,
                    'price_per_month': product.price_per_month
                },
                'location': {
                    'office_info': office_info,
                    'address_of_postamat': product.address_of_postamat
                },
                'status': {
                    'have_problem': bool(product.have_problem),
                    'who_is_reserved': product.who_is_reserved,
                    'start_of_reservation': product.start_of_reservation
                },
                'image_url': f'/get-product-image/{product.id}'
            },
            'orders_history': orders_history,
            'statistics': {
                'total_orders': total_orders,
                'active_orders': active_orders,
                'returned_orders': returned_orders,
                'rejected_orders': rejected_orders,
                'orders_by_type': {
                    'office_orders': office_orders,
                    'postamat_orders': postamat_orders
                },
                'financial': {
                    'total_revenue': total_revenue,
                    'average_order_value': round(total_revenue / total_orders, 2) if total_orders > 0 else 0,
                    'revenue_per_hour': round(total_revenue / total_rental_hours, 2) if total_rental_hours > 0 else 0
                },
                'usage': {
                    'total_rental_hours': round(total_rental_hours, 2),
                    'average_rental_time': round(total_rental_hours / total_orders, 2) if total_orders > 0 else 0,
                    'unique_clients': len(unique_clients)
                },
                'photos': {
                    'total_photos': total_photos,
                    'orders_with_photos': orders_with_photos,
                    'photo_coverage': round(orders_with_photos / total_orders * 100, 1) if total_orders > 0 else 0
                },
                'problems': {
                    'problems_count': problems_count,
                    'product_has_problems': bool(product.have_problem),
                    'problem_rate': round(problems_count / total_orders * 100, 1) if total_orders > 0 else 0
                }
            },
            'timeline': {
                'first_order': orders_history[-1]['timeline'] if orders_history else None,
                'last_order': last_order['timeline'] if last_order else None,
                'total_usage_days': None  # Можем добавить расчет общего периода использования
            },
            'meta': {
                'generated_at': datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
                'total_records': total_orders,
                'data_sources': ['unified_orders', 'legacy_orders', 'order_photos', 'users']
            }
        }

        return JSONResponse(result)

    except Exception as e:
        print(f"Error getting product history: {e}")
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()
