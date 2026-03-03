"""
Loaders Package
==================
Purpose:
    Handles final data delivery to Elasticsearch/OpenSearch.
"""

from .factory import LoaderFactory
from .base import BaseLoader
from .schemas import IngestorConfig

__all__ = [
    "LoaderFactory",
    "BaseLoader",
    "IngestorConfig"
]