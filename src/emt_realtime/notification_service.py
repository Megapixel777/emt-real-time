from __future__ import annotations

import logging
from dataclasses import dataclass

from emt_realtime.arrival_service import ArrivalService
from emt_realtime.models import BusArrival

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class NotificationKey:
    stop_id: int
    line: str
    bus_id: int


class NotificationService:

    def __init__(
        self,
        arrival_service: ArrivalService,
        notification_sender,
    ):
        self.arrival_service = arrival_service
        self.notification_sender = notification_sender

        # Autobuses para los que ya hemos enviado
        # una notificación.
        self._notified: set[NotificationKey] = set()

    def check_favorite(
        self,
        stop_id: int,
        line: str,
        notification_minutes: int,
    ) -> list[BusArrival]:

        arrivals = (
            self.arrival_service
            .get_stop_arrivals(stop_id)
        )

        matching_arrivals = [
            arrival
            for arrival in arrivals
            if (
                arrival.line.strip().lower()
                == line.strip().lower()
                and arrival.minutes
                <= notification_minutes
            )
        ]

        notified = []

        for arrival in matching_arrivals:

            key = NotificationKey(
                stop_id=stop_id,
                line=arrival.line,
                bus_id=arrival.bus_id,
            )

            # Ya avisamos de este autobús.
            if key in self._notified:

                logger.debug(
                    "Ya notificado: "
                    "línea=%s bus=%s parada=%s",
                    arrival.line,
                    arrival.bus_id,
                    stop_id,
                )

                continue

            self.notification_sender.send(
                line=arrival.line,
                destination=arrival.destination,
                minutes=arrival.minutes,
                distance=arrival.distance_meters,
            )

            self._notified.add(key)

            notified.append(arrival)

            logger.info(
                "Notificación enviada: "
                "línea=%s bus=%s parada=%s "
                "min=%s",
                arrival.line,
                arrival.bus_id,
                stop_id,
                arrival.minutes,
            )

        return notified

    def reset_favorite(
        self,
        stop_id: int,
        line: str,
    ) -> None:

        self._notified = {
            key
            for key in self._notified
            if not (
                key.stop_id == stop_id
                and key.line.lower()
                == line.lower()
            )
        }

    def clear(self) -> None:
        self._notified.clear()

