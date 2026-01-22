from pydantic import BaseModel

class OpensearchConfig(BaseModel):
    """
    Purpose:
        Configuration schema for Opensearch connections.
    """
    schema_type: str
    host: str
    port: int
    verify_certs: bool = False