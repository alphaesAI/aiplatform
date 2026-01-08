from pydantic import BaseModel, ConfigDict, Field
from typing import List, Optional

class GmailExtractorConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    query: str
    batch_size: int
    extraction_mode: str
    fields: List[str] = Field(default_factory=list)

class RDBMSTableConfig(BaseModel):
    """Schema for a single table entry in RDBMS"""
    model_config = ConfigDict(extra="allow")

    table_name: str
    schema_name: str = Field(default="public", alias="schema")
    columns: Optional[List[str]] = Field(default_factory=list)

class RDBMSExtractorConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    
    # This validates that 'tables' is a list of RDBMSTableConfig objects
    tables: List[RDBMSTableConfig]