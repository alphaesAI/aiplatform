class BaseTransformer:
    def transform(self, data, config):
        raise NotImplementedError("You must define the transform method in the child class!")