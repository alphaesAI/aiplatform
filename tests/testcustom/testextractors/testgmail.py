import unittest
from typing import Dict, Any

from src.custom.extractors.gmail import GmailExtractor

# 1. Create the mock service
# This class pretends to be the google api client
class MockGmailService:
    def users(self):
        return self

    def messages(self):
        return self

    def getProfile(self, userId: str):
        return self

    def list(self, userId: str, q: str, maxResults: int):
        self.last_query = q
        return self

    def execute(self):
        return {
            "messages": [
                {"id": "msg_101", "threadId": "t1"},
                {"id": "msg_102", "threadId": "t2"},
                {"id": "msg_103", "threadId": "t3"}
            ],
            "emailAddress": "alphagenaitest@gmail.com"
        }

# 2. The test class
class TestGmailExtractorInit(unittest.TestCase):
    
    def setUp(self):
        self.mock_connection = MockGmailService()
        self.sample_config = {
            "query": "is:unread",
            "batch_size": 10,
            "fields": ["from", "subject"],
            "extraction_mode": "full"
        }
        # We save it to 'self.extractor' so all methods can see it
        self.extractor = GmailExtractor(connection=self.mock_connection, config=self.sample_config)

    def test_init(self):
        """ Test if __init__ correctly extracts the email address from the profile """

        # Assert (Check)
        # We check if self.source_id matches the email in our Mock service
        self.assertEqual(self.extractor.source_id, "alphagenaitest@gmail.com")

        # Also we check if the config was converted correctly to the schema object
        self.assertEqual(self.extractor.config.batch_size, 10)
        self.assertEqual(self.extractor.config.query, "is:unread")

    def test_get_message_ids(self):
        """ Test if the method correctly returns the gmail's dict response into a simple ID list """

        # We run the specific method we are testing
        ids = self.extractor._get_message_ids()

        # Assert (Check)
        # 1. Did we get exactly  3 IDs?
        self.assertEqual(len(ids), 3)

        # 2. Are the IDs correct?
        self.assertEqual(ids[0], "msg_101")

        # 3. Is it actually a list of strings?
        self.assertIsInstance(ids, list)


if __name__ == '__main__':
    unittest.main()