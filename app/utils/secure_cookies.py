from fastapi import HTTPException
from itsdangerous import Signer


def set_signed_cookie(signer: Signer, resp, key, value):
    signed_value = signer.sign(value).decode()
    resp.set_cookie(
        key=key,
        value=signed_value,
        httponly=True,
        secure=False,
        samesite='Lax'
    )
    return resp


def get_verified_cookie(signer: Signer, request, key):
    signed_value = request.cookies.get(key)
    try:
        user_role = signer.unsign(signed_value).decode()
        if user_role is None:
            raise HTTPException(status_code=401, detail='Требуется авторизация')
        return user_role
    except Exception:
        raise HTTPException(status_code=403, detail='Please, login again')

