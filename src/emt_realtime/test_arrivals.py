from emt_realtime.arrival_service import ArrivalService
from emt_realtime.emt_client import EMTClient

import os

from dotenv import load_dotenv


load_dotenv()


def main() -> None:
    client = EMTClient(
        client_id=os.environ["EMT_CLIENT_ID"],
        passkey=os.environ["EMT_PASSKEY"],
    )

    service = ArrivalService(client)

    arrivals = service.get_stop_arrivals(72)

    print("\nPróximos autobuses:\n")

    for arrival in arrivals:
        print(
            f"Línea {arrival.line} → "
            f"{arrival.destination} → "
            f"{arrival.minutes} min "
            f"({arrival.distance_meters} m)"
        )


if __name__ == "__main__":
    main()