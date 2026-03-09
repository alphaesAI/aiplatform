"""
Module: Google Drive Extractor
Purpose:
    Extracts file metadata from Google Drive.

Features:
    - Pagination support
    - Incremental ingestion using modifiedTime
    - Airflow Variable state tracking
"""

import logging
from typing import Dict, Any, Generator, List

# Airflow Variable is used to store the last successful pipeline run timestamp
from airflow.models import Variable

# Used to update the timestamp after successful extraction
from datetime import datetime

from .base import BaseExtractor
from .schemas.googledrive import GoogleDriveExtractorConfig

logger = logging.getLogger(__name__)


class GoogleDriveExtractor(BaseExtractor):
    """
    Extracts file metadata from Google Drive.

    Supports two modes:
    1. Full extraction
       - Retrieves all files from the specified folder.

    2. Incremental extraction
       - Retrieves only files modified after the last pipeline run.
    """

    def __init__(self, connection, config: Dict[str, Any]):
        """
        Initialize the extractor.

        Parameters
        ----------
        connection : Google Drive API service
            Authenticated Google Drive client.

        config : Dict[str, Any]
            Extraction configuration loaded from YAML.
        """

        # Store the Drive API connection
        self.connection = connection

        # Validate configuration using schema
        self.config = GoogleDriveExtractorConfig(**config)

        # Extraction parameters
        self.folder_id = self.config.folder_id
        self.page_size = self.config.page_size

        # Incremental ingestion configuration
        self.incremental_enabled = self.config.incremental.get("enabled", False)
        self.timestamp_field = self.config.incremental.get(
            "timestamp_field", "modifiedTime"
        )

    def _get_last_timestamp(self) -> str:
        """
        Retrieve the last successful pipeline execution timestamp
        from Airflow Variables.

        Returns
        -------
        str
            Timestamp used for incremental filtering.
        """

        return Variable.get(
            "googledrive_last_run_timestamp",
            default_var="1970-01-01T00:00:00Z"
        )

    def _update_timestamp(self) -> None:
        """
        Update the Airflow Variable with the current timestamp
        after a successful extraction run.
        """

        Variable.set(
            "googledrive_last_run_timestamp",
            datetime.utcnow().isoformat()
        )

    def _build_query(self) -> str:
        """
        Construct the Google Drive query used for file retrieval.

        Returns
        -------
        str
            Google Drive API query.
        """

        # Base query to fetch files within the configured folder
        query = f"'{self.folder_id}' in parents"

        # Apply incremental filter if enabled
        if self.incremental_enabled:
            last_timestamp = self._get_last_timestamp()

            query += f" and {self.timestamp_field} > '{last_timestamp}'"

            logger.info(
                "Incremental ingestion enabled. Fetching files modified after %s",
                last_timestamp
            )

        return query

    def extract(self) -> Generator[Dict[str, Any], None, None]:
        """
        Extract file metadata records from Google Drive.

        Returns
        -------
        Generator[Dict[str, Any]]
            Stream of Google Drive file metadata.
        """

        # Build query based on extraction mode
        query = self._build_query()

        page_token = None

        while True:
            # Google Drive API request
            response = (
                self.connection.files()
                .list(
                    q=query,
                    pageSize=self.page_size,
                    fields="nextPageToken, files(id, name, mimeType, modifiedTime, owners)",
                    pageToken=page_token,
                )
                .execute()
            )

            # Extract file list from response
            files: List[Dict[str, Any]] = response.get("files", [])

            logger.info("Fetched %d files from Google Drive", len(files))

            # Yield each file record
            for file in files:
                yield file

            # Handle pagination
            page_token = response.get("nextPageToken")

            if not page_token:
                break

        # After successful extraction update timestamp
        if self.incremental_enabled:
            self._update_timestamp()

