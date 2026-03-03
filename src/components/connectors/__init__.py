import logging

"""
Connectors Package
==================
Purpose:
    A unified interface for connecting to various data sources.

"""

# Import the Factory for easy external access
# Other connectors are lazily imported through the factory
from .factory import ConnectorFactory

# Define the public API for the package
__all__ = [
    "ConnectorFactory",
]

# Set a default logger for the package to prevent "No handler found" warnings
logging.getLogger(__name__).addHandler(logging.NullHandler())