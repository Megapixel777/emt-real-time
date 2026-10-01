from emt_realtime.models import BusArrival
from emt_realtime.notification_service import NotificationService


class FakeArrivalService:
    def __init__(self, arrivals):
        self.arrivals = arrivals
        self.calls = []

    def get_stop_arrivals(self, stop_id):
        self.calls.append(stop_id)
        return self.arrivals


class FakeNotificationSender:
    def __init__(self):
        self.notifications = []

    def send(
        self,
        line,
        destination,
        minutes,
        distance,
    ):
        self.notifications.append(
            {
                "line": line,
                "destination": destination,
                "minutes": minutes,
                "distance": distance,
            }
        )


def make_arrival(
    line="49",
    destination="PITIS",
    minutes=3,
    distance=500,
    bus_id=4740,
):
    return BusArrival(
        line=line,
        destination=destination,
        minutes=minutes,
        distance_meters=distance,
        bus_id=bus_id,
    )


def test_sends_notification_when_bus_is_within_limit():
    arrival = make_arrival(
        minutes=3,
        bus_id=4740,
    )

    arrival_service = FakeArrivalService(
        [arrival]
    )

    sender = FakeNotificationSender()

    service = NotificationService(
        arrival_service,
        sender,
    )

    notified = service.check_favorite(
        stop_id=1503,
        line="49",
        notification_minutes=3,
    )

    assert len(notified) == 1
    assert len(sender.notifications) == 1

    assert sender.notifications[0]["line"] == "49"
    assert sender.notifications[0]["minutes"] == 3


def test_does_not_notify_same_bus_twice():
    arrival = make_arrival(
        minutes=3,
        bus_id=4740,
    )

    arrival_service = FakeArrivalService(
        [arrival]
    )

    sender = FakeNotificationSender()

    service = NotificationService(
        arrival_service,
        sender,
    )

    service.check_favorite(
        stop_id=1503,
        line="49",
        notification_minutes=3,
    )

    service.check_favorite(
        stop_id=1503,
        line="49",
        notification_minutes=3,
    )

    assert len(sender.notifications) == 1


def test_can_notify_different_bus_same_line():
    first_bus = make_arrival(
        minutes=3,
        bus_id=4740,
    )

    second_bus = make_arrival(
        minutes=5,
        bus_id=4734,
    )

    arrival_service = FakeArrivalService(
        [first_bus]
    )

    sender = FakeNotificationSender()

    service = NotificationService(
        arrival_service,
        sender,
    )

    service.check_favorite(
        stop_id=1503,
        line="49",
        notification_minutes=5,
    )

    arrival_service.arrivals = [
        second_bus
    ]

    service.check_favorite(
        stop_id=1503,
        line="49",
        notification_minutes=5,
    )

    assert len(sender.notifications) == 2


def test_does_not_notify_different_line():
    arrival = make_arrival(
        line="49",
        minutes=2,
        bus_id=4740,
    )

    arrival_service = FakeArrivalService(
        [arrival]
    )

    sender = FakeNotificationSender()

    service = NotificationService(
        arrival_service,
        sender,
    )

    notified = service.check_favorite(
        stop_id=1503,
        line="126",
        notification_minutes=5,
    )

    assert notified == []
    assert sender.notifications == []


def test_reset_favorite_allows_notification_again():
    arrival = make_arrival(
        minutes=3,
        bus_id=4740,
    )

    arrival_service = FakeArrivalService(
        [arrival]
    )

    sender = FakeNotificationSender()

    service = NotificationService(
        arrival_service,
        sender,
    )

    service.check_favorite(
        stop_id=1503,
        line="49",
        notification_minutes=3,
    )

    assert len(sender.notifications) == 1

    service.reset_favorite(
        stop_id=1503,
        line="49",
    )

    service.check_favorite(
        stop_id=1503,
        line="49",
        notification_minutes=3,
    )

    assert len(sender.notifications) == 2