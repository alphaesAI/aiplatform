"""
Module: Document Transformer
Purpose: Handles unstructured data (like Gmail body text) by extracting clean text 
and automatically segmenting it into searchable chunks using txtai's Textractor.
"""

from typing import Dict, Any, Iterator
from .base import BaseTransformer

class DocumentTransformer(BaseTransformer):
    """
    Purpose: A specialized transformer for processing unstructured text documents.
    It utilizes txtai's Textractor to perform both text extraction (e.g., HTML to Markdown)
    and logical segmentation (e.g., sentence splitting) in a single pass.
    """

    def __init__(self, data: any, config: Dict[str, Any]):
        """
        Purpose: Initializes the transformer with data and configuration.
        
        Args:
            data (Any): Raw record or list of records (e.g., extracted Gmail data).
            config (Dict[str, Any]): Configuration dictionary containing 'textractor' 
                                     and 'segmentation' settings from YAML.
        """

        super().__init__(config)

        # Lazy import to prevent Airflow DAG import timeouts
        from txtai.pipeline.data import Textractor

        self.data = data
        
        # Pull both sections from YAML
        textractor_cfg = config.get("textractor", {})
        segmentation_cfg = config.get("segmentation", {})
        
        # Initialize Textractor with combined settings to enable automatic segmentation
        self.textractor = Textractor(
            **textractor_cfg,
            **segmentation_cfg  # This triggers the 'automatic' segmentation
        )

    def __call__(self) -> Iterator[Dict[str, Any]]:
        """
        Purpose: Executes the transformation and segmentation process for all records.
        
        Returns:
            Iterator[Dict[str, Any]]: A generator yielding individual chunks formatted 
                                      for indexing (including _index and _source).
        """
        # Ensure data is iterable (handles single dict or list of dicts)
        records = self.data if isinstance(self.data, list) else [self.data]
        
        for record in records:
            # 1. Automatic Extraction & Segmentation
            # Returns a list of strings based on the 'maxlength' and 'sentences' config
            chunks = self.textractor(record.get("body", ""))
            
            for i, chunk in enumerate(chunks):
                # 2. Build the intermediate clean_row dictionary
                chunk_dict = {
                    "id": f"{record.get('id')}#chunk{i}",
                    "text": chunk,
                    "source_id": record.get("source_id"),
                    "metadata": record.get("metadata", {})
                }

                # 3. Use the inherited BaseTransformer.transform() to wrap for the Loader
                yield self.transform(chunk_dict)