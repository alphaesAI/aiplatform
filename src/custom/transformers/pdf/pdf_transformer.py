import logging
import asyncio
from pathlib import Path
from typing import Dict, Any, List, Optional

from ..base import BaseTransformer
from .engine import DoclingEngine  
from ..schemas import PdfContent

logger = logging.getLogger(__name__)

"""
pdf_transformer.py
====================================
Purpose:
    Acts as the high-level orchestrator for PDF processing. It manages 
    asynchronous concurrency, delegates parsing to the DoclingEngine, 
    and wraps the resulting structured data into index-ready formats.
"""

class PDFTransformer(BaseTransformer):
    """
    Purpose:
        Generic PDF Transformer Entry Point.
        Transforms local PDF files into standardized, index-ready records
        by delegating low-level parsing to DoclingEngine.
    """

    def __init__(self, data: List[Path], config: Dict[str, Any]):
        """
        Purpose:
            Initializes the transformer with a list of file paths and 
            configuration for the underlying parsing engine.

        Args:
            data (List[Path]): List of PDF file paths to process.
            config (Dict): Contains 'index_name' and 'docling' sub-config.
        """
        # Initialize BaseTransformer (handles index_name and basic cleaning)
        super().__init__(config)
        
        self.pdf_paths = data
        # Extract the docling sub-dictionary
        docling_dict = config.get("docling", {})
        
        # This validates everything (concurrency, ocr, etc.)
        self.docling_config = DoclingConfig(**docling_dict)
        
        self.engine = DoclingEngine(docling_dict)
        self.semaphore = asyncio.Semaphore(self.docling_config.max_concurrency)
    
    async def __call__(self) -> List[Dict[str, Any]]:
        """
        Purpose:
            Triggers the async transformation process and returns 
            standardized records for all provided PDFs.

        Returns:
            List[Dict[str, Any]]: A list of formatted records ready for Elasticsearch/OpenSearch.
        """
        if not self.pdf_paths:
            logger.warning("PdfTransformer received empty path list.")
            return []

        logger.info(f"Starting PDF transformation: {len(self.pdf_paths)} files")
        
        # Create tasks for parallel processing
        tasks = [self._process_single_pdf(p) for p in self.pdf_paths]
        
        # Gather results
        results = await asyncio.gather(*tasks, return_exceptions=True)
        
        final_records = []
        for res in results:
            if isinstance(res, dict):
                final_records.append(res)
            elif isinstance(res, Exception):
                logger.error(f"Transformation task failed: {res}")
                
        return final_records

    async def _process_single_pdf(self, pdf_path: Path) -> Optional[Dict[str, Any]]:
        """
        Purpose:
            Internal workflow for a single PDF:
            Validate -> Parse -> Standardize -> Format for Indexing.

        Args:
            pdf_path (Path): Path to the single PDF file to process.

        Returns:
            Optional[Dict[str, Any]]: Standardized index record or None if processing failed.
        """
        if not pdf_path.exists():
            logger.error(f"File not found: {pdf_path}")
            return None

        async with self.semaphore:
            try:
                # 1. Parse PDF into structured Pydantic model
                content: Optional[PdfContent] = await self.engine.parse_pdf(pdf_path)
                
                if not content:
                    return None

                # 2. Convert model to dict for cleaning
                raw_dict = content.model_dump()

                # 3. Use BaseTransformer.transform to clean types and add _index/_source
                return self.transform(raw_dict)

            except PDFValidationError as e:
                logger.warning(f"Validation failed for {pdf_path.name}: {e}")
                return None
            except Exception as e:
                logger.error(f"Unexpected error transforming {pdf_path.name}: {e}")
                return None