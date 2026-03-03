"""
Module: Health Queue Pipeline DAG
Purpose: Process health data from queue (health_data_queue table)
         Transform and load into Elasticsearch
"""

import warnings
import logging

warnings.filterwarnings("ignore", category=DeprecationWarning)
logging.getLogger("skops").setLevel(logging.ERROR)

from datetime import datetime
from airflow import DAG
from airflow.operators.python import PythonOperator
from typing import Any, Dict, List

import os
import sys
PROJECT_ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '../../../'))
if PROJECT_ROOT not in sys.path:
    sys.path.insert(0, PROJECT_ROOT)

from src.components.credentials.factory import CredentialFactory
from src.components.connectors.factory import ConnectorFactory
from src.components.extractors.factory import ExtractorFactory
from src.components.transformers.factory import TransformerFactory
from src.components.loaders.factory import LoaderFactory
from src.components.utils.reader import load_yml

logger = logging.getLogger(__name__)

CONFIG_PATH = "/home/surya/aiplatform-general/dags/structure/health/config/health_queue.yml"


def psqlcredential_task(**kwargs: Any) -> Dict[str, Any]:
    """Fetch Postgres credentials for queue database."""
    logger.info("Fetching Postgres credentials for health queue.")
    return CredentialFactory.get_provider(
        mode="airflow",
        conn_id="health_queue_db"
    ).get_credentials()


def escredential_task(**kwargs: Any) -> Dict[str, Any]:
    """Fetch Elasticsearch credentials."""
    logger.info("Fetching Elasticsearch credentials.")
    return CredentialFactory.get_provider(
        mode="airflow",
        conn_id="elasticsearch"
    ).get_credentials()


def extraction_task(ti: Any, **kwargs: Any) -> Dict[str, Any]:
    """Extract pending records from health_data_queue."""
    creds = ti.xcom_pull(task_ids="get_psql_creds")
    creds["type"] = "postgresql+psycopg2"  
    config = load_yml(CONFIG_PATH).get("postgres", {}).get("extraction", {})

    connector = ConnectorFactory.get_connector(connector_type="rdbms", config=creds)
    connection = connector()

    try:
        extractor = ExtractorFactory.get_extractor(
            extractor_type="rdbms",
            connection=connection,
            config=config
        )
        data = extractor()
        
        #Convert UUID to string for serialization
        for table_name, rows in data.items():
            for row in rows:
                if 'queue_id' in row and row['queue_id']:
                    row['queue_id'] = str(row['queue_id'])
        return data
    finally:
        connection.close()


def transformation_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    """Transform extracted records into bulk ES actions."""
    raw_data = ti.xcom_pull(task_ids="extract_queue_data")
    config = load_yml(CONFIG_PATH).get("elasticsearch", {}).get("load", {})

    clean_data = {}
    for table_name, rows in (raw_data or {}).items():
        valid_rows = []
        for row in rows:
            # Remove queue-specific fields
            row.pop("status", None)
            row.pop("retry_count", None)
            row.pop("error_message", None)
            row.pop("created_at", None)
            row.pop("updated_at", None)

            if _is_valid_session_row(row):
                valid_rows.append(row)
            else:
                logger.warning("Dropping invalid row: %s", row)

        clean_data[table_name] = valid_rows

    transformer = TransformerFactory.get_transformer(
        transformer_type="json",
        data=clean_data,
        config=config
    )

    return list(transformer())


def loading_task(ti: Any, **kwargs: Any) -> None:
    """Load transformed records to Elasticsearch."""
    transformed_data = ti.xcom_pull(task_ids="transform_queue_data")
    es_creds = ti.xcom_pull(task_ids="get_es_creds")
     # Fix port mapping from Airflow connection
    if 'schema' in es_creds and not es_creds.get('port'):
        es_creds['port'] = int(es_creds.pop('schema'))
    
    # Set schema to http if not set
    if not es_creds.get('schema'):
        es_creds['schema'] = 'http'
        
    # Disable SSL verification for localhost
    es_creds['verify_certs'] = False
    #es_creds['use_ssl'] = False
    config = load_yml(CONFIG_PATH).get("elasticsearch", {}).get("load", {})

    connector = ConnectorFactory.get_connector(connector_type="elasticsearch", config=es_creds)
    es_connection = connector()

    loader = LoaderFactory.get_loader(
        load_type="elasticsearch",
        connection=es_connection,
        config=config
    )

    loader(data=transformed_data)
    logger.info("Queue data loaded to Elasticsearch successfully.")


def update_status_task(ti: Any, **kwargs: Any) -> None:
    """Update processed records status to 'completed'."""
    raw_data = ti.xcom_pull(task_ids="extract_queue_data") or {}
    rows = raw_data.get("health_data_queue", [])

    if not rows:
        logger.info("No rows to update.")
        return

    creds = ti.xcom_pull(task_ids="get_psql_creds")
    creds["type"] = "postgresql+psycopg2"  # Add type field
    connector = ConnectorFactory.get_connector(connector_type="rdbms", config=creds)
    connection = connector()

    try:
        from sqlalchemy import text
        queue_ids = [row.get("queue_id") for row in rows if row.get("queue_id")]

        if queue_ids:
            placeholders = ','.join([f"'{qid}'" for qid in queue_ids])
            update_sql = text(f"""
                UPDATE health_data_queue
                SET status = 'completed', updated_at = NOW()
                WHERE queue_id IN ({placeholders})
            """)
            connection.execute(update_sql)
            #connection.commit()
            logger.info(f"Updated {len(queue_ids)} records to completed status.")
    finally:
        connection.close()


def _is_valid_session_row(row: Dict[str, Any]) -> bool:
    """Validate session row data."""
    try:
        if row.get("duration_minutes", 0) < 0:
            return False
        if row.get("avg_hr_bpm", 0) < 0 or row.get("max_hr_bpm", 0) < 0:
            return False
        if row.get("avg_hr_bpm", 0) > row.get("max_hr_bpm", 0) and row.get("max_hr_bpm", 0) > 0:
            return False
        return True
    except Exception:
        return False


default_args = {
    "owner": "health_team",
    "retries": 1
}

with DAG(
    "health_queue_pipeline",
    default_args=default_args,
    start_date=datetime(2026, 3, 1),
    schedule="@hourly",
    catchup=False,
    max_active_runs=1,
    tags=["health", "queue", "postgres"]
) as dag:

    get_psql_creds = PythonOperator(
        task_id="get_psql_creds",
        python_callable=psqlcredential_task
    )

    get_es_creds = PythonOperator(
        task_id="get_es_creds",
        python_callable=escredential_task
    )

    extract_queue_data = PythonOperator(
        task_id="extract_queue_data",
        python_callable=extraction_task
    )

    transform_queue_data = PythonOperator(
        task_id="transform_queue_data",
        python_callable=transformation_task
    )

    load_to_es = PythonOperator(
        task_id="load_to_es",
        python_callable=loading_task
    )

    update_status = PythonOperator(
        task_id="update_status",
        python_callable=update_status_task
    )

    # Task dependencies
    get_psql_creds >> extract_queue_data >> transform_queue_data >> load_to_es >> update_status
    get_es_creds >> load_to_es