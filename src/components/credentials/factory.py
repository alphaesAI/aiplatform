import logging
logger = logging.getLogger(__name__)

"""
factory.py
====================================
Purpose:
    Implements a Factory to decide which credential storage backend to use.
    Supports switching between cloud/orchestrator (Airflow) and local environments.
"""

class CredentialFactory:
    """
    Purpose:
        Orchestrator for credential providers. It selects the appropriate 
        class based on the execution 'mode'.
    """

    @staticmethod
    def get_provider(mode: str, conn_id: str):
        """
        Purpose:
            Returns an instance of a CredentialProvider based on the mode.

        Args:
            mode (str): The source of credentials ('airflow' or 'local').
            conn_id (str): The identifier for the specific connection.

        Returns:
            CredentialProvider: An initialized provider instance.

        Raises:
            ValueError: If an unsupported mode is provided.
        """
        mode = mode.lower().strip()
        logger.info(f"CredentialFactory selecting provider for mode: {mode}")

        # Lazy imports to avoid dependency issues
        if mode == "airflow":
            from .airflow import AirflowCredentials
            return AirflowCredentials(conn_id)
        
        elif mode == "local":
            # Note: Ensure LocalCredentials is imported/defined
            # from .local import LocalCredentials
            # return LocalCredentials(conn_id)
            logger.warning("Local mode requested but LocalCredentials implementation was not provided in snippet.")
            raise NotImplementedError("LocalCredentials not yet implemented.")
        
        else:
            error_msg = f"Unknown mode: {mode}. Use 'airflow' or 'local'."
            logger.error(error_msg)
            raise ValueError(error_msg)