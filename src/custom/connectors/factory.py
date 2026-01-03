from .rdbms import RDBMSConnector
from .gmail import GmailConnector
from .elasticsearch import ESConnector
from .opensearch import OpensearchConnector

class ConnectorFactory:
    """
    The Orchestrator. 
    It just picks the right CLASS based on the category you provide.
    """

    @classmethod
    def get_connector(cls, connector_type: str, config: str):
        """
        Simply returns the connector. 
        The connector will fetch its own credentials.
        """
        if connector_type == "rdbms":
            return RDBMSConnector(config=config)
        elif connector_type == "gmail":
            return GmailConnector(config=config)
        elif connector_type == "elasticsearch":
            return ESConnector(config=config)
        elif connector_type == "opensearch":
            return OpensearchConnector(config=config)