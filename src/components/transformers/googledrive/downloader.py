import logging
import io

logger = logging.getLogger(__name__)


class GoogleDriveDownloader:
    """
    Responsible for downloading file content from Google Drive.
    """

    def __init__(self, connection):
        """
        Parameters
        ----------
        connection : Google Drive API client
        """
        self.connection = connection

    def download_file(self, file_id: str) -> bytes:
        """
        Download file content from Google Drive.

        Parameters
        ----------
        file_id : str
            Google Drive file identifier.

        Returns
        -------
        bytes
            File content.
        """

        logger.info(f"Downloading file {file_id}")

        request = self.connection.files().get_media(fileId=file_id)

        buffer = io.BytesIO()
        buffer.write(request.execute())

        return buffer.getvalue()