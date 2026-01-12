import logging
from typing import Dict, List, Any, Iterator

# 1. Module-level Conditional Import
try:
    from txtai.embeddings import Embeddings as EmbEngine
    TXTAI_AVAILABLE = True
except ImportError:
    TXTAI_AVAILABLE = False

from .schemas import EmbeddingsConfig

logger = logging.getLogger(__name__)

"""
txtai.py
====================================
Purpose:
    Converts text chunks into vector arrays using txtai. This module
    allows for semantic search capabilities in the downstream database.
"""

class TxtaiEmbeddings:
    """
    Purpose:
        Generates semantic vectors for text chunks while preserving metadata.
        Operates as a middleware between the Transformer and the Loader.
    """

    @staticmethod
    def available() -> bool:
        return TXTAI_AVAILABLE

    def __init__(self, data: Iterator[Dict[str, Any]], config: Dict[str, Any]):
        """
        Purpose: Initializes the embedding engine with a specific model.

        Args:
            data (Iterator[Dict[str, Any]]): Stream of records in Elasticsearch format.
            config (Dict[str, Any]): Configuration containing 'path' for the model.
        
        Raises:
            ImportError: If txtai is not installed in the environment.
        """
        # CRITICAL GUARD: Don't let the code run if txtai is missing
        if not self.available():
            raise ImportError(
                "txtai is not installed. Please install it with 'pip install txtai' "
                "to use the TxtaiEmbeddings module."
            )

        self.data = data
        self.config = EmbeddingsConfig(**config)
        self.engine = EmbEngine(self.config.model_dump())
        logger.info(f"Embeddings engine initialized with model: {self.config.path}")

    def embed(self) -> Iterator[Dict[str, Any]]:
        """
        Purpose: 
            Iterates through records and adds a 'vector' field to the '_source'.
            This is designed to work seamlessly with the Transformer output.

        Yields:
            Dict[str, Any]: Records updated with vector embeddings.
        """
        for record in self.data:
            source_data = record.get("_source", {})
            text = source_data.get("text", "")
            
            if text:
                logger.debug(f"Generating embedding for record ID: {source_data.get('id')}")
                vector_array = self.engine.transform(text)
                source_data["vector"] = vector_array.tolist()
            
            yield record

    def query(self, text: str) -> List[float]:
        """
        Purpose: Converts a search string into a vector for querying.

        Args:
            text (str): The search query.

        Returns:
            List[float]: The vector representation of the query.
        """
        pass