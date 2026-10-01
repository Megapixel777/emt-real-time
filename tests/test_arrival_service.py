from emt_realtime.arrival_service import ArrivalService


class FakeEMTClient:
    def get_arrivals(self, stop_id: int) -> dict:
        assert stop_id == 72

        return {
            "data": [
                {
                    "Arrive": [
                        {
                            "line": "27",
                            "destination": "EMBAJADORES",
                            "estimateArrive": 120,
                            "DistanceBus": 350,
                            "bus": 1234,
                        },
                        {
                            "line": "14",
                            "destination": "Atocha",
                            "estimateArrive": 305,
                            "DistanceBus": 800,
                            "bus": 5678,
                        },
                    ]
                }
            ]
        }


def test_get_stop_arrivals_transforms_emt_data():
    service = ArrivalService(FakeEMTClient())

    arrivals = service.get_stop_arrivals(72)

    assert len(arrivals) == 2

    assert arrivals[0].line == "27"
    assert arrivals[0].destination == "EMBAJADORES"
    assert arrivals[0].minutes == 2
    assert arrivals[0].distance_meters == 350
    assert arrivals[0].bus_id == 1234

    assert arrivals[1].line == "14"
    assert arrivals[1].destination == "Atocha"
    assert arrivals[1].minutes == 5
    assert arrivals[1].distance_meters == 800
    assert arrivals[1].bus_id == 5678