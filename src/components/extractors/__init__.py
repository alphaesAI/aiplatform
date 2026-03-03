"""
Extractors Package
==================
Purpose:
    Handles logic for pulling and normalizing data from various sources.
"""

from .factory import ExtractorFactory
from .base import BaseExtractor

__all__ = [
    "ExtractorFactory",
    "BaseExtractor",
]