import logging
from typing import Dict, Any, Iterator, List
from .base import BaseTransformer

logger = logging.getLogger(__name__)

"""
json_transformer.py
====================================
Purpose:
    Specialized transformer for structured data (like RDBMS results). 
    Standardizes records for bulk indexing.
"""

class JsonTransformer(BaseTransformer):
    """
    Purpose: 
        Processes structured multi-table data into a format suitable 
        for bulk indexing using shared base logic.
    """

    def __init__(self, data: Dict[str, List[Dict[str, Any]]], config: Dict[str, Any]):
        """
        Purpose: Initializes the transformer with the dataset and configuration.

        Args:
            data (Dict[str, List[Dict[str, Any]]]): Raw data organized by table names.
            config (Dict[str, Any]): Configuration containing 'index_name'.
        """
        super().__init__(config)
        self.data = data

    def __call__(self) -> Iterator[Dict[str, Any]]:
        """
        Purpose: Processes the structured data through the transformation logic.

        Returns:
            Iterator[Dict[str, Any]]: A generator yielding indexed-ready records.
        """
        if not self.data:
            logger.warning("JsonTransformer received empty data.")
            return

        for table_name, rows in self.data.items():
            logger.info(f"Transforming table: {table_name} ({len(rows)} rows)")
            for row in rows:
                yield self.transform(row)