"""
Extractors Package
==================
Purpose:
    Handles logic for pulling and normalizing data from various sources.
"""

from .factory import ExtractorFactory
from .base import BaseExtractor
from .rdbms import RDBMSExtractor
from .gmail import GmailExtractor
from .arxiv import PDFParserService

__all__ = [
    "ExtractorFactory",
    "BaseExtractor",
    "RDBMSExtractor",
    "GmailExtractor",
    "PDFParserService",
]