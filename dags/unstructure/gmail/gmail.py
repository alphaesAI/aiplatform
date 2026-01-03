"""
Module: Gmail Data Pipeline DAG
Purpose: Orchestrates the end-to-end flow of Gmail data. 
Steps: 
1. Authenticates via Airflow Connections.
2. Extracts raw email content (Body/Metadata/Attachments).
3. Transforms and segments text into chunks for Vector Search.
"""

import warnings

# Filter out the specific Airflow deprecation warnings coming from skops
warnings.filterwarnings("ignore", category=DeprecationWarning, module="skops")
warnings.filterwarnings("ignore", message="The `airflow.utils.*` attribute is deprecated")

from airflow import DAG
from datetime import datetime, timedelta
from airflow.providers.standard.operators.python import PythonOperator
from typing import Any, Dict, List

from src.custom.credentials.factory import CredentialFactory
from src.custom.connectors.factory import ConnectorFactory
from src.custom.extractors.factory import ExtractorFactory
from src.custom.transformers.factory import TransformerFactory
from src.custom.utils.reader import load_yml, load_pickle

def credentials_task(**kwargs: Any) -> Dict[str, Any]:
    """
    Purpose: Retrieves Gmail API credentials from the Airflow metadata database.
    
    Returns:
        Dict[str, Any]: A dictionary containing credentials and token paths.
    """
    return CredentialFactory.get_provider(mode="airflow", conn_id="gmail").get_credentials()

def extraction_task(ti: Any, **kwargs: Any) -> List[Dict[str, Any]]:
    """
    Purpose: Connects to the Gmail service and pulls raw email data based on config filters.
    
    Args:
        ti (Any): Airflow Task Instance used to pull credentials from XCom.
        
    Returns:
        List[Dict[str, Any]]: A list of raw email records (body, metadata, ids).
    """
    # 1. Setup Authentication in memory
    creds = ti.xcom_pull(task_ids='get_credentials')
    if not creds:
        raise ValueError("No credentials received from previous task.")
    
    creds['token_dict'] = load_pickle(creds['token_path'])
    
    # 2. Load Extraction Config
    full_config = load_yml("dags/unstructure/gmail/config/extractor.yml")
    gmail_config = full_config.get('gmail_pipeline', {})
    
    # 3. Connect and Extract
    connector = ConnectorFactory.get_connector(connector_type="gmail", config=creds)
    service = connector()
    extractor = ExtractorFactory.get_extractor("gmail", service, gmail_config.get('extraction'))
    
    # 4. Return list to XCom (Caution: Ensure batch_size is small)
    return list(extractor.extract())

def transformation_task(ti: Any, **kwargs: Any) -> None:
    """
    Purpose: Uses the TransformerFactory to clean, segment, and chunk raw email text.
    
    Args:
        ti (Any): Airflow Task Instance used to pull raw records from XCom.
    """
    # 1. Get the data
    raw_records = ti.xcom_pull(task_ids='extract_gmail_data')
    
    # 2. Get the config
    full_config = load_yml("dags/unstructure/gmail/config/extractor.yml")
    trans_config = full_config.get('gmail_pipeline', {}).get('transformation', {})
    
    # 3. Pass ALL THREE to the factory
    transformer = TransformerFactory.get_transformer(
        transformer_type="document", 
        data=raw_records,  # Pass the data here!
        config=trans_config
    )
    
    # 4. Execute
    for chunk in transformer(): # No need to pass record here anymore
        print(f"Ready for Loader: {chunk}")

# --- DAG Definition ---

default_args = {
    'owner': 'data_team',
    'retries': 0,
    'retry_delay': timedelta(minutes=5)
}

with DAG(
    'gmail_data_pipeline', 
    default_args=default_args,
    start_date=datetime(2025, 1, 1), 
    schedule="@daily", 
    catchup=False,
    tags=["gmail", "txtai"]
) as dag:

    credential = PythonOperator(
        task_id='get_credentials', 
        python_callable=credentials_task
    )
    
    extraction = PythonOperator(
        task_id='extract_gmail_data', 
        python_callable=extraction_task
    )
    
    transformation = PythonOperator(
        task_id='transform_data', 
        python_callable=transformation_task
    )

    credential >> extraction >> transformation