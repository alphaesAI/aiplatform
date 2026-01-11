import logging
import re
from typing import List, Optional, Dict, Any, Iterator

from .base import BaseTransformer
from src.custom.schemas import PdfContent, ChunkMetadata, TextChunk

logger = logging.getLogger(__name__)

class TextChunker(BaseTransformer):
    """
    Transformer Layer — Text Chunker
    
    Purpose:
        Takes structured PdfContent and slices it into smaller, 
        overlapping TextChunks for AI/Vector search.
    """

    def __init__(self, data: List[PdfContent], config: Dict[str, Any]):
        """
        Initialize with parsed PDF objects and chunking settings.
        """
        super().__init__(config)
        self.parsed_pdfs = data
        
        # Pull settings from config or use defaults
        chunk_cfg = config.get("chunking", {})
        self.chunk_size = chunk_cfg.get("chunk_size", 600)
        self.overlap_size = chunk_cfg.get("overlap_size", 120)
        self.min_chunk_size = chunk_cfg.get("min_chunk_size", 120)

        if self.overlap_size >= self.chunk_size:
            raise ValueError("Overlap must be smaller than chunk size")

    def __call__(self) -> Iterator[Dict[str, Any]]:
        """
        Entry point: Processes all PDFs and yields cleaned, indexed chunks.
        """
        for pdf in self.parsed_pdfs:
            # 1. Decide if we use section-aware or raw chunking
            chunks = self._chunk_pdf(pdf)
            
            for chunk in chunks:
                # 2. Standardize via BaseTransformer.transform
                yield self.transform(chunk.model_dump())

    def _chunk_pdf(self, pdf: PdfContent) -> List[TextChunk]:
        """Core logic to route chunking style."""
        # Use generic metadata source if arxiv_id is missing
        source_id = pdf.metadata.get("arxiv_id") or pdf.metadata.get("source_file", "unknown")

        if pdf.sections:
            return self._chunk_by_sections(pdf, source_id)
        
        return self._chunk_raw_text(pdf.raw_text, source_id)

    def _chunk_by_sections(self, pdf: PdfContent, source_id: str) -> List[TextChunk]:
        """Slices text while respecting section headers."""
        chunks = []
        buffer = []
        buffer_wc = 0
        idx = 0

        for sec in pdf.sections:
            text = (sec.content or "").strip()
            words = re.findall(r"\S+", text)
            wc = len(words)

            # Case 1: Tiny section -> buffer it to merge with the next one
            if wc < 100:
                buffer.append(text)
                buffer_wc += wc
                continue

            # Flush buffer if we have one before starting this section
            if buffer:
                combined = "\n\n".join(buffer)
                chunks.append(self._build_chunk(combined, idx, source_id, "Combined Intro/Small Sections"))
                idx += 1
                buffer, buffer_wc = [], 0

            # Case 2: Perfect size -> one chunk
            if 100 <= wc <= 800:
                chunks.append(self._build_chunk(text, idx, source_id, sec.title))
                idx += 1
            
            # Case 3: Huge section -> slice with overlap
            else:
                split_chunks = self._chunk_raw_text(text, source_id, idx, sec.title)
                chunks.extend(split_chunks)
                idx += len(split_chunks)

        # Final flush
        if buffer:
            chunks.append(self._build_chunk("\n\n".join(buffer), idx, source_id, "Trailing Sections"))
        
        return chunks

    def _chunk_raw_text(self, text: str, source_id: str, start_idx: int = 0, section_title: str = "Body") -> List[TextChunk]:
        """Fallback: Slides a window across text regardless of sections."""
        words = re.findall(r"\S+", text)
        
        if len(words) <= self.min_chunk_size:
            return [self._build_chunk(text, start_idx, source_id, section_title)]

        chunks = []
        pos = 0
        current_idx = start_idx

        while pos < len(words):
            end = min(pos + self.chunk_size, len(words))
            part = " ".join(words[pos:end])
            
            chunks.append(self._build_chunk(part, current_idx, source_id, section_title))
            current_idx += 1
            # Move position forward by (Size - Overlap)
            pos += (self.chunk_size - self.overlap_size)

        return chunks

    def _build_chunk(self, text: str, idx: int, source_id: str, section_title: str) -> TextChunk:
        """Constructs the final Pydantic object."""
        return TextChunk(
            text=text,
            arxiv_id=source_id,
            metadata=ChunkMetadata(
                chunk_index=idx,
                section_title=section_title,
                word_count=len(text.split()),
                start_char=0, # Simplified
                end_char=len(text),
                overlap_with_previous=self.overlap_size if idx > 0 else 0,
                overlap_with_next=self.overlap_size
            )
        )