from qdrant_client import QdrantClient
from typing import Dict, Any
from .base import BaseConnector

class QdrantCloudConnector(BaseConnector):
    def __init__(self, config: Dict[str, Any]):
        """
        Required Params in config dict:
        - url: The 'Endpoint' from your Qdrant Cloud dashboard (usually ends in :6333)
        - api_key: Your API Key for authentication
        """
        self.config = config
        self.client = QdrantClient(
            url=config.get("url"), 
            api_key=config.get("api_key"),
            prefer_grpc=True  # Recommended for high-performance ingestion
        )

    def connect(self):
        """
        Returns the QdrantClient instance.
        """
        return self.client