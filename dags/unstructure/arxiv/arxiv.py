"""
Module: arXiv Data Pipeline - Credentials Phase
Purpose: Orchestrates the retrieval of credentials for arXiv and Elasticsearch.
"""

import warnings
import httpx
import logging
from airflow import DAG
from datetime import datetime
from airflow.providers.standard.operators.python import PythonOperator
from typing import Any, Dict

from src.custom.credentials.factory import CredentialFactory
from src.custom.connectors.factory import ConnectorFactory

logger = logging.getLogger(__name__)

def arxiv_credentials(**kwargs: Any) -> Dict[str, Any]:
    """
    Purpose: 
        Retrieves arXiv specific credentials or API configurations from Airflow.
    Args:
        **kwargs: Airflow context arguments.
    Returns:
        Dict[str, Any]: Credential dictionary.
    """
    logger.info("Retrieving arXiv credentials.")
    return CredentialFactory.get_provider(mode="airflow", conn_id="arxiv").get_credentials()

def arxiv_connection(ti, **kwargs: Any) -> str:
    """
    Purpose: 
        Initializes the ArxivConnector to verify the connection.
    Args:
        ti: Airflow Task Instance for XCom access.
    Returns:
        str: Success message.
    """
    config = ti.xcom_pull(task_ids="arxiv_credentials")

    if not config:
        raise ValueError("No config found in XCom!")

    connector = ConnectorFactory.get_connector(connector_type="arxiv", config=config)
    logger.info("arXiv connection initialized.")
    return "connection created successfully."

def es_credentials(**kwargs: Any) -> Dict[str, Any]:
    """
    Purpose: 
        Retrieves Elasticsearch credentials for the loading phase.
    Args:
        **kwargs: Airflow context arguments.
    Returns:
        Dict[str, Any]: Credential dictionary.
    """
    logger.info("Retrieving Elasticsearch credentials.")
    return CredentialFactory.get_provider(
        mode="airflow", 
        conn_id="elasticsearch"
    ).get_credentials()

default_args = {
    'owner': 'data_team',
    'retries': 0,
    'execution_timeout': None
}

with DAG(
    'arxive_pipeline',
    default_args=default_args,
    start_date=datetime(2026, 1, 1),
    schedule="@daily",
    catchup=False,
    tags=["arxiv", "txtai", "elasticsearch"]
) as dag:

    arxiv_credentials_task = PythonOperator(
        task_id='arxiv_credentials',
        python_callable=arxiv_credentials,
    )

    arxiv_connection_task = PythonOperator(
        task_id='arxiv_connection',
        python_callable=arxiv_connection,
    )

    es_credentials_task = PythonOperator(
        task_id='es_credentials',
        python_callable=es_credentials,
    )

    [arxiv_credentials_task, es_credentials_task] >> arxiv_connection_task