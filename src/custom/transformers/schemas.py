from pydantic import BaseModel, ConfigDict, Field
from typing import List, Dict, Any, Optional

# --- CONFIG SCHEMAS ---

class DocumentTransformerConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    
    # Nested configs for txtai
    textractor: Dict[str, Any] = Field(default_factory=dict)
    segmentation: Dict[str, Any] = Field(default_factory=dict)

class JsonTransformerConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    index_name: Optional[str] = None

class StructuredRecord(BaseModel):
    """
    Ensures that every SQL row has an ID. 
    The 'extra="allow"' setting lets it keep all other columns automatically.
    """
    model_config = ConfigDict(extra="allow") 
    id: Any
    
# --- DATA SCHEMAS (The "Contract") ---

class TransformerInputRecord(BaseModel):
    """The schema for what an Extractor MUST provide"""
    id: str
    source_id: str
    body: str = ""
    attachments: List[str] = Field(default_factory=list)
    metadata: Dict[str, Any] = Field(default_factory=dict)

class TransformerOutputChunk(BaseModel):
    """The schema for what goes into your Vector Database"""
    id: str
    text: str
    source_id: str
    metadata: Dict[str, Any]