import unittest
from src.custom.connectors.arxiv import ArxivConnector

# We use IsolatedAsyncioTestCase for async/await support
class TestArxivConnector(unittest.IsolatedAsyncioTestCase):

    async def asyncSetUp(self):
        """ Runs before every async test """
        self.config = {
            "base_url": "http://export.arxiv.org/api/query",
            "timeout_seconds": 5
        }
        self.connector = ArxivConnector(self.config)

    async def test_init(self):
        """ Test if settings are stored correctly without creating a client """
        self.assertEqual(self.connector.base_url, "http://export.arxiv.org/api/query")
        # The client should be None until we explicitly connect
        self.assertIsNone(self.connector._client)

    async def test_connect_creates_and_reuses_client(self):
        """ Test that we create a client and reuse it (Singleton pattern) """
        # 1. Create client
        client1 = await self.connector.connect()
        self.assertIsNotNone(self.connector._client)
        
        # 2. Call connect again
        client2 = await self.connector.connect()
        
        # 3. Check if they are exactly the same object in memory
        self.assertIs(client1, client2)

    async def test_close_clears_client(self):
        """ Test that close() properly shuts down the session """
        # Setup: open a connection
        await self.connector.connect()
        
        # Act: Close it
        await self.connector.close()
        
        # Assert: The internal client should be None again
        self.assertIsNone(self.connector._client)

    async def test_call_shorthand(self):
        """ Test if calling the object directly returns the client """
        client = await self.connector()
        self.assertIsNotNone(client)

if __name__ == '__main__':
    unittest.main()