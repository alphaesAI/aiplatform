from pydantic import BaseModel, ConfigDict

class OpensearchConfig(BaseModel):
    """
    Purpose:
        Configuration schema for Opensearch connections.
    """
    model_config = ConfigDict(protected_namespaces=())

    schema: str
    host: str
    port: int
    verify_certs: bool = False