import logging
from elasticsearch import Elasticsearch
from .schemas import ElasticsearchConfig

# --- Logger Setup ---
logger = logging.getLogger(__name__)

"""
es_connector.py
====================================
Purpose:
    This file defines the ESConnector class, which simplifies the process 
    of establishing a connection to an Elasticsearch cluster.
    
    It handles URL construction, SSL verification settings, and connection 
    verification via ping.
"""

class ElasticsearchConnector:
    """
    Purpose:
        The ESConnector class acts as a wrapper around the official Elasticsearch client.
        It manages configuration parsing and lazy initialization of the client connection.
    """

    def __init__(self, config: dict):
        """
        Purpose:
            Initializes the ESConnector with a configuration dictionary.
            Note: The actual connection is not established until connect() or the object is called.

        Args:
            config (dict): A dictionary containing connection parameters.
                           Expected keys: 'schema', 'host', 'port'.
                           Optional keys: 'verify_certs'.
        """
        # If Airflow put the schema in 'database', move it to 'schema'
        if config.get("database") and not config.get("schema"):
            config["schema"] = config.pop("database")
        
        self.config = ElasticsearchConfig(**config)
        self._client = None
        logger.debug("ESConnector initialized with config keys: %s", list(config.keys()))

    def __call__(self) -> Elasticsearch:
        """
        Purpose:
            Allows the class instance to be called like a function to retrieve the client.
            It ensures the connection is established before returning the client.

        Returns:
            Elasticsearch: An active Elasticsearch client instance.
        """
        logger.info("ESConnector invoked. Ensuring connection is established.")
        self.connect()
        return self._client

    def connect(self) -> None:
        """
        Purpose:
            Constructs the connection URL, initializes the Elasticsearch client,
            and verifies the connection by pinging the server.

        Args:
            None

        Returns:
            None

        Raises:
            ConnectionError: If the client fails to ping the Elasticsearch host.
        """
        protocol = self.config.schema
        host = self.config.host
        port = self.config.port

        es_host = f"{protocol}://{host}:{port}"
        verify_certs = self.config.verify_certs

        logger.info(f"Attempting to connect to Elasticsearch at: {es_host}")

        try:
            self._client = Elasticsearch(
                [es_host],
                verify_certs=verify_certs
            )
            
            # Verify connection
            if not self._client.ping():
                error_msg = f"Could not connect to Elasticsearch at {es_host}. Ping failed."
                logger.error(error_msg)
                raise ConnectionError(error_msg)
            
            logger.info("Successfully connected to Elasticsearch.")

        except Exception as e:
            logger.exception(f"An error occurred while initializing Elasticsearch client: {e}")
            raise