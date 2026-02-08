from pyspark.sql import DataFrame
from pyspark.sql.functions import col, lower, trim
from ..base import BaseTransformer

class TableTransformer(BaseTransformer):
    def __init__(self, data: DataFrame, config: dict):
        # Pass ONLY config to BaseTransformer to avoid errors
        super().__init__(config) 
        self.data = data

    def __call__(self):
        return self.transform()

    def transform(self, data: DataFrame = None, config: dict = None) -> DataFrame:
        # Use the data passed during init if none is provided here
        df = data if data is not None else self.data
        cfg = config if config is not None else self.config
        
        target_columns = cfg.get("normalize_columns", [])
        for column in target_columns:
            if column in df.columns:
                df = df.withColumn(column, lower(trim(col(column))))
        
        # Drop the ghost column caused by trailing commas in the CSV
        df = df.drop("_c21")
        return df