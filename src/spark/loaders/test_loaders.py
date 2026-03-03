import logging
from pyspark.sql import SparkSession
from src.components.loaders.spark.espark import ElasticsearchSparkLoader

# Setup basic logging to see the progress
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

def run_independent_test():
    # 1. Initialize Spark Session with the ES Connector
    # Replace version with yours (e.g., 8.14.0) if different
    spark = SparkSession.builder \
        .appName("IndependentLoaderTest") \
        .config("spark.jars.packages", "org.elasticsearch:elasticsearch-spark-30_2.12:8.11.0") \
        .master("local[*]") \
        .getOrCreate()

    # 2. Read the "Independent" data from your local storage
    # Note: We point to the FOLDER, not the individual part files
    storage_path = "file:///home/logi/github/alphaesai/aiplatform/static/health_data/embedder"
    
    logger.info(f"Reading data from: {storage_path}")
    df = spark.read.parquet(storage_path)
    
    # 3. Quick sanity check
    count = df.count()
    logger.info(f"Successfully loaded {count} rows into Spark Executors.")
    df.show(5)
    df.printSchema() # Verify 'row_vector' is an ArrayType(FloatType)
    
    # Check for empty vectors
    from pyspark.sql.functions import size, col
    empty_vectors = df.filter(size(col("row_vector")) == 0).count()
    logger.info(f"Found {empty_vectors} rows with empty vectors")
    
    # Filter out empty vectors before sending to ES
    df = df.filter(size(col("row_vector")) > 0)
    filtered_count = df.count()
    logger.info(f"After filtering empty vectors: {filtered_count} rows remaining")

    # 4. Define your ES Configuration
    es_config = {
        "host": "localhost",
        "port": 9200,
        "login": "elastic",
        "password": "yourpassword",
        "index_name": "medical_independent_test",
        "write_mode": "overwrite" # Use 'overwrite' to clear previous tests
    }

    # 5. Initialize and Run the Loader
    # 5. Initialize and Run the Loader
    try:
        # Check ES connectivity manually first if you're worried
        logger.info(f"Connecting to ES at {es_config['host']}:{es_config['port']}...")
        
        loader = ElasticsearchSparkLoader(df, es_config)
        
        # THIS IS THE TRIGGER: This sends data from Slaves to ES
        logger.info("ACTUAL INGESTION STARTING...")
        loader() 
        
        logger.info("Ingestion completed successfully! Check your ES index.")
    except Exception as e:
        logger.error(f"Ingestion failed: {e}")
    finally:
        spark.stop()

if __name__ == "__main__":
    run_independent_test()