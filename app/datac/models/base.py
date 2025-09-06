import bcrypt
from sqlalchemy import create_engine
from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import sessionmaker
from ..db_session import get_engine

# Базовый класс для всех моделей
Base = declarative_base()

# Получаем engine через функцию из db_session
def get_db_engine():
    return get_engine()

# Создаем session factory
def get_session_factory():
    engine = get_db_engine()
    return sessionmaker(bind=engine)

# Для обратной совместимости
engine = None
session = None

def get_password_hash(password: str) -> str:
    """Хеширование пароля с помощью bcrypt"""
    salt = bcrypt.gensalt()
    return bcrypt.hashpw(password.encode('utf-8'), salt).decode('utf-8')
