import logging
import requests
import os
from pyspark.sql import DataFrame
from pyspark.sql.functions import size, col

logger = logging.getLogger(__name__)

class ElasticsearchSparkLoader:
    def __init__(self, spark, config: dict):
        self.spark = spark
        self.config = config
        self.index_name = config['index_name']
        
        # Paths
        self.pem_path = os.path.abspath(config.get("ssl_pem_path", ""))
        self.jks_path = os.path.abspath(config.get("ssl_jks_path", ""))
        self.jks_pw = config.get("jks_password", "changeit")
        
        # Connection Settings
        self.use_ssl = config.get("use_ssl", "true").lower() == "true"
        self.protocol = "https" if self.use_ssl else "http"
        self.host = config.get("host", "localhost")
        self.port = config.get("port", 9200)
        
        # Authentication
        self.username = config.get("username")
        self.password = config.get("password")
        self.auth = (self.username, self.password) if self.username else None

    def _prepare_index(self):
        """Driver-side check using Python Requests + PEM Certificate."""
        index_url = f"{self.protocol}://{self.host}:{self.port}/{self.index_name}"
        verify_ssl = self.pem_path if self.use_ssl else False
        
        logger.info(f"Pre-flight check: {index_url} (Using PEM: {self.pem_path})")
        
        try:
            # 1. Check Index
            resp = requests.head(index_url, auth=self.auth, verify=verify_ssl, timeout=10)
            
            if resp.status_code != 200:
                logger.info(f"Creating index {self.index_name}...")
                payload = {
                    "settings": self.config.get("settings", {}),
                    "mappings": self.config.get("mappings", {})
                }
                requests.put(index_url, json=payload, auth=self.auth, verify=verify_ssl).raise_for_status()
            
            # 2. Wait for Health
            health_url = f"{self.protocol}://{self.host}:{self.port}/_cluster/health/{self.index_name}?wait_for_status=yellow"
            requests.get(health_url, auth=self.auth, verify=verify_ssl, timeout=15).raise_for_status()
            
        except Exception as e:
            logger.error(f"Driver failed to connect to ES: {e}")
            raise

    def load(self, df: DataFrame):
        """Executor-side load using Spark + JKS Truststore."""
        self._prepare_index()

        vector_col = self.config.get("vector_column", "row_vector")
        df_clean = df.filter(size(col(vector_col)) > 0)

        es_options = {
            "es.nodes": self.host,
            "es.port": str(self.port),
            "es.resource": self.index_name,
            "es.mapping.id": "id",
            "es.write.operation": "index",
            
            # --- Connectivity & Network ---
            "es.nodes.wan.only": "true",       # Essential for Docker/Cloud/Localhost bridge
            "es.nodes.discovery": "false",     # Set to false when wan.only is true
            "es.protocol": self.protocol,
            
            # --- SSL & Security (JKS) ---
            "es.net.ssl": "true" if self.use_ssl else "false",
            "es.net.ssl.truststore.location": f"file://{self.jks_path}",
            "es.net.ssl.truststore.pass": self.jks_pw,
            "es.net.ssl.truststore.type": "JKS",
            "es.net.ssl.cert.allow.self.signed": "true",
            "es.net.http.auth.user": self.username or "",
            "es.net.http.auth.pass": self.password or "",

            # --- High Volume / Performance Tuning ---
            "es.batch.size.entries": "1000",      # Number of docs per batch
            "es.batch.size.bytes": "10mb",        # Max size per batch
            "es.batch.write.retry.count": "3",    # Retry on 429 (Circuit Breaker) errors
            "es.batch.write.retry.wait": "10s",   # Wait time between retries
            "es.http.timeout": "5m",              # Higher timeout for large vectors
            
            # --- Metadata Handling ---
            "es.mapping.exclude": "id",           # Prevents 'id' from being duplicated in the _source
            "es.input.json": "false"              # Set to true only if input is already JSON strings
        }

        logger.info(f"Handing off to Spark for distributed load...")
        (df_clean.write
            .format("org.elasticsearch.spark.sql")
            .options(**es_options)
            .mode(self.config.get("write_mode", "append"))
            .save())

    def __call__(self, df: DataFrame):
        self.load(df)