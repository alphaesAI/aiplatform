"""
Module: Gmail Extractor
Purpose: Provides a specialized class to extract unstructured email data 
from the Gmail API. It handles authentication via a provided connector, 
discovers the monitored account profile automatically, and normalizes 
raw API responses into a standard dictionary format.
"""

import base64
from typing import Dict, Any, List, Iterator

class GmailExtractor:
    """
    Purpose: Extracts and normalizes email data from a specific Gmail account.
    This class acts as an iterator that yields cleaned email records.
    """

    def __init__(self, connection: Any, config: Dict[str, Any]):
        """
        Purpose: Initializes the extractor and identifies the monitored account.
        
        Args:
            connection (Any): The authenticated Google Resource service object.
            config (Dict[str, Any]): Configuration dictionary containing query, 
                                     batch_size, extraction_mode, and fields.
        """
        self.service = connection
        self.config: Dict[str, Any] = config
        
        # Production Level: Fetch the email address once during initialization
        profile = self.service.users().getProfile(userId='me').execute()
        self.source_id: str = profile.get('emailAddress', 'unknown_account')

    def __call__(self) -> Iterator[Dict[str, Any]]:
        """
        Purpose: Allows the class to be called as a function to start extraction.
        
        Returns:
            Iterator[Dict[str, Any]]: A generator yielding normalized email records.
        """
        return self.extract()

    def extract(self) -> Iterator[Dict[str, Any]]:
        """
        Purpose: Orchestrates the retrieval and normalization process.
        
        Yields:
            Dict[str, Any]: A normalized dictionary representing a single email.
        """
        message_ids: List[str] = self._get_message_ids()

        for msg_id in message_ids:
            raw_msg: Dict[str, Any] = self.service.users().messages().get(
                userId='me', 
                id=msg_id, 
                format=self.config.get('extraction_mode')
            ).execute()

            yield self._normalize_message(raw_msg)
        
    def _get_message_ids(self) -> List[str]:
        """
        Purpose: Fetches a list of message IDs matching the query from the configuration.
        
        Returns:
            List[str]: A list of Gmail internal message IDs.
        """
        query: str = self.config.get('query')
        batch_size: int = self.config.get('batch_size')

        result = self.service.users().messages().list(
            userId='me', q=query, maxResults=batch_size
        ).execute()

        messages: List[Dict[str, str]] = result.get('messages', [])
        return [m['id'] for m in messages]

    def _normalize_message(self, raw_msg: Dict[str, Any]) -> Dict[str, Any]:
        """
        Purpose: Transforms raw API response into a flattened, standard dictionary.
        
        Args:
            raw_msg (Dict[str, Any]): The raw dictionary returned by the Gmail API.
            
        Returns:
            Dict[str, Any]: A dictionary containing id, source, source_id, metadata, and body.
        """
        headers: List[Dict[str, str]] = raw_msg.get('payload', {}).get('headers', [])
        allowed_headers: List[str] = self.config.get('fields')
        
        # Build metadata from whitelisted headers
        metadata: Dict[str, str] = {
            h['name'].lower(): h['value'] 
            for h in headers 
            if h['name'].lower() in allowed_headers
        }

        body: str = self._extract_body(raw_msg.get('payload', {}))
        
        return {
            "id": raw_msg.get('id'),
            "source": "gmail",
            "source_id": self.source_id,
            "metadata": metadata,
            "body": body.strip() if body else ""
        }

    def _extract_body(self, payload: Dict[str, Any]) -> str:
        """
        Purpose: Recursively traverses the email payload to find and decode the body content.
        
        Args:
            payload (Dict[str, Any]): The payload part of the Gmail message.
            
        Returns:
            str: The decoded UTF-8 string of the email body.
        """
        # Case 1: Body data is directly in this part
        if 'data' in payload.get('body', {}):
            return base64.urlsafe_b64decode(payload['body']['data']).decode('utf-8')
        
        # Case 2: Body is nested within parts (multipart emails)
        if 'parts' in payload:
            for part in payload['parts']:
                body: str = self._extract_body(part)
                if body:
                    return body
        return ""