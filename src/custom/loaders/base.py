class BaseLoader:
    """
    Abstract base class for all data loaders.
    """
    def load(self, data):
        """
        Enforces the implementation of the load method in child classes.
        
        args:
            data (dict): The data dictionary to be loaded.
            
        returns:
            None: This method must be overridden by child classes.
        """
        raise NotImplementedError("Child classes must implement the load method!")