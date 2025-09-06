from fastapi import APIRouter, Depends, HTTPException
from pydantic import BaseModel
from typing import List, Optional
import base64

from app.mobile_api.auth import verify_api_key
from app.datac.db_session import create_session
from app.datac.__all_models import Product

router = APIRouter(prefix="/mobile/products", tags=["Mobile Products"])

class ProductResponse(BaseModel):
    id: int
    name: str
    description: Optional[str]
    price_per_hour: float
    price_per_day: float
    price_per_month: float
    category: Optional[str]
    size: str
    image_base64: Optional[str] = None
    office_id: Optional[int]
    address_of_postamat: Optional[str]
    is_available: bool
    have_problem: bool

class ProductsListResponse(BaseModel):
    success: bool
    products: List[ProductResponse] = []
    message: str = None

@router.get("/by-category/{category_name}", response_model=ProductsListResponse)
async def get_products_by_category(
    category_name: str,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Получение товаров по категории"""
    session = create_session()
    try:
        products = session.query(Product).filter(Product.category == category_name).all()

        products_list = []
        for product in products:
            # Конвертируем изображение в base64 если есть
            image_base64 = None
            if product.image:
                image_base64 = base64.b64encode(product.image).decode('utf-8')

            # Проверяем доступность товара (не зарезервирован и нет проблем)
            is_available = (product.who_is_reserved is None and product.have_problem == 0)

            products_list.append(ProductResponse(
                id=product.id,
                name=product.name,
                description=product.description,
                price_per_hour=product.price_per_hour,
                price_per_day=product.price_per_day,
                price_per_month=product.price_per_month,
                category=product.category,
                size=product.size,
                image_base64=image_base64,
                office_id=product.office_id,
                address_of_postamat=product.address_of_postamat,
                is_available=is_available,
                have_problem=bool(product.have_problem)
            ))

        return ProductsListResponse(
            success=True,
            products=products_list,
            message=f"Найдено {len(products_list)} товаров в категории '{category_name}'"
        )

    except Exception as e:
        return ProductsListResponse(
            success=False,
            message=f"Ошибка получения товаров: {str(e)}"
        )
    finally:
        session.close()

@router.get("/{product_id}", response_model=ProductResponse)
async def get_product_by_id(
    product_id: int,
    api_key_valid: bool = Depends(verify_api_key)
):
    """Получение детальной информации о товаре"""
    session = create_session()
    try:
        product = session.query(Product).filter(Product.id == product_id).first()
        if not product:
            raise HTTPException(status_code=404, detail="Product not found")

        # Конвертируем изображение в base64 если есть
        image_base64 = None
        if product.image:
            image_base64 = base64.b64encode(product.image).decode('utf-8')

        # Проверяем доступность товара
        is_available = (product.who_is_reserved is None and product.have_problem == 0)

        return ProductResponse(
            id=product.id,
            name=product.name,
            description=product.description,
            price_per_hour=product.price_per_hour,
            price_per_day=product.price_per_day,
            price_per_month=product.price_per_month,
            category=product.category,
            size=product.size,
            image_base64=image_base64,
            office_id=product.office_id,
            address_of_postamat=product.address_of_postamat,
            is_available=is_available,
            have_problem=bool(product.have_problem)
        )

    except Exception as e:
        if isinstance(e, HTTPException):
            raise e
        raise HTTPException(status_code=500, detail=f"Error retrieving product: {str(e)}")
    finally:
        session.close()
