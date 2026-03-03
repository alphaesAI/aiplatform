from .sparkembedder import SparkEmbedder
from pyspark.sql import DataFrame

class EmbedderFactory:
    @staticmethod
    def create(type: str, data: DataFrame, config: dict):
        if type == "spark":
            return SparkEmbedder(data, config)
        else:
            raise ValueError(f"Unknown embedder type: {type}")