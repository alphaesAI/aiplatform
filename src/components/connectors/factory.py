import logging
from typing import Any

logger = logging.getLogger(__name__)

"""
connector_factory.py
====================================
Purpose:
    Implementation of the Factory pattern to route requests to specific 
    connector classes based on a string identifier.
"""

class ConnectorFactory:
    """
    Purpose:
        The Orchestrator class that selects the appropriate connector 
        class based on user input.
    """

    @classmethod
    def get_connector(cls, connector_type: str, config: Any):
        """
        Purpose:
            Instantiates the requested connector class using the provided config.

        Args:
            connector_type (str): The type identifier ('rdbms', 'gmail', etc.).
            config (Any): Configuration data required by the chosen connector.

        Returns:
            Object: An instance of the requested connector class.
            
        Raises:
            ValueError: If the connector_type is not recognized.
        """
        logger.info(f"Factory creating connector for: {connector_type}")
        
        connector_type = connector_type.lower()
        
        # Lazy imports to avoid dependency issues when connector is not used
        from .rdbms import RDBMSConnector
        
        if connector_type == "rdbms":
            return RDBMSConnector(config=config)
        elif connector_type == "gmail":
            from .gmail import GmailConnector
            return GmailConnector(config=config)
        elif connector_type == "arxiv":
            from .arxiv import ArxivConnector
            return ArxivConnector(config=config)
        elif connector_type == "elasticsearch":
            from .elasticsearch import ElasticsearchConnector
            return ElasticsearchConnector(config=config)
        elif connector_type == "opensearch":
            from .opensearch import OpensearchConnector
            return OpensearchConnector(config=config)
        elif connector_type == "jina":
            from .jina import JinaConnector
            return JinaConnector(config=config)
        elif connector_type == "s3":
            from .s3 import S3Connector
            return S3Connector(config=config)
        else:
            error_msg = f"Unsupported connector type: {connector_type}"
            logger.error(error_msg)
            raise ValueError(error_msg)