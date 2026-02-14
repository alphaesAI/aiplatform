from pyspark.sql import DataFrame
from pyspark.sql.functions import col, lower, trim, concat_ws, struct

class TableTransformer:
    def __init__(self, data: DataFrame, config: dict):
        self.config = config 
        self.data = data

    def __call__(self):
        return self.transform()

    def transform(self, data: DataFrame = None, config: dict = None) -> DataFrame:
        df = data if data is not None else self.data
        cfg = config if config is not None else self.config
        
        # 1. Drop Ghost Columns
        df = df.drop("_c21")

        # 2. Optional Normalization (Works even if list is empty or None)
        target_columns = cfg.get("normalize_columns") or []
        for column in target_columns:
            if column in df.columns:
                df = df.withColumn(column, lower(trim(col(column))))

        # 3. Dynamic ID Discovery (Fallback to first column)
        id_col_name = cfg.get("id_column") or df.columns[0]
        
        # 4. Dynamic Text Concatenation (Cast everything to String)
        text_cols = [col(c).cast("string") for c in df.columns if c != id_col_name]

        # 5. Return Standardized Structure
        return df.select(
            col(id_col_name).alias("id"),
            concat_ws(" ", *text_cols).alias("text"),
            struct("*").alias("metadata")
        )