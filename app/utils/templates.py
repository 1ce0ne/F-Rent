import os
from fastapi.templating import Jinja2Templates

base_dir = os.path.dirname(os.path.dirname(__file__))
templates = Jinja2Templates(directory=os.path.join(base_dir, 'templates'))


