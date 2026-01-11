from enum import Enum
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, Field

class ParserType(str, Enum):
    DOCLING = "docling"

class PaperSection(BaseModel):
    title: str
    content: str
    level: int = 1

class PaperFigure(BaseModel):
    caption: Optional[str] = None # Arxiv figures sometimes lack captions
    id: str

class PaperTable(BaseModel):
    id: str
    caption: Optional[str] = None
    title: Optional[str] = None
    content: Optional[str] = None
    metadata: Dict[str, Any] = Field(default_factory=dict)

class PdfContent(BaseModel):
    sections: List[PaperSection] = Field(default_factory=list)
    figures: List[PaperFigure] = Field(default_factory=list)
    tables: List[PaperTable] = Field(default_factory=list)
    raw_text: str
    references: List[str] = Field(default_factory=list)
    parser_used: ParserType
    metadata: Dict[str, Any] = Field(default_factory=dict)