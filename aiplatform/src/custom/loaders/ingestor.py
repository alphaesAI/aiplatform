from elasticsearch import helpers
from .base import BaseLoader

class Ingestor(BaseLoader):
    """
    Parent class to handle common connection and index setup.
    """
    def __init__(self, connection, config):
        """
        Initializes the ingestor.
        args:
            connection (Elasticsearch): The ES client.
            config (dict): YAML configuration.
        """
        self.connection = connection
        self.config = config

    def create(self):
        """
        Create the index if it doesn't exist.
        returns:
            None
        """
        name = self.config.get("index_name")
        body = {
            "settings": self.config.get("settings", {}),
            "mappings": self.config.get("mappings", {})
        }
        if not self.connection.indices.exists(index=name):
            self.connection.indices.create(index=name, body=body)
            print(f"Index {name} created.")

    def __call__(self, data):
        """
        The Gatekeeper: Ensures index exists, then runs the specific load logic.
        args:
            data (dict): Data to be loaded.
        """
        # 1. Always check the index first
        self.create()
        
        # 2. Run the specific logic of the child class
        return self.load(data)

class SingleIngestor(Ingestor):
    def load(self, data):
        """
        Loads data row by row.
        args:
            data (dict): Table-mapped rows.
        """
        for action in data:
            self.connection.index(
                index=action["_index"],
                document=action["_source"]
            )
            
class BulkIngestor(Ingestor):
    def load(self, data):
        """
        Loads data in bulk batches.
        args:
            data (dict): Table-mapped rows.
        """
        
        helpers.bulk(self.connection, data)