from .ingestor import SingleIngestor, BulkIngestor

class LoaderFactory:
    """
    Factory class to generate the appropriate Elasticsearch ingestor.
    """
    @staticmethod
    def get_loader(load_type, connection, config):
        """
        Returns an instance of a specific ingestor based on the load type.
        
        args:
            load_type (str): The type of loader ('single' or 'bulk').
            connection (Elasticsearch): The established Elasticsearch client.
            config (dict): Configuration dictionary from the loader.yml.
            
        returns:
            Ingestor: An instance of SingleIngestor or BulkIngestor.
        """
        if load_type == "single":
            return SingleIngestor(connection=connection, config=config)
        elif load_type == "bulk":
            return BulkIngestor(connection=connection, config=config)
        else:
            raise ValueError(f"Loader type '{load_type}' is not supported")