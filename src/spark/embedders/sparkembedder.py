import logging
from typing import Iterator
import pandas as pd
from pyspark.sql import DataFrame
from pyspark.sql.types import ArrayType, FloatType
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)

class SparkEmbedder:
    def __init__(self, data: DataFrame, config: dict):
        self.data = data
        self.config = config
        self.model_name = config.get("model_name", "all-MiniLM-L6-v2")
        self.output_col = config.get("output_column", "row_vector")

    def embed(self) -> DataFrame:
        # 1. Capture variables for the closure
        model_name = self.model_name
        output_col = self.output_col
        
        # 2. Define the schema of the result
        # mapInPandas requires us to define the schema we are returning
        output_schema = self.data.schema.add(output_col, ArrayType(FloatType()))

        # 3. The Map Function (Runs on Workers)
        def embed_batches(iterator: Iterator[pd.DataFrame]) -> Iterator[pd.DataFrame]:
            # --- SETUP: Runs ONCE per worker process ---
            logger.info(f"Loading model {model_name} on worker...")
            model = SentenceTransformer(model_name)
            
            for batch_df in iterator:
                # --- EXECUTION: Runs for every batch ---
                # Generate embeddings for the 'text' column values
                embeddings = model.encode(
                    batch_df["text"].tolist(), 
                    show_progress_bar=False
                )
                
                # Add the vectors as a new column in the Pandas DataFrame
                batch_df[output_col] = embeddings.tolist()
                
                # Yield the updated batch back to Spark
                yield batch_df

        # 4. Trigger the distributed transformation
        return self.data.mapInPandas(embed_batches, schema=output_schema)