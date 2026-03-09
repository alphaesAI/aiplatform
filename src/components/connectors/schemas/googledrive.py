from pydantic import BaseModel, Field
from typing import Optional, List


class GoogleDriveConfig(BaseModel):
    """
    Schema validating configuration for GoogleDriveConnector.
    """

    service_account_file: str = Field(
        ...,
        description="Path to the Google service account JSON credentials file."
    )

    scopes: Optional[List[str]] = Field(
        default=["https://www.googleapis.com/auth/drive.readonly"],
        description="OAuth scopes for Google Drive access."
    )