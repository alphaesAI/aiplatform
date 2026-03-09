
"""
Module: Google Drive Document Transformer
Purpose:
    Converts Google Drive files into clean text documents ready for embedding.

Features:
    - MIME type routing
    - PDF support
    - text file support
    - extensible parser architecture
"""

import logging
from typing import Dict, Any

from pypdf import PdfReader

from ..base import BaseTransformer
from .downloader import GoogleDriveDownloader

logger = logging.getLogger(__name__)


class GoogleDriveDocumentTransformer(BaseTransformer):
    """
    Transformer that converts Google Drive files into text documents.
    """

    def __init__(self, connection):
        """
        Initialize the transformer.

        Parameters
        ----------
        connection : Google Drive API client
        """

        self.downloader = GoogleDriveDownloader(connection)

    def _parse_pdf(self, file_bytes: bytes) -> str:
        """
        Extract text from PDF file.
        """

        from io import BytesIO

        reader = PdfReader(BytesIO(file_bytes))

        text = ""

        for page in reader.pages:
            text += page.extract_text() or ""

        return text

    def _parse_text(self, file_bytes: bytes) -> str:
        """
        Decode plain text file.
        """

        return file_bytes.decode("utf-8", errors="ignore")

    def transform(self, record: Dict[str, Any]) -> Dict[str, Any]:
        """
        Transform Google Drive metadata into text document.

        Parameters
        ----------
        record : Dict
            File metadata from extractor.

        Returns
        -------
        Dict
            Structured document for embedding pipeline.
        """

        file_id = record["id"]
        file_name = record.get("name")
        mime_type = record.get("mimeType")

        logger.info(f"Processing file: {file_name}")

        # Download file bytes
        file_bytes = self.downloader.download_file(file_id)

        text = ""

        # MIME routing
        if mime_type == "application/pdf":

            text = self._parse_pdf(file_bytes)

        elif mime_type == "text/plain":

            text = self._parse_text(file_bytes)

        else:

            logger.warning(
                "Unsupported MIME type: %s | skipping text extraction",
                mime_type
            )

        return {
            "doc_id": file_id,
            "text": text,
            "metadata": {
                "source": "googledrive",
                "name": file_name,
                "mimeType": mime_type
            }
        }

