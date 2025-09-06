from sqlalchemy import Column, String, ForeignKey, LargeBinary, Integer
from sqlalchemy.orm import Mapped, mapped_column
from .base import Base


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

    # Новое поле для комментариев при возврате
    comment: Mapped[str] = mapped_column(String(1000), nullable=True)  # Комментарий сотрудника при возврате

    # Метаданные
    created_at: Mapped[str] = mapped_column(String(50), nullable=True)
    updated_at: Mapped[str] = mapped_column(String(50), nullable=True)


class OrderPhotos(Base):
    __tablename__ = 'order_photos'

    photo_id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    order_id: Mapped[int] = mapped_column(Integer, ForeignKey('unified_orders.order_id'), nullable=False)
    photo_data = Column(LargeBinary(length=16777215), nullable=False)  # Само фото в бинарном формате
    photo_type: Mapped[int] = mapped_column(nullable=False)  # 0 - до аренды, 1 - после возврата
    created_at: Mapped[str] = mapped_column(String(50), nullable=False)  # Дата и времени создания фото
    file_name: Mapped[str] = mapped_column(String(255), nullable=True)  # Оригинальное имя файла
    file_size: Mapped[int] = mapped_column(nullable=True)  # Размер файла в байтах
    content_type: Mapped[str] = mapped_column(String(100), nullable=True)  # MIME тип (image/jpeg, image/png)
