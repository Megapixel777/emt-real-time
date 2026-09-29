import os

import requests
from dotenv import load_dotenv



load_dotenv()

EMT_CLIENT_ID = os.getenv("EMT_CLIENT_ID")
EMT_PASSKEY = os.getenv("EMT_PASSKEY")

BASE_URL = "https://openapi.emtmadrid.es/v2"


class EMTClient:
    def __init__(self, client_id: str, passkey: str):
        self.client_id = client_id
        self.passkey = passkey
        self.access_token: str | None = None

    def login(self) -> str:
        url = f"{BASE_URL}/mobilitylabs/user/login/"

        headers = {
            "X-ClientId": self.client_id,
            "passKey": self.passkey,
        }

        response = requests.get(
            url,
            headers=headers,
            timeout=10,
        )

        if response.status_code == 403:
            raise RuntimeError(
                "EMT ha rechazado la autenticación (HTTP 403). "
                "Comprueba que la aplicación de MobilityLabs esté aprobada."
            )

        response.raise_for_status()

        data = response.json()

        self.access_token = data["data"][0]["accessToken"]

        return self.access_token

    def get_arrivals(self, stop_id: int) -> dict:
        if not self.access_token:
            self.login()

        url = f"{BASE_URL}/transport/busemtmad/stops/{stop_id}/arrives/"

        headers = {
            "accessToken": self.access_token,
            "Content-Type": "application/json",
        }

        payload = {
            "statistics": "N",
            "cultureInfo": "ES",
            "Text_StopRequired_YN": "Y",
            "Text_EstimationsRequired_YN": "Y",
            "Text_IncidencesRequired_YN": "N",
        }

        response = requests.post(
            url,
            headers=headers,
            json=payload,
            timeout=10,
        )

        print(f"HTTP status arrivals: {response.status_code}")

        response.raise_for_status()

        return response.json()


def main() -> None:
    if not EMT_CLIENT_ID:
        raise RuntimeError("Falta EMT_CLIENT_ID en .env")

    if not EMT_PASSKEY:
        raise RuntimeError("Falta EMT_PASSKEY en .env")

    client = EMTClient(
        client_id=EMT_CLIENT_ID,
        passkey=EMT_PASSKEY,
    )

    token = client.login()

    print("Autenticación EMT correcta.")
    print("Access token obtenido correctamente.")

    arrivals = client.get_arrivals(72)

    print("\nDatos de llegada:")
    print(arrivals)


if __name__ == "__main__":
    main()