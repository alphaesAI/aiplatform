import logging
from opensearchpy import OpenSearch
from .schemas import OpensearchConfig

logger = logging.getLogger(__name__)

"""
opensearch.py
====================================
Purpose:
    Provides a connector for OpenSearch clusters, handling URL building 
    and connection health checks.
"""

class OpensearchConnector:
    """
    Purpose:
        A wrapper for the OpenSearch client to manage connection settings 
        and cluster pings.
    """

    def __init__(self, config: dict):
        """
        Purpose:
            Initializes the OpensearchConnector with cluster details.

        Args:
            config (dict): Contains 'schema', 'host', 'port', and 'verify_certs'.
        """
        self.config = OpensearchConfig(**config)
        self._client = None

    def __call__(self):
        """
        Purpose:
            Ensures connection and returns the OpenSearch client instance.

        Returns:
            opensearchpy.OpenSearch: The active OpenSearch client.
        """
        self.connect()
        return self._client

    def connect(self):
        """
        Purpose:
            Initializes the OpenSearch client and verifies the connection via ping.

        Args:
            None

        Returns:
            None

        Raises:
            ConnectionError: If the OpenSearch cluster is unreachable.
        """
        protocol = self.config.schema_type
        host = self.config.host
        port = self.config.port
        opensearch_host = f"{protocol}://{host}:{port}"
        
        logger.info(f"Attempting to connect to OpenSearch at {opensearch_host}")

        self._client = OpenSearch(
            [opensearch_host],
            verify_certs=self.config.verify_certs
        )

        if not self._client.ping():
            error_msg = f"Could not connect to OpenSearch at {opensearch_host}"
            logger.error(error_msg)
            raise ConnectionError(error_msg)
        
        logger.info("OpenSearch connection verified.")