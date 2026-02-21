from pydantic import BaseModel
from typing import Optional
class GmailExtractorConfig(BaseModel):

    query: str
    batch_size: int
    extraction_mode: str
    fields: list[str]
    #NEW: configurable attachment storage
    temp_dir: Optional[str] = None