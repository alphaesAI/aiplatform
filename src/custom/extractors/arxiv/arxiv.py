import asyncio
import logging
import time
import xml.etree.ElementTree as ET
from urllib.parse import urlencode, quote
from typing import Dict, List, Optional, Any

from .downloader import ArxivDownloader
from ..base import BaseExtractor
from ..schemas import ArxivExtractorConfig, ArxivDownloaderConfig

logger = logging.getLogger(__name__)

"""
arxiv_extractor.py
====================================
Purpose:
    Handles the structured extraction of academic paper metadata from the 
    arXiv API and manages the associated PDF download workflow.
"""

class ArxivExtractor(BaseExtractor):
    """
    Purpose:
        Main Entry Point for Arxiv Extraction.
        Coordinates metadata fetching and PDF downloading.
    """

    def __init__(self, connection, config: Dict[str, Any]):
        """
        Purpose: Initializes the extractor with a connector and configuration.

        Args:
            connection: Shared connection object (ArxivConnector).
            config (Dict[str, str]): Configuration dictionary for categories and limits.
        """
        self.connection = connection
        # Validate main extractor config
        self.config = ArxivExtractorConfig(**config)
        
        # Validate and inject the downloader-specific config
        downloader_cfg = ArxivDownloaderConfig(**config)
        self.downloader = ArxivDownloader(connection, downloader_cfg)

        self._last_request_time: Optional[float] = None

        logger.info(f"ArxivExtractor initialized for category: {self.config.search_category}")

    async def __call__(self, **kwargs) -> List[Dict]:
        """
        Purpose: Trigger the main extraction workflow.
        
        Returns:
            List[Dict]: List of papers with metadata and local file paths.
        """
        return await self.extract(**kwargs)

    async def extract(self, **kwargs) -> List[Dict]:
        """
        Purpose: Orchestrates the two-step extraction process.
        1. Fetch Metadata from API.
        2. Download associated PDFs to local storage.
        3. Return combined results.

        Returns:
            List[Dict]: Enriched paper data.
        """
        # Step 1: Fetch Metadata
        papers = await self.fetch_papers(**kwargs)

        # Step 2: Download PDFs (Sharing the connector logic)
        for paper in papers:
            pdf_path = await self.downloader.download(paper)
            paper["local_pdf_path"] = str(pdf_path) if pdf_path else None
            
        return papers

    async def fetch_papers(
        self,
        max_results: Optional[int] = None,
        start: int = 0,
        sort_by: str = "submittedDate",
        sort_order: str = "descending",
        from_date: Optional[str] = None,
        to_date: Optional[str] = None,
    ) -> List[Dict]:
        """
        Purpose: Performs the HTTP request to the Arxiv Atom Feed API.

        Args:
            max_results (int, optional): Number of papers to fetch.
            start (int): Offset for pagination.
            sort_by (str): Criteria to sort results.
            sort_order (str): Order of results.
            from_date (str, optional): Start date for search query.
            to_date (str, optional): End date for search query.

        Returns:
            List[Dict]: Parsed metadata for found papers.
        """

        await self._rate_limit()

        # Logic: Use kwarg override if provided, otherwise fallback to validated config
        max_results = max_results or self.config.max_results
        category = self.config.search_category

        search_query = f"cat:{category}"
        if from_date or to_date:
            date_from = f"{from_date}0000" if from_date else "*"
            date_to = f"{to_date}2359" if to_date else "*"
            search_query += f"+AND+submittedDate:[{date_from}+TO+{date_to}]"

        params = {
            "search_query": search_query,
            "start": start,
            "max_results": max_results,
            "sortBy": sort_by,
            "sortOrder": sort_order,
        }

        url = f"{self.config.base_url}?{urlencode(params, quote_via=quote, safe=':+[]*')}"

        logger.info(f"Requesting Arxiv feed: {url}")
        client = await self.connection() 
        response = await client.get(url)
        response.raise_for_status()

        return self._parse_xml(response.text)

    def _parse_xml(self, xml_data: str) -> List[Dict]:
        """
        Purpose: Parses the Arxiv Atom XML response into a Python list of dictionaries.

        Args:
            xml_data (str): Raw XML string from the API.

        Returns:
            List[Dict]: Normalized metadata fields.
        """
        root = ET.fromstring(xml_data)
        # Fix: Access namespaces through the validated Pydantic config
        entries = root.findall("atom:entry", self.config.namespaces)
        results = []

        for entry in entries:
            paper = {
                "arxiv_id": entry.find("atom:id", self.config.namespaces).text.split("/")[-1],
                "title": entry.find("atom:title", self.config.namespaces).text.strip(),
                "abstract": entry.find("atom:summary", self.config.namespaces).text.strip(),
                "published_date": entry.find("atom:published", self.config.namespaces).text,
                "authors": [
                    a.find("atom:name", self.config.namespaces).text
                    for a in entry.findall("atom:author", self.config.namespaces)
                ],
                "categories": [
                    c.get("term")
                    for c in entry.findall("atom:category", self.config.namespaces)
                ],
                "pdf_url": self._get_pdf(entry),
            }
            results.append(paper)

        return results

    def _get_pdf(self, entry) -> str:
        """
        Purpose: Extracts the PDF link from an Atom entry.

        Args:
            entry: XML element representing a paper entry.

        Returns:
            str: The URL of the PDF document.
        """
        # Fix: Access namespaces through the validated Pydantic config
        for link in entry.findall("atom:link", self.config.namespaces):
            if link.get("type") == "application/pdf":
                return link.get("href")
        return ""

    async def _rate_limit(self):
        """
        Purpose: Internal helper to ensure Arxiv API rate limits are respected.
        """
        if self._last_request_time is not None:
            elapsed = time.time() - self._last_request_time
            # Fix: Use rate_limit_delay from validated config
            if elapsed < self.config.rate_limit_delay:
                await asyncio.sleep(self.config.rate_limit_delay - elapsed)
        self._last_request_time = time.time()