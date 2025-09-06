from fastapi import APIRouter, Depends
from pydantic import BaseModel
from typing import List

from app.mobile_api.auth import verify_api_key
from app.datac.db_session import create_session
from app.datac.__all_models import ParcelAutomat, Office

router = APIRouter(prefix="/mobile/postamats", tags=["Mobile Postamats"])

class PostamatResponse(BaseModel):
    id: int
    address: str
    latitude: str
    longitude: str
    qr_code_id: str

class OfficeResponse(BaseModel):
    id: int
    address: str
    latitude: str
    longitude: str

class PostamatsListResponse(BaseModel):
    success: bool
    postamats: List[PostamatResponse] = []
    offices: List[OfficeResponse] = []
    message: str = None

@router.get("/list", response_model=PostamatsListResponse)
async def get_postamats_and_offices(
    api_key_valid: bool = Depends(verify_api_key)
):
    """
    Получение списка всех постаматов и офисов
    """
    session = create_session()
    try:
        # Получить все постаматы
        postamats = session.query(ParcelAutomat).all()
        postamats_list = [
            PostamatResponse(
                id=p.id,
                address=p.address,
                latitude=p.first_coordinate,
                longitude=p.second_coordinate,
                qr_code_id=p.qr_code_id
            )
            for p in postamats
        ]

        # Получить все офисы
        offices = session.query(Office).all()
        offices_list = [
            OfficeResponse(
                id=o.id,
                address=o.address,
                latitude=o.first_coordinate,
                longitude=o.second_coordinate
            )
            for o in offices
        ]

        return PostamatsListResponse(
            success=True,
            postamats=postamats_list,
            offices=offices_list,
            message="Список постаматов и офисов получен успешно"
        )

    except Exception as e:
        return PostamatsListResponse(
            success=False,
            message=f"Ошибка получения данных: {str(e)}"
        )
    finally:
        session.close()

@router.get("/nearby")
async def get_nearby_postamats(
    latitude: float,
    longitude: float,
    radius_km: float = 5.0,
    api_key_valid: bool = Depends(verify_api_key)
):
    """
    Получение ближайших постаматов и офисов по координатам
    (Упрощенная версия - возвращает все, для точного расчета расстояний нужны дополнительные библиотеки)
    """
    session = create_session()
    try:
        # Получить все постаматы и офисы (в реальном приложении здесь должен быть расчет расстояний)
        postamats = session.query(ParcelAutomat).all()
        offices = session.query(Office).all()

        postamats_list = [
            {
                "id": p.id,
                "address": p.address,
                "latitude": p.first_coordinate,
                "longitude": p.second_coordinate,
                "qr_code_id": p.qr_code_id,
                "type": "postamat"
            }
            for p in postamats
        ]

        offices_list = [
            {
                "id": o.id,
                "address": o.address,
                "latitude": o.first_coordinate,
                "longitude": o.second_coordinate,
                "type": "office"
            }
            for o in offices
        ]

        return {
            "success": True,
            "locations": postamats_list + offices_list,
            "message": f"Найдено {len(postamats_list + offices_list)} локаций"
        }

    except Exception as e:
        return {
            "success": False,
            "message": f"Ошибка получения данных: {str(e)}"
        }
    finally:
        session.close()
