import unittest
from unittest.mock import MagicMock, patch
from src.custom.credentials.airflow import AirflowCredentials

class TestAirflowCredentials(unittest.TestCase):
    @patch("src.custom.credentials.airflow.BaseHook.get_connection")
    def test_get_credentials_success(self, mock_get_connection):
        """ Should correctly map airflow connection attributes to a flat dictionary. """

        # 1. Setup the mock connection object
        mock_conn = MagicMock()
        mock_conn.host = "localhost"
        mock_conn.port = 5432
        mock_conn.login = "admin"
        mock_conn.password = "secret_pass"
        mock_conn.schema = "production_db"
        mock_conn.extra_dejson = {"account": "aws_account_1", "region": "us-east-1"}

        mock_get_connection.return_value = mock_conn

        # 2. Execute
        provider = AirflowCredentials(conn_id="my_postgres_conn")
        creds = provider.get_credentials()

        # 3. Assertions
        mock_get_connection.assert_called_once_with("my_postgres_conn")

        assert creds["host"] == "localhost"
        assert creds["user"] == "admin"
        assert creds["password"] == "secret_pass"
        assert creds["database"] == "production_db"

        # Check if dictionary unpacking of extra_dejson worked
        assert creds["account"] == "aws_account_1"
        assert creds["region"] == "us-east-1"

    @patch("src.custom.credentials.airflow.BaseHook.get_connection")
    def test_get_credentials_missing_connection(self, mock_get_connection):
        """ Should allow Airflow exceptions to propagate if conn_id is not found """
        # Simulate Airflow raising an error when a connection doesn't exist
        mock_get_connection.side_effect = Exception("The conn_id `missing_id` isn't defined.")
        
        provider = AirflowCredentials(conn_id="missing_id")
        
        with self.assertRaises(Exception):
            provider.get_credentials()

if __name__ == '__main__':
    unittest.main()