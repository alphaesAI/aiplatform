import logging
from typing import Any, Dict

logger = logging.getLogger(__name__)

"""
factory.py
====================================
Purpose:
    Provides a universal entry point for creating specific transformer instances.
"""

class TransformerFactory:
    """
    Purpose:
        Factory class to route data to the correct Transformer implementation.
    """

    @staticmethod
    def get_transformer(transformer_type: str, data: Any, config: Dict[str, Any]):
        """
        Purpose: Instantiates the requested transformer based on type.

        Args:
            transformer_type (str): Type of transformation ('document' or 'json').
            data (Any): The dataset to be transformed.
            config (Dict[str, Any]): Parameters for the transformer.

        Returns:
            BaseTransformer: An instance of a specific transformer.

        Raises:
            ValueError: If an unknown transformer type is provided.
        """
        logger.info(f"TransformerFactory creating transformer for type: {transformer_type}")
        transformer_type = transformer_type.lower().strip()

        # Lazy imports to avoid dependency issues when transformer is not used
        if transformer_type == "document":
            from .document import DocumentTransformer
            return DocumentTransformer(data, config)
        
        elif transformer_type == "json":
            from .json_transformer import JsonTransformer
            return JsonTransformer(data, config)
        
        elif transformer_type == "chunker":
            from .arxiv import TextChunker
            return TextChunker(data, config)
        
        elif transformer_type == "pdf":
            from .arxiv import PDFTransformer
            return PDFTransformer(data, config)
        
        elif transformer_type == "table":
            from .spark.table import TableTransformer
            return TableTransformer(data, config)
        
        else:
            error_msg = f"Unknown transformer type: {transformer_type}"
            logger.error(error_msg)
            raise ValueError(error_msg)