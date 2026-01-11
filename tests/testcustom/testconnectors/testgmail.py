import unittest
from unittest.mock import patch, MagicMock
from src.custom.connectors.gmail import GmailConnector

class TestGmailConnector(unittest.TestCase):

    def setUp(self):
        """ Setup fake token data for testing """
        self.sample_config = {
            "token_dict": {
                "token": "fake_access_token",
                "refresh_token": "fake_refresh_token",
                "client_id": "fake_id",
                "client_secret": "fake_secret"
            }
        }
        self.connector = GmailConnector(self.sample_config)

    def test_init_state(self):
        """ Verify service starts as None """
        self.assertIsNone(self.connector._service)

    # We patch BOTH Google libraries so no real web requests are made
    @patch("src.custom.connectors.gmail.build")
    @patch("src.custom.connectors.gmail.Credentials")
    def test_connect_success(self, mock_creds, mock_build):
        """ Test successful service creation """
        
        # 1. ARRANGE
        # Setup the fake credentials return
        mock_creds.from_authorized_user_info.return_value = MagicMock()
        # Setup the fake service return
        mock_service = MagicMock()
        mock_build.return_value = mock_service

        # 2. ACT
        service = self.connector.connect()

        # 3. ASSERT
        # Did we call Google's library with our token?
        mock_creds.from_authorized_user_info.assert_called_once_with(
            self.sample_config["token_dict"],
            scopes=['https://www.googleapis.com/auth/gmail.readonly']
        )
        self.assertIsNotNone(self.connector._service)
        self.assertEqual(service, mock_service)

    @patch("src.custom.connectors.gmail.build")
    @patch("src.custom.connectors.gmail.Credentials")
    def test_reuses_existing_service(self, mock_creds, mock_build):
        """ Verify it doesn't rebuild the service if it already exists """
        
        # 1. ARRANGE: Pretend a service already exists
        fake_existing_service = MagicMock()
        self.connector._service = fake_existing_service

        # 2. ACT
        service = self.connector.connect()

        # 3. ASSERT
        # Google's 'build' should NOT be called again
        mock_build.assert_not_called()
        self.assertEqual(service, fake_existing_service)

if __name__ == '__main__':
    unittest.main()