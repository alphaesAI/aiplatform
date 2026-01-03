from pathlib import Path
import yaml
import pickle
import json
from typing import Any

def load_yml(file_path: str) -> dict:
    path = Path(file_path)

    if not path.exists():
        raise FileNotFoundError(f"Configuration file not found: {path.absolute()}")

    if path.suffix not in ['.yml', '.yaml']:
        raise ValueError(f"File {path.name} is not a valid YAML file.")

    try:
        with path.open('r') as f:
            return yaml.safe_load(f)
    except yaml.YAMLError as e:
        raise ValueError(f"Error parsing YAML file {path.name}: {e}")

def load_pickle(file_path: str) -> dict:
    path = Path(file_path)
    if not path.exists():
        raise FileNotFoundError(f"File not found: {path}")

    with path.open('rb') as f:
        header = f.read(1)
        f.seek(0)
        
        if header == b'\x80':
            data = pickle.load(f)
            # Handle Google's specific Credential objects
            return json.loads(data.to_json()) if hasattr(data, 'to_json') else data
        else:
            # Fallback to JSON if not a pickle
            return json.loads(path.read_text())