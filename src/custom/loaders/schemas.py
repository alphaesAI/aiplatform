from pydantic import BaseModel, ConfigDict, Field
from typing import Dict, Any

class IngestorConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    index_name: str
    settings: Dict[str, Any] = Field(default_factory=dict)
    mappings: Dict[str, Any] = Field(default_factory=dict)