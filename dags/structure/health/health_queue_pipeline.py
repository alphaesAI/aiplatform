"""
Module: Health Queue Pipeline DAG
Purpose: Process queue data from health_data_queue table
         and load into Elasticsearch.
"""

import warnings
import logging

warnings.filterwarnings("ignore", category=DeprecationWarning)
warnings.filterwarnings("ignore", module="skops")
logging.getLogger("skops").setLevel(logging.ERROR)

from datetime import datetime
from airflow import DAG
from airflow.providers.standard.operators.python import PythonOperator
from typing import Any, Dict, List

from src.components.credentials.factory import CredentialFactory
from src.components.connectors.factory import ConnectorFactory
from src.components.extractors.factory import ExtractorFactory
from src.components.transformers.factory import TransformerFactory
from src.components.loaders.factory import LoaderFactory
from src.components.utils.reader import load_yml

logger = logging.getLogger(__name__)

CONFIG_PATH = "dags/structure/health/config/health_queue.yml"


# ---------- Task Functions ----------

def psqlcredential_task(**kwargs: Any) -> Dict[str, Any]:
    """Fetch Postgres credentials."""
    logger.info("Fetching Postgres credentials for queue.")
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
    """Extract pending rows from health_data_queue."""
    creds = ti.xcom_pull(task_ids="get_psql_creds")
    creds["type"] = "postgresql+psycopg2"

    config = load_yml(CONFIG_PATH).get("postgres", {}).get("extraction", {})

    connector = ConnectorFactory.get_connector(
        connector_type="rdbms",
        config=creds
    )

    connection = connector()

    try:
        extractor = ExtractorFactory.get_extractor(
            extractor_type="rdbms",
            connection=connection,
            config=config
        )

        data = extractor()

        # convert UUID → string for XCom serialization
        for table, rows in data.items():
            for row in rows:
                if row.get("queue_id"):
                    row["queue_id"] = str(row["queue_id"])

        return data

    finally:
        connection.close()


def transformation_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    """Transform queue rows into Elasticsearch bulk actions."""
    raw_data = ti.xcom_pull(task_ids="extract_queue_data")

    config = load_yml(CONFIG_PATH).get("elasticsearch", {}).get("load", {})

    transformer = TransformerFactory.get_transformer(
        transformer_type="json",
        data=raw_data,
        config=config
    )

    return list(transformer())


def loading_task(ti: Any, **kwargs: Any) -> None:
    """Load transformed data to Elasticsearch."""
    transformed_data = ti.xcom_pull(task_ids="transform_queue_data")
    es_creds = ti.xcom_pull(task_ids="get_es_creds")

    if "schema" in es_creds and not es_creds.get("port"):
        es_creds["port"] = int(es_creds.pop("schema"))

    if not es_creds.get("schema"):
        es_creds["schema"] = "http"

    es_creds["verify_certs"] = False

    config = load_yml(CONFIG_PATH).get("elasticsearch", {}).get("load", {})

    connector = ConnectorFactory.get_connector(
        connector_type="elasticsearch",
        config=es_creds
    )

    es_connection = connector()

    loader = LoaderFactory.get_loader(
        load_type="elasticsearch",
        connection=es_connection,
        config=config
    )

    loader(data=transformed_data)

    logger.info("Queue data loaded successfully.")


def update_status_task(ti: Any, **kwargs: Any) -> None:
    """Update processed queue rows to completed."""
    raw_data = ti.xcom_pull(task_ids="extract_queue_data") or {}
    rows = raw_data.get("health_data_queue", [])

    if not rows:
        logger.info("No queue records to update.")
        return

    creds = ti.xcom_pull(task_ids="get_psql_creds")
    creds["type"] = "postgresql+psycopg2"

    connector = ConnectorFactory.get_connector(
        connector_type="rdbms",
        config=creds
    )

    connection = connector()

    try:
        from sqlalchemy import text

        queue_ids = [row["queue_id"] for row in rows if row.get("queue_id")]

        if queue_ids:
            ids = ",".join([f"'{qid}'" for qid in queue_ids])

            update_sql = text(f"""
                UPDATE health_data_queue
                SET status='completed', updated_at=NOW()
                WHERE queue_id IN ({ids})
            """)

            connection.execute(update_sql)

            logger.info("Updated %s records.", len(queue_ids))

    finally:
        connection.close()


# ---------- DAG Definition ----------

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

    # Dependencies
    get_psql_creds >> extract_queue_data >> transform_queue_data >> load_to_es >> update_status
    get_es_creds
    [transform_queue_data, get_es_creds] >> load_to_es