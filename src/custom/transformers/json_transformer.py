"""
Module: JSON Transformer
Purpose: Specialized transformer for structured JSON or Dictionary data. 
It inherits from BaseTransformer to standardize and wrap structured records 
for the indexing script.
"""

from typing import Dict, Any, Iterator, List
from .base import BaseTransformer

class JsonTransformer(BaseTransformer):
    """
    Purpose: Processes structured multi-table data into a format suitable 
    for bulk indexing, utilizing shared base transformation logic.
    """

    def __init__(self, data: Dict[str, List[Dict[str, Any]]], config: Dict[str, Any]):
        """
        Purpose: Initializes the transformer with the dataset and configuration.

        Args:
            data (Dict[str, List[Dict[str, Any]]]): The raw data, typically organized by table names.
            config (Dict[str, Any]): Configuration containing 'index_name'.
        """
        super().__init__(config)
        self.data = data

    def __call__(self) -> Iterator[Dict[str, Any]]:
        """
        Purpose: Makes the class callable to execute the transformation process.

        Returns:
            Iterator[Dict[str, Any]]: A generator yielding indexed-ready records.
        """
        if not self.data:
            return

        for table_name, rows in self.data.items():
            for row in rows:
                # Use the shared logic from the parent BaseTransformer
                yield self.transform(row)