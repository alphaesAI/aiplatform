"""
Module: Embeddings
Purpose: Converts text chunks into vector arrays using txtai.
Integration: Uses a top-level conditional import pattern to prevent Airflow timeouts.
"""

import logging
from typing import Dict, List, Any, Iterator

# Setup logging
logger = logging.getLogger(__name__)

# Conditional import pattern
try:
    from txtai.embeddings import Embeddings as EmbEngine
    TXTAI = True
except ImportError:
    TXTAI = False


class Embeddings:
    """
    Purpose: Generates semantic vectors for text chunks while preserving metadata.
    
    Responsibilities:
    - Verify if txtai is available.
    - Transform text to vectors using the Numpy backend.
    - Preserve source_id and chunk_id alignment.
    """

    @staticmethod
    def available():
        """
        Purpose: Checks if the txtai library is installed in the environment.
        
        Returns:
            bool: True if txtai is available, False otherwise.
        """
        return TXTAI

    def __init__(self, config: Dict[str, Any]):
        """
        Purpose: Initializes the embedding engine.
        
        Args:
            config (Dict[str, Any]): Configuration containing 'path' for the model.
        
        Raises:
            ImportError: If txtai is not installed.
        """
        if not Embeddings.available():
            logger.error("txtai is not installed. Pipeline cannot generate vectors.")
            raise ImportError('txtai engine is not available - install "txtai" to enable embeddings.')

        self.config = config
        model_path = self.config.get("path", "sentence-transformers/all-MiniLM-L6-v2")

        # Initialize engine in pass-through mode (Numpy)
        self.engine = EmbEngine({
            "path": model_path,
            "content": False,
            "backend": "numpy"
        })
        logger.info(f"Embeddings engine initialized with model: {model_path}")

    def embed(self, data: Iterator[Dict[str, Any]]) -> Iterator[Dict[str, Any]]:
        """
        Purpose: Processes records wrapped in Elasticsearch format.
        """
        for record in data:
            # Reach into the '_source' created by BaseTransformer
            source_data = record.get("_source", {})
            text = source_data.get("text", "")
            
            if text:
                # Generate and add vector INSIDE the _source
                vector_array = self.engine.transform(text)
                source_data["vector"] = vector_array.tolist()
            
            # Yield the whole record (including _index and updated _source)
            yield record

    def query(self, text: str) -> List[float]:
        pass