from pydantic import BaseModel, ConfigDict
from typing import Optional, List

class TableTransformerConfig(BaseModel):
    """
    Configuration schema for table data transformation.
    """
    model_config = ConfigDict(protected_namespaces=(), extra="forbid")
    
    # Column configuration
    id_column: Optional[str] = None  # Use first column if not specified
    normalize_columns: Optional[List[str]] = []  # Columns to normalize (lowercase, trim)
    
    """ Example configuration:
    config_data = {
        "id_column": "Row ID",
        "normalize_columns": ["Customer Name", "Product Name"]
    }
    """