from unittest.mock import Mock, patch

import requests

from emt_realtime.emt_client import EMTClient

def test_login_returns_access_token():
    client = EMTClient(
        client_id="test-client",
        passkey="test-passkey",
    )

    mock_response = Mock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "data": [
            {
                "accessToken": "fake-token"
            }
        ]
    }

    with patch(
        "emt_realtime.emt_client.requests.get",
        return_value=mock_response,
    ):
        token = client.login()

    assert token == "fake-token"
    assert client.access_token == "fake-token"


def test_login_raises_error_on_403():
    client = EMTClient(
        client_id="test-client",
        passkey="test-passkey",
    )

    mock_response = Mock()
    mock_response.status_code = 403
    mock_response.raise_for_status.side_effect = requests.HTTPError(
        "403 Client Error: Forbidden"
    )

    with patch(
        "emt_realtime.emt_client.requests.get",
        return_value=mock_response,
    ):
        try:
            client.login()
        except requests.HTTPError:
            pass
        else:
            raise AssertionError(
                "client.login() debería lanzar HTTPError con un 403"
            )


def test_get_arrivals_uses_existing_token():
    client = EMTClient(
        client_id="test-client",
        passkey="test-passkey",
    )

    client.access_token = "existing-token"

    mock_response = Mock()
    mock_response.status_code = 200
    mock_response.json.return_value = {
        "data": [
            {
                "Arrive": []
            }
        ]
    }

    with patch(
        "emt_realtime.emt_client.requests.post",
        return_value=mock_response,
    ) as mock_post:

        result = client.get_arrivals(72)

    assert result == {
        "data": [
            {
                "Arrive": []
            }
        ]
    }

    mock_post.assert_called_once()
