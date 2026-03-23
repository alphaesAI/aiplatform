from qdrant_client import QdrantClient
from typing import Dict, Any
from .base import BaseConnector

class QdrantCloudConnector(BaseConnector):
    def __init__(self, config: Dict[str, Any]):
        """
        Required Params in config dict:
        Option 1 (Direct):
        - url: The 'Endpoint' from your Qdrant Cloud dashboard (usually ends in :6333)
        - api_key: Your API Key for authentication
        
        Option 2 (Airflow Connection):
        - host: Qdrant Cloud host
        - port: Port (usually 443 for HTTPS)
        - schema: Schema (https)
        - password: API Key
        """
        self.config = config
        
        # Handle both direct config and Airflow connection format
        if config.get("url") and config.get("api_key"):
            # Direct cloud configuration
            url = config.get("url")
            api_key = config.get("api_key")
        elif config.get("host"):
            # Airflow connection format - construct URL from components
            host = config.get("host")
            port = config.get("port", 443)
            schema = config.get("schema", "https")
            api_key = config.get("password")
            
            # Clean host - remove schema if present
            if host.startswith("http://"):
                host = host.replace("http://", "")
            elif host.startswith("https://"):
                host = host.replace("https://", "")
            
            # Construct full URL
            if port in [80, 443]:  # Standard ports don't need to be specified
                url = f"{schema}://{host}"
            else:
                url = f"{schema}://{host}:{port}"
        else:
            raise ValueError("Invalid configuration: need either (url, api_key) or (host, port, schema, password)")
        
        self.client = QdrantClient(
            url=url, 
            api_key=api_key,
            prefer_grpc=True  # Recommended for high-performance ingestion
        )
        
        # Debug logging (remove in production)
        import logging
        logger = logging.getLogger(__name__)
        logger.info(f"QdrantCloudConnector initialized - URL: {url}, API Key: {'***' + api_key[-4:] if api_key else 'None'}")

    def connect(self):
        """
        Returns the QdrantClient instance.
        """
        return self.client