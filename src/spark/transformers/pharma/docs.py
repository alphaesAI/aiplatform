from pyspark.sql.functions import udf, col
from pyspark.sql.types import StringType
from .base import TransformerStrategy

class DocStrategy(TransformerStrategy):
    def execute(self, df, config):
        @udf(returnType=StringType())
        def docling_extract(content_bytes):
            from docling.document_converter import DocumentConverter
            from io import BytesIO
            converter = DocumentConverter()
            result = converter.convert(BytesIO(content_bytes))
            return result.document.export_to_markdown()

        return df.withColumn("text", docling_extract(col("content")))