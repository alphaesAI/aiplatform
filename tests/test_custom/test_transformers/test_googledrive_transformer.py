
"""
Unit tests for GoogleDriveDocumentTransformer
"""

from src.components.transformers.googledrive.document_transformer import (
    GoogleDriveDocumentTransformer
)


class MockDownloader:

    def download_file(self, file_id):

        return b"hello world"


class MockConnection:
    pass


def test_transformer_text_file(monkeypatch):

    transformer = GoogleDriveDocumentTransformer(connection=MockConnection())

    transformer.downloader = MockDownloader()

    record = {
        "id": "1",
        "name": "test.txt",
        "mimeType": "text/plain"
    }

    result = transformer.transform(record)

    assert result["doc_id"] == "1"
    assert result["text"] == "hello world"
    assert result["metadata"]["source"] == "googledrive"


def test_transformer_pdf(monkeypatch):

    transformer = GoogleDriveDocumentTransformer(connection=MockConnection())

    transformer.downloader = MockDownloader()

    # mock pdf parser
    monkeypatch.setattr(
        transformer,
        "_parse_pdf",
        lambda x: "mock pdf text"
    )

    record = {
        "id": "2",
        "name": "test.pdf",
        "mimeType": "application/pdf"
    }

    result = transformer.transform(record)

    assert result["doc_id"] == "2"
    assert result["text"] == "mock pdf text"

