import uuid
from sqlalchemy import select

from app.datac.__all_models import (
    OfficeWorkerUsers,
    JuniorAdminUsers,
    SeniorAdminUsers,
    OwnerAdminUsers,
    PostamatWorkerUsers,
    Office, Product, OfficeCells, ParcelAutomat, Cell
)


def add_product_to_cell(session, cell_id, product_id, postamat_id, address_of_postamat):
    cell = session.query(Cell).get(cell_id)
    product = session.query(Product).get(product_id)
    cell.product_id = product_id
    product.address_of_postamat = address_of_postamat

    session.commit()


def add_product_to_cell_office(session, office_id, office_cell_id, office_product_id):
    # Проверяем, существует ли уже запись для данной ячейки
    existing_cell = session.query(OfficeCells).filter_by(
        office_id=office_id,
        office_cell_id=office_cell_id
    ).first()

    if existing_cell:
        # Обновляем существующую запись
        existing_cell.office_product_id = office_product_id
    else:
        # Создаем новую запись
        new_cell = OfficeCells(
            office_id=office_id,
            office_cell_id=office_cell_id,
            office_product_id=office_product_id
        )
        session.add(new_cell)

    # Обновляем адрес товара
    product = session.query(Product).get(office_product_id)
    if product:
        office = session.query(Office).get(office_id)
        if office:
            product.address_of_postamat = office.address

    session.commit()


def get_free_products(session):
    """
    Получить список свободных товаров (не находящихс�� в ячейках).
    """
    # Получаем ID товаров, которые уже находятся в ячейках постаматов
    occupied_in_cells = select(Cell.product_id).where(Cell.product_id.isnot(None))

    # Получаем ID товаров, которые находятся в офисных ячейках
    occupied_in_office_cells = select(OfficeCells.office_product_id).where(
        OfficeCells.office_product_id.isnot(None)
    )

    # Получаем товары, которые не находятся ни в каких ячейках
    free_products = session.query(Product).filter(
        ~Product.id.in_(occupied_in_cells),
        ~Product.id.in_(occupied_in_office_cells)
    ).all()

    return free_products


def add_new_product(session, name, price_per_hour, price_per_day, price_per_month, description, size, category, image_bytes, office_id=None):
    """
    Добавить новый товар в базу данных.
    """
    product_uuid = str(uuid.uuid4())

    new_product = Product(
        name=name,
        description=description,
        price_per_hour=float(price_per_hour),
        price_per_day=float(price_per_day),
        price_per_month=float(price_per_month),
        product_uuid=product_uuid,
        size=size,
        category=category,
        image=image_bytes,
        office_id=office_id  # Добавляем привязку к офису
    )

    session.add(new_product)
    session.commit()
    return new_product


def add_office_with_cells(session, address, first_coordinate, second_coordinate, num_cells=20):
    """
    Добавить новый офис с указанным количеством ячеек.
    """
    # Создаем офис
    new_office = Office(
        address=address,
        first_coordinate=first_coordinate,
        second_coordinate=second_coordinate
    )
    session.add(new_office)
    session.flush()  # Получаем ID офиса

    # Создаем ячейки для офиса
    for cell_id in range(1, num_cells + 1):
        office_cell = OfficeCells(
            office_id=new_office.id,
            office_cell_id=cell_id
        )
        session.add(office_cell)

    session.commit()
    return new_office.id


def add_postamat_with_cells(session, address, first_coordinate, second_coordinate, num_cells=20):
    """
    Добавить новый постамат с указанным количеством ячеек.
    """
    qr_code_id = str(uuid.uuid4())

    # Создаем постамат
    new_postamat = ParcelAutomat(
        address=address,
        first_coordinate=first_coordinate,
        second_coordinate=second_coordinate,
        qr_code_id=qr_code_id
    )
    session.add(new_postamat)
    session.flush()  # Получаем ID постамата

    # Создаем ячейки для постамата
    sizes = ['S', 'M', 'L', 'XL']  # Разные размеры ячеек
    for cell_id in range(1, num_cells + 1):
        size = sizes[cell_id % len(sizes)]  # Циклически распределяем размеры
        cell = Cell(
            size=size,
            parcel_automat_id=new_postamat.id
        )
        session.add(cell)

    session.commit()
    return new_postamat.id


# Функции для работы со старыми таблицами работников (для совместимости)
def check_worker_credentials(session, username, password, worker_type):
    """
    Проверить учетные данные работника в старых таблицах.
    DEPRECATED: Используйте новую таблицу Workers вместо этого.
    """
    worker_classes = {
        'postamat_worker': PostamatWorkerUsers,
        'office_worker': OfficeWorkerUsers,
        'junior_admin': JuniorAdminUsers,
        'senior_admin': SeniorAdminUsers,
        'owner': OwnerAdminUsers
    }

    worker_class = worker_classes.get(worker_type)
    if not worker_class:
        return None

    # Определяем поля для каждого типа работника
    uid_field = f"{worker_type}_uid"
    passwd_field = f"{worker_type}_passwd"

    if worker_type == 'owner':
        uid_field = "owner_uid"
        passwd_field = "owner_passwd"

    worker = session.query(worker_class).filter(
        getattr(worker_class, uid_field) == username,
        getattr(worker_class, passwd_field) == password
    ).first()

    return worker
