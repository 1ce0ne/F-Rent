from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import User, BannedUsers, ReasonsForBan, get_password_hash

router = APIRouter()


@router.post('/api/add-user')
@access_required({'senior_admin', 'owner'})
async def api_add_user(request: Request):
    data = await request.json()
    name = data.get('name')
    password = data.get('password')
    phone_number = data.get('phone_number')
    if not name or not password or not phone_number:
        return JSONResponse({'error': 'Не указаны обязательные поля'}, status_code=400)
    session = create_session()
    try:
        if session.query(User).filter_by(phone_number=phone_number).first():
            return JSONResponse({'error': 'Пользователь с таким номером уже существует'}, status_code=409)
        hashed_password = get_password_hash(password)
        new_user = User(
            name=name,
            password=hashed_password,
            phone_number=phone_number
        )
        session.add(new_user)
        session.commit()
        return JSONResponse({'success': True, 'user_id': new_user.id})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.put('/api/update-user/{user_id}')
@access_required({'senior_admin', 'owner'})
async def api_update_user_put(user_id: int, request: Request):
    data = await request.json()
    session = create_session()
    try:
        user_to_update = session.query(User).filter_by(id=user_id).first()
        if not user_to_update:
            return JSONResponse({'error': 'User not found'}, status_code=404)
        if 'name' in data:
            user_to_update.name = data['name']
        if 'phone_number' in data:
            user_to_update.phone_number = data['phone_number']
        if 'password' in data and data['password']:
            user_to_update.password = get_password_hash(data['password'])
        session.commit()
        return JSONResponse({'success': True, 'message': 'User updated successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-user-details/{user_id}')
@access_required({'senior_admin', 'owner'})
async def api_get_user_details(user_id: int, request: Request):
    session = create_session()
    try:
        user = session.query(User).filter_by(id=user_id).first()
        if not user:
            return JSONResponse({'error': 'User not found'}, status_code=404)
        # --- Добавлено: ищем бан ---
        ban_info = None
        if user.banned:
            ban = session.query(BannedUsers).filter_by(banned_user_number=user.phone_number).order_by(
                BannedUsers.ban_id.desc()).first()
            if ban:
                # Можно добавить сюда причину, дату окончания и т.д.
                reason = session.query(ReasonsForBan).filter_by(reason_id=ban.reason_id).first()
                reason_text = reason.reason_ban_name if reason else ''
                period = reason.ban_period if reason else ''
                ban_info = f"{reason_text} ({period}) до {ban.ban_end_time if ban.ban_end_time else 'навсегда'}"
            else:
                ban_info = 'Забанен'
        return JSONResponse({
            'id': user.id,
            'name': user.name,
            'phone_number': user.phone_number,
            'balance': "0 ₽",
            'role': 'Клиент',
            'ban_info': ban_info or '-'
        })
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.delete('/api/delete-user/{user_id}')
@access_required({'senior_admin', 'owner'})
async def api_delete_user(user_id: int, request: Request):
    session = create_session()
    try:
        user_to_delete = session.query(User).filter_by(id=user_id).first()
        if not user_to_delete:
            return JSONResponse({'error': 'User not found'}, status_code=404)
        session.delete(user_to_delete)
        session.commit()
        return JSONResponse({'success': True, 'message': 'User deleted successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/update-user/{user_id}')
@access_required({'senior_admin', 'owner'})
async def api_update_user(user_id: int, request: Request):
    data = await request.json()
    session = create_session()
    try:
        user_to_update = session.query(User).filter_by(id=user_id).first()
        if not user_to_update:
            return JSONResponse({'error': 'User not found'}, status_code=404)
        if 'name' in data:
            user_to_update.name = data['name']
        if 'phone_number' in data:
            user_to_update.phone_number = data['phone_number']
        session.commit()
        return JSONResponse({'success': True, 'message': 'User updated successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-ban-reasons')
@access_required({'senior_admin', 'owner'})
async def api_get_ban_reasons(request: Request):
    session = create_session()
    try:
        reasons = session.query(ReasonsForBan).all()
        result = [{
            'reason_id': r.reason_id,
            'name': r.reason_ban_name,
            'description': r.reason_ban_description,
            'ban_period': r.ban_period,  # Добавлено поле периода бана
            'ban_reason': r.reason_ban_name  # Явно добавляем текст причины
        } for r in reasons]
        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/ban-user/{user_id}')
@access_required({'senior_admin', 'owner'})
async def api_ban_user(user_id: int, request: Request):
    data = await request.json()
    reason_id = data.get('reason_id')
    ban_end_time = data.get('ban_end_time')
    session = create_session()
    try:
        user = session.query(User).filter_by(id=user_id).first()
        if not user:
            return JSONResponse({'error': 'User not found'}, status_code=404)
        banned_user = BannedUsers(
            reason_id=reason_id,
            banned_user_number=user.phone_number,
            ban_end_time=ban_end_time
        )
        session.add(banned_user)
        user.banned = 1
        session.commit()
        return JSONResponse({'success': True, 'message': 'User banned successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-users')
@access_required({'senior_admin', 'owner'})
async def api_get_users(request: Request):
    session = create_session()
    try:
        users = session.query(User).all()
        result = [{
            'id': u.id,
            'name': u.name,
            'phone_number': u.phone_number,
            'balance': "0 ₽",
            'role': 'Клиент'
        } for u in users]
        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/unban-user/{user_id}')
@access_required({'senior_admin', 'owner'})
async def api_unban_user(user_id: int, request: Request):
    session = create_session()
    try:
        user = session.query(User).filter_by(id=user_id).first()
        if not user:
            return JSONResponse({'error': 'User not found'}, status_code=404)
        # Снимаем флаг бана
        user.banned = 0
        # Удаляем все записи о бане этого пользователя
        session.query(BannedUsers).filter_by(banned_user_number=user.phone_number).delete()
        session.commit()
        return JSONResponse({'success': True, 'message': 'User unbanned successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()
