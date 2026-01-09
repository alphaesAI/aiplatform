import pytest
from src.custom.loaders.embeddings import Embeddings

def test_embeddings_workflow():
    # 1. Setup: Real configuration and sample data
    config = {"path": "sentence-transformers/all-MiniLM-L6-v2"}
    
    # This sample data mimics exactly what your Transformer outputs
    sample_input = [
        {
            "_index": "gmail_index",
            "_source": {
                "id": "msg_01#chunk0",
                "text": "Hello, this is a test email about Windsurf Wave 13.",
                "metadata": {"subject": "Test"},
                "source_id": "test@gmail.com"
            }
        },
        {
            "_index": "gmail_index",
            "_source": {
                "id": "msg_01#chunk1",
                "text": "It supports multi-agent sessions.",
                "metadata": {"subject": "Test"},
                "source_id": "test@gmail.com"
            }
        }
    ]

    # 2. Action: Initialize and Run
    # Verify staticmethod check
    assert Embeddings.available() is True
    
    embedder = Embeddings(config)
    # Convert generator to list so we can inspect it
    results = list(embedder.embed(sample_input))

    # 3. Assertions: Verify the output structure and data
    assert len(results) == 2
    for record in results:
        # Check if Elasticsearch wrapper is preserved
        assert "_index" in record
        assert "_source" in record
        
        # Check if vector was added to the _source
        source = record["_source"]
        assert "vector" in source
        
        # Check vector properties
        vector = source["vector"]
        assert isinstance(vector, list)
        assert len(vector) == 384  # MiniLM-L6-v2 dimension
        assert isinstance(vector[0], float)

    print("\nEmbedding Test Passed: Vectors generated and aligned correctly.")

if __name__ == "__main__":
    test_embeddings_workflow()


# Check that the attachments are being embedded.

"""
curl -X GET "localhost:9200/gmail_index/_search?pretty" -H 'Content-Type: application/json' -d'
{
  "query": {
    "match": {
      "text":"Data is collected from multiple sources using connectors"
    }
  }
}'
"""

# run the above commands in the terminal to check if the attachments are being embedded. (the text field should contain the attachment content)