from .sparkconnector import SparkConnector


class ConnectorFactory:
    @staticmethod
    def create(type: str, config: dict):
        if type == "spark":
            return SparkConnector(config)
        else:
            raise ValueError(f"Unknown connector type: {type}")