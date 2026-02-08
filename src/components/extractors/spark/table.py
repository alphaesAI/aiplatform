import logging
from pyspark.sql import SparkSession

logger = logging.getLogger(__name__)

class CSVExtractor:
    """
    Handles the 'Lazy' extraction of CSV data from S3 using an 
    existing Spark connection provided by the Connector.
    """
    def __init__(self, connection: SparkSession, config: dict):
        """
        :param connection: The active SparkSession (from ConnectorFactory)
        :param config: Dictionary containing 's3_path' and 'batch_size_mb'
        """
        self.connection = connection
        self.config = config
        logger.debug("CSVExtractor initialized with Spark connection.")

    def __call__(self):
        """Allows the extractor to be called directly: extractor()"""
        return self.extract()

    def extract(self):
        """
        Sets the partition strategy and returns a DataFrame blueprint.
        """
        s3_path = self.config.get("s3_path")
        if not s3_path:
            raise ValueError("s3_path is missing from the configuration.")
            
        # Ensure path uses s3a for Spark compatibility
        if s3_path.startswith("s3://"):
            s3_path = s3_path.replace("s3://", "s3a://")

        # 1. Configure the 'Tungsten' Chunk Size
        batch_size_mb = self.config.get("batch_size_mb", 20)
        size_in_bytes = int(batch_size_mb) * 1024 * 1024
        
        # This tells the Executors how much data to hold in RAM at once
        self.connection.conf.set("spark.sql.files.maxPartitionBytes", str(size_in_bytes))

        logger.info(f"Preparing lazy read from {s3_path} with {batch_size_mb}MB partitions.")

        # 2. Define the Read Logic (Lazy Blueprint)
        # Spark only checks the file structure; it doesn't download rows yet.
        df = self.connection.read \
            .option("header", "true") \
            .option("inferSchema", "true") \
            .csv(s3_path)

        return df