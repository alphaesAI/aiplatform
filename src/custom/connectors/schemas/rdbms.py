from pydantic import BaseModel

class RDBMSConfig(BaseModel):
    """ 
    Purpose:
        Configuration schema for RDBMS connections.
    """
    db_type: str
    host: str
    port: int
    username: str
    password: str
    database: str