from sqlalchemy import Column, String, Integer, DateTime, Boolean, Text
from sqlalchemy.orm import Mapped, mapped_column
from datetime import datetime
from .base import Base


class UserCard(Base):
    """Модель для хранения привязанных карт пользователей"""
    __tablename__ = 'user_cards'

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[str] = mapped_column(String(50), nullable=False)
    card_token: Mapped[str] = mapped_column(String(256), nullable=False)
    last4_digits: Mapped[str] = mapped_column(String(4), nullable=False)
    card_type: Mapped[str] = mapped_column(String(50), nullable=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)


class TelegramUser(Base):
    """Модель для хранения данных пользователей Telegram"""
    __tablename__ = 'telegram_users'

    id: Mapped[int] = mapped_column(primary_key=True)
    user_id: Mapped[str] = mapped_column(String(50), nullable=False)  # ID пользователя в системе
    telegram_id: Mapped[int] = mapped_column(Integer, nullable=False, unique=True)
    telegram_username: Mapped[str] = mapped_column(String(100), nullable=True)
    first_name: Mapped[str] = mapped_column(String(100), nullable=True)
    last_name: Mapped[str] = mapped_column(String(100), nullable=True)
    phone_number: Mapped[str] = mapped_column(String(20), nullable=True)
    verified_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    is_verified: Mapped[bool] = mapped_column(Boolean, default=True)


class PaymentTransaction(Base):
    """Модель для хранения транзакций платежей"""
    __tablename__ = 'payment_transactions'

    id: Mapped[int] = mapped_column(primary_key=True)
    transaction_id: Mapped[str] = mapped_column(String(100), nullable=False, unique=True)
    user_id: Mapped[str] = mapped_column(String(50), nullable=False)
    order_id: Mapped[str] = mapped_column(String(100), nullable=True)
    amount: Mapped[str] = mapped_column(String(20), nullable=False)
    currency: Mapped[str] = mapped_column(String(10), default='RUB')
    status: Mapped[str] = mapped_column(String(50), nullable=False)
    operation_type: Mapped[str] = mapped_column(String(50), nullable=False)
    card_first_six: Mapped[str] = mapped_column(String(6), nullable=True)
    card_last_four: Mapped[str] = mapped_column(String(4), nullable=True)
    card_type: Mapped[str] = mapped_column(String(50), nullable=True)
    gateway_name: Mapped[str] = mapped_column(String(100), nullable=True)
    test_mode: Mapped[bool] = mapped_column(Boolean, default=False)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    updated_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    raw_data: Mapped[str] = mapped_column(Text, nullable=True)  # Полные данные от CloudPayments
