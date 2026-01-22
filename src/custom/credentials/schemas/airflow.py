from pydantic import BaseModel
from typing import Optional, Dict, Any

class AirflowConnectionSchema(BaseModel):
    """
    Validates and masks sensitive data fetched from Airflow.
    """

    host: Optional[str] = None
    port: Optional[int] = None
    user: Optional[str] = None
    password: Optional[str] = None
    database: Optional[str] = None
    extra: Optional[dict] = None
    base_url: Optional[str] = None
    timeout: Optional[int] = None
    schema_type: Optional[str] = None
    host: Optional[str] = None
    port: Optional[int] = None
    verify_certs: Optional[bool] = None
    token_dict: Optional[Dict[str, Any]] = None
    api_key: Optional[str] = None
