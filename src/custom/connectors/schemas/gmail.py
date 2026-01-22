from pydantic import BaseModel
from typing import Dict, Any, List

class GmailConfig(BaseModel):
    """
    Purpose:
        Configuration schema for Gmail connections.
    """
    token_dict: Dict[str, Any]
    scopes: List[str]  # This allows you to pass ['https://www.googleapis.com/auth/gmail.readonly']