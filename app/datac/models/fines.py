from sqlalchemy import Column, Integer, String, Float, DateTime, Text, ForeignKey
from sqlalchemy.orm import relationship
from datetime import datetime

from .base import Base


class Fines(Base):
    """
    Модель таблицы штрафов
    Хранит информацию о штрафах, выписанных пользователям за повреждение/потерю товаров
    """
    __tablename__ = 'fines'

    fine_id = Column(Integer, primary_key=True, autoincrement=True, comment='Уникальный ID штрафа')

    # Связи с другими таблицами
    user_id = Column(Integer, nullable=False, comment='ID пользователя, которому выписан штраф')
    product_id = Column(Integer, nullable=False, comment='ID товара, за который выписан штраф')
    order_id = Column(Integer, nullable=True, comment='ID заказа, связанного со штрафом')

    # Основная информация о штрафе
    fine_amount = Column(Float, nullable=False, comment='Сумма штрафа в рублях')
    fine_reason = Column(String(500), nullable=False, comment='Причина выписывания штрафа')
    fine_description = Column(Text, nullable=True, comment='Подробное описание проблемы')

    # Статус штрафа
    fine_status = Column(String(50), nullable=False, default='unpaid',
                         comment='Статус штрафа: unpaid, paid, canceled, disputed')

    # Даты и время
    created_at = Column(DateTime, nullable=False, default=datetime.utcnow, comment='Дата и время создания штрафа')
    paid_at = Column(DateTime, nullable=True, comment='Дата и время оплаты штрафа')
    due_date = Column(DateTime, nullable=True, comment='Крайний срок оплаты штрафа')

    # Информация о создателе штрафа
    created_by_worker_id = Column(Integer, nullable=False, comment='ID сотрудника, который создал штраф')
    created_by_worker_role = Column(String(50), nullable=False, comment='Роль сотрудника, который создал штраф')

    # Платежная информация
    payment_transaction_id = Column(String(255), nullable=True, comment='ID транзакции оплаты штрафа')
    payment_method = Column(String(100), nullable=True, comment='Способ оплаты штрафа')

    # Дополнительные поля
    admin_notes = Column(Text, nullable=True, comment='Заметки администратора по штрафу')

    def __repr__(self):
        return f"<Fine(fine_id={self.fine_id}, user_id={self.user_id}, amount={self.fine_amount}, status='{self.fine_status}')>"

    def to_dict(self):
        """Преобразование объекта штрафа в словарь"""
        return {
            'fine_id': self.fine_id,
            'user_id': self.user_id,
            'product_id': self.product_id,
            'order_id': self.order_id,
            'fine_amount': self.fine_amount,
            'fine_reason': self.fine_reason,
            'fine_description': self.fine_description,
            'fine_status': self.fine_status,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'paid_at': self.paid_at.isoformat() if self.paid_at else None,
            'due_date': self.due_date.isoformat() if self.due_date else None,
            'created_by_worker_id': self.created_by_worker_id,
            'created_by_worker_role': self.created_by_worker_role,
            'payment_transaction_id': self.payment_transaction_id,
            'payment_method': self.payment_method,
            'admin_notes': self.admin_notes
        }

    @property
    def is_overdue(self):
        """Проверка, просрочен ли штраф"""
        if self.due_date and self.fine_status == 'unpaid':
            return datetime.utcnow() > self.due_date
        return False

    @property
    def is_paid(self):
        """Проверка, оплачен ли штраф"""
        return self.fine_status == 'paid'

    def mark_as_paid(self, transaction_id=None, payment_method=None):
        """Отметить штраф как оплаченный"""
        self.fine_status = 'paid'
        self.paid_at = datetime.utcnow()
        if transaction_id:
            self.payment_transaction_id = transaction_id
        if payment_method:
            self.payment_method = payment_method

    def cancel_fine(self, admin_note=None):
        """Отменить штраф"""
        self.fine_status = 'canceled'
        if admin_note:
            self.admin_notes = admin_note
