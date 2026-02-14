import logging
import requests
import urllib3
from pyspark.sql.functions import size, col

# Disable SSL warnings for local dev
urllib3.disable_warnings(urllib3.exceptions.InsecureRequestWarning)
logger = logging.getLogger(__name__)

class ElasticsearchSparkLoader:
    def __init__(self, spark, config: dict):
        self.spark = spark
        self.config = config
        self.vector_col = config.get("vector_column", "row_vector")
        self.checkpoint_path = config.get("checkpoint_path")
        
        # Build Base URL
        host = self.config.get("host", "localhost").replace("http://", "").replace("https://", "")
        self.base_url = f"http://{host}:{self.config.get('port', 9200)}"
        self.index_url = f"{self.base_url}/{self.config['index_name']}"

    def _ensure_index(self):
        """Creates index and BLOCKS until the primary shard is active."""
        try:
            # 1. Check if exists
            check = requests.head(self.index_url, verify=False, timeout=5)
            
            if check.status_code == 200:
                logger.info(f"Index '{self.config['index_name']}' already exists.")
                return

            # 2. Create Index with minimal settings
            logger.info(f"Creating index '{self.config['index_name']}'...")
            payload = {
                "settings": self.config.get("settings", {}),
                "mappings": self.config.get("mappings", {})
            }
            create_res = requests.put(self.index_url, json=payload, verify=False)
            
            if create_res.status_code != 200:
                logger.error(f"CRITICAL: Failed to create index. ES Response: {create_res.text}")
                raise RuntimeError(f"Index creation failed: {create_res.text}")

            # 3. BLOCKING WAIT: This prevents the 'unavailable_shards_exception'
            logger.info("Waiting for primary shard to activate (status=yellow)...")
            health_url = f"{self.base_url}/_cluster/health/{self.config['index_name']}?wait_for_status=yellow&timeout=30s"
            health_res = requests.get(health_url, verify=False)
            
            if health_res.status_code != 200:
                logger.error("CRITICAL: Index created but shard failed to activate in time.")
                raise RuntimeError("Shard activation timeout.")
            
            logger.info("Index is Green/Yellow and ready for ingestion.")

        except Exception as e:
            logger.error(f"Setup Failed: {str(e)}")
            raise

    def load(self):
        # 1. Read Data
        logger.info(f"Loading Parquet from: {self.checkpoint_path}")
        df = self.spark.read.parquet(self.checkpoint_path)

        # 2. Ensure Index Readiness
        self._ensure_index()

        # 3. Clean Data
        df_clean = df.filter(size(col(self.vector_col)) > 0)

        # 4. Barebones ES Options
        clean_host = self.config.get("host", "localhost").replace("http://", "").replace("https://", "").rstrip("/")
        es_options = {
            "es.nodes": clean_host,
            "es.port": str(self.config.get("port", 9200)),
            "es.resource": self.config["index_name"],
            "es.mapping.id": "id",
            "es.nodes.wan.only": "true",
            "es.nodes.discovery": "false",
            "es.write.operation": "index",
            "es.batch.write.retry.count": "0",    # Fail immediately on error
            "es.batch.size.entries": "1000",      # Standard batch
            "es.http.timeout": "5m"               # Give it plenty of time
        }

        # 5. Write
        try:
            logger.info("Starting ingestion...")
            (df_clean.write
                .format("org.elasticsearch.spark.sql")
                .options(**es_options)
                .mode(self.config.get("write_mode", "append"))
                .save())
            logger.info("Ingestion completed successfully.")
        except Exception as e:
            logger.error(f"INGESTION FAILED: {str(e)}")
            raise

    def __call__(self):
        self.load()