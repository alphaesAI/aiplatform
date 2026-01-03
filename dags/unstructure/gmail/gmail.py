"""
Module: Gmail Data Pipeline DAG
Purpose: Orchestrates the end-to-end extraction process for Gmail data. 
It retrieves credentials from Airflow, loads configuration from YAML, 
and uses specialized Factories to connect to and extract data from Gmail.
"""

import json
from typing import Any, Dict
from airflow import DAG
from datetime import datetime, timedelta
from airflow.providers.standard.operators.python import PythonOperator

# Architecture imports
from src.custom.credentials.factory import CredentialFactory
from src.custom.connectors.factory import ConnectorFactory
from src.custom.extractors.factory import ExtractorFactory
from src.custom.utils.reader import load_yml, load_pickle

def credentials(**kwargs: Any) -> Dict[str, Any]:
    """
    Purpose: Retrieves Gmail API credentials using the Credential Factory.
    
    Args:
        **kwargs: Airflow task context.
        
    Returns:
        Dict[str, Any]: A dictionary containing credential metadata (like token_path).
    """
    provider = CredentialFactory.get_provider(mode="airflow", conn_id="gmail")
    return provider.get_credentials()

def extraction(ti: Any, **kwargs: Any) -> None:
    """
    Purpose: Performs the data extraction in a secure, zero-disk manner.
    It pulls credentials from XCom, loads the token into memory, 
    and iterates through extracted Gmail records.
    
    Args:
        ti: Airflow Task Instance (used for XCom pull).
        **kwargs: Airflow task context.
    """
    # 1. Pull credentials from the previous task
    creds: Dict[str, Any] = ti.xcom_pull(task_ids='credential_task')
    if not creds or 'token_path' not in creds:
        raise KeyError("Missing 'token_path' in credentials.")

    # 2. Zero-Disk: Load the token dictionary directly into RAM
    token_dict: Dict[str, Any] = load_pickle(creds['token_path'])
    creds['token_dict'] = token_dict
    
    # 3. Load Extractor Configuration from YAML
    full_yml: Dict[str, Any] = load_yml("dags/unstructure/gmail/config/extractor.yml")
    extractor_config: Dict[str, Any] = full_yml.get('gmail_pipeline', {}).get('extraction', {})

    # 4. Initialize Connector (In-Memory Auth)
    connector_obj = ConnectorFactory.get_connector(connector_type="gmail", config=creds)
    service = connector_obj()

    # 5. Initialize Extractor and process records
    extractor = ExtractorFactory.get_extractor(
        extractor_type="gmail", 
        connection=service, 
        config=extractor_config
    )

    for record in extractor.extract():
        # Print full JSON record to Airflow logs for visibility
        print(f"Extracted Record: {json.dumps(record)}")

# --- DAG Configuration ---

default_args: Dict[str, Any] = {
    'owner': 'data_team',
    'retries': 0,
    'retry_delay': timedelta(minutes=2)
}

with DAG(
    'gmail_data_pipeline',
    default_args=default_args,
    start_date=datetime(2025, 1, 1),
    schedule="@daily",
    catchup=False,
    tags=["gmail"]
) as dag:

    credentials_task = PythonOperator(
        task_id='credential_task',
        python_callable=credentials,
    )

    extraction_task = PythonOperator(
        task_id='extraction_task',
        python_callable=extraction,
    )

    credentials_task >> extraction_task