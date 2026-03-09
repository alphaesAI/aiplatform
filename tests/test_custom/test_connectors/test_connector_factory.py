from src.components.connectors.factory import ConnectorFactory


def test_factory_creates_googledrive_connector():

    config = {
        "service_account_file": "dummy.json"
    }

    connector = ConnectorFactory.get_connector(
        connector_type="googledrive",
        config=config
    )

    assert connector is not None
    assert connector.__class__.__name__ == "GoogleDriveConnector"