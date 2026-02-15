import logging
import requests
from pyspark.sql import DataFrame
from pyspark.sql.functions import size, col

logger = logging.getLogger(__name__)

class ElasticsearchSparkLoader:
    def __init__(self, spark, config: dict):
        self.spark = spark
        self.config = config
        self.index_name = config['index_name']
        
        # Build URL for pre-ingestion check
        host = config.get("host", "localhost").replace("http://", "").replace("https://", "")
        self.base_url = f"http://{host}:{config.get('port', 9200)}"

    def _prepare_index(self):
        """Uses YAML settings to ensure ES is ready."""
        index_url = f"{self.base_url}/{self.index_name}"
        
        # 1. Check if index exists
        response = requests.head(index_url, timeout=5)
        
        if response.status_code != 200:
            logger.info(f"Creating index {self.index_name} with YAML specs...")
            # 2. Use settings and mappings DIRECTLY from YAML
            payload = {
                "settings": self.config.get("settings", {}),
                "mappings": self.config.get("mappings", {})
            }
            requests.put(index_url, json=payload).raise_for_status()
            
            # 3. Wait for green/yellow status before starting Spark
            health_url = f"{self.base_url}/_cluster/health/{self.index_name}?wait_for_status=yellow"
            requests.get(health_url).raise_for_status()

    def load(self, df: DataFrame):
        """The main Spark-distributed loading logic."""
        # Run pre-flight check on Driver
        self._prepare_index()

        # Filter out any rows where embedding failed
        vector_col = self.config.get("vector_column", "row_vector")
        df_clean = df.filter(size(col(vector_col)) > 0)

        # ES-Spark Native Options
        es_options = {
            "es.nodes": self.config.get("host"),
            "es.port": str(self.config.get("port")),
            "es.resource": self.index_name,
            "es.mapping.id": "id",
            "es.write.operation": "index",
            "es.nodes.wan.only": "true",
            "es.batch.write.retry.count": "5",
            "es.batch.size.bytes": "10mb",
            "es.http.timeout": "5m"
        }

        logger.info(f"Starting distributed load to index: {self.index_name}")
        (df_clean.write
            .format("org.elasticsearch.spark.sql")
            .options(**es_options)
            .mode(self.config.get("write_mode", "append"))
            .save())

    def __call__(self, df: DataFrame):
        self.load(df)