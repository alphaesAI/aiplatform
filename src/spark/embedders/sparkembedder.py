import logging
import pandas as pd
from pyspark.sql import DataFrame
from pyspark.sql.types import ArrayType, FloatType
from pyspark.sql.functions import pandas_udf
from sentence_transformers import SentenceTransformer
from .schemas.sparkembedder import SparkEmbedderConfig

logger = logging.getLogger(__name__)

class SparkEmbedder:
    def __init__(self, data: DataFrame, config: dict):
        self.data = data
        self.config = SparkEmbedderConfig(**config)
        self.model_name = self.config.model_name
        self.output_col = self.config.output_column

    def embed(self) -> DataFrame:
        # 1. Capture variables for the closure
        model_name = self.model_name
        output_col = self.output_col
        
        # 2. Create pandas UDF for distributed embedding generation
        @pandas_udf(ArrayType(FloatType()))
        def embed_udf(text_series: pd.Series) -> pd.Series:
            # This runs on workers, need to load model here
            model = SentenceTransformer(model_name)
            embeddings = model.encode(text_series.tolist(), show_progress_bar=False)
            return pd.Series(embeddings.tolist())
        
        # 3. Apply the UDF to add embedding column
        return self.data.withColumn(output_col, embed_udf(self.data["text"]))