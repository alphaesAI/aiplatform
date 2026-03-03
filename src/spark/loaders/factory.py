from .elasticsearch import ElasticsearchSparkLoader
from pyspark.sql import DataFrame

class LoaderFactory:
    @staticmethod
    def create(type: str, data: DataFrame, config: dict):
        if type == "elasticsearch":
            return ElasticsearchSparkLoader(data, config)
        else:
            raise ValueError(f"Unknown loader type: {type}")