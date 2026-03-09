import logging
from typing import Dict, Any

from google.oauth2 import service_account
from googleapiclient.discovery import build

from .base import BaseConnector
from .schemas.googledrive import GoogleDriveConfig

logger = logging.getLogger(__name__)


class GoogleDriveConnector(BaseConnector):
    """
    Connector responsible for establishing a connection
    to the Google Drive API using a service account.
    """

    def __init__(self, config: Dict[str, Any]):
        self.config = GoogleDriveConfig(**config)
        self._client = None

    def connect(self):
        """
        Establish and return a Google Drive API client.
        """

        if self._client:
            logger.debug("Reusing existing Google Drive client")
            return self._client

        credentials = service_account.Credentials.from_service_account_file(
            self.config.service_account_file,
            scopes=self.config.scopes
        )

        self._client = build(
            "drive",
            "v3",
            credentials=credentials,
            cache_discovery=False
        )

        logger.info("Google Drive client initialized")

        return self._client