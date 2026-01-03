from .json_transformer import JsonTransformer

class TransformerFactory:
    @staticmethod
    def get_transformer(transformer_type, data, config):
        if transformer_type == "json":
            return JsonTransformer(data=data, config=config)
        else:
            raise ValueError(f"Transformer type {transformer_type} is not supported")