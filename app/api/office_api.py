from fastapi import APIRouter, Request
from fastapi.responses import JSONResponse

from app.utils.auth import access_required
from app.datac.db_session import create_session
from app.datac.__all_models import OfficeCells, Product, Office
from data.Scripts import add_office_with_cells

router = APIRouter()


@router.get('/api/get-office-cells/{office_id}')
@access_required()
async def get_office_cells(office_id: int, request: Request):
    session = create_session()
    try:
        cells = session.query(OfficeCells).filter_by(office_id=office_id).all()
        result = {
            'office_id': office_id,
            'cells': [{
                'cell_id': cell.office_cell_id,
                'product_id': cell.office_product_id
            } for cell in cells]
        }
        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-cell-info/{office_id}/{cell_id}')
@access_required()
async def get_cell_info(office_id: int, cell_id: int, request: Request):
    session = create_session()
    try:
        cell = session.query(OfficeCells).filter_by(
            office_id=office_id,
            office_cell_id=cell_id
        ).first()
        if not cell:
            return JSONResponse({'empty': True})
        product = session.query(Product).filter_by(id=cell.office_product_id).first()
        if not product:
            return JSONResponse({'empty': True})
        result = {
            'empty': False,
            'product': {
                'id': product.id,
                'name': product.name,
                'description': product.description,
                'price_per_hour': product.price_per_hour,
                'price_per_day': product.price_per_day,
                'price_per_month': product.price_per_month,
                'image_url': f'/get-product-image/{product.id}'
            }
        }
        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/update-cell/{office_id}/{cell_id}')
@access_required()
async def update_cell(office_id: int, cell_id: int, request: Request):
    session = create_session()
    try:
        data = await request.json()
        product_id = data.get('product_id')
        cell = session.query(OfficeCells).filter_by(office_id=office_id, office_cell_id=cell_id).first()
        office = session.query(Office).filter_by(id=office_id).first()

        # Если убираем товар из ячейки
        if not product_id and cell.office_product_id:
            product = session.query(Product).filter_by(id=cell.office_product_id).first()
            if product:
                product.address_of_postamat = None
            cell.office_product_id = None

        # Если добавляем товар в ячейку
        elif product_id:
            product = session.query(Product).filter_by(id=product_id).first()
            if product and office:
                product.address_of_postamat = office.address
            cell.office_product_id = product_id

        session.commit()
        return JSONResponse({'success': True})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.post('/api/add-office')
@access_required({'senior_admin', 'owner'})
async def api_add_office(request: Request):
    data = await request.json()
    address = data.get('address')
    first_coordinate = data.get('first_coordinate')
    second_coordinate = data.get('second_coordinate')

    if not address or not first_coordinate or not second_coordinate:
        return JSONResponse({'error': 'Не указаны обязательные поля'}, status_code=400)

    session = create_session()
    try:
        # Создаем новый офис с ячейками
        office_id = add_office_with_cells(session, address, first_coordinate, second_coordinate)
        return JSONResponse({'success': True, 'office_id': office_id})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.delete('/api/delete-office/{office_id}')
@access_required({'senior_admin', 'owner'})
async def api_delete_office(office_id: int, request: Request):
    session = create_session()
    try:
        office = session.query(Office).filter_by(id=office_id).first()
        if not office:
            return JSONResponse({'error': 'Office not found'}, status_code=404)

        # Удаляем ячейки офиса
        session.query(OfficeCells).filter_by(office_id=office_id).delete()

        # Удаляем сам офис
        session.delete(office)
        session.commit()

        return JSONResponse({'success': True, 'message': 'Office deleted successfully'})
    except Exception as e:
        session.rollback()
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()


@router.get('/api/get-offices')
@access_required()
async def api_get_offices(request: Request):
    session = create_session()
    try:
        offices = session.query(Office).all()
        result = [{
            'id': office.id,
            'address': office.address,
            'first_coordinate': office.first_coordinate,
            'second_coordinate': office.second_coordinate
        } for office in offices]
        return JSONResponse(result)
    except Exception as e:
        return JSONResponse({'error': str(e)}, status_code=500)
    finally:
        session.close()
