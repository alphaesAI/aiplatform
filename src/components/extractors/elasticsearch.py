import logging
from typing import Dict, Any, Iterator, List, Optional

from airflow.models import Variable
from elasticsearch import Elasticsearch

from .base import BaseExtractor
from .schemas.elasticsearch import ElasticsearchExtractorConfig

logger = logging.getLogger(__name__)


class ElasticsearchExtractor(BaseExtractor):
    """
    Extracts documents from Elasticsearch as a data source.

    Supports:
    - Bulk extraction (full scan)
    - Incremental extraction (checkpoint-based)
    Uses search_after for safe deep pagination.
    """

    def __init__(self, connection: Elasticsearch, config: Dict[str, Any]):
        self.client = connection
        self.config = ElasticsearchExtractorConfig(**config)

        # Validate incremental requirements early (fail fast)
        if self.config.extraction_mode == "incremental":
            if not self.config.incremental_field:
                raise ValueError(
                    "incremental_field is required for incremental extraction"
                )
            if not self.config.checkpoint_key:
                raise ValueError(
                    "checkpoint_key is required for incremental extraction"
                )

        # Default sort field
        self.sort_field = (
            self.config.sort_field or self.config.incremental_field
        )

    def __call__(self) -> Iterator[Dict[str, Any]]:
        return self.extract()

    def extract(self) -> Iterator[Dict[str, Any]]:
        """
        Entry point for extraction.
        """
        if self.config.extraction_mode == "bulk":
            yield from self._bulk_extract()
        else:
            yield from self._incremental_extract()

    # -------------------------
    # BULK EXTRACTION
    # -------------------------
    def _bulk_extract(self) -> Iterator[Dict[str, Any]]:
        logger.info(
            "Starting BULK extraction from Elasticsearch index '%s'",
            self.config.index_name,
        )

        search_after: Optional[List[Any]] = None

        while True:
            body = {
                "size": self.config.batch_size,
                "sort": [{"_id": "asc"}],
            }

            if search_after:
                body["search_after"] = search_after

            if self.config.fields:
                body["_source"] = self.config.fields

            response = self.client.search(
                index=self.config.index_name,
                body=body,
            )

            hits = response.get("hits", {}).get("hits", [])
            if not hits:
                break

            for hit in hits:
                yield hit["_source"]

            search_after = hits[-1]["sort"]

        logger.info("Bulk extraction completed")

    # -------------------------
    # INCREMENTAL EXTRACTION
    # -------------------------
    def _incremental_extract(self) -> Iterator[Dict[str, Any]]:
        logger.info(
            "Starting INCREMENTAL extraction from Elasticsearch index '%s'",
            self.config.index_name,
        )

        checkpoint = Variable.get(
            self.config.checkpoint_key,
            default_var=None,
        )

        logger.info(
            "Using checkpoint key '%s' with value: %s",
            self.config.checkpoint_key,
            checkpoint,
        )

        search_after: Optional[List[Any]] = None
        last_value: Optional[Any] = checkpoint

        while True:
            query = {"match_all": {}}

            if checkpoint is not None:
                query = {
                    "range": {
                        self.config.incremental_field: {
                            "gt": checkpoint
                        }
                    }
                }

            body = {
                "size": self.config.batch_size,
                "query": query,
                "sort": [{self.sort_field: "asc"}],
            }

            if search_after:
                body["search_after"] = search_after

            if self.config.fields:
                body["_source"] = self.config.fields

            response = self.client.search(
                index=self.config.index_name,
                body=body,
            )

            hits = response.get("hits", {}).get("hits", [])
            if not hits:
                break

            for hit in hits:
                source = hit["_source"]
                last_value = source.get(self.sort_field)
                yield source

            search_after = hits[-1]["sort"]

        # Persist checkpoint AFTER successful extraction
        if last_value is not None:
            Variable.set(self.config.checkpoint_key, last_value)
            logger.info(
                "Updated checkpoint '%s' to value: %s",
                self.config.checkpoint_key,
                last_value,
            )

        logger.info("Incremental extraction completed")