from airflow import DAG
from datetime import datetime, timedelta
from airflow.operators.python import PythonOperator

from src.custom.credentials.factory import CredentialFactory
from src.custom.connectors.factory import ConnectorFactory
from src.custom.extractors.factory import ExtractorFactory
from src.custom.transformers.factory import TransformerFactory
from src.custom.loaders.factory import LoaderFactory
from src.custom.utils.reader import load_yml

def psqlcredential(**kwargs):
    """ 
    Fetch raw credentials for postgres. 
    """
    provider = CredentialFactory.get_provider(mode="airflow", conn_id="healthdb")
    config = provider.get_credentials()
    return config

def escredential(**kwargs):
    """ 
    Fetch raw credentials for elasticsearch. 
    """
    provider = CredentialFactory.get_provider(mode="airflow", conn_id="elasticsearch")
    config = provider.get_credentials()
    return config

def extraction(ti, **kwargs):
    creds = ti.xcom_pull(task_ids='psqlcredential_task')

    if not creds:
        raise ValueError("No credentials found in XCom!")
    
    config_path = "dags/structure/health/config/extractor.yml"
    full_yml = load_yml(config_path)
    
    extract_config = full_yml.get("postgres", {}).get("extraction", {})

    connector = ConnectorFactory.get_connector(connector_type="rdbms", config=creds)

    connection = connector()

    try:
        extractor = ExtractorFactory.get_extractor(extractor_type="rdbms", 
            connection=connection, 
            config=extract_config)
        
        # This is now a dictionary of LISTS (JSON format)
        data_json = extractor() 

        for table_name, rows in data_json.items():
            print(f"Extracted {len(rows)} records from {table_name}")

        return data_json # Airflow handles this JSON object perfectly
        
    finally:
        connection.close()

def transformation(ti, **kwargs):
    raw_data = ti.xcom_pull(task_ids='extraction_task')

    if not raw_data:
        raise ValueError("No data received from extraction task!")

    config_path = "dags/structure/health/config/transformer.yml"
    full_yml = load_yml(config_path) or {}

    if not full_yml:
        full_yml = {}

    transformer_config = full_yml.get("postgres", {}).get("transformation", {})

    transformer = TransformerFactory.get_transformer(
        transformer_type="json",
        data=raw_data,
        config=transformer_config
        )

    normalized_data = transformer()

    for table_name, rows in normalized_data.items():
        print(f"successfully transformed {len(rows)} records for {table_name}")

    return normalized_data

def loading(ti, **kwargs):
    transformed_data = ti.xcom_pull(task_ids='transformation_task')
    es_creds = ti.xcom_pull(task_ids='escredential_task')

    if not transformed_data:
        raise ValueError("No transformed data found in XCom!")
    
    if not es_creds:
        raise ValueError("No Elasticsearch credentials found in XCom!")

    config_path = "dags/structure/health/config/loader.yml"
    full_yml = load_yml(config_path) or {}
    loader_config = full_yml.get("elasticsearch", {}).get("load", {})

    connector = ConnectorFactory.get_connector(connector_type="elasticsearch", config=es_creds)
    es_connection = connector()

    loader = LoaderFactory.get_loader(
        load_type="bulk",
        connection=es_connection,
        config=loader_config
    )

    loader(data=transformed_data)
    print("successfully loaded data to Elasticsearch")

default_args = {
    'owner': 'data_team',
    'retries': 0,
    'retry_delay': timedelta(minutes=2)
}

with DAG(
    'health_data_pipeline',
    default_args=default_args,
    start_date=datetime(2025, 1, 1),
    schedule="@daily",
    catchup=False,
    tags=["structure", "health"]
) as dag:

    credentials_task = PythonOperator(
        task_id='psqlcredential_task',
        python_callable=psqlcredential,
    )

    es_credential_task = PythonOperator(
        task_id='escredential_task',
        python_callable=escredential,
    )

    extraction_task = PythonOperator(
        task_id='extraction_task',
        python_callable=extraction,
    )

    transformation_task = PythonOperator(
        task_id='transformation_task',
        python_callable=transformation,
    )

    loading_task = PythonOperator(
        task_id='loading_task',
        python_callable=loading,
    )

    [credentials_task, es_credential_task] >> extraction_task
    extraction_task >> transformation_task >> loading_task