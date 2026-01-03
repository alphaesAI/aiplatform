class TransformerFactory:
    @staticmethod
    def get_transformer(transformer_type: str, data: any, config: dict):
        """
        Purpose: Universal entry point for all transformations.
        """
        if transformer_type == "document":
            
            from .document import DocumentTransformer
            return DocumentTransformer(data, config)
        
        elif transformer_type == "json":

            from .json_transformer import JsonTransformer
            return JsonTransformer(data, config)
        else:
            raise ValueError(f"Unknown type: {transformer_type}")