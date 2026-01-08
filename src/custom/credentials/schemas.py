from pydantic import BaseModel, ConfigDict
from typing import Optional

class AirflowConnectionSchema(BaseModel):
    """
    Validates and masks sensitive data fetched from Airflow.
    """
    model_config = ConfigDict(populate_by_name=True, extra="allow")

    host: Optional[str] = None
    port: Optional[int] = None
    user: Optional[str] = None
    password: Optional[str] = None
    database: Optional[str] = None