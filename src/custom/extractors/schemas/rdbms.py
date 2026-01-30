from pydantic import BaseModel, ConfigDict
from typing import List, Optional

class RDBMSTableConfig(BaseModel):
    """Schema for a single table entry in RDBMS"""

    model_config = ConfigDict(protected_namespaces=())
    table_name : str
    schema: str
    columns: Optional[List[str]]

class RDBMSExtractorConfig(BaseModel):
    
    # This validates that 'tables' is a list of RDBMSTableConfig objects
    tables: List[RDBMSTableConfig]

    """ The config Dict will look like this
    config_data = {
        "tables": [
            {
                "table_name": "users",
                "schema": "public",
                "columns": ["id", "name"]  # List of strings
            },
            {
                "table_name": "orders",
                "schema": "sales",
                "columns": ["order_id", "total", "status"]
            }
        ]
    } 
    
    Here, it's not dict it's an pydantic object.
    """