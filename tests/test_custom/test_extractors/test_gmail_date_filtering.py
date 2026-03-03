import unittest
from unittest.mock import MagicMock
from src.components.extractors.gmail import GmailExtractor


class TestGmailDateFiltering(unittest.TestCase):

    def setUp(self):
        """Setup mock Gmail service."""
        self.mock_service = MagicMock()
        self.mock_service.users().getProfile().execute.return_value = {
            "emailAddress": "test@example.com"
        }
        self.mock_service.users().messages().list().execute.return_value = {
            "messages": []
        }

    def test_date_filtering_with_start_date(self):
        """Test that start_date is added to Gmail query."""
        config = {
            "query": "subject:test",
            "batch_size": 10,
            "extraction_mode": "full",
            "fields": ["subject"],
            "start_date": "2024-01-01"
        }

        extractor = GmailExtractor(self.mock_service, config)
        extractor._get_message_ids()

        # Verify the query includes after: filter
        call_args = self.mock_service.users().messages().list.call_args
        query = call_args[1]["q"]
        self.assertIn("after:2024/01/01", query)

    def test_date_filtering_with_end_date(self):
        """Test that end_date is added to Gmail query."""
        config = {
            "query": "subject:test",
            "batch_size": 10,
            "extraction_mode": "full",
            "fields": ["subject"],
            "end_date": "2024-12-31"
        }

        extractor = GmailExtractor(self.mock_service, config)
        extractor._get_message_ids()

        # Verify the query includes before: filter
        call_args = self.mock_service.users().messages().list.call_args
        query = call_args[1]["q"]
        self.assertIn("before:2024/12/31", query)

    def test_date_filtering_with_both_dates(self):
        """Test that both start_date and end_date are added to query."""
        config = {
            "query": "subject:test",
            "batch_size": 10,
            "extraction_mode": "full",
            "fields": ["subject"],
            "start_date": "2024-01-01",
            "end_date": "2024-12-31"
        }

        extractor = GmailExtractor(self.mock_service, config)
        extractor._get_message_ids()

        call_args = self.mock_service.users().messages().list.call_args
        query = call_args[1]["q"]
        self.assertIn("after:2024/01/01", query)
        self.assertIn("before:2024/12/31", query)

    def test_date_filtering_without_dates(self):
        """Test that query works without date filters."""
        config = {
            "query": "subject:test",
            "batch_size": 10,
            "extraction_mode": "full",
            "fields": ["subject"]
        }

        extractor = GmailExtractor(self.mock_service, config)
        extractor._get_message_ids()

        call_args = self.mock_service.users().messages().list.call_args
        query = call_args[1]["q"]
        self.assertEqual(query, "subject:test")

    def test_batch_size_configuration(self):
        """Test that batch_size is configurable."""
        config = {
            "query": "test",
            "batch_size": 50,
            "extraction_mode": "full",
            "fields": ["subject"]
        }

        extractor = GmailExtractor(self.mock_service, config)
        extractor._get_message_ids()

        call_args = self.mock_service.users().messages().list.call_args
        self.assertEqual(call_args[1]["maxResults"], 50)


if __name__ == "__main__":
    unittest.main()