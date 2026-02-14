import logging
import pandas as pd
from pyspark.sql.functions import pandas_udf, col
from pyspark.sql.types import ArrayType, FloatType
from sentence_transformers import SentenceTransformer

logger = logging.getLogger(__name__)

class SparkEmbedder:
    def __init__(self, data, config: dict):
        self.data = data
        self.config = config
        self.model_name = config.get("model_name", "all-MiniLM-L6-v2")

    def embed(self):
        """Matches the call in your DAG: embedder.embed()"""
        df = self.data
        model_name = self.model_name
        output_col = self.config.get("output_column", "row_vector")

        # Use Pandas UDF for much better performance
        @pandas_udf(ArrayType(FloatType()))
        def embed_func(texts: pd.Series) -> pd.Series:
            # Initialize model inside the worker to avoid serialization issues
            model = SentenceTransformer(model_name)
            embeddings = model.encode(texts.tolist(), show_progress_bar=False)
            return pd.Series(embeddings.tolist())

        logger.info(f"Generating vectors in column: {output_col}")
        final_df = df.withColumn(output_col, embed_func(col("text")))
        
        return final_df