import unittest
from unittest.mock import patch, MagicMock
from src.custom.connectors.rdbms import RDBMSConnector

class TestRDBMSConnector(unittest.TestCase):

    def setUp(self):
        """ Setup sample configuration for a Postgres database """
        self.config = {
            "db_type": "postgresql",
            "username": "admin",
            "password": "secret_password",
            "host": "localhost",
            "port": 5432,
            "database": "analytics_db"
        }
        self.connector = RDBMSConnector(self.config)

    def test_init(self):
        """ Verify config mapping """
        self.assertEqual(self.connector.config.host, "localhost")
        self.assertEqual(self.connector.config.database, "analytics_db")
    
    @patch("src.custom.connectors.rdbms.create_engine")
    def test_connect_success(self, mock_create_engine):
        """ Test that connection is established if SELECT 1 works """
        
        # 1. ARRANGE: Create a chain of mocks to simulate engine -> connection -> execute
        mock_engine = MagicMock()
        mock_conn = MagicMock()
        
        # This simulates the 'with self._engine.connect() as conn:' block
        mock_engine.connect.return_value.__enter__.return_value = mock_conn
        mock_create_engine.return_value = mock_engine

        # 2. ACT
        self.connector.connect()

        # 3. ASSERT
        # Verify create_engine was called (checking URL construction indirectly)
        self.assertTrue(mock_create_engine.called)
        # Verify the test query 'SELECT 1' was executed
        mock_conn.execute.assert_called()
        self.assertIsNotNone(self.connector._connection)
        
        print("\nRDBMS Connection Test Passed: Handshake successful.")

    @patch("src.custom.connectors.rdbms.create_engine")
    def test_connect_failure(self, mock_create_engine):
        """ Test that exception is raised if the database is unreachable """
        
        # 1. ARRANGE: Make create_engine raise an error immediately
        mock_create_engine.side_effect = Exception("DB Connection Refused")

        # 2. ACT & ASSERT
        with self.assertRaises(Exception):
            self.connector.connect()
        
        print("RDBMS Failure Test Passed: Exception raised correctly.")

if __name__ == '__main__':
    unittest.main()