from functools import wraps
from fastapi import HTTPException, Depends, Request
from fastapi.responses import RedirectResponse
from app.utils.secure_cookies import get_verified_cookie
from app.utils.security import signer

# Маппинг старых ролей на новые для обратной совместимости
ROLE_COMPATIBILITY_MAP = {
    'worker_postamat': 'postamat_worker',
    'worker_office': 'office_worker',
    'admin_junior': 'junior_admin',
    'admin_senior': 'senior_admin',
    'owner': 'owner'
}

def access_required(roles=None):
    """
    Декоратор для проверки авторизации и ролей
    roles: None — только авторизация,
           str — одна роль,
           set/list/tuple — несколько ролей
    """

    def decorator(func):
        @wraps(func)
        async def wrapper(*args, **kwargs):
            request = kwargs.get('request') or (args[0] if args else None)
            user_role = get_verified_cookie(signer, request, 'user_role') if request else None

            if not user_role:
                return RedirectResponse(url='/login')

            # Нормализуем роль через маппинг совместимости
            normalized_role = ROLE_COMPATIBILITY_MAP.get(user_role, user_role)

            # Если роли не указаны, то достаточно только авторизации
            if roles is None:
                return await func(*args, **kwargs)

            # Проверяем роли
            allowed_roles = set()
            if isinstance(roles, str):
                allowed_roles.add(roles)
            elif isinstance(roles, (list, tuple, set)):
                allowed_roles.update(roles)

            # Нормализуем разрешенные роли
            normalized_allowed_roles = set()
            for role in allowed_roles:
                normalized_allowed_roles.add(ROLE_COMPATIBILITY_MAP.get(role, role))

            if normalized_role not in normalized_allowed_roles:
                return RedirectResponse(url='/login')

            return await func(*args, **kwargs)

        return wrapper
    return decorator


def get_current_user(request: Request):
    """
    Функция для получения текущего пользователя для мобильного API
    """
    user_id = get_verified_cookie(signer, request, 'user_id')
    user_role = get_verified_cookie(signer, request, 'user_role')

    if not user_id or not user_role:
        raise HTTPException(status_code=401, detail="Пользователь не авторизован")

    return {
        'id': user_id,
        'role': user_role
    }
