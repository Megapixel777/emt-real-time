import requests
from datetime import datetime


class EMTClient:

    TOKEN_URL = (
        "https://openapi.emtmadrid.es/"
        "v1/mobilitylabs/user/login/"
    )

    ARRIVALS_URL = (
        "https://openapi.emtmadrid.es/"
        "v2/transport/busemtmad/stops/"
    )

    def __init__(
        self,
        client_id: str,
        passkey: str,
    ):
        self.client_id = client_id
        self.passkey = passkey
        self.access_token = None

    # ==================================================
    # AUTENTICACIÓN
    # ==================================================

    def login(self):

        headers = {
            "X-ClientId": self.client_id,
            "passKey": self.passkey,
        }

        response = requests.get(
            self.TOKEN_URL,
            headers=headers,
            timeout=10,
        )

        response.raise_for_status()

        data = response.json()

        self.access_token = (
            data["data"][0]["accessToken"]
        )

        print(
            "Autenticación EMT correcta."
        )

        return self.access_token

    # ==================================================
    # LLEGADAS
    # ==================================================

    def get_arrivals(
        self,
        stop_id: int,
    ):

        if not self.access_token:
            self.login()

        headers = {
            "accessToken": self.access_token,
            "Content-Type": "application/json",
        }

        url = (
            f"{self.ARRIVALS_URL}"
            f"{stop_id}/arrives/"
        )

        # Parámetros necesarios para obtener
        # las estimaciones de llegada.
        body = {
            "cultureInfo": "ES",
            "Text_StopRequired_YN": "Y",
            "Text_EstimationsRequired_YN": "Y",
            "Text_IncidencesRequired_YN": "Y",
            "DateTime_Referenced_Incidencies_YYYYMMDD": (
                datetime.now().strftime("%Y%m%d")
            ),
        }

        response = requests.post(
            url,
            headers=headers,
            json=body,
            timeout=10,
        )

        print(
            f"HTTP status arrivals: "
            f"{response.status_code}"
        )

        response.raise_for_status()

        data = response.json()

        print("========== RESPUESTA EMT ==========")
        print(data)
        print("===================================")

        return data

