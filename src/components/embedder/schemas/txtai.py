from pydantic import BaseModel

class EmbeddingsConfig(BaseModel):

    # Default model if none is provided in Airflow
    path: str
    content: bool
    backend: str