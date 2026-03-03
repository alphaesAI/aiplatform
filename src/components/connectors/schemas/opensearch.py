from pydantic import BaseModel, ConfigDict, Field
from typing import Optional

class OpensearchConfig(BaseModel):
    """
    Purpose:
        Configuration schema for Opensearch connections.
    """
    model_config = ConfigDict(protected_namespaces=())

    schema_type: str = Field(alias="schema")
    host: str
    port: int
    verify_certs: bool = True
    ca_certs: Optional[str] = None
    login: Optional[str] = None
    password: Optional[str] = None