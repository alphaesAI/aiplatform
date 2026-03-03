import logging
import yaml
from pyspark.sql import SparkSession

from src.components.credentials.factory import CredentialFactory
from src.spark.connectors.factory import ConnectorFactory
from src.spark.extractors.factory import ExtractorFactory
from src.spark.transformers.factory import TransformerFactory
from src.spark.embedders.factory import EmbedderFactory
from src.spark.loaders.factory import LoaderFactory
from src.components.utils import load_yml

# Path to our updated YAML
CONFIG_PATH = "/home/logi/github/alphaesai/aiplatform/dags/structure/aws/config/config.yml"
config = load_yml(CONFIG_PATH)

logger = logging.getLogger(__name__)

def spark_pipeline():
    # 1. CREDENTIAL
    logger.info("Fetching S3 credentials from Local Connection.")
    creds = CredentialFactory.get_provider(mode="local", conn_id="s3").get_credentials()

    # 2. CONNECTOR
    connector = ConnectorFactory.create("spark", creds)
    spark = connector.connect()

    # 3. EXTRACT
    extractor = ExtractorFactory.create("table", spark, config['extractor'])
    df = extractor.extract()

    # 4. TRANSFORM (Cleaning & Structing)
    transformer = TransformerFactory.create("table", df, config['transformation'])
    df_transformed = transformer.transform()

    # 5. EMBED (Vectorizing via mapInPandas)
    embedder = EmbedderFactory.create("spark", df_transformed, config['embedding'])
    df_embedded = embedder.embed()
    
    # Debug: Show result
    print("✅ Embedding completed!")
    print(f"DataFrame schema: {df_embedded.schema}")
    print(f"Sample data:")
    df_embedded.show(1, truncate=80)

    # 6. LOAD (Push to Elasticsearch)
    loader = LoaderFactory.create("elasticsearch", df_embedded, config['elasticsearch'])
    loader.load(df_embedded)
    print("🎉 Pipeline completed successfully! Data loaded to Elasticsearch!")

    spark.stop()

if __name__ == "__main__":
    spark_pipeline()