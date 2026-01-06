import logging
from .ingestor import SingleIngestor, BulkIngestor

logger = logging.getLogger(__name__)

"""
factory.py
====================================
Purpose:
    Factory class to generate the appropriate Elasticsearch ingestor.
"""

class LoaderFactory:
    """
    Purpose:
        Orchestrates the selection of the loading strategy.
    """
    @staticmethod
    def get_loader(load_type: str, connection, config: dict):
        """
        Purpose: Returns an instance of a specific ingestor.

        Args:
            load_type (str): 'single' or 'bulk'.
            connection (Elasticsearch): The established ES client.
            config (dict): Configuration for index settings and mappings.

        Returns:
            BaseLoader: An initialized SingleIngestor or BulkIngestor.

        Raises:
            ValueError: If the load_type is unsupported.
        """
        logger.info(f"LoaderFactory creating '{load_type}' loader.")
        load_type = load_type.lower().strip()

        if load_type == "single":
            return SingleIngestor(connection=connection, config=config)
        elif load_type == "bulk":
            return BulkIngestor(connection=connection, config=config)
        else:
            error_msg = f"Loader type '{load_type}' is not supported."
            logger.error(error_msg)
            raise ValueError(error_msg)