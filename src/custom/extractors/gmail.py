import base64
from typing import Dict, Any, List, Iterator

class GmailExtractor:
    def __init__(self, connection, config: Dict[str, Any]):
        self.service = connection
        self.config = config

    def __call__(self):
        return self.extract()

    def extract(self):
        message_ids = self._get_message_ids()

        for msg_id in message_ids:
            raw_msg = self.service.users().messages().get(
                userId='me', id=msg_id, format=self.config.get('extraction_mode')
            ).execute()

            yield self._normalize_message(raw_msg)
        
    def _get_message_ids(self) -> List[str]:
        query = self.config.get('query')
        batch_size = self.config.get('batch_size')

        result = self.service.users().messages().list(
            userId='me', q=query, maxResults=batch_size
        ).execute()

        messages = result.get('messages', [])
        return [m['id'] for m in messages]

    def _normalize_message(self, raw_msg: Dict[str, Any]) -> Dict[str, Any]:
        headers = raw_msg.get('payload', {}).get('headers', [])
        # Whitelist specified in your YAML
        allowed_headers = self.config.get('fields', ['subject', 'from', 'date'])
        
        metadata = {h['name'].lower(): h['value'] for h in headers 
                    if h['name'].lower() in allowed_headers}

        body = self._extract_body(raw_msg.get('payload', {}))
        
        return {
            "id": raw_msg.get('id'), # This is the Gmail Internal ID
            "source": "gmail",
            "metadata": metadata,
            "body": body.strip() if body else ""
        }

    def _extract_body(self, payload: Dict[str, Any]) -> str:
            """Recursively finds and decodes the email body."""
            if 'data' in payload.get('body', {}):
                return base64.urlsafe_b64decode(payload['body']['data']).decode('utf-8')
            
            if 'parts' in payload:
                for part in payload['parts']:
                    body = self._extract_body(part)
                    if body:
                        return body
            return ""
            