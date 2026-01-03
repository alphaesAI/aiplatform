from airflow import DAG
from datetime import datetime, timedelta
from airflow.operators.python import PythonOperator

from src.custom.credentials.factory import CredentialFactory
from src.custom.connectors.factory import ConnectorFactory
from src.custom.extractors.factory import ExtractorFactory
from src.custom.transformers.factory import TransformerFactory
from src.custom.loaders.factory import LoaderFactory
from src.custom.utils.reader import load_yml

# 1. Fetch Postgres Credentials
def psqlcredential(**kwargs):
    provider = CredentialFactory.get_provider(mode="airflow", conn_id="healthdb")
    return provider.get_credentials()

# 2. Fetch Elasticsearch Credentials
def escredential(**kwargs):
    provider = CredentialFactory.get_provider(mode="airflow", conn_id="elasticsearch")
    return provider.get_credentials()

# 3. Extraction (Postgres to Python Dict)
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
        extractor = ExtractorFactory.get_extractor(
            extractor_type="rdbms", 
            connection=connection, 
            config=extract_config
        )
        data_json = extractor() 
        return data_json 
    finally:
        connection.close()

# 4. Process and Load (COMBINED for Generator support)
def process_and_load(ti, **kwargs):
    # Pull data and credentials
    raw_data = ti.xcom_pull(task_ids='extraction_task')
    es_creds = ti.xcom_pull(task_ids='escredential_task')

    if not raw_data:
        raise ValueError("No data received from extraction task!")

    # Get Loader Config (Index Name, Shards, etc.)
    config_path = "dags/structure/health/config/loader.yml"
    loader_config = load_yml(config_path).get("elasticsearch", {}).get("load", {})

    # TRANSFORM: Create the Generator
    transformer_obj = TransformerFactory.get_transformer(
        transformer_type="json",
        data=raw_data,
        config=loader_config # Pass loader config so transformer knows index_name
    )
    
    # This activates the generator (the "machine")
    actions_generator = transformer_obj()

    # LOAD: Pass the generator to the Ingestor
    connector = ConnectorFactory.get_connector(connector_type="elasticsearch", config=es_creds)
    es_connection = connector()

    loader_obj = LoaderFactory.get_loader(
        load_type="bulk",
        connection=es_connection,
        config=loader_config
    )

    # Ingestor pulls data from the generator one-by-one
    loader_obj(data=actions_generator)
    print("Successfully transformed and loaded data to Elasticsearch")

# --- DAG Definition ---

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

    psql_creds_task = PythonOperator(
        task_id='psqlcredential_task',
        python_callable=psqlcredential,
    )

    es_creds_task = PythonOperator(
        task_id='escredential_task',
        python_callable=escredential,
    )

    extract_task = PythonOperator(
        task_id='extraction_task',
        python_callable=extraction,
    )

    # Combined task to handle the Generator streaming
    transform_and_load_task = PythonOperator(
        task_id='transform_and_load_task',
        python_callable=process_and_load,
    )

    # Dependency Flow
    [psql_creds_task, es_creds_task] >> extract_task >> transform_and_load_task