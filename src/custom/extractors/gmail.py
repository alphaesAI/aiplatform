import logging
import base64
from typing import Dict, Any, List, Iterator
from .base import BaseExtractor

logger = logging.getLogger(__name__)

"""
gmail.py
====================================
Purpose:
    Handles extraction and normalization of email data from the Gmail API.
"""

class GmailExtractor(BaseExtractor):
    """
    Purpose: 
        Extracts and normalizes email data. Acts as an iterator to 
        handle memory-efficient processing of large batches.
    """

    def __init__(self, connection: Any, config: Dict[str, Any]):
        """
        Purpose: Initializes the Gmail extractor and identifies the source account.

        Args:
            connection (Any): Authenticated Gmail API service object.
            config (Dict[str, Any]): Configuration for 'query', 'batch_size', etc.
        """
        self.service = connection
        self.config = config
        
        # Identify the email address associated with the token
        profile = self.service.users().getProfile(userId='me').execute()
        self.source_id = profile.get('emailAddress', 'unknown_account')
        logger.info(f"GmailExtractor initialized for account: {self.source_id}")

    def __call__(self) -> Iterator[Dict[str, Any]]:
        """
        Purpose: Call the instance to begin extraction.
        
        Returns:
            Iterator[Dict[str, Any]]: Generator of normalized email records.
        """
        return self.extract()

    def extract(self) -> Iterator[Dict[str, Any]]:
        """
        Purpose: Coordinates fetching message IDs and yielding normalized data.

        Yields:
            Dict[str, Any]: A single normalized email record.
        """
        logger.info(f"Starting Gmail extraction with query: {self.config.get('query')}")
        message_ids = self._get_message_ids()

        for msg_id in message_ids:
            try:
                raw_msg = self.service.users().messages().get(
                    userId='me', 
                    id=msg_id, 
                    format=self.config.get('extraction_mode', 'full')
                ).execute()

                yield self._normalize_message(raw_msg)
            except Exception as e:
                logger.error(f"Failed to extract message {msg_id}: {e}")
                continue
        
    def _get_message_ids(self) -> List[str]:
        """
        Purpose: Searches Gmail for IDs matching the config query.

        Returns:
            List[str]: List of message IDs.
        """
        query = self.config.get('query')
        batch_size = self.config.get('batch_size', 10)

        result = self.service.users().messages().list(
            userId='me', q=query, maxResults=batch_size
        ).execute()

        messages = result.get('messages', [])
        return [m['id'] for m in messages]

    def _normalize_message(self, raw_msg: Dict[str, Any]) -> Dict[str, Any]:
        """
        Purpose: Flattens raw API JSON into a standard schema.

        Args:
            raw_msg (Dict[str, Any]): Raw JSON from Google.

        Returns:
            Dict[str, Any]: Normalized dictionary.
        """
        headers = raw_msg.get('payload', {}).get('headers', [])
        allowed_headers = self.config.get('fields', [])
        
        metadata = {
            h['name'].lower(): h['value'] 
            for h in headers 
            if h['name'].lower() in allowed_headers
        }

        body = self._extract_body(raw_msg.get('payload', {}))
        
        return {
            "id": raw_msg.get('id'),
            "source": "gmail",
            "source_id": self.source_id,
            "metadata": metadata,
            "body": body.strip() if body else ""
        }

    def _extract_body(self, payload: Dict[str, Any]) -> str:
        """
        Purpose: Recursively decodes base64 body content.

        Args:
            payload (Dict[str, Any]): Gmail payload part.

        Returns:
            str: Decoded body string.
        """
        if 'data' in payload.get('body', {}):
            return base64.urlsafe_b64decode(payload['body']['data']).decode('utf-8')
        
        if 'parts' in payload:
            for part in payload['parts']:
                body = self._extract_body(part)
                if body:
                    return body
        return ""