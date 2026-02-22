import os
from dotenv import load_dotenv
import logging

# Set up logger
logger = logging.getLogger(__name__)

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