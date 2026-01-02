from .airflow import AirflowCredentials

class CredentialFactory:
    """
    Decides which 'Source' to use for credential management.
    Uses 'conn_id' as the master key for both Airflow and Local YAML.
    """
    @staticmethod
    def get_provider(mode: str, conn_id: str):
        if mode == "airflow":
            return AirflowCredentials(conn_id)
        
        elif mode == "local":
            return LocalCredentials(conn_id)
        
        else:
            raise ValueError(f"Unknown mode: {mode}. Use 'airflow' or 'local'.")