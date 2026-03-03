import logging

from pyspark.sql import DataFrame
from pyspark.sql.functions import col, lower, trim, concat_ws, struct

logger = logging.getLogger(__name__)

class TableTransformer:
    def __init__(self, data: DataFrame, config: dict):
        self.config = config 
        self.data = data

    def __call__(self):
        return self.transform()

    def transform(self, data: DataFrame = None, config: dict = None) -> DataFrame:
        df = data if data is not None else self.data
        cfg = config if config is not None else self.config
    
        # 1. Dynamic Drop (last column)
        df = df.drop(df.columns[-1])

        # 2. Preparation
        target_columns = set(cfg.get("normalize_columns") or [])
        id_col_name = cfg.get("id_column") or df.columns[0]
    
        # 3. Single-Pass Transformation Logic
        # We build a list of expressions to avoid multiple withColumn calls
        select_columns = []
        for c in df.columns:
            if c in target_columns:
                select_columns.append(lower(trim(col(c))).alias(c))
            else:
                select_columns.append(col(c))
            
        df_normalized = df.select(*select_columns)

        # 4. Final Projection
        # Note: Excluding id/text from metadata to save space
        metadata_cols = [c for c in df_normalized.columns if c not in [id_col_name]]
        
        # Create a universal text field by concatenating ALL columns
        # This makes the pipeline work with any dataset
        text_col = concat_ws(" ", *[col(c).cast("string") for c in metadata_cols])
        
        # Debug: Log available columns
        logger.info(f"Available columns after transformation: {df_normalized.columns}")
        logger.info(f"Creating universal text field from all columns")
        
        return df_normalized.select(
            col(id_col_name).alias("id"),
            text_col.alias("text"),
            struct(*metadata_cols).alias("metadata")
        )