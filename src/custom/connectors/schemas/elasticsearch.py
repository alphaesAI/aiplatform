from pydantic import BaseModel

class ElasticsearchConfig(BaseModel):
    """
    Purpose:
        Configuration schema for Elasticsearch connections.
    """
    schema_type: str
    host: str
    port: int
    verify_certs: bool = False