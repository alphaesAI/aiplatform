"""
Loaders Package
==================
Purpose:
    Handles final data delivery to Elasticsearch/OpenSearch.
"""

from .factory import LoaderFactory
from .base import BaseLoader
from .elasticsearch import ElasticsearchIngestor, ElasticsearchSingleIngestor, ElasticsearchBulkIngestor
from .opensearch import OpensearchIngestor, OpensearchSingleIngestor, OpensearchBulkIngestor

__all__ = [
    "LoaderFactory",
    "BaseLoader",
    "ElasticsearchIngestor",
    "ElasticsearchSingleIngestor",
    "ElasticsearchBulkIngestor",
    "OpensearchIngestor",
    "OpensearchSingleIngestor",
    "OpensearchBulkIngestor",
    "Embeddings"
]