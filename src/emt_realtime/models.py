from dataclasses import dataclass


@dataclass
class BusArrival:
    line: str
    destination: str
    minutes: int
    distance_meters: int
    bus_id: int

