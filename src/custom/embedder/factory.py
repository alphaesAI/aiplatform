import logging
from typing import Any

from .esai import EsaiEmbeddings
from .jina import JinaEmbeddingsService

logger = logging.getLogger(__name__)

"""
connector_factory.py
====================================
Purpose:
    Implementation of the Factory pattern to route requests to specific 
    connector classes based on a string identifier.
"""

class EmbeddingFactory:
    """
    Purpose:
        The Orchestrator class that selects the appropriate connector 
        class based on user input.
    """

    @classmethod
    def get_embedding(cls, embedding_type: str, config: Any):
        """
        Purpose:
            Instantiates the requested connector class using the provided config.

        Args:
            embedding_type (str): The type identifier ('esai', 'jina', etc.).
            config (Any): Configuration data required by the chosen connector.

        Returns:
            Object: An instance of the requested connector class.
            
        Raises:
            ValueError: If the embedding_type is not recognized.
        """
        logger.info(f"Factory creating connector for: {embedding_type}")
        
        embedding_type = embedding_type.lower()
        
        if embedding_type == "esai":
            return EsaiEmbeddings(config=config)
        elif embedding_type == "jina":
            return JinaEmbeddingsService(config=config)
        else:
            error_msg = f"Unsupported embedding type: {embedding_type}"
            logger.error(error_msg)
            raise ValueError(error_msg)