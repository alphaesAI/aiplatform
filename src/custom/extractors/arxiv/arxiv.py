import asyncio
import logging
import time
import xml.etree.ElementTree as ET
from urllib.parse import urlencode, quote
from typing import Dict, List, Optional, Any

from .downloader import ArxivDownloader
from ..base import BaseExtractor

logger = logging.getLogger(__name__)

class ArxivExtractor(BaseExtractor):
    """
    Main Entry Point for Arxiv Extraction.
    Coordinates metadata fetching and PDF downloading.
    """

    def __init__(self, connector, config: Dict[str, str]):
        self.connector = connector
        self.config = config

        # Internalize config values needed for metadata logic
        self.ns = config.get("namespaces", {})
        self.base_url = config.get("base_url")
        self.rate_limit_delay = config.get("rate_limit_delay")
        # self.max_results = config.get("max_results")
        # self.category = config.get("search_category")
        self._last_request_time: Optional[float] = None

        # Initialize the downloader helper with the SAME connector
        self.downloader = ArxivDownloader(connector, config)

        logger.info(f"ArxivExtractor initialized for category: {config.get('search_category')}")

    async def __call__(self, **kwargs) -> List[Dict]:
        """Trigger the main extraction workflow."""
        return await self.extract(**kwargs)

    async def extract(self, **kwargs) -> List[Dict]:
        """
        1. Fetch Metadata
        2. Download associated PDFs
        3. Return combined results
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

        await self._rate_limit()

        max_results = max_results or self.config.get("max_results")
        category = self.config.get("search_category")

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

        url = f"{self.base_url}?{urlencode(params, quote_via=quote, safe=':+[]*')}"

        logger.info(f"Requesting Arxiv feed: {url}")
        client = await self.connector() # Reusing shared connection
        response = await client.get(url)
        response.raise_for_status()

        return self._parse_xml(response.text)

    def _parse_xml(self, xml_data: str) -> List[Dict]:
        root = ET.fromstring(xml_data)
        entries = root.findall("atom:entry", self.ns)
        results = []

        for entry in entries:
            paper = {
                "arxiv_id": entry.find("atom:id", self.ns).text.split("/")[-1],
                "title": entry.find("atom:title", self.ns).text.strip(),
                "abstract": entry.find("atom:summary", self.ns).text.strip(),
                "published_date": entry.find("atom:published", self.ns).text,
                "authors": [
                    a.find("atom:name", self.ns).text
                    for a in entry.findall("atom:author", self.ns)
                ],
                "categories": [
                    c.get("term")
                    for c in entry.findall("atom:category", self.ns)
                ],
                "pdf_url": self._get_pdf(entry),
            }
            results.append(paper)

        return results

    def _get_pdf(self, entry) -> str:
        for link in entry.findall("atom:link", self.ns):
            if link.get("type") == "application/pdf":
                return link.get("href")
        return ""

    async def _rate_limit(self):
        if self._last_request_time is not None:
            elapsed = time.time() - self._last_request_time
            if elapsed < self.rate_limit_delay:
                await asyncio.sleep(self.rate_limit_delay - elapsed)
        self._last_request_time = time.time()
