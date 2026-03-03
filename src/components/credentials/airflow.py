import logging
from airflow.hooks.base import BaseHook
from airflow.exceptions import AirflowNotFoundException
from airflow.models.connection import Connection
from airflow.utils.session import create_session
from .base import CredentialProvider
from .schemas import AirflowConnectionSchema

logger = logging.getLogger(__name__)

"""
airflow.py
====================================
Purpose:
    Provides a mechanism to extract credentials from Airflow's internal 
    Metadata Database using Connection IDs.
"""

class AirflowCredentials(CredentialProvider):
    """
    Purpose:
        Generic Airflow Credential Provider.
        It maps Airflow Connection objects into a flat dictionary format 
        compatible with our connectors.
    """

    def __init__(self, conn_id: str):
        """
        Purpose:
            Initializes the provider with a specific Airflow connection ID.

        Args:
            conn_id (str): The unique identifier for the connection in Airflow.
        """
        self.conn_id = conn_id
        logger.debug(f"AirflowCredentials initialized for conn_id: {conn_id}")

    def get_credentials(self) -> dict:
        """
        Purpose:
            Fetches the connection object from Airflow and unpacks it.
            Merges core fields (host, login) with JSON extras.

        Returns:
            dict: Unified dictionary of connection parameters.
        """
        logger.info(f"Fetching credentials from Airflow for conn_id: {self.conn_id}")
        
        try:
            # 1. Fetch the connection object from Airflow runtime context
            # In some Airflow 3 execution contexts this can fail to resolve,
            # so we fall back to direct metadata DB lookup below.
            conn = BaseHook.get_connection(self.conn_id)
        except AirflowNotFoundException:
            logger.warning(
                "BaseHook could not resolve conn_id '%s'. Falling back to metadata DB lookup.",
                self.conn_id
            )
            conn = self._get_connection_from_metadata()
        try:

            # 2. Build the core dictionary
            # host, port, login, password, and schema are standard Airflow fields
            creds = {
                "host": conn.host,
                "port": conn.port,
                "login": conn.login,
                "password": conn.password,
                "schema": conn.schema, 
                **conn.extra_dejson  # Merges extras (like verify_certs, schema, etc.)
            }
            logger.info(f"Unpacking creds for {self.conn_id}. Keys found: {list(creds.keys())}")
            # This ensures 'port' is an int and 'password' is treated as a secret
            validated_conn = AirflowConnectionSchema(**creds)
            logger.debug(f"Successfully unpacked credentials for {self.conn_id}")
            
            # Converts a validated Pydantic object back into a standard Python dictionary.
            return validated_conn.model_dump(exclude_none=True)

        except Exception:
            logger.exception(f"Failed to retrieve Airflow connection: {self.conn_id}")
            raise

    def _get_connection_from_metadata(self):
        """Read connection directly from Airflow metadata DB."""
        with create_session() as session:
            conn = session.query(Connection).filter(Connection.conn_id == self.conn_id).one_or_none()

        if conn is None:
            raise AirflowNotFoundException(f"The conn_id `{self.conn_id}` isn't defined")
        return conn
