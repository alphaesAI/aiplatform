import unittest
from unittest.mock import MagicMock, patch
from src.components.loaders.elasticsearch import ElasticsearchSingleIngestor


class TestElasticsearchSingleIngestor(unittest.TestCase):

    def setUp(self):
        """Setup mock connection and config."""
        self.mock_connection = MagicMock()
        self.mock_connection.indices.exists.return_value = True

        self.config = {
            "index_name": "test-index",
            "settings": {},
            "mappings": {}
        }

    def test_single_ingestor_loads_documents_individually(self):
        """Test that SingleIngestor indexes documents one by one."""
        ingestor = ElasticsearchSingleIngestor(self.mock_connection, self.config)

        data = [
            {"_index": "test-index", "_source": {"id": 1, "text": "doc1"}},
            {"_index": "test-index", "_source": {"id": 2, "text": "doc2"}},
            {"_index": "test-index", "_source": {"id": 3, "text": "doc3"}}
        ]

        ingestor.load(data)

        # Verify index() was called 3 times (once per document)
        self.assertEqual(self.mock_connection.index.call_count, 3)

        # Verify first call arguments
        first_call = self.mock_connection.index.call_args_list[0]
        self.assertEqual(first_call[1]["index"], "test-index")
        self.assertEqual(first_call[1]["document"]["id"], 1)

    def test_single_ingestor_creates_index_if_not_exists(self):
        """Test that index is created if it doesn't exist."""
        self.mock_connection.indices.exists.return_value = False

        ingestor = ElasticsearchSingleIngestor(self.mock_connection, self.config)
        ingestor.create()

        self.mock_connection.indices.create.assert_called_once()

    def test_single_ingestor_skips_index_creation_if_exists(self):
        """Test that index creation is skipped if index exists."""
        self.mock_connection.indices.exists.return_value = True

        ingestor = ElasticsearchSingleIngestor(self.mock_connection, self.config)
        ingestor.create()

        self.mock_connection.indices.create.assert_not_called()

    def test_single_ingestor_call_method(self):
        """Test that calling ingestor triggers create and load."""
        ingestor = ElasticsearchSingleIngestor(self.mock_connection, self.config)

        data = [{"_index": "test-index", "_source": {"id": 1}}]

        ingestor(data)

        self.mock_connection.index.assert_called_once()


if __name__ == "__main__":
    unittest.main()