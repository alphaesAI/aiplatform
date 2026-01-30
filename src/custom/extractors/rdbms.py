# Note: need to update the rows to generator

import logging
from sqlalchemy import text
from typing import Dict, Any, List
from .base import BaseExtractor
from .schemas import RDBMSExtractorConfig

logger = logging.getLogger(__name__)

"""
rdbms.py
====================================
Purpose:
    Extracts tabular data from relational databases using SQLAlchemy.
"""

class RDBMSExtractor(BaseExtractor):
    """
    Purpose: 
        Performs bulk extraction from one or more database tables based 
        on a provided schema and column configuration.
    """

    def __init__(self, connection: Any, config: Dict[str, Any]):
        """
        Purpose: Initializes the extractor with a database connection.

        Args:
            connection (Any): SQLAlchemy connection object.
            config (Dict[str, Any]): Config containing a list of 'tables'.
        """
        self.connection = connection
        self.config = RDBMSExtractorConfig(**config)

    def __call__(self) -> Dict[str, List[Dict[str, Any]]]:
        """
        Purpose: Executes the extract method.

        Returns:
            Dict[str, List[Dict[str, Any]]]: Results keyed by table name.
        """
        return self.extract()

    def extract(self) -> Dict[str, List[Dict[str, Any]]]:
        """
        Purpose: Loops through tables in config and executes SELECT queries.

        Returns:
            Dict[str, List[Dict[str, Any]]]: A map of table names to rows.
        """
        results = {}
        tables = self.config.tables

        for table in tables:
            name = table.table_name
            schema = table.schema
            cols = table.columns

            column_query = "*" if not cols else ", ".join(cols)
            query = f"SELECT {column_query} FROM {schema}.{name}"
            
            logger.info(f"Extracting data from {schema}.{name}")

            try:
                result_proxy = self.connection.execute(text(query))
                # mappings() allows dict-like access to row columns
                rows = [dict(row) for row in result_proxy.mappings()]
                results[name] = rows
                logger.debug(f"Successfully extracted {len(rows)} rows from {name}")
            
            except Exception as e:
                logger.exception(f"Error extracting {schema}.{name}")
                raise e
        
        return results
        