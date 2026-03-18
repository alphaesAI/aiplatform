import logging
from pyspark.sql import DataFrame
from pyspark.sql.functions import col, lower, element_at, split
""" 
    col: references df columns like "path",
    element_at: gets element at specific position from array
"""
from .docs import DocStrategy
from .images import ImageStrategy

logger = logging.getLogger(__name__)

class PharmaTransformer:
    def __init__(self, df: DataFrame, config: dict):
        self.df = df
        self.config = config
        # Define file type categories
        img_types = ['png', 'jpg', 'jpeg', 'gif', 'bmp', 'tiff']
        doc_types = ['pdf', 'docx', 'doc', 'txt', 'rtf', 'html', 'htm', 'md', 'csv', 'json', 'xml', 'xlsx', 'pptx']
        
        # Map categories to strategies
        self.strategies = {}
        for img_type in img_types:
            self.strategies[img_type] = ImageStrategy()
        for doc_type in doc_types:
            self.strategies[doc_type] = DocStrategy()

    def transform(self) -> DataFrame:
        # 1. Identify File Extension
        df_typed = self.df.withColumn(
            "file_type", 
            lower(element_at(split(col("path"), "\."), -1))
        )

        # 2. Group by type and Process
        """ 
            distinct() : gets unique file types (removes duplicates);
            collect() : executes the operation and returns a list of rows
        """
        distinct_types = [row.file_type for row in df_typed.select("file_type").distinct().collect()]
        processed_dfs = []

        for f_type in distinct_types:
            type_df = df_typed.filter(col("file_type") == f_type)
            handler = self.strategies.get(f_type, DocStrategy()) # Default to Doc
            
            logger.info(f"Processing {f_type} using {handler.__class__.__name__}")
            processed_dfs.append(handler.execute(type_df, self.config))

        # 3. Union results
        final_df = processed_dfs[0]
        for next_df in processed_dfs[1:]:
            final_df = final_df.union(next_df)

        return final_df.select("path", "text")