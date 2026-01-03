from typing import Dict, Any, Iterator
from .base import BaseTransformer

class DocumentTransformer(BaseTransformer):
    def __init__(self, data: any, config: Dict[str, Any]):
        super().__init__(config)

        from txtai.pipeline.data import Textractor

        self.data = data
        
        # Pull both sections from YAML
        textractor_cfg = config.get("textractor", {})
        segmentation_cfg = config.get("segmentation", {})
        
        # We merge them or pass segmentation parameters into Textractor
        # txtai Textractor accepts segmentation parameters directly
        self.textractor = Textractor(
            **textractor_cfg,
            **segmentation_cfg  # This triggers the 'automatic' segmentation
        )

    def __call__(self) -> Iterator[Dict[str, Any]]:
        records = self.data if isinstance(self.data, list) else [self.data]
        
        for record in records:
            # Because we passed segmentation config to __init__, 
            # this call now returns a LIST of chunks automatically.
            chunks = self.textractor(record.get("body", ""))
            
            for i, chunk in enumerate(chunks):
                chunk_dict = {
                    "id": f"{record.get('id')}#chunk{i}",
                    "text": chunk,
                    "source_id": record.get("source_id"),
                    "metadata": record.get("metadata", {})
                }
                yield self.transform(chunk_dict)