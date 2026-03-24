import logging
from typing import Any, Iterator, Dict
from qdrant_client import QdrantClient, models
from qdrant_client.http.exceptions import UnexpectedResponse
from .base import BaseLoader
from .schemas import IngestorConfig
import uuid

logger = logging.getLogger(__name__)

class QdrantIngestor(BaseLoader):
    """
    Purpose:
        Handles the lifecycle of a Qdrant Collection including 
        schema-less payload indexing and vector configuration.
    """
    def __init__(self, connection: QdrantClient, config: Dict[str, Any]):
        """
        Purpose:
            Handles the lifecycle of a Qdrant Collection including 
            schema-less payload indexing and vector configuration.
        """
        self.connection = connection
        if "index_name" not in config:
            # You may need to adjust this depending on how your Factory 
            # extracts the sub-dictionary from the YAML.
            logger.warning("index_name not found in local config. Ensure it is passed to the Ingestor.")
        self.config = IngestorConfig(**config)

    def create(self) -> None:
        """Ensures the collection and payload indexes exist."""
        name = self.config.index_name
        settings = self.config.settings
        hnsw = settings.get("hnsw_config", {})

        # 1. Vector Configuration
        vector_config = models.VectorParams(
            size=settings.get("size", 384),
            distance=models.Distance.COSINE,
            on_disk=settings.get("on_disk", True)
        )

        # 2. Collection Creation
        try:
            if not self.connection.collection_exists(collection_name=name):
                logger.info(f"Creating Qdrant collection: {name}")
                self.connection.create_collection(
                    collection_name=name,
                    vectors_config=vector_config,
                    hnsw_config=models.HnswConfigDiff(
                        m=hnsw.get("m", 16),
                        ef_construct=hnsw.get("ef_construct", 512),
                        on_disk=True
                    )
                )
                
                # 3. Payload Indexing (Mappings)
                if self.config.mappings and "properties" in self.config.mappings:
                    self._initialize_payload_indexes(name, self.config.mappings["properties"])
            else:
                logger.debug(f"Collection {name} already exists.")
        except UnexpectedResponse as e:
            logger.error(f"Failed to initialize Qdrant collection: {e}")
            raise

    def _initialize_payload_indexes(self, name: str, properties: Dict[str, str]):
        """Maps YAML types to Qdrant Payload Schema Types."""
        type_map = {
            "keyword": models.PayloadSchemaType.KEYWORD,
            "integer": models.PayloadSchemaType.INTEGER,
            "datetime": models.PayloadSchemaType.DATETIME,
            "text": models.PayloadSchemaType.TEXT
        }

        for field, f_type in properties.items():
            schema_type = type_map.get(f_type)
            if schema_type:
                self.connection.create_payload_index(
                    collection_name=name,
                    field_name=field,
                    field_schema=schema_type
                )
                logger.info(f"Created payload index: {field} ({f_type})")

    def load(self, data: Iterator[models.PointStruct]):
        """Performs batch upsert to Qdrant Cloud."""
        logger.info(f"Starting bulk upload to {self.config.index_name}")
        try:
            points = []
            for i, record in enumerate(data):
                if isinstance(record, dict):
                    # Handle dictionary format from txtai embedder
                    source_data = record.get("_source", record)
                    
                    # Debug logging to see the structure
                    logger.debug(f"Record {i} keys: {list(source_data.keys())}")
                    logger.debug(f"Record {i} id: {source_data.get('id')}")
                    
                    # Extract ID with fallbacks
                    record_id = source_data.get("id")
                    if not record_id:
                        # Try other common ID fields
                        record_id = source_data.get("chunk_id") or source_data.get("_id") or source_data.get("chunk_index")
                    
                    # Ensure we have a valid ID (convert to string if needed)
                    if record_id is None:
                        record_id = str(uuid.uuid4())
                    else:
                        record_id = str(record_id)
                    
                    point = models.PointStruct(
                        id=record_id,
                        vector=source_data.get("vector", []),
                        payload=source_data
                    )
                    points.append(point)
                else:
                    # Already a PointStruct object
                    points.append(record)
            
            self.connection.upload_points(
                collection_name=self.config.index_name,
                points=points,
                batch_size=100,
                wait=True
            )
            logger.info("Ingestion complete.")
        except Exception as e:
            logger.error(f"Ingestion failed: {e}")
            raise

    def __call__(self, data: Iterator[models.PointStruct]):
        self.create()
        return self.load(data)