from sqlalchemy import Column, String, LargeBinary, Integer, DateTime, ForeignKey
from sqlalchemy.orm import Mapped, mapped_column, relationship
from datetime import datetime
from .base import Base


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
    # Новое поле для статуса модерации паспорта
    # 0 = не добавлен, 1 = на модерации, 2 = отклонен, 3 = одобрен
    passport_status: Mapped[int] = mapped_column(Integer, default=0, nullable=False)

    # Связь с фотографиями паспорта
    photos = relationship("PassportPhoto", back_populates="passport")


class PassportPhoto(Base):
    """Фотографии паспортов пользователей"""
    __tablename__ = 'passport_photos'

    id: Mapped[int] = mapped_column(primary_key=True)
    passport_id: Mapped[int] = mapped_column(ForeignKey('passports.user_passport_id'), nullable=False)
    photo_data = Column(LargeBinary(length=67108864), nullable=False)  # Увеличиваем лимит до 64MB для паспортов
    photo_type: Mapped[int] = mapped_column(Integer, nullable=False)  # 0 = первая страница, 1 = страница с пропиской
    file_name: Mapped[str] = mapped_column(String(255), nullable=True)
    file_size: Mapped[int] = mapped_column(Integer, nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)

    # Связь с паспортом
    passport = relationship("Passport", back_populates="photos")


class PassportEncryptionKey(Base):
    __tablename__ = 'server_passport_keys'

    id: Mapped[int] = mapped_column(primary_key=True)
    encryption_key: Mapped[str] = mapped_column(String(256), nullable=True)
    user_number: Mapped[str] = mapped_column(String(25))


class TechnicalWorksUpdate(Base):
    __tablename__ = 'technical_works_and_update'
    work_id: Mapped[int] = mapped_column(primary_key=True)
    type_of_work: Mapped[str] = mapped_column(String(50))
    term_of_work: Mapped[str] = mapped_column(String(100), nullable=True)
    version_of_update: Mapped[str] = mapped_column(String(50), nullable=True)
    link_on_update: Mapped[str] = mapped_column(String(150), nullable=True)
    visibility_enabled: Mapped[int] = mapped_column(default=0)


class ReportToSend(Base):
    __tablename__ = 'report_to_send'
    report_id: Mapped[int] = mapped_column(primary_key=True)
    product_photo = Column(LargeBinary(length=16777215), nullable=True)
    report_send: Mapped[int] = mapped_column(default=0)
    product_id: Mapped[int] = mapped_column()
    feedback: Mapped[str] = mapped_column(String(250))
    review: Mapped[int] = mapped_column()
