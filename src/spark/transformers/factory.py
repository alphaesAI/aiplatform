from .table import TableTransformer
from pyspark.sql import DataFrame

class TransformerFactory:
    @staticmethod
    def create(type: str, data: DataFrame, config: dict):
        if type == "table":
            return TableTransformer(data, config)
        else:
            raise ValueError(f"Unknown transformer type: {type}")