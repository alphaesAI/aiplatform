from pydantic import BaseModel, ConfigDict
from typing import Optional, List

class SparkConnectorConfig(BaseModel):
    model_config = ConfigDict(extra="allow")
    login: str
    password: str
    region_name: Optional[str] = None
    host: Optional[str] = None
    app_name: Optional[str] = None
    packages: Optional[List[str]] = None