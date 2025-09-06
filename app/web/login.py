from fastapi import APIRouter, Request, Response, Form, status
from fastapi.responses import RedirectResponse
import secrets
import time
import os
import re

from app.utils.templates import templates
from app.utils.secure_cookies import set_signed_cookie
from app.utils.security import signer
from app.datac.db_session import create_session
from app.datac.__all_models import Workers, WorkerRoles

router = APIRouter()

# Временное хранилище для сессий двухфакторной аутентификации
pending_2fa_sessions = {}

# Маппинг ролей на URL для редиректа после входа
ROLE_REDIRECT_MAP = {
    'postamat_worker': '/worker-postamat',
    'office_worker': '/worker/products',
    'junior_admin': '/admin-junior/offices',
    'senior_admin': '/admin-senior/postamats',
    'owner': '/owner/statistics'
}

def normalize_phone(phone):
    """Нормализация номера телефона для сравнения"""
    if not phone:
        return ""
    # Убираем все нецифровые символы
    digits_only = re.sub(r'\D', '', phone)
    # Если номер начинается с 8, заменяем на 7
    if digits_only.startswith('8') and len(digits_only) == 11:
        digits_only = '7' + digits_only[1:]
    # Если номер начинается с 7 и имеет 11 цифр - это правильный формат
    if digits_only.startswith('7') and len(digits_only) == 11:
        return digits_only
    # Если 10 цифр, добавляем 7 в начало
    if len(digits_only) == 10:
        return '7' + digits_only
    return digits_only

@router.api_route('/login', methods=['GET', 'POST'])
async def login(request: Request, response: Response, username: str = Form(None), password: str = Form(None)):
    if request.method == 'GET':
        return templates.TemplateResponse('login/login.html', {'request': request})

    if not username or not password:
        return templates.TemplateResponse('login/login.html', {
            'request': request,
            'error': 'Введите логин и пароль'
        })

    session = create_session()
    try:
        # Проверяем аутентификацию
        worker_query = session.query(Workers, WorkerRoles).join(
            WorkerRoles, Workers.role_id == WorkerRoles.role_id
        ).filter(
            Workers.worker_uid == username,
            Workers.worker_passwd == password,
            Workers.is_active == 1
        ).first()

        if worker_query:
            worker, role = worker_query

            # Проверяем, включена ли двухфакторная аутентификация и есть ли Telegram username
            if worker.telegram_2fa_enabled and worker.telegram_username:
                # Создаем временную сессию для 2FA
                session_token = secrets.token_urlsafe(32)
                pending_2fa_sessions[session_token] = {
                    'worker_id': worker.worker_id,
                    'role_name': role.role_name,
                    'username': worker.worker_uid,
                    'required_telegram_username': worker.telegram_username,
                    'timestamp': time.time()
                }

                # Перенаправляем на страницу подтверждения через Telegram
                return templates.TemplateResponse('login/telegram_confirm.html', {
                    'request': request,
                    'session_token': session_token,
                    'username': worker.worker_uid,
                    'telegram_username': worker.telegram_username,
                    'telegram_bot_username': os.getenv('BOT_USERNAME', '')
                })
            else:
                # Двухфакторная аутентификация отключена - обычный вход
                redirect_url = ROLE_REDIRECT_MAP.get(role.role_name, '/worker/products')
                resp = RedirectResponse(url=redirect_url, status_code=status.HTTP_302_FOUND)

                resp = set_signed_cookie(signer, resp, 'user_role', role.role_name)
                resp = set_signed_cookie(signer, resp, 'worker_id', str(worker.worker_id))
                resp = set_signed_cookie(signer, resp, 'worker_username', worker.worker_uid)

                return resp

        return templates.TemplateResponse('login/login.html',
                                          {'request': request, 'error': 'Неверный логин/пароль'})

    except Exception as e:
        print(f"Login error: {e}")
        return templates.TemplateResponse('login/login.html', {'request': request, 'error': 'Ошибка системы'})
    finally:
        session.close()


@router.post('/telegram-auth-confirm')
async def telegram_auth_confirm(request: Request):
    """Подтверждение двухфакторной аутентификации через Telegram по номеру телефона"""
    data = await request.json()
    session_token = data.get('session_token')
    telegram_data = data.get('telegram_data')

    if not session_token or session_token not in pending_2fa_sessions:
        return {'success': False, 'error': 'Недействительная сессия'}

    session_data = pending_2fa_sessions[session_token]

    # Проверяем, не истекла ли сессия (5 минут)
    if time.time() - session_data['timestamp'] > 300:
        del pending_2fa_sessions[session_token]
        return {'success': False, 'error': 'Сессия истекла'}

    # Проверяем номер телефона из Telegram
    if not telegram_data or 'username' not in telegram_data:
        return {'success': False, 'error': 'Telegram не предоставил данные'}

    telegram_username = telegram_data['username']
    required_telegram_username = session_data['required_telegram_username']

    if telegram_username != required_telegram_username:
        return {'success': False, 'error': f'Имя пользователя в Telegram не совпадает. Ожидается: {required_telegram_username}, получено: {telegram_username}'}

    # Номера совпадают - аутентификация прошла успешно
    del pending_2fa_sessions[session_token]

    return {
        'success': True,
        'worker_data': {
            'worker_id': session_data['worker_id'],
            'role_name': session_data['role_name'],
            'username': session_data['username']
        }
    }


@router.post('/complete-telegram-login')
async def complete_telegram_login(request: Request):
    """Завершение авторизации через Telegram с установкой cookies и редиректом"""
    data = await request.json()
    worker_data = data.get('worker_data')

    if not worker_data:
        return {'success': False, 'error': 'Недостаточно данных для завершения входа'}

    worker_id = worker_data.get('worker_id')
    role_name = worker_data.get('role_name')
    username = worker_data.get('username')

    if not all([worker_id, role_name, username]):
        return {'success': False, 'error': 'Неполные данные пользователя'}

    # Определяем URL для редиректа
    redirect_url = ROLE_REDIRECT_MAP.get(role_name, '/worker/products')

    # Создаем редирект с установкой cookies
    resp = RedirectResponse(url=redirect_url, status_code=status.HTTP_302_FOUND)
    resp = set_signed_cookie(signer, resp, 'user_role', role_name)
    resp = set_signed_cookie(signer, resp, 'worker_id', str(worker_id))
    resp = set_signed_cookie(signer, resp, 'worker_username', username)

    return resp


@router.get('/set-telegram-cookies')
async def set_telegram_cookies(request: Request, worker_id: str, role_name: str, username: str):
    """GET endpoint для установки cookies после Telegram авторизации"""
    # Определяем URL для редиректа
    redirect_url = ROLE_REDIRECT_MAP.get(role_name, '/worker/products')

    # Создаем редирект с установкой cookies
    resp = RedirectResponse(url=redirect_url, status_code=status.HTTP_302_FOUND)
    resp = set_signed_cookie(signer, resp, 'user_role', role_name)
    resp = set_signed_cookie(signer, resp, 'worker_id', str(worker_id))
    resp = set_signed_cookie(signer, resp, 'worker_username', username)

    return resp


@router.get('/logout')
async def logout():
    response = RedirectResponse(url='/login')
    response.delete_cookie('user_role')
    response.delete_cookie('worker_id')
    response.delete_cookie('worker_username')
    return response
