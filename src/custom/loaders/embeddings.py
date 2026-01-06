import logging
from typing import Dict, List, Any, Iterator

logger = logging.getLogger(__name__)

"""
embeddings.py
====================================
Purpose:
    Converts text chunks into vector arrays using txtai. This module
    allows for semantic search capabilities in the downstream database.
"""

# Conditional import pattern to prevent Airflow DAG import timeouts
try:
    from txtai.embeddings import Embeddings as EmbEngine
    TXTAI = True
except ImportError:
    TXTAI = False

class Embeddings:
    """
    Purpose:
        Generates semantic vectors for text chunks while preserving metadata.
        Operates as a middleware between the Transformer and the Loader.
    """

    @staticmethod
    def available() -> bool:
        """
        Purpose: Checks if the txtai library is installed.
        
        Returns:
            bool: True if available, False otherwise.
        """
        return TXTAI

    def __init__(self, config: Dict[str, Any]):
        """
        Purpose: Initializes the embedding engine with a specific model.

        Args:
            config (Dict[str, Any]): Configuration containing 'path' for the model.
        
        Raises:
            ImportError: If txtai is not installed in the environment.
        """
        if not Embeddings.available():
            logger.error("txtai is not installed. Vector generation disabled.")
            raise ImportError('txtai engine is not available - install "txtai" to enable embeddings.')

        self.config = config
        model_path = self.config.get("path", "sentence-transformers/all-MiniLM-L6-v2")

        self.engine = EmbEngine({
            "path": model_path,
            "content": False,
            "backend": "numpy"
        })
        logger.info(f"Embeddings engine initialized with model: {model_path}")

    def embed(self, data: Iterator[Dict[str, Any]]) -> Iterator[Dict[str, Any]]:
        """
        Purpose: 
            Iterates through records and adds a 'vector' field to the '_source'.
            This is designed to work seamlessly with the Transformer output.

        Args:
            data (Iterator[Dict[str, Any]]): Stream of records in Elasticsearch format.

        Yields:
            Dict[str, Any]: Records updated with vector embeddings.
        """
        for record in data:
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
        return self.engine.transform(text).tolist()