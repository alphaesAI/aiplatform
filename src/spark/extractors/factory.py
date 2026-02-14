from .table import TableExtractor
from pyspark.sql import SparkSession

class ExtractorFactory:
    @staticmethod
    def create(type: str, connection: SparkSession, config: dict):
        if type == "table":
            return TableExtractor(connection, config)
        else:
            raise ValueError(f"Unknown extractor type: {type}")