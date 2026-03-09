"""
Module: Google Drive Data Pipeline DAG
Purpose: Orchestrates Google Drive data flow:
Credentials -> Extraction -> Transformation -> Embedding -> Loading
"""

import warnings
import logging

warnings.filterwarnings("ignore", category=DeprecationWarning)
warnings.filterwarnings("ignore", module="skops")

logging.getLogger("skops").setLevel(logging.ERROR)

from airflow import DAG
from datetime import datetime
from airflow.providers.standard.operators.python import PythonOperator
from typing import Any, Dict, List

# Factory Imports
from src.components.credentials.factory import CredentialFactory
from src.components.connectors.factory import ConnectorFactory
from src.components.extractors.factory import ExtractorFactory
from src.components.transformers.factory import TransformerFactory
from src.components.loaders.factory import LoaderFactory
from src.components.embedder.factory import EmbedderFactory
from src.components.utils.reader import load_yml

logger = logging.getLogger(__name__)


# -------------------------
# Task Functions
# -------------------------

def credentials_task(**kwargs: Any) -> Dict[str, Any]:
    """
    Fetch Google Drive credentials.
    """
    logger.info("Retrieving Google Drive credentials.")
    return CredentialFactory.get_provider(
        mode="airflow",
        conn_id="googledrive"
    ).get_credentials()


def es_credentials_task(**kwargs: Any) -> Dict[str, Any]:
    """
    Fetch Elasticsearch credentials.
    """
    logger.info("Retrieving Elasticsearch credentials.")
    return CredentialFactory.get_provider(
        mode="airflow",
        conn_id="elasticsearch"
    ).get_credentials()


def extraction_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    """
    Extract files metadata from Google Drive.
    """

    creds = ti.xcom_pull(task_ids='get_credentials')

    full_config = load_yml("dags/unstructure/googledrive/config/config.yml")
    gdrive_config = full_config.get('googledrive_pipeline', {})

    connector = ConnectorFactory.get_connector(
        connector_type="googledrive",
        config=creds
    )

    service = connector()

    extractor = ExtractorFactory.get_extractor(
        extractor_type="googledrive",
        connection=service,
        config=gdrive_config.get('extraction')
    )

    data = list(extractor.extract())

    logger.info(f"Extracted {len(data)} Google Drive files.")
    return data


def transformation_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    """
    Transform Google Drive files into document chunks.
    """

    raw_records = ti.xcom_pull(task_ids='extract_gdrive_data')

    full_config = load_yml("dags/unstructure/googledrive/config/config.yml")
    trans_config = full_config.get('googledrive_pipeline', {}).get('transformation', {})

    transformer = TransformerFactory.get_transformer(
        transformer_type="googledrive",
        data=raw_records,
        config=trans_config
    )

    return list(transformer())


def embedder_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    """
    Generate embeddings for document chunks.
    """

    chunks = ti.xcom_pull(task_ids='transform_data')

    full_config = load_yml("dags/unstructure/googledrive/config/config.yml")
    embed_config = full_config.get('googledrive_pipeline', {}).get('embeddings', {})

    embedder = EmbedderFactory.get_embedder(
        embedder_type="txtai",
        data=chunks,
        config=embed_config
    )

    return list(embedder.embed())


def loading_task(ti: Any, **kwargs: Any) -> None:
    """
    Load enriched data into Elasticsearch.
    """

    enriched_data = ti.xcom_pull(task_ids='generate_embeddings')
    es_creds = ti.xcom_pull(task_ids='get_es_credentials')

    full_config = load_yml("dags/unstructure/googledrive/config/config.yml")
    load_config = full_config.get('googledrive_pipeline', {}).get('load', {})

    connector = ConnectorFactory.get_connector(
        connector_type="elasticsearch",
        config=es_creds
    )

    es_conn = connector()

    loader = LoaderFactory.get_loader(
        load_type="elasticsearch",
        connection=es_conn,
        config=load_config
    )

    loader(data=enriched_data)

    logger.info("Google Drive data loading complete.")


# -------------------------
# DAG Definition
# -------------------------

default_args = {'owner': 'alpha_team', 'retries': 0}

with DAG(
    'googledrive_data_pipeline',
    default_args=default_args,
    start_date=datetime(2026, 1, 1),
    schedule="@monthly",
    catchup=False,
    max_active_runs=1,
    tags=["googledrive", "txtai", "elasticsearch"]
) as dag:

    get_credentials = PythonOperator(
        task_id='get_credentials',
        python_callable=credentials_task
    )

    get_es_credentials = PythonOperator(
        task_id='get_es_credentials',
        python_callable=es_credentials_task
    )

    extract_gdrive_data = PythonOperator(
        task_id='extract_gdrive_data',
        python_callable=extraction_task
    )

    transform_data = PythonOperator(
        task_id='transform_data',
        python_callable=transformation_task
    )

    generate_embeddings = PythonOperator(
        task_id='generate_embeddings',
        python_callable=embedder_task
    )

    load_to_es = PythonOperator(
        task_id='load_to_es',
        python_callable=loading_task
    )

    # Execution Flow
    get_credentials >> extract_gdrive_data >> transform_data >> generate_embeddings >> load_to_es

    [generate_embeddings, get_es_credentials] >> load_to_es