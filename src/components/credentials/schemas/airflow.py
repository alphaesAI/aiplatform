from pydantic import BaseModel, ConfigDict
from typing import Optional, List

class AirflowConnectionSchema(BaseModel):
    """
    Validates and masks sensitive data fetched from Airflow.
    """
    # This tells Pydantic it's okay if we use names that might overlap with internal protected names.

    model_config = ConfigDict(protected_namespaces=(), extra="forbid")
    host: Optional[str] = None
    port: Optional[int] = None
    login: Optional[str] = None
    password: Optional[str] = None
    schema: Optional[str] = None  # Airflow DB name or ES Protocol
    type: Optional[str] = None    # SQLAlchemy dialect+driver
    verify_certs: Optional[bool] = None

    # Explicit Gmail/OAuth2 Fields (Found in your logs)
    refresh_token: Optional[str] = None
    token_uri: Optional[str] = None
    client_id: Optional[str] = None
    client_secret: Optional[str] = None
    scopes: Optional[List[str]] = None
    universe_domain: Optional[str] = None
    account: Optional[str] = None
    expiry: Optional[str] = None

    region_name: Optional[str] = None