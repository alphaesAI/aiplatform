import unittest
from sqlalchemy import create_engine, text
from src.custom.extractors.rdbms import RDBMSExtractor

class TestRDBMSIntegration(unittest.TestCase):

    def setUp(self):
        """ Establish a real connection and prepare data """
        # 1. Setup Connection Details
        self.db_url = "postgresql+psycopg2://logi:12345@localhost:5432/structured_data_pipeline"
        self.engine = create_engine(self.db_url)
        self.connection = self.engine.connect()
        
        # 2. Create a real table and insert data for testing
        # We use the 'public' schema as is standard in Postgres
        self.connection.execute(text("DROP TABLE IF EXISTS public.test_users;"))
        self.connection.execute(text("""
            CREATE TABLE public.test_users (
                id SERIAL PRIMARY KEY,
                username VARCHAR(50),
                email VARCHAR(50)
            );
        """))
        self.connection.execute(text("""
            INSERT INTO public.test_users (username, email) 
            VALUES ('logi_user', 'logi@example.com'), ('test_bot', 'bot@example.com');
        """))
        # SQLAlchemy requires a commit for some drivers
        self.connection.execute(text("COMMIT;"))

        # 3. Setup Extractor Config
        self.config = {
            "tables": [
                {
                    "table_name": "test_users",
                    "schema_name": "public",
                    "columns": ["username", "email"]
                }
            ]
        }
        
        # Initialize the REAL extractor with the REAL connection
        self.extractor = RDBMSExtractor(connection=self.connection, config=self.config)

    def tearDown(self):
        """ Clean up the database after the test """
        self.connection.execute(text("DROP TABLE public.test_users;"))
        self.connection.execute(text("COMMIT;"))
        self.connection.close()
        self.engine.dispose()

    def test_actual_extraction(self):
        """ Test if the extractor can pull real data from local Postgres """
        # Act
        results = self.extractor.extract()

        # Assert
        self.assertIn("test_users", results)
        rows = results["test_users"]
        
        self.assertEqual(len(rows), 2)
        self.assertEqual(rows[0]["username"], "logi_user")
        self.assertEqual(rows[1]["username"], "test_bot")
        
        print(f"\nIntegration Test Success! Extracted: {rows}")

if __name__ == '__main__':
    unittest.main()