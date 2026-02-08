from pydantic import BaseModel, ConfigDict
from typing import Optional

class S3Config(BaseModel):
    model_config = ConfigDict(extra="allow")
    login: str
    password: str
    region_name: Optional[str]
    host: str