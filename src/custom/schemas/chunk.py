from pydantic import BaseModel, Field

class ChunkMetadata(BaseModel):
    chunk_index: int
    section_title: str
    word_count: int
    start_char: int
    end_char: int
    overlap_with_previous: int
    overlap_with_next: int

class TextChunk(BaseModel):
    text: str
    arxiv_id: str
    metadata: ChunkMetadata