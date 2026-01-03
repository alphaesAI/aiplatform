from google.oauth2.credentials import Credentials
from googleapiclient.discovery import build

class GmailConnector:
    def __init__(self, config: dict):
        self.config = config
        self._service = None

    def __call__(self):
        return self.connect()

    def connect(self):
        if not self._service:
            token_path = self.config.get("token_path")
            if not token_path:
                raise ValueError("token_path is missing from connector config")
                
            creds = Credentials.from_authorized_user_file(
                token_path, 
                scopes=['https://www.googleapis.com/auth/gmail.readonly']
            )
            self._service = build('gmail', 'v1', credentials=creds, cache_discovery=False)
        return self._service