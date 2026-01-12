from pydantic import BaseModel, ConfigDict, Field
from typing import List, Optional, Dict

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

# --- 1. Downloader Specific Config (Infrastructure) ---
class ArxivDownloaderConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    
    download_dir: str = Field(default="/tmp/aiplatform/arxiv/downloads")
    rate_limit_delay: float = Field(default=3.0)
    max_retries: int = Field(default=3)
    retry_backoff: int = Field(default=2)

# --- 2. Extractor Specific Config (Logic) ---
class ArxivExtractorConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    base_url: str = Field(default="https://export.arxiv.org/api/query")
    search_category: str = Field(default="cs.AI")
    max_results: int = Field(default=10)
    namespaces: Dict[str, str] = Field(default_factory=lambda: {
        "atom": "http://www.w3.org/2005/Atom",
        "opensearch": "http://a9.com/-/spec/opensearch/1.1/",
        "arxiv": "http://arxiv.org/schemas/atom"
    })
    
    downloader: ArxivDownloaderConfig = Field(default_factory=ArxivDownloaderConfig)