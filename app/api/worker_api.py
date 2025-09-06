from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse
from datetime import datetime

from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import Workers, WorkerRoles, WorkerOfficeAssignment, Office

router = APIRouter()

# Маппинг ролей для отображения
ROLE_DISPLAY_MAP = {
    'postamat_worker': 'Работник постамата',
    'office_worker': 'Офисный работник',
    'junior_admin': 'Младший администратор',
    'senior_admin': 'Старший администратор',
    'owner': 'Владелец'
}


@router.get('/api/get-workers')
@access_required({'senior_admin', 'owner'})
async def api_get_workers(request: Request):
    """Получение списка всех работников"""
    session = create_session()
    try:
        # Получаем всех работников с их ролями через JOIN
        workers_query = session.query(Workers, WorkerRoles).join(
            WorkerRoles, Workers.role_id == WorkerRoles.role_id
        ).filter(Workers.is_active == 1).all()

        workers = []
        for worker, role in workers_query:
            workers.append({
                'id': worker.worker_id,
                'name': worker.worker_uid,
                'position': ROLE_DISPLAY_MAP.get(role.role_name, role.role_description),
                'role': role.role_name,
                'role_id': role.role_id,
                'phone_number': worker.phone_number,
                'telegram_username': worker.telegram_username,
                'telegram_2fa_enabled': worker.telegram_2fa_enabled,
                'created_at': worker.created_at,
                'is_active': worker.is_active
            })

        return JSONResponse(workers)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-workers-for-senior')
@access_required({'senior_admin', 'owner'})
async def api_get_workers_for_senior(request: Request):
    """Получение работников для старшего администратора (исключая владельцев)"""
    session = create_session()
    try:
        # Получаем работников кроме владельцев
        workers_query = session.query(Workers, WorkerRoles).join(
            WorkerRoles, Workers.role_id == WorkerRoles.role_id
        ).filter(
            Workers.is_active == 1,
            WorkerRoles.role_name != 'owner'
        ).all()

        workers = []
        for worker, role in workers_query:
            workers.append({
                'id': worker.worker_id,
                'name': worker.worker_uid,
                'position': ROLE_DISPLAY_MAP.get(role.role_name, role.role_description),
                'role': role.role_name,
                'role_id': role.role_id,
                'phone_number': worker.phone_number,
                'telegram_username': worker.telegram_username,
                'telegram_2fa_enabled': worker.telegram_2fa_enabled,
                'created_at': worker.created_at,
                'is_active': worker.is_active
            })

        return JSONResponse(workers)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-worker-details/{worker_id}')
@access_required({'senior_admin', 'owner'})
async def api_get_worker_details(request: Request, worker_id: int):
    """Получение детальной информации о работнике"""
    session = create_session()
    try:
        # Получаем работника с ролью
        worker_query = session.query(Workers, WorkerRoles).join(
            WorkerRoles, Workers.role_id == WorkerRoles.role_id
        ).filter(Workers.worker_id == worker_id).first()

        if not worker_query:
            return JSONResponse({'error': 'Worker not found'}, status_code=404)

        worker, role = worker_query
        return JSONResponse({
            'id': worker.worker_id,
            'name': worker.worker_uid,
            'position': ROLE_DISPLAY_MAP.get(role.role_name, role.role_description),
            'role': role.role_name,
            'role_id': role.role_id,
            'phone_number': worker.phone_number,
            'telegram_username': worker.telegram_username,
            'telegram_2fa_enabled': worker.telegram_2fa_enabled,
            'created_at': worker.created_at,
            'is_active': worker.is_active
        })

    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.delete('/api/delete-worker/{worker_id}')
@access_required({'senior_admin', 'owner'})
async def api_delete_worker(request: Request, worker_id: int):
    """Удаление работника (деактивация)"""
    session = create_session()
    try:
        worker = session.query(Workers).filter_by(worker_id=worker_id).first()
        if not worker:
            return JSONResponse({'error': 'Worker not found'}, status_code=404)

        # Вместо физического удаления делаем деактивацию
        worker.is_active = 0
        session.commit()

        return JSONResponse({'success': True, 'message': 'Worker deactivated successfully'})

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/add-worker')
@access_required({'senior_admin', 'owner'})
async def api_add_worker(request: Request):
    """Добавление нового работника"""
    data = await request.json()
    worker_type = data.get('worker_type')  # role_name
    username = data.get('username')
    password = data.get('password')
    phone_number = data.get('phone_number')  # Новое поле

    if not worker_type or not username or not password:
        return JSONResponse({'error': 'Не указаны обязательные поля'}, status_code=400)

    session = create_session()
    try:
        # Проверяем, существует ли роль
        role = session.query(WorkerRoles).filter_by(role_name=worker_type).first()
        if not role:
            return JSONResponse({'error': 'Некорректный тип работника'}, status_code=400)

        # Проверяем уникальность логина
        existing_worker = session.query(Workers).filter_by(worker_uid=username).first()
        if existing_worker:
            return JSONResponse({'error': 'Работник с таким логином уже существует'}, status_code=409)

        # Создаем нового работника
        new_worker = Workers(
            worker_uid=username,
            worker_passwd=password,
            role_id=role.role_id,
            phone_number=phone_number,  # Добавляем номер телефона
            created_at=datetime.now().strftime('%Y-%m-%d %H:%M:%S'),
            is_active=1
        )

        session.add(new_worker)
        session.commit()

        return JSONResponse({'success': True, 'worker_id': new_worker.worker_id})

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.put('/api/update-worker/{worker_id}')
@access_required({'senior_admin', 'owner'})
async def api_update_worker(worker_id: int, request: Request):
    """Обновление информации о работнике"""
    data = await request.json()
    session = create_session()
    try:
        worker = session.query(Workers).filter_by(worker_id=worker_id).first()
        if not worker:
            return JSONResponse({'error': 'Worker not found'}, status_code=404)

        # Обновляем поля
        if 'username' in data:
            # Проверяем уникальность нового логина
            existing_worker = session.query(Workers).filter(
                Workers.worker_uid == data['username'],
                Workers.worker_id != worker_id
            ).first()
            if existing_worker:
                return JSONResponse({'error': 'Работник с таким логином уже существует'}, status_code=409)
            worker.worker_uid = data['username']

        if 'password' in data:
            worker.worker_passwd = data['password']

        if 'phone_number' in data:  # Новое поле
            worker.phone_number = data['phone_number']

        if 'telegram_username' in data:  # Новое поле для Telegram username
            telegram_username = data['telegram_username'].strip()
            # Убираем @ если пользователь его указал
            if telegram_username.startswith('@'):
                telegram_username = telegram_username[1:]
            worker.telegram_username = telegram_username if telegram_username else None

        if 'worker_type' in data:
            # Обновляем роль
            role = session.query(WorkerRoles).filter_by(role_name=data['worker_type']).first()
            if not role:
                return JSONResponse({'error': 'Некорректный тип работника'}, status_code=400)
            worker.role_id = role.role_id

        if 'is_active' in data:
            worker.is_active = int(data['is_active'])

        session.commit()
        return JSONResponse({'success': True, 'message': 'Worker updated successfully'})

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-worker-roles')
@access_required({'senior_admin', 'owner'})
async def api_get_worker_roles(request: Request):
    """Получение списка всех ролей работников"""
    session = create_session()
    try:
        roles = session.query(WorkerRoles).all()
        result = []
        for role in roles:
            result.append({
                'role_id': role.role_id,
                'role_name': role.role_name,
                'role_description': role.role_description,
                'display_name': ROLE_DISPLAY_MAP.get(role.role_name, role.role_description)
            })

        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.put('/api/toggle-worker-2fa/{worker_id}')
@access_required({'senior_admin', 'owner'})
async def api_toggle_worker_2fa(worker_id: int, request: Request):
    """Включение/выключение двухфакторной аутентификации для работника"""
    data = await request.json()
    enabled = data.get('enabled', False)

    session = create_session()
    try:
        worker = session.query(Workers).filter_by(worker_id=worker_id).first()
        if not worker:
            return JSONResponse({'error': 'Worker not found'}, status_code=404)

        # Если включаем 2FA, проверяем наличие номера телефона
        if enabled and not worker.phone_number:
            return JSONResponse({'error': 'Для включения 2FA необходимо указать номер телефона'}, status_code=400)

        worker.telegram_2fa_enabled = enabled
        session.commit()

        return JSONResponse({
            'success': True,
            'message': f'2FA {"включена" if enabled else "выключена"}',
            'telegram_2fa_enabled': worker.telegram_2fa_enabled
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/worker-office-assignments/{worker_id}')
@access_required({'senior_admin', 'owner'})
async def get_worker_office_assignments(request: Request, worker_id: int):
    """Получение списка офисов, к которым привязан работник"""
    session = create_session()
    try:
        assignments = session.query(WorkerOfficeAssignment, Office).join(
            Office, WorkerOfficeAssignment.office_id == Office.id
        ).filter(
            WorkerOfficeAssignment.worker_id == worker_id,
            WorkerOfficeAssignment.is_active == True
        ).all()

        result = []
        for assignment, office in assignments:
            result.append({
                'assignment_id': assignment.id,
                'office_id': office.id,
                'office_address': office.address,
                'assigned_at': assignment.assigned_at.isoformat(),
                'is_active': assignment.is_active
            })

        return JSONResponse({'assignments': result})
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/assign-worker-to-office')
@access_required({'senior_admin', 'owner'})
async def assign_worker_to_office(request: Request):
    """Привязка работника к офису"""
    data = await request.json()
    worker_id = data.get('worker_id')
    office_id = data.get('office_id')

    if not worker_id or not office_id:
        return JSONResponse({'error': 'Не указаны worker_id или office_id'}, status_code=400)

    session = create_session()
    try:
        # Проверяем существование работника
        worker = session.query(Workers).filter_by(worker_id=worker_id).first()
        if not worker:
            return JSONResponse({'error': 'Работник не найден'}, status_code=404)

        # Проверяем существование офиса
        office = session.query(Office).filter_by(id=office_id).first()
        if not office:
            return JSONResponse({'error': 'Офис не найден'}, status_code=404)

        # Проверяем, не привязан ли уже работник к этому офису
        existing = session.query(WorkerOfficeAssignment).filter_by(
            worker_id=worker_id,
            office_id=office_id,
            is_active=True
        ).first()

        if existing:
            return JSONResponse({'error': 'Работник уже привязан к этому офису'}, status_code=409)

        # Получаем ID текущего пользователя (кто назначает)
        current_user = getattr(request.state, 'user', None)
        assigned_by = current_user.worker_id if current_user else None

        # Создаем новую привязку
        assignment = WorkerOfficeAssignment(
            worker_id=worker_id,
            office_id=office_id,
            assigned_by=assigned_by,
            assigned_at=datetime.utcnow(),
            is_active=True
        )

        session.add(assignment)
        session.commit()

        return JSONResponse({
            'success': True,
            'assignment_id': assignment.id,
            'message': f'Работник {worker.worker_uid} привязан к офису {office.address}'
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.delete('/api/unassign-worker-from-office/{assignment_id}')
@access_required({'senior_admin', 'owner'})
async def unassign_worker_from_office(request: Request, assignment_id: int):
    """Отвязка работника от офиса"""
    session = create_session()
    try:
        assignment = session.query(WorkerOfficeAssignment).filter_by(id=assignment_id).first()
        if not assignment:
            return JSONResponse({'error': 'Привязка не найдена'}, status_code=404)

        # Деактивируем привязку вместо удаления
        assignment.is_active = False
        session.commit()

        return JSONResponse({
            'success': True,
            'message': 'Работник отвязан от офиса'
        })

    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/office-workers/{office_id}')
@access_required({'senior_admin', 'owner'})
async def get_office_workers(request: Request, office_id: int):
    """Получение списка работников, привязанных к офису"""
    session = create_session()
    try:
        workers = session.query(WorkerOfficeAssignment, Workers, WorkerRoles).join(
            Workers, WorkerOfficeAssignment.worker_id == Workers.worker_id
        ).join(
            WorkerRoles, Workers.role_id == WorkerRoles.role_id
        ).filter(
            WorkerOfficeAssignment.office_id == office_id,
            WorkerOfficeAssignment.is_active == True,
            Workers.is_active == 1
        ).all()

        result = []
        for assignment, worker, role in workers:
            result.append({
                'assignment_id': assignment.id,
                'worker_id': worker.worker_id,
                'worker_name': worker.worker_uid,
                'worker_role': ROLE_DISPLAY_MAP.get(role.role_name, role.role_description),
                'phone_number': worker.phone_number,
                'assigned_at': assignment.assigned_at.isoformat()
            })

        return JSONResponse({'workers': result})
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/worker-available-offices/{worker_id}')
@access_required({'senior_admin', 'owner'})
async def get_worker_available_offices(request: Request, worker_id: int):
    """Получение офисов, к которым можно привязать работника"""
    session = create_session()
    try:
        # Получаем все офисы
        all_offices = session.query(Office).all()

        # Получаем офисы, к которым уже привязан работник
        assigned_office_ids = session.query(WorkerOfficeAssignment.office_id).filter_by(
            worker_id=worker_id,
            is_active=True
        ).all()
        assigned_ids = [office_id[0] for office_id in assigned_office_ids]

        # Фильтруем доступные офисы
        available_offices = []
        for office in all_offices:
            if office.id not in assigned_ids:
                available_offices.append({
                    'id': office.id,
                    'address': office.address,
                    'coordinates': {
                        'lat': office.first_coordinate,
                        'lng': office.second_coordinate
                    }
                })

        return JSONResponse({'offices': available_offices})
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/worker-assigned-offices')
@access_required({'office_worker', 'junior_admin', 'senior_admin', 'owner'})
async def get_worker_assigned_offices(request: Request):
    """Получение офисов, к которым привязан текущий работник"""
    session = create_session()
    try:
        current_user = getattr(request.state, 'user', None)
        if not current_user:
            return JSONResponse({'error': 'Пользователь не авторизован'}, status_code=401)

        worker_id = current_user.worker_id

        offices = session.query(WorkerOfficeAssignment, Office).join(
            Office, WorkerOfficeAssignment.office_id == Office.id
        ).filter(
            WorkerOfficeAssignment.worker_id == worker_id,
            WorkerOfficeAssignment.is_active == True
        ).all()

        result = []
        for assignment, office in offices:
            result.append({
                'office_id': office.id,
                'address': office.address,
                'coordinates': {
                    'lat': office.first_coordinate,
                    'lng': office.second_coordinate
                }
            })

        return JSONResponse({'offices': result})
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()
