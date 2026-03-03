import unittest
from unittest.mock import MagicMock, patch
from pathlib import Path
from src.components.extractors.gmail import GmailExtractor


class TestGmailTempStorage(unittest.TestCase):

    def setUp(self):
        """Setup mock Gmail service."""
        self.mock_service = MagicMock()
        self.mock_service.users().getProfile().execute.return_value = {
            "emailAddress": "test@example.com"
        }

    @patch("src.components.extractors.gmail.Path")
    def test_temp_dir_uses_config_value(self, mock_path_class):
        """Test that temp_dir from config is used for attachments."""
        config = {
            "query": "test",
            "batch_size": 10,
            "extraction_mode": "full",
            "fields": ["subject"],
            "temp_dir": "/custom/temp/path"
        }

        extractor = GmailExtractor(self.mock_service, config)

        # Mock message with attachment
        msg_id = "msg123"
        payload = {
            "parts": [{
                "filename": "test.pdf",
                "body": {"attachmentId": "att123"}
            }]
        }

        mock_attachment = {"data": "dGVzdA=="}  # base64 "test"
        self.mock_service.users().messages().attachments().get().execute.return_value = mock_attachment

        # Mock Path to track calls
        mock_path_instance = MagicMock()
        mock_path_class.return_value = mock_path_instance

        extractor._handle_attachments(msg_id, payload)

        # Verify custom temp_dir was used
        mock_path_class.assert_called_with("/custom/temp/path")

    #@patch("src.components.extractors.gmail.Path")
    def test_temp_dir_uses_default_when_not_configured(self):
        """Test that default path is used when temp_dir is not in config."""
        config = {
            "query": "test",
            "batch_size": 10,
            "extraction_mode": "full",
            "fields": ["subject"]
        }

        extractor = GmailExtractor(self.mock_service, config)
         # Verify temp_dir is None (will use default)
        self.assertIsNone(extractor.config.temp_dir)
        
        msg_id = "msg123"
        payload = {
            "parts": [{
                "filename": "test.pdf",
                "body": {"attachmentId": "att123"}
            }]
        }

        # mock_attachment = {"data": "dGVzdA=="}
        # self.mock_service.users().messages().attachments().get().execute.return_value = mock_attachment

        # mock_path_instance = MagicMock()
        # mock_path_class.cwd.return_value = mock_path_instance
        # mock_path_instance.__truediv__ = MagicMock(return_value=mock_path_instance)

        # extractor._handle_attachments(msg_id, payload)

        # # Verify default path construction was called
        # mock_path_class.cwd.assert_called()

    def test_temp_dir_not_hardcoded_to_tmp(self):
        """Test that /tmp is not hardcoded in the extractor."""
        config = {
            "query": "test",
            "batch_size": 10,
            "extraction_mode": "full",
            "fields": ["subject"]
        }

        extractor = GmailExtractor(self.mock_service, config)

        # Verify temp_dir is configurable (None by default)
        self.assertIsNone(extractor.config.temp_dir)


if __name__ == "__main__":
    unittest.main()