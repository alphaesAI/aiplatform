
"""
Unit tests for GoogleDriveConnector
"""

import pytest

from src.components.connectors.googledrive import GoogleDriveConnector


def test_connector_initialization():

    config = {
        "service_account_file": "dummy.json",
        "scopes": ["https://www.googleapis.com/auth/drive.readonly"]
    }

    connector = GoogleDriveConnector(config=config)

    assert connector.config.service_account_file == "dummy.json"
    assert connector.config.scopes is not None


def test_connector_connect_method(monkeypatch):

    class MockService:
        pass

    def mock_connect(self):
        return MockService()

    monkeypatch.setattr(
        GoogleDriveConnector,
        "connect",
        mock_connect
    )

    config = {"service_account_file": "dummy.json"}

    connector = GoogleDriveConnector(config)

    service = connector.connect()

    assert isinstance(service, MockService)

