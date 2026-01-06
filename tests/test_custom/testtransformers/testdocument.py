import pytest
from unittest.mock import MagicMock, patch
from src.custom.transformers.document_transformer import DocumentTransformer

class TestDocumentTransformer:

    @patch("txtai.pipeline.data.Textractor")
    def test_transformer_segmentation(self, mock_textractor_class):
        """Should split a single record into multiple chunks correctly"""

        # 1. Setup Mock Textractor behavior
        mock_instance = mock_textractor_class.return_value
        # Simulate Textractor splitting one text into two chunks
        mock_instance.return_value = ["Chunk 1 text", "Chunk 2 text"]

        # 2. Prepare Input Data
        raw_data = {
            "id": "msg_123",
            "body": "This is a long text that should be split.",
            "source_id": "gmail_source",
            "metadata": {"folder": "inbox"}
        }
        
        config = {
            "textractor": {"sentences": True},
            "segmentation": {"maxlength": 100}
        }

        # 3. Execute
        transformer = DocumentTransformer(data=raw_data, config=config)
        results = list(transformer())  # Convert generator to list to inspect

        # 4. Assertions
        assert len(results) == 2
        
        # Verify the first chunk
        # Note: We assume self.transform() returns the dict (or wraps it)
        # If your BaseTransformer adds an '_index' key, check for that here!
        assert results[0]["id"] == "msg_123#chunk0"
        assert results[0]["text"] == "Chunk 1 text"

        # Verify the second chunk
        assert results[1]["id"] == "msg_123#chunk1"
        assert results[1]["text"] == "Chunk 2 text"

        # Ensure Textractor was called with the body
        mock_instance.assert_called_with("This is a long text that should be split.")

    @patch("txtai.pipeline.data.Textractor")
    def test_transformer_handles_list_input(self, mock_textractor_class):
        """Should process multiple records if data is passed as a list"""
        mock_instance = mock_textractor_class.return_value
        mock_instance.return_value = ["Single Chunk"]

        list_data = [
            {"id": "1", "body": "Body 1"},
            {"id": "2", "body": "Body 2"}
        ]
        
        transformer = DocumentTransformer(data=list_data, config={})
        results = list(transformer())

        assert len(results) == 2
        assert results[0]["id"] == "1#chunk0"
        assert results[1]["id"] == "2#chunk0"