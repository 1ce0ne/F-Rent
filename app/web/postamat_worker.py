from fastapi import APIRouter, Request

from app.utils.templates import templates
from app.utils.auth import access_required

router = APIRouter()

@router.get('/worker-postamat')
@access_required('worker_postamat')
async def worker_postamat(request: Request):
    return templates.TemplateResponse('worker/postamat/worker_postamat.html', {'request': request})
