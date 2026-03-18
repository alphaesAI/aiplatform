from pydantic import BaseModel, ConfigDict

class QdrantCloudConfig(BaseModel):
    """
    Purpose:
        Configuration schema for Qdrant Cloud connections.
    """
    model_config = ConfigDict(protected_namespaces=())

    url: str      # Cluster Endpoint (e.g., https://xxx.qdrant.io)
    api_key: str  # Required for Cloud authentication