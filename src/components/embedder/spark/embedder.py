# src/custom/transformers/spark/embedder.py
from pyspark.sql.functions import udf, col, concat_ws
from pyspark.sql.types import ArrayType, FloatType, StringType
from sentence_transformers import SentenceTransformer

class SparkEmbedder:
    def __init__(self, data, config: dict):
        self.data = data  # This is the Spark DataFrame
        self.config = config
        self.model_name = config.get("model_name", "all-MiniLM-L6-v2")

    def __call__(self, spark):
        # We need the spark session to handle the broadcast
        return self.apply_embedding(spark, self.data)

    def apply_embedding(self, spark, df):
        all_columns = df.columns
        model = SentenceTransformer(self.model_name)
        
        # Broadcast the model weights to the 47 partitions
        sc = spark.sparkContext
        broadcast_model = sc.broadcast(model)

        def embed_func(text):
            if not text: return []
            return broadcast_model.value.encode(text).tolist()

        spark_udf = udf(embed_func, ArrayType(FloatType()))

        temp_df = df.withColumn(
            "combined_text", 
            concat_ws(" ", *[col(c).cast(StringType()) for c in all_columns])
        )
        
        output_col = self.config.get("output_column", "row_vector")
        return temp_df.withColumn(output_col, spark_udf(col("combined_text"))).drop("combined_text")