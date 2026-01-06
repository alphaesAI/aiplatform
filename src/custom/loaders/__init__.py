"""
Loaders Package
==================
Purpose:
    Handles final data delivery to Elasticsearch/OpenSearch.
"""

from .factory import LoaderFactory
from .base import BaseLoader
from .embeddings import Embeddings

__all__ = [
    "LoaderFactory",
    "BaseLoader",
    "Embeddings"
]