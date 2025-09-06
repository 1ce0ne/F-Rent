import sqlalchemy as sa
from sqlalchemy.orm import sessionmaker
import os
import dotenv

dotenv.load_dotenv()

# MySQL подключение
DB_USER = os.environ.get('DB_USER')
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_HOST = os.environ.get('DB_HOST')
DB_PORT = int(os.environ.get('DB_PORT', 3306))
DB_NAME = os.environ.get('DB_NAME')

mysql_engine = None
mysql_session_factory = None


def init_mysql():
    """
    Инициализация MySQL подключения.
    """
    global mysql_engine, mysql_session_factory

    if mysql_engine:
        return mysql_engine

    connection_string = f'mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}'
    mysql_engine = sa.create_engine(connection_string, echo=False)
    mysql_session_factory = sessionmaker(bind=mysql_engine)

    return mysql_engine


def create_session():
    """
    Создание новой сессии для работы с базой данных.
    """
    global mysql_session_factory

    if not mysql_session_factory:
        init_mysql()

    return mysql_session_factory()


def get_engine():
    """
    Получение движка базы данных.
    """
    global mysql_engine

    if not mysql_engine:
        init_mysql()

    return mysql_engine
