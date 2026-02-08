import logging
from typing import Dict, Any
from pyspark.sql import DataFrame

logger = logging.getLogger(__name__)

class ElasticsearchSparkLoader:
    def __init__(self, data: DataFrame, config: Dict[str, Any]):
        """
        Args:
            data (DataFrame): The Spark DataFrame to be ingested.
            config (Dict): A single dictionary containing BOTH credentials 
                          and indexing settings.
        """
        self.data = data
        self.config = config

    def __call__(self):
        """Triggers the Spark load."""
        self.load()

    def load(self):
        index_name = self.config.get("index_name")
        # Change default to 'append' to prevent overwrite-mapping conflicts during init
        mode = self.config.get("write_mode", "append") 

        # Clean the host (remove http/https and trailing slashes)
        raw_host = self.config.get("host", "localhost")
        clean_host = raw_host.replace("https://", "").replace("http://", "").rstrip("/")

        es_options = {
            "es.nodes": clean_host,
            "es.port": str(self.config.get("port", 9200)),
            "es.net.http.auth.user": self.config.get("login"),
            "es.net.http.auth.pass": self.config.get("password"),
            "es.resource": index_name,
            "es.nodes.wan.only": "true",       # Essential for local/cloud connections
            "es.nodes.discovery": "false",    # STOP Spark from looking for internal IPs
            "es.index.auto.create": "true",   # Force creation if missing
            "es.input.json": "false",
            "es.write.operation": "index"     # Standard ingestion operation
        }

        logger.info(f"Writing to ES Index: {index_name} at {clean_host}:{es_options['es.port']}")

        try:
            (self.data.write
                .format("org.elasticsearch.spark.sql")
                .options(**es_options)
                .mode(mode)
                .save())
            logger.info("Distributed ingestion complete.")
        except Exception as e:
            logger.error(f"Spark-ES Save Failed: {str(e)}")
            raise
