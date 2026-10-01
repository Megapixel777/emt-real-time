from emt_realtime.emt_client import EMTClient
from emt_realtime.models import BusArrival


class ArrivalService:
    def __init__(self, client: EMTClient):
        self.client = client

    def get_stop_arrivals(
        self,
        stop_id: int,
    ) -> list[BusArrival]:

        response = self.client.get_arrivals(stop_id)

        arrivals = response["data"][0]["Arrive"]

        return [
            BusArrival(
                line=arrival["line"],
                destination=arrival["destination"],
                minutes=round(
                    arrival["estimateArrive"] / 60
                ),
                distance_meters=arrival["DistanceBus"],
                bus_id=arrival["bus"],
            )
            for arrival in arrivals
        ]