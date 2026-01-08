from pydantic import BaseModel, ConfigDict, Field
from typing import Optional, Dict, Any

class ElasticsearchConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    schema_type: str = Field(default="http", alias="schema")
    host: str
    port: int
    verify_certs: bool = False

class OpensearchConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    schema_type: str = Field(default="http", alias="schema")
    host: str
    port: int
    verify_certs: bool = False

class RDBMSConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    db_type: str = Field(alias="type")
    host: str
    port: int
    username: str = Field(alias="user")
    password: str
    database: str
    
class GmailConfig(BaseModel):
    token_dict: Dict[str, Any]