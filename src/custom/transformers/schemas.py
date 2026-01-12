from pydantic import BaseModel, ConfigDict, Field, model_validator
from typing import List, Dict, Any, Optional
from enum import Enum

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

class ParserType(str, Enum):
    DOCLING = "docling"
    PYPDF = "pypdf"

class PaperSection(BaseModel):
    title: str
    content: str

class PaperTable(BaseModel):
    id: str
    title: Optional[str] = None
    caption: str = "Extracted Table"
    content: str  # Markdown representation
    metadata: Dict[str, Any] = Field(default_factory=dict)

class PaperFigure(BaseModel):
    id: str
    caption: str = "Extracted Figure"

# --- NEW CONFIG SCHEMA ---

class DoclingConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    
    max_pages: int = Field(default=20)
    max_file_size_mb: int = Field(default=15)
    do_table_structure: bool = Field(default=True)
    do_ocr: bool = Field(default=False)
    max_concurrency: int = Field(default=4)

# Updated PdfContent to include metadata field used in your code
class PdfContent(BaseModel):
    sections: List[PaperSection] = Field(default_factory=list)
    figures: List[PaperFigure] = Field(default_factory=list)
    tables: List[PaperTable] = Field(default_factory=list)
    raw_text: str
    references: List[str] = Field(default_factory=list)
    parser_used: ParserType = ParserType.DOCLING
    metadata: Dict[str, Any] = Field(default_factory=dict)

class ChunkingConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)
    
    chunk_size: int = Field(default=600)
    overlap_size: int = Field(default=120)
    min_chunk_size: int = Field(default=120)

    # Simple Pydantic validation to prevent infinite loops
    @model_validator(mode='after')
    def check_overlap(self) -> 'ChunkingConfig':
        if self.overlap_size >= self.chunk_size:
            raise ValueError("overlap_size must be smaller than chunk_size")
        return self

class ChunkMetadata(BaseModel):
    """
    Detailed tracking for each slice of text.
    Helps the AI know exactly where this piece came from.
    """
    chunk_index: int
    section_title: Optional[str] = "Body"
    word_count: int
    start_char: int
    end_char: int
    overlap_with_previous: int
    overlap_with_next: int

class TextChunk(BaseModel):
    """
    The final 'unit' of data that gets embedded and indexed.
    """
    model_config = ConfigDict(populate_by_name=True)

    text: str
    metadata: ChunkMetadata
    arxiv_id: str  # This links the chunk back to the original paper