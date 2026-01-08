import warnings
import logging

# Standard ignore for common Airflow and library warnings
warnings.filterwarnings("ignore", category=DeprecationWarning)
# Specific ignore for the skops/txtai noise
warnings.filterwarnings("ignore", module="skops")

# Set external loggers to ERROR only to reduce noise
logging.getLogger("skops").setLevel(logging.ERROR)

from datetime import datetime, timedelta
from airflow import DAG
from airflow.providers.standard.operators.python import PythonOperator

# Internal imports
from src.custom.credentials.factory import CredentialFactory
from src.custom.connectors.factory import ConnectorFactory
from src.custom.extractors.factory import ExtractorFactory
from src.custom.transformers.factory import TransformerFactory
from src.custom.loaders.factory import LoaderFactory
from src.custom.utils.reader import load_yml

# Setup logger for the DAG
logger = logging.getLogger(__name__)

"""
health_dag.py
====================================
Purpose:
    Main Airflow DAG orchestrating the 'Health' data pipeline. 
    It extracts data from a PostgreSQL database, transforms it into an 
    Elasticsearch-compatible JSON format, and loads it via bulk ingestion.
"""

def psqlcredential(**kwargs):
    """
    Purpose:
        Retrieves PostgreSQL credentials using the Airflow Connection ID.

    Returns:
        dict: Standardized credential dictionary.
    """
    logger.info("Fetching Postgres credentials via CredentialFactory.")
    provider = CredentialFactory.get_provider(mode="airflow", conn_id="healthdb")
    return provider.get_credentials()

def escredential(**kwargs):
    """
    Purpose:
        Retrieves Elasticsearch credentials using the Airflow Connection ID.

    Returns:
        dict: Standardized credential dictionary.
    """
    logger.info("Fetching Elasticsearch credentials via CredentialFactory.")
    provider = CredentialFactory.get_provider(mode="airflow", conn_id="elasticsearch")
    return provider.get_credentials()

def extraction(ti, **kwargs):
    """
    Purpose:
        Connects to RDBMS and extracts raw data as a Python dictionary.

    Args:
        ti (TaskInstance): Airflow Task Instance for XCom pulling.

    Returns:
        dict: Raw data extracted from specified tables.
    """
    creds = ti.xcom_pull(task_ids='psqlcredential_task')
    if not creds:
        error_msg = "Extraction failed: No Postgres credentials found in XCom."
        logger.error(error_msg)
        raise ValueError(error_msg)
    
    config_path = "dags/structure/health/config/extractor.yml"
    full_yml = load_yml(config_path)
    extract_config = full_yml.get("postgres", {}).get("extraction", {})

    logger.info("Initializing RDBMS connector and establishing connection.")
    connector = ConnectorFactory.get_connector(connector_type="rdbms", config=creds)
    connection = connector()

    try:
        extractor = ExtractorFactory.get_extractor(
            extractor_type="rdbms", 
            connection=connection, 
            config=extract_config
        )
        data_json = extractor() 
        logger.info("Data extraction from RDBMS successful.")
        return data_json 
    finally:
        logger.debug("Closing RDBMS database connection.")
        connection.close()

def process_and_load(ti, **kwargs):
    """
    Purpose:
        Orchestrates the Transform-and-Load phase. It uses a Generator 
        to stream data from the Transformer to the Loader without 
        loading the entire transformed dataset into memory.

    Args:
        ti (TaskInstance): Airflow Task Instance for XCom pulling.
    """
    # Pull data and credentials
    raw_data = ti.xcom_pull(task_ids='extraction_task')
    es_creds = ti.xcom_pull(task_ids='escredential_task')

    if not raw_data:
        error_msg = "Process and Load failed: No raw data received from extraction task."
        logger.error(error_msg)
        raise ValueError(error_msg)

    # Load configuration
    config_path = "dags/structure/health/config/loader.yml"
    loader_config = load_yml(config_path).get("elasticsearch", {}).get("load", {})

    # --- TRANSFORM ---
    logger.info("Initializing JsonTransformer to create actions generator.")
    transformer_obj = TransformerFactory.get_transformer(
        transformer_type="json",
        data=raw_data,
        config=loader_config
    )
    
    # This creates the generator iterator
    actions_generator = transformer_obj()

    # --- LOAD ---
    logger.info("Initializing Elasticsearch connector for ingestion.")
    connector = ConnectorFactory.get_connector(connector_type="elasticsearch", config=es_creds)
    es_connection = connector()

    loader_obj = LoaderFactory.get_loader(
        load_type="bulk",
        connection=es_connection,
        config=loader_config
    )

    logger.info("Starting bulk ingestion to Elasticsearch via generator streaming.")
    loader_obj(data=actions_generator)
    logger.info("Pipeline execution finished: Successfully transformed and loaded data.")

# --- DAG Definition ---



default_args = {
    'owner': 'data_team',
    'retries': 0,
    'retry_delay': timedelta(minutes=2)
}

with DAG(
    'health_data_pipeline',
    default_args=default_args,
    start_date=datetime(2025, 1, 1),
    schedule="@daily",
    catchup=False,
    doc_md=__doc__,
    tags=["structure", "health"]
) as dag:

    psql_creds_task = PythonOperator(
        task_id='psqlcredential_task',
        python_callable=psqlcredential,
    )

    es_creds_task = PythonOperator(
        task_id='escredential_task',
        python_callable=escredential,
    )

    extract_task = PythonOperator(
        task_id='extraction_task',
        python_callable=extraction,
    )

    transform_and_load_task = PythonOperator(
        task_id='transform_and_load_task',
        python_callable=process_and_load,
    )

    # Dependency Flow
    [psql_creds_task, es_creds_task] >> extract_task >> transform_and_load_task