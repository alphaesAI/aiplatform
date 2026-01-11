import unittest
from unittest.mock import patch, MagicMock
from src.custom.connectors.elasticsearch import ElasticsearchConnector

class TestElasticsearchConnector(unittest.TestCase):

    def setUp(self):
        """ Setup basic config for Elasticsearch """
        self.config = {
            "schema_type": "http",
            "host": "localhost",
            "port": 9200,
            "verify_certs": False
        }
        self.connector = ElasticsearchConnector(self.config)

    def test_init(self):
        """ Verify config is parsed correctly and client is lazy-loaded """
        self.assertEqual(self.connector.config.host, "localhost")
        self.assertIsNone(self.connector._client)

    # This 'patch' decorator intercepts the real Elasticsearch class
    # and replaces it with a fake one for this test only.
    @patch("src.custom.connectors.elasticsearch.Elasticsearch")
    def test_connect_success(self, mock_es_class):
        """ Test successful connection when ping returns True """
        
        # 1. ARRANGE: Make the fake ping return True
        mock_instance = MagicMock()
        mock_instance.ping.return_value = True
        mock_es_class.return_value = mock_instance

        # 2. ACT
        self.connector.connect()

        # 3. ASSERT
        self.assertIsNotNone(self.connector._client)
        mock_instance.ping.assert_called_once() # Verify ping was actually checked
        print("\nES Connection Test Passed: Ping successful.")

    @patch("src.custom.connectors.elasticsearch.Elasticsearch")
    def test_connect_failure(self, mock_es_class):
        """ Test that ConnectionError is raised if ping fails """
        
        # 1. ARRANGE: Make the fake ping return False
        mock_instance = MagicMock()
        mock_instance.ping.return_value = False
        mock_es_class.return_value = mock_instance

        # 2. ACT & ASSERT
        # We expect a ConnectionError because the ping failed
        with self.assertRaises(ConnectionError):
            self.connector.connect()

if __name__ == '__main__':
    unittest.main()