
"""
Unit tests for GoogleDriveExtractor
"""

import pytest
import sys
from unittest.mock import MagicMock

sys.modules["airflow"] = MagicMock()
sys.modules["airflow.models"] = MagicMock()

from src.components.extractors.googledrive import GoogleDriveExtractor


class MockFiles:

    def list(self, **kwargs):
        return self

    def execute(self):

        return {
            "files": [
                {
                    "id": "1",
                    "name": "file1.txt",
                    "mimeType": "text/plain",
                    "modifiedTime": "2026-01-01T00:00:00Z"
                }
            ]
        }


class MockDriveService:

    def files(self):
        return MockFiles()


def test_extractor_returns_records():

    config = {
        "folder_id": "root",
        "page_size": 10,
        "incremental": {
            "enabled": False
        }
    }

    extractor = GoogleDriveExtractor(
        connection=MockDriveService(),
        config=config
    )

    results = list(extractor.extract())

    assert len(results) == 1
    assert results[0]["id"] == "1"

