from .credentials import CONNECTIONS
from ..base import CredentialProvider

class LocalCredentialProvider(CredentialProvider):
    def __init__(self, conn_id: str):
        self.conn_id = conn_id
        self.config = CONNECTIONS

    def get_credentials(self):
        if self.conn_id in self.config:
            return self.config[self.conn_id]
        else:
            raise ValueError(f"Invalid connection id: {self.conn_id}")