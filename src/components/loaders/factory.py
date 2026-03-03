import logging

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
    def get_loader(load_type: str, config: dict, connection=None, data=None):
        """
        Purpose: Returns an instance of a specific ingestor.

        Args:
            load_type (str): 'elasticsearch' or 'opensearch'.
            connection (Elasticsearch): The established ES client.
            config (dict): Configuration for index settings and mappings.

        Returns:
            BaseLoader: An initialized BulkIngestor.

        Raises:
            ValueError: If the load_type is unsupported.
        """
        logger.info(f"LoaderFactory creating '{load_type}' loader.")
        load_type = load_type.lower().strip()

        # Lazy imports to avoid dependency issues when loader is not used
        if load_type == "elasticsearch":
            from .elasticsearch import ElasticsearchBulkIngestor
            return ElasticsearchBulkIngestor(connection=connection, config=config)
        elif load_type == "opensearch":
            from .opensearch import OpensearchBulkIngestor
            return OpensearchBulkIngestor(connection=connection, config=config)
        elif load_type == "spark":
            from .spark import ElasticsearchSparkLoader
            return ElasticsearchSparkLoader(data=data, config=config)
        else:
            error_msg = f"Loader type '{load_type}' is not supported."
            logger.error(error_msg)
            raise ValueError(error_msg)