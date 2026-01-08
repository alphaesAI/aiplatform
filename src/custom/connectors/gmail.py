import logging
from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build
from .schemas import GmailConfig

logger = logging.getLogger(__name__)

"""
gmail.py
====================================
Purpose:
    Handles authentication and service creation for the Google Gmail API 
    using OAuth2 credentials.
"""

class GmailConnector:
    """
    Purpose:
        A connector to manage the Gmail API service instance. 
        It uses authorized user info to build a discovery service.
    """

    def __init__(self, config: dict):
        """
        Purpose:
            Initializes the GmailConnector with OAuth2 configuration.

        Args:
            config (dict): Must contain 'token_dict' with valid OAuth2 tokens.
        """
        self.config = GmailConfig(**config)
        self._service = None

    def __call__(self):
        """
        Purpose:
            Returns the Gmail service instance when called.

        Returns:
            googleapiclient.discovery.Resource: The Gmail API service object.
        """
        return self.connect()

    def connect(self):
        """
        Purpose:
            Builds the Gmail API service if it hasn't been initialized yet.
            Uses the 'gmail.readonly' scope by default.

        Args:
            None

        Returns:
            googleapiclient.discovery.Resource: The active Gmail service.
        """
        if not self._service:
            logger.info("Initializing Gmail API service...")
            try:
                token_info = self.config.token_dict
                creds = Credentials.from_authorized_user_info(
                    token_info,
                    scopes=['https://www.googleapis.com/auth/gmail.readonly']
                )
                self._service = build('gmail', 'v1', credentials=creds, cache_discovery=False)
                logger.info("Gmail service successfully built.")
            except Exception as e:
                logger.exception("Failed to build Gmail service.")
                raise
        return self._service