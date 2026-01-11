"""
Transformers Package
====================
Purpose:
    Handles cleaning, data-type standardization, and segmentation 
    of raw data into search-ready formats.
"""

from .factory import TransformerFactory
from .base import BaseTransformer
from .pdf import DoclingEngine, PDFTransformer
from .text import TextChunker

__all__ = [
    "TransformerFactory",
    "BaseTransformer",
    "DoclingEngine",
    "PDFTransformer",
    "TextChunker",
]