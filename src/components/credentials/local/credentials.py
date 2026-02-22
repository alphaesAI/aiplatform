import logging
from ..base import CredentialProvider
from .config import CONNECTIONS

# Set up logger
logger = logging.getLogger(__name__)

class LocalCredentialProvider(CredentialProvider):
    def __init__(self, conn_id: str):
        self.conn_id = conn_id
        self.config = CONNECTIONS
        logger.info(f"Initialized LocalCredentialProvider for connection: {conn_id}")

    def get_credentials(self):
        if self.conn_id in self.config:
            return self.config[self.conn_id]
        else:
            error_msg = f"Invalid connection id: {self.conn_id}. Available connections: {list(self.config.keys())}"
            logger.error(error_msg)
            raise ValueError(error_msg)