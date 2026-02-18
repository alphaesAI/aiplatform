import os
from dotenv import load_dotenv
from ..base import CredentialProvider

# Load environment variables from .env file
load_dotenv(os.path.join(os.path.dirname(__file__), '.env'))

CONNECTIONS = {
    "rdbms": {
        "host": "localhost",
        "port": 5432,
        "database": "structured_data_pipeline",
        "login": "logi",
        "password": 12345,
    },
    "elasticsearch": {
        "host": "localhost",
        "port": 9200,
    },
    "s3": {
        "login": os.getenv("AWS_ACCESS_KEY_ID"),
        "password": os.getenv("AWS_SECRET_ACCESS_KEY"),
        "region_name": os.getenv("AWS_REGION", "us-east-1"),
        "host": os.getenv("HOST"),
        "bucket_name": os.getenv("S3_BUCKET_NAME"),
    }
}

# Keep original for backward compatibility
rdbms = CONNECTIONS["rdbms"]
elasticsearch = CONNECTIONS["elasticsearch"]
s3 = CONNECTIONS["s3"]

class LocalCredentialProvider(CredentialProvider):
    def __init__(self, conn_id: str):
        self.conn_id = conn_id
        self.config = CONNECTIONS

    def get_credentials(self):
        if self.conn_id in self.config:
            return self.config[self.conn_id]
        else:
            raise ValueError(f"Invalid connection id: {self.conn_id}")