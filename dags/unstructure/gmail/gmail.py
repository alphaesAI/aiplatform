"""
Module: Gmail Data Pipeline DAG
Purpose: Orchestrates Gmail data flow: Credentials -> Extraction -> Transformation -> Embedding -> Loading.
"""

import warnings
import logging
from airflow import DAG
from datetime import datetime, timedelta
from airflow.providers.standard.operators.python import PythonOperator
from typing import Any, Dict, List

# Factory Imports
from src.custom.credentials.factory import CredentialFactory
from src.custom.connectors.factory import ConnectorFactory
from src.custom.extractors.factory import ExtractorFactory
from src.custom.transformers.factory import TransformerFactory
from src.custom.loaders.factory import LoaderFactory
from src.custom.loaders.embeddings import Embeddings
from src.custom.utils.reader import load_yml, load_pickle

logger = logging.getLogger(__name__)

# --- CLEAN WARNING BLOCK ---
warnings.filterwarnings("ignore", category=DeprecationWarning)
warnings.filterwarnings("ignore", message=".*attribute is deprecated.*")

try:
    from airflow.exceptions import DeprecatedImportWarning
    warnings.filterwarnings("ignore", category=DeprecatedImportWarning, module="skops")
except ImportError:
    pass

logging.getLogger("skops").setLevel(logging.ERROR)
# --- END WARNING BLOCK ---

def credentials_task(**kwargs: Any) -> Dict[str, Any]:
    """
    Purpose: 
        Fetches Gmail credentials via the CredentialFactory.
    Args:
        **kwargs: Airflow context arguments.
    Returns:
        Dict[str, Any]: The credential dictionary.
    """
    logger.info("Retrieving Gmail credentials.")
    return CredentialFactory.get_provider(mode="airflow", conn_id="gmail").get_credentials()

def es_credentials_task(**kwargs: Any) -> Dict[str, Any]:
    """
    Purpose: 
        Fetches Elasticsearch credentials via the CredentialFactory.
    Args:
        **kwargs: Airflow context arguments.
    Returns:
        Dict[str, Any]: The credential dictionary.
    """
    logger.info("Retrieving Elasticsearch credentials.")
    return CredentialFactory.get_provider(mode="airflow", conn_id="elasticsearch").get_credentials()

def extraction_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    """
    Purpose: 
        Connects to Gmail and extracts raw message data.
    Args:
        ti: Airflow Task Instance for XCom access.
    Returns:
        List[Dict[str, Any]]: List of extracted raw records.
    """
    creds = ti.xcom_pull(task_ids='get_credentials')
    creds['token_dict'] = load_pickle(creds['token_path'])
    
    full_config = load_yml("dags/unstructure/gmail/config/extractor.yml")
    gmail_config = full_config.get('gmail_pipeline', {})
    
    connector = ConnectorFactory.get_connector(connector_type="gmail", config=creds)
    service = connector()
    extractor = ExtractorFactory.get_extractor("gmail", service, gmail_config.get('extraction'))
    
    data = list(extractor.extract())
    logger.info(f"Extracted {len(data)} records from Gmail.")
    return data

def transformation_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    """
    Purpose: 
        Transforms raw records into segmented document chunks.
    Args:
        ti: Airflow Task Instance for XCom access.
    Returns:
        List[Dict[str, Any]]: List of transformed chunks.
    """
    raw_records = ti.xcom_pull(task_ids='extract_gmail_data')
    full_config = load_yml("dags/unstructure/gmail/config/extractor.yml")
    trans_config = full_config.get('gmail_pipeline', {}).get('transformation', {})
    
    transformer = TransformerFactory.get_transformer(
        transformer_type="document", 
        data=raw_records, 
        config=trans_config
    )
    return list(transformer())

def embedding_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    """
    Purpose: 
        Generates semantic vector embeddings for the text chunks.
    Args:
        ti: Airflow Task Instance for XCom access.
    Returns:
        List[Dict[str, Any]]: Records enriched with vector data.
    """
    chunks = ti.xcom_pull(task_ids='transform_data')
    embed_config = load_yml("dags/unstructure/gmail/config/extractor.yml").get('gmail_pipeline', {}).get('embeddings', {})
    
    embedder = Embeddings(embed_config)
    return list(embedder.embed(chunks))

def loading_task(ti: Any, **kwargs: Any) -> None:
    """
    Purpose: 
        Loads the final enriched records into Elasticsearch.
    Args:
        ti: Airflow Task Instance for XCom access.
    """
    enriched_data = ti.xcom_pull(task_ids='generate_embeddings')
    es_creds = ti.xcom_pull(task_ids='get_es_credentials')
    load_config = load_yml("dags/unstructure/gmail/config/extractor.yml").get('gmail_pipeline', {}).get('load', {})

    connector = ConnectorFactory.get_connector(connector_type="elasticsearch", config=es_creds)
    es_conn = connector()
    
    loader = LoaderFactory.get_loader(load_type="bulk", connection=es_conn, config=load_config)
    loader(data=enriched_data)
    logger.info("Gmail data loading complete.")

# --- DAG Definition ---
default_args = {'owner': 'data_team', 'retries': 0}

with DAG(
    'gmail_data_pipeline', 
    default_args=default_args,
    start_date=datetime(2025, 1, 1), 
    schedule="@daily", 
    catchup=False,
    tags=["gmail", "txtai", "elasticsearch"]
) as dag:

    get_credentials = PythonOperator(task_id='get_credentials', python_callable=credentials_task)
    get_es_credentials = PythonOperator(task_id='get_es_credentials', python_callable=es_credentials_task)
    extract_gmail_data = PythonOperator(task_id='extract_gmail_data', python_callable=extraction_task)
    transform_data = PythonOperator(task_id='transform_data', python_callable=transformation_task)
    generate_embeddings = PythonOperator(task_id='generate_embeddings', python_callable=embedding_task)
    load_to_es = PythonOperator(task_id='load_to_es', python_callable=loading_task)

    [get_credentials, get_es_credentials] >> extract_gmail_data >> transform_data >> generate_embeddings >> load_to_es