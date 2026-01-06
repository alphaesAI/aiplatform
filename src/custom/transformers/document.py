import logging
from typing import Dict, Any, Iterator, List
from .base import BaseTransformer

logger = logging.getLogger(__name__)

"""
document.py
====================================
Purpose:
    Handles unstructured data (like Gmail body text) by extracting clean text 
    and automatically segmenting it into searchable chunks using txtai.
"""

class DocumentTransformer(BaseTransformer):
    """
    Purpose: 
        A specialized transformer for processing unstructured text documents.
        It performs both text extraction and logical segmentation (chunking).
    """

    def __init__(self, data: Any, config: Dict[str, Any]):
        """
        Purpose: Initializes the transformer and the txtai Textractor pipeline.
        
        Args:
            data (Any): Raw record or list of records (e.g., extracted Gmail data).
            config (Dict[str, Any]): Config containing 'textractor' and 'segmentation' settings.
        """
        super().__init__(config)
        
        # Lazy import to prevent environment overhead during orchestration startup
        from txtai.pipeline.data import Textractor

        self.data = data
        textractor_cfg = config.get("textractor", {})
        segmentation_cfg = config.get("segmentation", {})
        
        logger.info("Initializing txtai Textractor for document segmentation.")
        self.textractor = Textractor(
            **textractor_cfg,
            **segmentation_cfg
        )

    def __call__(self) -> Iterator[Dict[str, Any]]:
        """
        Purpose: Executes transformation and segmentation for all records.
        
        Returns:
            Iterator[Dict[str, Any]]: A generator yielding individual chunks 
                                      formatted for the Loader.
        """
        records = self.data if isinstance(self.data, list) else [self.data]
        logger.info(f"Starting document transformation for {len(records)} records.")
        
        for record in records:
            body_text = record.get("body", "")
            if not body_text:
                logger.warning(f"Record {record.get('id')} has an empty body. Skipping.")
                continue

            # Automatic Extraction & Segmentation
            chunks = self.textractor(body_text)
            
            for i, chunk in enumerate(chunks):
                chunk_dict = {
                    "id": f"{record.get('id')}#chunk{i}",
                    "text": chunk,
                    "source_id": record.get("source_id"),
                    "metadata": record.get("metadata", {})
                }
                yield self.transform(chunk_dict)