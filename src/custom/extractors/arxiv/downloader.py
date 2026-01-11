import asyncio
import logging
import time
from pathlib import Path
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)


class ArxivDownloader:
    """
    Infrastructure Layer - Arxiv PDF Downloader
    
    Now uses a shared connector to maintain a single HTTP session.
    """

    def __init__(self, connector, config: Dict[str, str]):
        """
        Initialize with shared connector and minimalistic config.
        """
        self.connector = connector
        
        # Folder setup
        self.download_dir = Path(config.get("download_dir", "./downloads"))
        self.download_dir.mkdir(parents=True, exist_ok=True)

        # Operational settings from config
        self.rate_limit_delay = config.get("rate_limit_delay", 3)
        self.max_retries = config.get("max_retries", 3)
        self.retry_backoff = config.get("retry_backoff", 2)
        
        self._last_request_time: Optional[float] = None

        logger.info(f"PDF Downloader initialized | Target: {self.download_dir}")

    async def _rate_limit(self):
        """Respect arXiv's polite usage policy."""
        if self._last_request_time is not None:
            elapsed = time.time() - self._last_request_time
            if elapsed < self.rate_limit_delay:
                wait = self.rate_limit_delay - elapsed
                await asyncio.sleep(wait)
        self._last_request_time = time.time()

    async def download(self, paper: Dict, force: bool = False) -> Optional[Path]:
        """
        Download PDF using the shared connector session.
        """
        pdf_url = paper.get("pdf_url")
        arxiv_id = paper.get("arxiv_id")

        if not pdf_url or not arxiv_id:
            logger.error("Missing pdf_url or arxiv_id")
            return None

        pdf_path = self.download_dir / f"{arxiv_id}.pdf"

        # Cache check
        if pdf_path.exists() and not force:
            logger.info(f"Using cached PDF: {pdf_path.name}")
            return pdf_path

        await self._rate_limit()
        
        # Use the SHARED client from the connector
        client = await self.connector()

        logger.info(f"Downloading: {arxiv_id}")

        for attempt in range(1, self.max_retries + 1):
            try:
                # Streaming the download to save memory
                async with client.stream("GET", pdf_url) as response:
                    response.raise_for_status()
                    
                    with open(pdf_path, "wb") as f:
                        async for chunk in response.aiter_bytes():
                            f.write(chunk)

                logger.info(f"Download successful: {pdf_path.name}")
                return pdf_path

            except Exception as e:
                if attempt == self.max_retries:
                    logger.error(f"Failed {arxiv_id} after {attempt} retries: {e}")
                    return None

                wait = self.retry_backoff * attempt
                logger.warning(f"Retry {attempt}/{self.max_retries} in {wait}s...")
                await asyncio.sleep(wait)

        return None
    