import logging

from .factory import EmbeddingFactory
from .esai import EsaiEmbeddings
from .jina import JinaEmbeddingsService

# Define the public API for the package
__all__ = [
    "EmbeddingFactory",
    "EsaiEmbeddings",
    "JinaEmbeddingsService",
]

# Set a default logger for the package to prevent "No handler found" warnings
logging.getLogger(__name__).addHandler(logging.NullHandler())