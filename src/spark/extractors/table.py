import logging
from pyspark.sql import SparkSession, DataFrame

logger = logging.getLogger(__name__)

class TableExtractor:
    """
    SOLID Extractor: Handles lazy loading for any file format (CSV, Parquet, JSON) 
    from S3/Local storage using Spark.
    """
    def __init__(self, connection: SparkSession, config: dict):
        """
        :param connection: The active SparkSession (from SparkConnector)
        :param config: Dictionary containing 'path', 'format', and 'options'
        """
        self.connection = connection
        self.config = config
        logger.debug("TableExtractor initialized.")

    def __call__(self) -> DataFrame:
        """Allows the extractor to be called directly: extractor()"""
        return self.extract()

    def extract(self) -> DataFrame:
        """
        Sets partition strategy and returns a Spark DataFrame (Lazy Blueprint).
        """
        # 1. Get configuration from YAML, set defaults if missing
        s3_path = self.config.get("path")
        format_type = self.config.get("format", "csv") # Supports 'csv', 'parquet', 'json'
        batch_size_mb = self.config.get("batch_size_mb", 20)
        
        if not s3_path:
            raise ValueError("Configuration 'path' is required for extraction.")

        # 2. Standardize protocol for Spark (s3a is required for Hadoop-AWS)
        s3_path = s3_path.replace("s3://", "s3a://")

        # 3. Tuning: Set 'maxPartitionBytes' to control Slave memory usage
        # This splits large files into smaller chunks for parallel processing
        size_in_bytes = int(batch_size_mb) * 1024 * 1024
        self.connection.conf.set("spark.sql.files.maxPartitionBytes", str(size_in_bytes))

        logger.info(f"Preparing lazy read ({format_type}) from {s3_path}")

        # 4. Dynamic Reader: Apply format-specific options (header, delimiter, etc.)
        # This replaces hardcoded .option("header", "true")
        reader = self.connection.read
        custom_options = self.config.get("options", {})
        
        for key, value in custom_options.items():
            reader = reader.option(key, str(value))

        # 5. Return the DataFrame
        # getattr(reader, 'csv')(path) is the same as reader.csv(path)
        # This allows the format to change without changing the code.
        try:
            df = getattr(reader, format_type)(s3_path)
            return df
        except AttributeError:
            raise ValueError(f"Unsupported Spark format: {format_type}")