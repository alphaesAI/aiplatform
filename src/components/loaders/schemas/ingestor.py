from pydantic import BaseModel
from typing import Dict, Any

class IngestorConfig(BaseModel):

    index_name: str
    settings: Dict[str, Any]
    mappings: Dict[str, Any]

    #NEW: ingestion strategy
    ingest_mode: str = "bulk" # bulk or single