import unittest
from unittest.mock import MagicMock, patch
from src.components.connectors.opensearch import OpensearchConnector


class TestOpensearchVerifyCerts(unittest.TestCase):

    @patch("src.components.connectors.opensearch.OpenSearch")
    def test_verify_certs_true_with_https(self, mock_os_class):
        """Test verify_certs=True works with HTTPS schema."""
        mock_instance = MagicMock()
        mock_instance.ping.return_value = True
        mock_os_class.return_value = mock_instance

        config = {
            "schema": "https",
            "host": "localhost",
            "port": 9200,
            "verify_certs": True
        }

        connector = OpensearchConnector(config)
        connector.connect()

        call_kwargs = mock_os_class.call_args[1]
        self.assertTrue(call_kwargs["verify_certs"])

    @patch("src.components.connectors.opensearch.OpenSearch")
    def test_verify_certs_true_fails_with_http(self, mock_os_class):
        """Test verify_certs=True raises error with HTTP schema."""
        config = {
            "schema": "http",
            "host": "localhost",
            "port": 9200,
            "verify_certs": True
        }

        connector = OpensearchConnector(config)

        with self.assertRaises(ValueError) as cm:
            connector.connect()

        self.assertIn("verify_certs=True requires schema_type='https'", str(cm.exception))

    @patch("src.components.connectors.opensearch.OpenSearch")
    def test_verify_certs_with_custom_ca(self, mock_os_class):
        """Test verify_certs=True with custom CA certificate path."""
        mock_instance = MagicMock()
        mock_instance.ping.return_value = True
        mock_os_class.return_value = mock_instance

        config = {
            "schema": "https",
            "host": "localhost",
            "port": 9200,
            "verify_certs": True,
            "ca_certs": "/path/to/ca.crt"
        }

        connector = OpensearchConnector(config)
        connector.connect()

        call_kwargs = mock_os_class.call_args[1]
        self.assertTrue(call_kwargs["verify_certs"])
        self.assertEqual(call_kwargs["ca_certs"], "/path/to/ca.crt")

    @patch("src.components.connectors.opensearch.OpenSearch")
    def test_verify_certs_false(self, mock_os_class):
        """Test verify_certs=False disables TLS verification."""
        mock_instance = MagicMock()
        mock_instance.ping.return_value = True
        mock_os_class.return_value = mock_instance

        config = {
            "schema": "https",
            "host": "localhost",
            "port": 9200,
            "verify_certs": False
        }

        connector = OpensearchConnector(config)
        connector.connect()

        call_kwargs = mock_os_class.call_args[1]
        self.assertFalse(call_kwargs["verify_certs"])


if __name__ == "__main__":
    unittest.main()