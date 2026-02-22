from pydantic import BaseModel, ConfigDict
from typing import Optional, Dict, Any

class TableExtractorConfig(BaseModel):
    """
    Configuration schema for table data extraction from S3/Local storage.
    """
    model_config = ConfigDict(protected_namespaces=(), extra="forbid")
    
    # Required fields
    path: str  # S3 or local file path
    
    # Optional fields with defaults
    format: Optional[str] = "csv"  # csv, parquet, json
    batch_size_mb: Optional[int] = 20
    options: Optional[Dict[str, Any]] = {}
    
    """ Example configuration:
    config_data = {
        "path": "s3a://bucket/data.csv",
        "format": "csv",
        "batch_size_mb": 20,
        "options": {
            "header": "true",
            "inferSchema": "true",
            "delimiter": ","
        }
    }
    """