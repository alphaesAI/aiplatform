from airflow import DAG
from datetime import datetime, timedelta
from airflow.providers.standard.operators.python import PythonOperator

# Importing the classes you shared
from src.custom.credentials.factory import CredentialFactory
from src.custom.connectors.factory import ConnectorFactory

def test_opensearch_connection(**kwargs):
    # 1. Get Credentials using your AirflowCredentials provider
    # This calls the get_credentials() method you shared
    creds_provider = CredentialFactory.get_provider(
        mode="airflow", 
        conn_id="opensearch"
    )
    creds = creds_provider.get_credentials()
    
    # 2. Initialize the Connector
    # This uses the OpensearchConnector class
    connector = ConnectorFactory.get_connector(
        connector_type="opensearch", 
        config=creds
    )
    
    # 3. Get the connection object
    # Calling connector() triggers .connect() and returns the OpenSearch client
    client = connector()
    
    # Simple execution check
    info = client.info()
    print(f"Connected to cluster: {info['cluster_name']}")
    return True

default_args = {
    'owner': 'airflow',
    'retries': 0
}

with DAG(
    'opensearch_connection_test',
    default_args=default_args,
    start_date=datetime(2026, 1, 1),
    schedule=None, # Manual trigger for testing
    catchup=False,
    tags=["infrastructure", "opensearch"]
) as dag:

    connection_task = PythonOperator(
        task_id='test_opensearch_connection',
        python_callable=test_opensearch_connection,
    )