import json
import tempfile
import os
from airflow import DAG
from datetime import datetime, timedelta
from airflow.providers.standard.operators.python import PythonOperator

from src.custom.credentials.factory import CredentialFactory
from src.custom.connectors.factory import ConnectorFactory
from src.custom.extractors.factory import ExtractorFactory
from src.custom.utils.reader import load_yml, load_pickle

def credentials(**kwargs):
    provider = CredentialFactory.get_provider(mode="airflow", conn_id="gmail")
    return provider.get_credentials()

def extraction(ti, **kwargs):
    creds = ti.xcom_pull(task_ids='credential_task')
    if not creds or 'token_path' not in creds:
        raise KeyError("Missing 'token_path' in credentials.")

    # Convert binary pickle to clean dict
    token_dict = load_pickle(creds['token_path'])

    # Use a NamedTemporaryFile so it's cleaned up properly
    with tempfile.NamedTemporaryFile(mode='w', suffix='.json', delete=False) as tmp:
        json.dump(token_dict, tmp)
        temp_token_path = tmp.name

    try:
        # Update creds for the connector
        creds['token_path'] = temp_token_path
        
        full_yml = load_yml("dags/unstructure/gmail/config/extractor.yml")
        extractor_config = full_yml.get('gmail_pipeline', {}).get('extraction', {})

        connector_obj = ConnectorFactory.get_connector(connector_type="gmail", config=creds)
        service = connector_obj()

        extractor = ExtractorFactory.get_extractor(
            extractor_type="gmail", 
            connection=service, 
            config=extractor_config
        )

        for record in extractor.extract():
            # FUTURE: Here is where we will inject the _id logic
            print(f"Extracted: {record.get('id')}")
            
    finally:
        # Ensure temp file is deleted even if extraction fails
        if os.path.exists(temp_token_path):
            os.remove(temp_token_path)

# ... (rest of DAG definition remains same)

default_args = {
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