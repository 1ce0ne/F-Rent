from sqlalchemy import Column, String, LargeBinary, Integer, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship
from .base import Base


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

    office_id: Mapped[int] = mapped_column(Integer, ForeignKey('office.id'), nullable=True)

    who_is_reserved: Mapped[str] = mapped_column(String(11), nullable=True)
    start_of_reservation: Mapped[str] = mapped_column(String(250), nullable=True)

    office = relationship("Office", back_populates="products")

    have_problem: Mapped[int] = mapped_column(default=0, nullable=False)  # 0 = нет проблем, 1 = есть проблемы


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

    products = relationship("Product", back_populates="office")
    worker_assignments = relationship("WorkerOfficeAssignment", back_populates="office")


class OfficeCells(Base):
    __tablename__ = 'office_cells'
    id: Mapped[int] = mapped_column(primary_key=True, autoincrement=True)
    office_id = Column(Integer, nullable=True)
    office_cell_id = Column(Integer, nullable=True)
    office_product_id = Column(Integer, nullable=True)
