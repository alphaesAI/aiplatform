from abc import ABC, abstractmethod
from pyspark.sql import DataFrame

class TransformerStrategy(ABC):
    @abstractmethod
    def execute(self, df: DataFrame, config: dict) -> DataFrame:
        """Universal method signature for all handlers."""
        pass