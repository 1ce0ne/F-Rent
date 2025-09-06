from .models.users import User, BannedUsers, ReasonsForBan
from .models.products import Product, ParcelAutomat, Cell, Office, OfficeCells
from .models.orders import Orders, OfficeOrders, OrderTypes, UnifiedOrders, OrderPhotos
from .models.workers import (
    WorkerRoles, Workers, WorkerOfficeAssignment, PostamatWorkerUsers, OfficeWorkerUsers,
    JuniorAdminUsers, SeniorAdminUsers, OwnerAdminUsers
)
from .models.security import (
    Card, EncryptionKey, Passport, PassportPhoto, PassportEncryptionKey,
    TechnicalWorksUpdate, ReportToSend
)
from .models.fines import Fines
from .models.payment import UserCard, TelegramUser, PaymentTransaction
from .models.base import Base, get_password_hash

# Для обратной совместимости создаем engine и session
from .db_session import get_engine, create_session

engine = get_engine()
session = create_session()
