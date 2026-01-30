from pydantic import BaseModel

class GmailExtractorConfig(BaseModel):

    query: str
    batch_size: int
    extraction_mode: str
    fields: list[str]