from abc import abstractmethod


class BaseConnector:
    @abstractmethod
    def connect(self):
        pass