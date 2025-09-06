import os

import bcrypt
import dotenv

from sqlalchemy import *
from sqlalchemy import create_engine, Column, LargeBinary
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column
from sqlalchemy.orm import sessionmaker

dotenv.load_dotenv()

DB_USER = os.environ.get('DB_USER')
DB_PASSWORD = os.environ.get('DB_PASSWORD')
DB_HOST = os.environ.get('DB_HOST')
DB_PORT = int(os.environ.get('DB_PORT'))
DB_NAME = os.environ.get('DB_NAME')

engine = create_engine(f'mysql+pymysql://{DB_USER}:{DB_PASSWORD}@{DB_HOST}:{DB_PORT}/{DB_NAME}')


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = 'users'

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(25))
    password: Mapped[str] = mapped_column(String(75))
    phone_number: Mapped[str] = mapped_column(String(11))
    # cards: Mapped[str] = mapped_column(Text, nullable=True)
    image = Column(LargeBinary(length=16777215), nullable=True)
    banned: Mapped[int] = mapped_column(default=0)
    have_a_passport: Mapped[str] = mapped_column(String(256), nullable=True, default=None)

    def verify_password(self, password: str):
        return bcrypt.checkpw(password.encode('utf-8'), self.password.encode('utf-8'))


def get_password_hash(password: str):
    return bcrypt.hashpw(password.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')


class Card(Base):
    __tablename__ = 'cards'

    id_card: Mapped[int] = mapped_column(primary_key=True)

    card: Mapped[str] = mapped_column(String(256))
    ccv: Mapped[str] = mapped_column(String(256))
    date: Mapped[str] = mapped_column(String(256))
    card_user: Mapped[str] = mapped_column(String(256))

    user_id: Mapped[int] = mapped_column()


class EncryptionKey(Base):
    __tablename__ = 'server_keys'

    id: Mapped[int] = mapped_column(primary_key=True)
    encryption_key: Mapped[str] = mapped_column(String(256), nullable=True)
    user_id: Mapped[str] = mapped_column(String(25))


class Passport(Base):
    __tablename__ = 'passports'
    user_passport_id: Mapped[int] = mapped_column(primary_key=True)
    user_passport_phone_number: Mapped[str] = mapped_column(String(256))
    user_passport_name: Mapped[str] = mapped_column(String(256))
    user_passport_birthday: Mapped[str] = mapped_column(String(256))
    user_passport_serial_number: Mapped[str] = mapped_column(String(256))
    user_passport_date_of_issue: Mapped[str] = mapped_column(String(256))
    user_passport_code: Mapped[str] = mapped_column(String(256))
    user_passport_issued: Mapped[str] = mapped_column(String(256))


class PassportEncryptionKey(Base):
    __tablename__ = 'server_passport_keys'

    id: Mapped[int] = mapped_column(primary_key=True)
    encryption_key: Mapped[str] = mapped_column(String(256), nullable=True)
    user_number: Mapped[str] = mapped_column(String(25))


class Orders(Base):
    __tablename__ = 'orders'

    order_id: Mapped[int] = mapped_column(primary_key=True)
    product_id: Mapped[str] = mapped_column(String(250))
    client_id: Mapped[str] = mapped_column(String(10000))

    start_time: Mapped[str] = Column(String(250), nullable=False)
    end_time: Mapped[str] = Column(String(250), nullable=False)

    rental_time: Mapped[int] = mapped_column(nullable=False)
    renewal_time: Mapped[str] = Column(String(250), nullable=True)

    rental_amount: Mapped[int] = mapped_column(nullable=False)

    returned: Mapped[int] = mapped_column(default=0)


class OfficeOrders(Base):
    __tablename__ = 'office_orders'

    order_id: Mapped[int] = mapped_column(primary_key=True)
    product_id: Mapped[str] = mapped_column(String(250))
    client_id: Mapped[str] = mapped_column(String(10000))

    start_time: Mapped[str] = Column(String(250), nullable=False)
    end_time: Mapped[str] = Column(String(250), nullable=False)

    rental_time: Mapped[int] = mapped_column(nullable=False)
    renewal_time: Mapped[str] = Column(String(250), nullable=True)

    rental_amount: Mapped[int] = mapped_column(nullable=False)

    returned: Mapped[int] = mapped_column(default=0)
    ready_for_return: Mapped[int] = mapped_column(default=0)

    issued: Mapped[int] = mapped_column(default=0)
    not_issued: Mapped[int] = mapped_column(default=0)
    accepted: Mapped[int] = mapped_column(default=0)

    address_office: Mapped[str] = Column(String(250), nullable=False)


class Product(Base):
    __tablename__ = 'products'

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(25))
    description: Mapped[str] = mapped_column(String(512), nullable=True)

    price_per_hour: Mapped[float] = mapped_column()
    price_per_day: Mapped[float] = mapped_column()
    price_per_month: Mapped[float] = mapped_column()

    image = Column(LargeBinary(length=16777215), nullable=True)
    product_uuid: Mapped[str] = mapped_column(String(36))
    size: Mapped[str] = mapped_column(String(36))
    category: Mapped[str] = mapped_column(String(128), nullable=True)

    address_of_postamat: Mapped[str] = mapped_column(String(255), nullable=True)

    who_is_reserved: Mapped[str] = mapped_column(String(11), nullable=True)
    start_of_reservation: Mapped[str] = mapped_column(String(250), nullable=True)


class ParcelAutomat(Base):
    __tablename__ = 'parcel_automats'

    id: Mapped[int] = mapped_column(primary_key=True)
    address: Mapped[str] = mapped_column(String(65))
    first_coordinate: Mapped[str] = mapped_column(String(65))
    second_coordinate: Mapped[str] = mapped_column(String(65))
    qr_code_id: Mapped[str] = mapped_column(String(36))


class Cell(Base):
    __tablename__ = 'cells'
    cell_id = Column(Integer, primary_key=True)
    size = Column(String(20), nullable=False)
    product_id = Column(Integer, nullable=True)
    parcel_automat_id = Column(Integer, ForeignKey('parcel_automats.id'))


class Office(Base):
    __tablename__ = 'office'
    id: Mapped[int] = mapped_column(primary_key=True)
    address: Mapped[str] = mapped_column(String(65))
    first_coordinate: Mapped[str] = mapped_column(String(65))
    second_coordinate: Mapped[str] = mapped_column(String(65))


class OfficeCells(Base):
    __tablename__ = 'office_cells'
    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    office_id = Column(Integer, nullable=True)
    office_cell_id = Column(Integer, nullable=True)
    office_product_id = Column(Integer, nullable=True)


class BannedUsers(Base):
    __tablename__ = 'banned_users'
    ban_id: Mapped[int] = mapped_column(primary_key=True)
    reason_id: Mapped[int] = mapped_column(nullable=False)
    banned_user_number: Mapped[str] = mapped_column(String(50))
    ban_end_time: Mapped[str] = mapped_column(String(50))


class ReasonsForBan(Base):
    __tablename__ = 'reasons_for_ban'
    reason_id: Mapped[int] = mapped_column(primary_key=True)
    reason_ban_name: Mapped[str] = mapped_column(String(50))
    reason_ban_description: Mapped[str] = mapped_column(String(250), nullable=True)
    ban_period: Mapped[str] = Column(String(250), nullable=True)


class TechnicalWorksUpdate(Base):
    __tablename__ = 'technical_works_and_update'
    work_id: Mapped[int] = mapped_column(primary_key=True)
    type_of_work: Mapped[str] = mapped_column(String(50))
    term_of_work: Mapped[str] = mapped_column(String(100), nullable=True)
    version_of_update: Mapped[str] = mapped_column(String(50), nullable=True)
    link_on_update: Mapped[str] = mapped_column(String(150), nullable=True)
    visibility_enabled: Mapped[int] = mapped_column(default=0)


class WorkerRoles(Base):
    __tablename__ = 'worker_roles'
    role_id: Mapped[int] = mapped_column(primary_key=True)
    role_name: Mapped[str] = mapped_column(String(50), unique=True)  # postamat_worker, office_worker, junior_admin, senior_admin, owner
    role_description: Mapped[str] = mapped_column(String(255), nullable=True)


class Workers(Base):
    __tablename__ = 'workers'
    worker_id: Mapped[int] = mapped_column(primary_key=True)
    worker_uid: Mapped[str] = mapped_column(String(50), unique=True)
    worker_passwd: Mapped[str] = mapped_column(String(50))
    role_id: Mapped[int] = mapped_column(ForeignKey('worker_roles.role_id'))
    created_at: Mapped[str] = mapped_column(String(50), nullable=True)
    is_active: Mapped[int] = mapped_column(default=1)


class OrderTypes(Base):
    __tablename__ = 'order_types'
    type_id: Mapped[int] = mapped_column(primary_key=True)
    type_name: Mapped[str] = mapped_column(String(50), unique=True)  # postamat, office
    type_description: Mapped[str] = mapped_column(String(255), nullable=True)
    created_at: Mapped[str] = mapped_column(String(50), nullable=True)


class UnifiedOrders(Base):
    __tablename__ = 'unified_orders'

    order_id: Mapped[int] = mapped_column(primary_key=True)
    product_id: Mapped[str] = mapped_column(String(250))
    client_id: Mapped[str] = mapped_column(String(10000))
    order_type_id: Mapped[int] = mapped_column(ForeignKey('order_types.type_id'))

    # Общие поля для всех типов заказов
    start_time: Mapped[str] = Column(String(250), nullable=False)
    end_time: Mapped[str] = Column(String(250), nullable=False)
    rental_time: Mapped[int] = mapped_column(nullable=False)
    renewal_time: Mapped[str] = Column(String(250), nullable=True)
    rental_amount: Mapped[int] = mapped_column(nullable=False)
    returned: Mapped[int] = mapped_column(default=0)

    # Поля специфичные для офисных заказов (nullable для постаматов)
    ready_for_return: Mapped[int] = mapped_column(default=0, nullable=True)
    issued: Mapped[int] = mapped_column(default=0, nullable=True)
    not_issued: Mapped[int] = mapped_column(default=0, nullable=True)
    accepted: Mapped[int] = mapped_column(default=0, nullable=True)
    address_office: Mapped[str] = Column(String(250), nullable=True)

    # Метаданные
    created_at: Mapped[str] = mapped_column(String(50), nullable=True)
    updated_at: Mapped[str] = mapped_column(String(50), nullable=True)


# Старые таблицы работников - оставляем для совместимости, но помечаем как deprecated
class PostamatWorkerUsers(Base):
    __tablename__ = 'postamat_worker_users'
    postamat_worker_id: Mapped[int] = mapped_column(primary_key=True)
    postamat_worker_uid: Mapped[str] = mapped_column(String(50))
    postamat_worker_passwd: Mapped[str] = mapped_column(String(50))


class OfficeWorkerUsers(Base):
    __tablename__ = 'office_worker_users'
    office_worker_id: Mapped[int] = mapped_column(primary_key=True)
    office_worker_uid: Mapped[str] = mapped_column(String(50))
    office_worker_passwd: Mapped[str] = mapped_column(String(50))


class JuniorAdminUsers(Base):
    __tablename__ = 'junior_admin_users'
    junior_admin_id: Mapped[int] = mapped_column(primary_key=True)
    junior_admin_uid: Mapped[str] = mapped_column(String(50))
    junior_admin_passwd: Mapped[str] = mapped_column(String(50))


class SeniorAdminUsers(Base):
    __tablename__ = 'senior_admin_users'
    senior_admin_id: Mapped[int] = mapped_column(primary_key=True)
    senior_admin_uid: Mapped[str] = mapped_column(String(50))
    senior_admin_passwd: Mapped[str] = mapped_column(String(50))


class OwnerAdminUsers(Base):
    __tablename__ = 'owner_users'
    owner_id: Mapped[int] = mapped_column(primary_key=True)
    owner_uid: Mapped[str] = mapped_column(String(50))
    owner_passwd: Mapped[str] = mapped_column(String(50))


class ReportToSend(Base):
    __tablename__ = 'report_to_send'
    report_id: Mapped[int] = mapped_column(primary_key=True)
    product_photo = Column(LargeBinary(length=16777215), nullable=True)
    report_send: Mapped[int] = mapped_column(default=0)
    product_id: Mapped[int] = mapped_column()
    feedback: Mapped[str] = mapped_column(String(250))
    review: Mapped[int] = mapped_column()


Base.metadata.create_all(engine)

Session = sessionmaker(bind=engine)
session = Session()
