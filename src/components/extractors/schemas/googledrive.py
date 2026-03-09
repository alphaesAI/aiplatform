
"""
Schema: GoogleDriveExtractorConfig
Purpose:
    Validates configuration used by GoogleDriveExtractor.
"""

from pydantic import BaseModel, Field
from typing import Optional, Dict


class GoogleDriveExtractorConfig(BaseModel):
    """
    Configuration schema for Google Drive extraction.
    """

    file_id: Optional[str] = Field(
        default=None,
        description="Specific file ID to extract"
    )

    folder_id: Optional[str] = Field(
        default=None,
        description="Google Drive folder ID to extract files from"
    )

    modified_after: Optional[str] = Field(
        default=None,
        description="Only extract files modified after this timestamp"
    )

    page_size: int = Field(
        default=100,
        description="Number of files retrieved per API page"
    )

    incremental: Optional[Dict] = Field(
        default={"enabled": False},
        description="Incremental ingestion configuration"
    )

