from sqlalchemy import String, ForeignKey, DateTime, Boolean, Integer
from sqlalchemy.orm import Mapped, mapped_column, relationship
from datetime import datetime
from .base import Base


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
    phone_number: Mapped[str] = mapped_column(String(20), nullable=True)  # Номер телефона работника
    telegram_username: Mapped[str] = mapped_column(String(50), nullable=True)  # Telegram username (без @)
    telegram_2fa_enabled: Mapped[bool] = mapped_column(Boolean, default=False)  # Включена ли 2FA через Telegram
    created_at: Mapped[str] = mapped_column(String(50), nullable=True)
    is_active: Mapped[int] = mapped_column(default=1)

    # Связи с офисами
    office_assignments = relationship("WorkerOfficeAssignment", back_populates="worker", foreign_keys="[WorkerOfficeAssignment.worker_id]")


class WorkerOfficeAssignment(Base):
    """Привязка работников к офисам - один работник может работать в нескольких офисах"""
    __tablename__ = 'worker_office_assignments'

    id: Mapped[int] = mapped_column(primary_key=True)
    worker_id: Mapped[int] = mapped_column(ForeignKey('workers.worker_id'), nullable=False)
    office_id: Mapped[int] = mapped_column(ForeignKey('office.id'), nullable=False)
    assigned_at: Mapped[datetime] = mapped_column(DateTime, default=datetime.utcnow)
    assigned_by: Mapped[int] = mapped_column(ForeignKey('workers.worker_id'), nullable=True)  # Кто назначил
    is_active: Mapped[bool] = mapped_column(Boolean, default=True)

    # Связи
    worker = relationship("Workers", back_populates="office_assignments", foreign_keys=[worker_id])
    office = relationship("Office", back_populates="worker_assignments")
    assigner = relationship("Workers", foreign_keys=[assigned_by])


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
