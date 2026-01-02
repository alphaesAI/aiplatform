from datetime import datetime, date
from decimal import Decimal
from .base import BaseTransformer

class JsonTransformer(BaseTransformer):
    def __init__(self, data, config):
        self.data = data
        self.config = config

    def __call__(self):
        return self.transform(self.data, self.config)

    def transform(self, data, config):
        if not data:
            return []

        index_name = config.get("index_name")

        for table_name, rows in data.items():
            for row in rows:
                clean_row = {}
        
                for key, value in row.items():
                    if isinstance(value, (datetime, date)):
                        clean_row[key] = value.isoformat()
                    elif isinstance(value, Decimal):
                        clean_row[key] = float(value)
                    elif value is not None and not isinstance(value, (str, int, float, bool, list, dict)):
                        clean_row[key] = str(value)
                    else:
                        clean_row[key] = value

                yield {
                    "_index": index_name,
                    "_source": clean_row
                }