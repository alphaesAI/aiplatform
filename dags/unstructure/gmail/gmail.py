"""
Module: Gmail Data Pipeline DAG
Purpose: Orchestrates Gmail data flow: Credentials -> Extraction -> Transformation -> Embedding -> Loading.
"""

import warnings
import logging

# --- CLEAN WARNING BLOCK ---
# 1. Ignore all deprecation warnings (The broad approach)
warnings.filterwarnings("ignore", category=DeprecationWarning)

# 2. Specifically ignore Airflow's internal "attribute is deprecated" spam
warnings.filterwarnings("ignore", message=".*attribute is deprecated.*")

# 3. Specifically ignore the 'skops' module's import warnings
try:
    from airflow.exceptions import DeprecatedImportWarning
    warnings.filterwarnings("ignore", category=DeprecatedImportWarning, module="skops")
except ImportError:
    pass

# 4. Turn off skops logging spam
logging.getLogger("skops").setLevel(logging.ERROR)
# --- END WARNING BLOCK ---

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


# --- Task Functions ---

def credentials_task(**kwargs: Any) -> Dict[str, Any]:
    return CredentialFactory.get_provider(mode="airflow", conn_id="gmail").get_credentials()

def es_credentials_task(**kwargs: Any) -> Dict[str, Any]:
    return CredentialFactory.get_provider(mode="airflow", conn_id="elasticsearch").get_credentials()

def extraction_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    creds = ti.xcom_pull(task_ids='get_credentials')
    creds['token_dict'] = load_pickle(creds['token_path'])
    
    full_config = load_yml("dags/unstructure/gmail/config/extractor.yml")
    gmail_config = full_config.get('gmail_pipeline', {})
    
    connector = ConnectorFactory.get_connector(connector_type="gmail", config=creds)
    service = connector()
    extractor = ExtractorFactory.get_extractor("gmail", service, gmail_config.get('extraction'))
    
    return list(extractor.extract())

def transformation_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    raw_records = ti.xcom_pull(task_ids='extract_gmail_data')
    full_config = load_yml("dags/unstructure/gmail/config/extractor.yml")
    trans_config = full_config.get('gmail_pipeline', {}).get('transformation', {})
    
    transformer = TransformerFactory.get_transformer(
        transformer_type="document", 
        data=raw_records, 
        config=trans_config
    )
    # Important: Return as list for the next task (Embeddings)
    return list(transformer())

def embedding_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    chunks = ti.xcom_pull(task_ids='transform_data')
    # Use the same config file, just get the embedding section
    embed_config = load_yml("dags/unstructure/gmail/config/extractor.yml").get('gmail_pipeline', {}).get('embeddings', {})
    
    embedder = Embeddings(embed_config)
    # Returns enriched dicts: {"_index":..., "_source": {... , "vector": [...]}}
    return list(embedder.embed(chunks))

def loading_task(ti: Any, **kwargs: Any) -> None:
    enriched_data = ti.xcom_pull(task_ids='generate_embeddings')
    es_creds = ti.xcom_pull(task_ids='get_es_credentials')
    load_config = load_yml("dags/unstructure/gmail/config/extractor.yml").get('gmail_pipeline', {}).get('load', {})

    connector = ConnectorFactory.get_connector(connector_type="elasticsearch", config=es_creds)
    es_conn = connector()
    
    # Get BulkIngestor via Factory
    loader = LoaderFactory.get_loader(load_type="bulk", connection=es_conn, config=load_config)
    loader(data=enriched_data)

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

    # 1. Credentials
    gmail_creds = PythonOperator(task_id='get_credentials', python_callable=credentials_task)
    es_creds = PythonOperator(task_id='get_es_credentials', python_callable=es_credentials_task)
    
    # 2. Extract
    extraction = PythonOperator(task_id='extract_gmail_data', python_callable=extraction_task)
    
    # 3. Transform (Chunks text and adds _index/_source wrapper)
    transformation = PythonOperator(task_id='transform_data', python_callable=transformation_task)
    
    # 4. Embed (Adds "vector" key inside _source)
    embedding = PythonOperator(task_id='generate_embeddings', python_callable=embedding_task)
    
    # 5. Load (Pushes to Elasticsearch)
    loading = PythonOperator(task_id='load_to_es', python_callable=loading_task)

    # Dependencies
    [gmail_creds, es_creds] >> extraction >> transformation >> embedding >> loading