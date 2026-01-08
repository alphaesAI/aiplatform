from pydantic import BaseModel, ConfigDict, Field
from typing import Dict, Any, Optional

class IngestorConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    index_name: str
    settings: Dict[str, Any] = Field(default_factory=dict)
    mappings: Dict[str, Any] = Field(default_factory=dict)

class EmbeddingsConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    # Default model if none is provided in Airflow
    path: str = "sentence-transformers/all-MiniLM-L6-v2"
    content: bool = False
    backend: str = "numpy"