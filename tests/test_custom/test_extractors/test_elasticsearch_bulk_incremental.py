import unittest
from unittest.mock import MagicMock, patch
from src.components.extractors.elasticsearch import ElasticsearchExtractor


class TestElasticsearchBulkIncremental(unittest.TestCase):

    def setUp(self):
        """Setup mock Elasticsearch client."""
        self.mock_client = MagicMock()

    def test_bulk_extraction_mode(self):
        """Test bulk extraction retrieves all documents."""
        config = {
            "index_name": "test-index",
            "extraction_mode": "bulk",
            "batch_size": 100,
            "sort_field": None
        }

        # Mock search response
        self.mock_client.search.side_effect = [
            {
                "hits": {
                    "hits": [
                        {"_source": {"id": 1}, "sort": [1]},
                        {"_source": {"id": 2}, "sort": [2]}
                    ]
                }
            },
            {"hits": {"hits": []}}  # Empty response to stop pagination
        ]

        extractor = ElasticsearchExtractor(self.mock_client, config)
        results = list(extractor.extract())

        self.assertEqual(len(results), 2)
        self.assertEqual(results[0]["id"], 1)
        self.assertEqual(results[1]["id"], 2)

    def test_bulk_extraction_uses_search_after(self):
        """Test that bulk extraction uses search_after for pagination."""
        config = {
            "index_name": "test-index",
            "extraction_mode": "bulk",
            "batch_size": 1,
            "sort_field": None
        }

        self.mock_client.search.side_effect = [
            {"hits": {"hits": [{"_source": {"id": 1}, "sort": [1]}]}},
            {"hits": {"hits": [{"_source": {"id": 2}, "sort": [2]}]}},
            {"hits": {"hits": []}}
        ]

        extractor = ElasticsearchExtractor(self.mock_client, config)
        list(extractor.extract())

        # Verify search_after was used in second call
        second_call = self.mock_client.search.call_args_list[1]
        self.assertIn("search_after", second_call[1]["body"])

    @patch("src.components.extractors.elasticsearch.Variable")
    def test_incremental_extraction_mode(self, mock_variable):
        """Test incremental extraction with checkpoint."""
        mock_variable.get.return_value = "2024-01-01"

        config = {
            "index_name": "test-index",
            "extraction_mode": "incremental",
            "incremental_field": "timestamp",
            "checkpoint_key": "last_sync",
            "batch_size": 100,
            "sort_field": None
        }

        self.mock_client.search.return_value = {
            "hits": {
                "hits": [
                    {"_source": {"id": 1, "timestamp": "2024-01-02"}, "sort": ["2024-01-02"]}
                ]
            }
        }

        extractor = ElasticsearchExtractor(self.mock_client, config)
        results = list(extractor.extract())

        # Verify checkpoint was retrieved
        mock_variable.get.assert_called_with("last_sync", default_var=None)

        # Verify range query was used
        call_args = self.mock_client.search.call_args[1]
        self.assertIn("range", call_args["body"]["query"])

    @patch("src.components.extractors.elasticsearch.Variable")
    def test_incremental_extraction_updates_checkpoint(self, mock_variable):
        """Test that checkpoint is updated after incremental extraction."""
        mock_variable.get.return_value = None

        config = {
            "index_name": "test-index",
            "extraction_mode": "incremental",
            "incremental_field": "timestamp",
            "checkpoint_key": "last_sync",
            "batch_size": 100,
            "sort_field": None
        }

        self.mock_client.search.return_value = {
            "hits": {
                "hits": [
                    {"_source": {"id": 1, "timestamp": "2024-01-02"}, "sort": ["2024-01-02"]}
                ]
            }
        }

        extractor = ElasticsearchExtractor(self.mock_client, config)
        list(extractor.extract())

        # Verify checkpoint was updated
        mock_variable.set.assert_called_with("last_sync", "2024-01-02")

    def test_incremental_mode_requires_incremental_field(self):
        """Test that incremental mode fails without incremental_field."""
        config = {
            "index_name": "test-index",
            "extraction_mode": "incremental",
            "checkpoint_key": "last_sync",
            "batch_size": 100,
            "sort_field": None
        }

        with self.assertRaises(ValueError) as cm:
            ElasticsearchExtractor(self.mock_client, config)

        self.assertIn("incremental_field is required", str(cm.exception))


if __name__ == "__main__":
    unittest.main()