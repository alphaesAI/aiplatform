from opensearchpy import OpenSearch

class OpensearchConnector:
    def __init__(self, config: dict):
        self.config = config
        self._client = None
    
    def __call__(self):
        self.connect()
        return self._client

    def connect(self):
        protocol = self.config.get("schema") # http or https
        host = self.config.get("host")
        port = self.config.get("port")

        openserach_host = f"{protocol}://{host}:{port}"  #http://localhost:9200

        self._client = OpenSearch(
            [openserach_host],
            verify_certs=self.config.get("verify_certs", False)
        )

        if not self._client.ping():
            raise ConnectionError(f"Could not connect to OpenSearch at {openserach_host}")