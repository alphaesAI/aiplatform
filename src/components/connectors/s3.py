# pyspark + s3 connection

import logging
from pyspark.sql import SparkSession
from .schemas import S3Config

logger = logging.getLogger(__name__)

class S3Connector:
    def __init__(self, config: dict):
        # This config comes from your AirflowCredentials provider
        self.config = S3Config(**config)
        self._spark = None

    def __call__(self):
        return self.connect()

    def connect(self) -> SparkSession:
        logger.info("Initializing Spark 4.1 Session (Protocol-Stripped)")
        try:
            # Maven coordinates
            packages = [
                "org.apache.hadoop:hadoop-aws:3.4.1",                   # Allows Spark to "talk" to S3 via the s3a:// protocol
                "com.amazonaws:aws-java-sdk-bundle:1.12.767",           # The official AWS library required by Hadoop to handle security and S3 requests
                "org.elasticsearch:elasticsearch-spark-30_2.13:8.14.0"  # Enables Spark to read from or write data directly to Elasticsearch
            ]

            # Clean the host: remove https:// and trailing slashes
            raw_host = self.config.host or ""
            clean_host = raw_host.replace("https://", "").replace("http://", "").rstrip("/")

            # s3a:// prefix to the Hadoop S3A file system class.
            builder = SparkSession.builder.appName("S3SparkConnector") \
                .config("spark.jars.packages", ",".join(packages)) \
                .config("spark.hadoop.fs.s3a.access.key", self.config.login) \
                .config("spark.hadoop.fs.s3a.secret.key", self.config.password) \
                .config("spark.hadoop.fs.s3a.impl", "org.apache.hadoop.fs.s3a.S3AFileSystem") \
                .config("spark.hadoop.fs.s3a.aws.credentials.provider", "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider") \
                .config("spark.hadoop.fs.s3a.committer.name", "magic") \
                .config("spark.hadoop.fs.s3a.committer.magic.enabled", "true") \
                .config("spark.hadoop.fs.s3a.fast.upload", "true")

            if clean_host:
                builder.config("spark.hadoop.fs.s3a.endpoint", clean_host)      # URL of the storage service (e.g., s3.amazonaws.com
                # Only use path-style for MinIO/Private Cloud; AWS prefers virtual-host
                if "amazonaws.com" not in clean_host:
                    builder.config("spark.hadoop.fs.s3a.path.style.access", "true") # Forces endpoint/bucket format instead of bucket.endpoint (required for MinIO)
            
            self._spark = builder.getOrCreate()
            return self._spark

        except Exception as e:
            logger.exception("Spark 4.1 connection logic failed.")
            raise