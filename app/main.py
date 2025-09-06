import os
import logging

from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.responses import RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.middleware.trustedhost import TrustedHostMiddleware
from starlette.middleware.base import BaseHTTPMiddleware

from app.datac.db_session import init_mysql

from app.web.login import router as login_router
from app.web.office_worker import router as office_worker_router
from app.web.postamat_worker import router as postamat_worker_router
from app.web.admin_junior import router as admin_junior_router
from app.web.admin_senior import router as admin_senior_router
from app.web.owner import router as owner_router

from app.api.office_api import router as office_api_router
from app.api.order_api import router as order_api_router
from app.api.owner_api import router as owner_api_router
from app.api.products_api import router as products_api_router
from app.api.returns_api import router as returns_api_router
from app.api.troubleshoot_api import router as troubleshoot_api_router
from app.api.user_api import router as user_api_router
from app.api.worker_api import router as worker_api_router
from app.api.photos_api import router as photos_api_router
from app.api.passport_api import router as passport_api_router
from app.api.fines_api import router as fines_api_router
from app.api.payment_api import router as payment_api_router
from app.api.telegram_api import router as telegram_api_router
from app.api.test_orders_api import router as test_orders_api_router

# Mobile API routers
from app.mobile_api.user_auth import router as mobile_auth_router
from app.mobile_api.postamats import router as mobile_postamats_router
from app.mobile_api.products import router as mobile_products_router
from app.mobile_api.profile import router as mobile_profile_router
from app.mobile_api.rental import router as mobile_rental_router
from app.mobile_api.payments import router as mobile_payments_router
from app.mobile_api.telegram import router as mobile_telegram_router
from app.mobile_api.passport import router as mobile_passport_router

from app.services.background_tasks import start_background_tasks

# Настройка логирования
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s'
)
logger = logging.getLogger(__name__)


class HTTPSMiddleware(BaseHTTPMiddleware):
    """Middleware для принудительного использования HTTPS на production"""
    async def dispatch(self, request: Request, call_next):
        # Проверяем заголовки от nginx прокси ПЕРВЫМ делом
        if request.headers.get('x-forwarded-proto') == 'https':
            request.scope['scheme'] = 'https'
        if request.headers.get('x-forwarded-host'):
            request.scope['server'] = (request.headers['x-forwarded-host'], 443)

        # Для production домена ПРИНУДИТЕЛЬНО устанавливаем HTTPS схему
        host = request.headers.get('host', '')
        if host in ['akkubatt-work.ru', 'www.akkubatt-work.ru']:
            # Это ключевая строка - принудительно HTTPS для генерации правильных URL
            request.scope['scheme'] = 'https'
            request.scope['server'] = (host, 443)
            # Также устанавливаем заголовки для совместимости
            request.scope['headers'] = list(request.scope.get('headers', []))
            request.scope['headers'].append((b'x-forwarded-proto', b'https'))

        response = await call_next(request)
        return response


class HTTPSRedirectMiddleware(BaseHTTPMiddleware):
    """Middleware для редиректа HTTP на HTTPS в production"""
    async def dispatch(self, request: Request, call_next):
        # Редиректим HTTP на HTTPS для production домена
        if (request.headers.get('x-forwarded-proto') == 'http' and
            request.headers.get('host') in ['akkubatt-work.ru', 'www.akkubatt-work.ru']):

            https_url = f"https://{request.headers.get('host')}{request.url.path}"
            if request.url.query:
                https_url += f"?{request.url.query}"

            logger.info(f"Redirecting HTTP to HTTPS: {request.url} -> {https_url}")
            return RedirectResponse(url=https_url, status_code=301)

        response = await call_next(request)
        return response


@asynccontextmanager
async def lifespan(app):
    # Инициализация MySQL базы данных
    try:
        init_mysql()
        print("✅ База данных MySQL успешно инициализирована")
    except Exception as e:
        print(f"❌ Ошибка инициализации базы данных: {e}")
        raise

    # Запуск фоновых задач
    background_task = start_background_tasks()
    print("✅ Фоновые задачи запущены")

    yield

    # Остановка фоновых задач при завершении приложения
    background_task.cancel()
    try:
        await background_task
    except Exception:
        pass


app = FastAPI(title='Client Site', description='Базовое FastAPI приложение для клиентского сайта', lifespan=lifespan)

# Подключаем статические файлы
static_dir = os.path.join(os.path.dirname(__file__), '..', 'static')
app.mount('/static', StaticFiles(directory=static_dir), name='static')

# Добавляем middleware в правильном порядке
app.add_middleware(HTTPSMiddleware)
app.add_middleware(HTTPSRedirectMiddleware)
app.add_middleware(TrustedHostMiddleware,
                   allowed_hosts=['akkubatt-work.ru', 'www.akkubatt-work.ru', 'localhost', '127.0.0.1'])

# Include Web routers
app.include_router(login_router)
app.include_router(office_worker_router)
app.include_router(postamat_worker_router)
app.include_router(admin_junior_router)
app.include_router(admin_senior_router)
app.include_router(owner_router)

# Include API routers
app.include_router(office_api_router)
app.include_router(order_api_router)
app.include_router(owner_api_router)
app.include_router(products_api_router)
app.include_router(returns_api_router)
app.include_router(troubleshoot_api_router)
app.include_router(user_api_router)
app.include_router(worker_api_router)
app.include_router(photos_api_router)
app.include_router(passport_api_router)
app.include_router(fines_api_router)
app.include_router(payment_api_router)
app.include_router(telegram_api_router)
app.include_router(test_orders_api_router)

# Include Mobile API routers
app.include_router(mobile_auth_router)
app.include_router(mobile_postamats_router)
app.include_router(mobile_products_router)
app.include_router(mobile_profile_router)
app.include_router(mobile_rental_router)
app.include_router(mobile_payments_router)
app.include_router(mobile_telegram_router)
app.include_router(mobile_passport_router)


@app.get('/')
async def root():
    return RedirectResponse(url='/login')


if __name__ == '__main__':
    import uvicorn
    uvicorn.run('main:app', host='0.0.0.0', port=4000, reload=True)
