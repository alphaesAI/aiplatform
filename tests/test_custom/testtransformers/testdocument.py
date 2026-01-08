# import pytest
# from unittest.mock import MagicMock, patch
# from src.custom.transformers.document_transformer import DocumentTransformer

# class TestDocumentTransformer:

#     @patch("txtai.pipeline.data.Textractor")
#     def test_transformer_segmentation(self, mock_textractor_class):
#         """Should split a single record into multiple chunks correctly"""

#         # 1. Setup Mock Textractor behavior
#         mock_instance = mock_textractor_class.return_value
#         # Simulate Textractor splitting one text into two chunks
#         mock_instance.return_value = ["Chunk 1 text", "Chunk 2 text"]

#         # 2. Prepare Input Data
#         raw_data = {
#             "id": "msg_123",
#             "body": "This is a long text that should be split.",
#             "source_id": "gmail_source",
#             "metadata": {"folder": "inbox"}
#         }
        
#         config = {
#             "textractor": {"sentences": True},
#             "segmentation": {"maxlength": 100}
#         }

#         # 3. Execute
#         transformer = DocumentTransformer(data=raw_data, config=config)
#         results = list(transformer())  # Convert generator to list to inspect

#         # 4. Assertions
#         assert len(results) == 2
        
#         # Verify the first chunk
#         # Note: We assume self.transform() returns the dict (or wraps it)
#         # If your BaseTransformer adds an '_index' key, check for that here!
#         assert results[0]["id"] == "msg_123#chunk0"
#         assert results[0]["text"] == "Chunk 1 text"

#         # Verify the second chunk
#         assert results[1]["id"] == "msg_123#chunk1"
#         assert results[1]["text"] == "Chunk 2 text"

#         # Ensure Textractor was called with the body
#         mock_instance.assert_called_with("This is a long text that should be split.")

#     @patch("txtai.pipeline.data.Textractor")
#     def test_transformer_handles_list_input(self, mock_textractor_class):
#         """Should process multiple records if data is passed as a list"""
#         mock_instance = mock_textractor_class.return_value
#         mock_instance.return_value = ["Single Chunk"]

#         list_data = [
#             {"id": "1", "body": "Body 1"},
#             {"id": "2", "body": "Body 2"}
#         ]
        
#         transformer = DocumentTransformer(data=list_data, config={})
#         results = list(transformer())

#         assert len(results) == 2
#         assert results[0]["id"] == "1#chunk0"
#         assert results[1]["id"] == "2#chunk0"


import os
from pathlib import Path
from txtai.pipeline.data import Textractor

BASE_DIR = "/home/logi/github/alphaesai/aiplatform/dags/unstructure/gmail/data/extractors"

def run():
    # Use 'available' to let txtai decide the best engine
    textractor = Textractor(paragraphs=True)
    path = Path(BASE_DIR)
    files = [str(f) for f in path.rglob("*") if f.is_file()]

    for f in files:
        print(f"\n--- Processing: {os.path.basename(f)} ---")
        try:
            # Simple check: If it's a .txt, read it directly
            if f.endswith(".txt"):
                with open(f, "r") as txt_file:
                    content = txt_file.read()
            else:
                # Let textractor handle PDFs/Docs
                content = textractor(f)

            # Print results
            if content:
                # txtai might return a list of chunks, join them for printing
                text_to_show = " ".join(content) if isinstance(content, list) else content
                print(f"SUCCESS! First 100 chars: {text_to_show[:100]}...")
            else:
                print("SKIPPED: No text extracted.")

        except Exception as e:
            # This is where Encryption or Format errors are caught
            print(f"FAILED: Could not read file. (Error: {str(e)[:50]}...)")

if __name__ == "__main__":
    run()