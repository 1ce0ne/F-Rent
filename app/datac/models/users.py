import bcrypt
from sqlalchemy import Column, String
from sqlalchemy.orm import Mapped, mapped_column

from .base import Base


class User(Base):
    __tablename__ = 'users'

    id: Mapped[int] = mapped_column(primary_key=True)
    name: Mapped[str] = mapped_column(String(25))
    password: Mapped[str] = mapped_column(String(75))
    phone_number: Mapped[str] = mapped_column(String(11))
    # cards: Mapped[str] = mapped_column(Text, nullable=True)
    banned: Mapped[int] = mapped_column(default=0)
    have_a_passport: Mapped[str] = mapped_column(String(256), nullable=True, default=None)

    def verify_password(self, password: str):
        return bcrypt.checkpw(password.encode('utf-8'), self.password.encode('utf-8'))


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
