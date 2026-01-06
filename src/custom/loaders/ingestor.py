import logging
from elasticsearch import helpers
from .base import BaseLoader

logger = logging.getLogger(__name__)

"""
ingestor.py
====================================
Purpose:
    Contains the logic for sending data to Elasticsearch using 
    either single-index or bulk-index strategies.
"""

class Ingestor(BaseLoader):
    """
    Purpose:
        Parent class to handle common connection management and index 
        initialization (settings/mappings).
    """
    def __init__(self, connection, config):
        """
        Purpose: Initializes the ingestor with an ES connection and config.

        Args:
            connection (Elasticsearch): The active ES client.
            config (dict): YAML configuration for index settings and mappings.
        """
        self.connection = connection
        self.config = config

    def create(self) -> None:
        """
        Purpose: Ensures the target index exists with proper mappings.

        Returns:
            None
        """
        name = self.config.get("index_name")
        body = {
            "settings": self.config.get("settings", {}),
            "mappings": self.config.get("mappings", {})
        }
        if not self.connection.indices.exists(index=name):
            logger.info(f"Index '{name}' does not exist. Creating with provided mappings.")
            self.connection.indices.create(index=name, body=body)
        else:
            logger.debug(f"Index '{name}' already exists.")

    def __call__(self, data):
        """
        Purpose: The entry point for loading. Ensures index setup before ingestion.

        Args:
            data (Any): Data to be loaded.
        """
        self.create()
        return self.load(data)

class SingleIngestor(Ingestor):
    """
    Purpose: Loads data row-by-row. Useful for small datasets or streaming.
    """
    def load(self, data):
        """
        Args:
            data (Iterator): Stream of records.
        """
        logger.info("Starting single-row ingestion.")
        count = 0
        for action in data:
            self.connection.index(
                index=action["_index"],
                document=action["_source"]
            )
            count += 1
        logger.info(f"Successfully indexed {count} documents individually.")

class BulkIngestor(Ingestor):
    """
    Purpose: Loads data in efficient batches using the Elasticsearch helpers.
    """
    def load(self, data):
        """
        Args:
            data (Iterator): Stream of records.
        """
        logger.info("Starting bulk ingestion.")
        success, failed = helpers.bulk(self.connection, data)
        logger.info(f"Bulk indexing complete. Success: {success}, Failed: {len(failed) if isinstance(failed, list) else failed}")