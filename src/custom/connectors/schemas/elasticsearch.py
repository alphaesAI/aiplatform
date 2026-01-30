from pydantic import BaseModel, ConfigDict

class ElasticsearchConfig(BaseModel):
    """
    Purpose:
        Configuration schema for Elasticsearch connections.
    """
    model_config = ConfigDict(protected_namespaces=())

    schema: str
    host: str
    port: int
    verify_certs: bool