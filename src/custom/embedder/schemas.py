from typing import Dict, List

from pydantic import BaseModel, ConfigDict


class JinaEmbeddingRequest(BaseModel):
    """Request model for Jina embeddings API."""

    model: str = "jina-embeddings-v3"
    task: str = "retrieval.passage"  # or "retrieval.query" for queries
    dimensions: int = 1024
    late_chunking: bool = False
    embedding_type: str = "float"
    input: List[str]


class JinaEmbeddingResponse(BaseModel):
    """Response model from Jina embeddings API."""

    model: str
    object: str = "list"
    usage: Dict[str, int]
    data: List[Dict]

class EmbeddingsConfig(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    # Default model if none is provided in Airflow
    path: str = "sentence-transformers/all-MiniLM-L6-v2"
    content: bool = False
    backend: str = "numpy"