import os

from dotenv import load_dotenv
from fastapi import FastAPI, HTTPException

from emt_realtime.arrival_service import ArrivalService
from emt_realtime.emt_client import EMTClient
from emt_realtime.models import BusArrival


load_dotenv()

app = FastAPI(
    title="EMT Real-Time API",
    description="API for real-time EMT Madrid bus arrivals",
    version="0.1.0",
)


def get_arrival_service() -> ArrivalService:
    client = EMTClient(
        client_id=os.environ["EMT_CLIENT_ID"],
        passkey=os.environ["EMT_PASSKEY"],
    )

    return ArrivalService(client)


@app.get(
    "/health",
    tags=["Health"],
    summary="Health check",
)
def health():
    return {"status": "ok"}


@app.get(
    "/stops/{stop_id}/arrivals",
    response_model=list[BusArrival],
    tags=["Bus Arrivals"],
    summary="Get real-time bus arrivals",
    description="Returns real-time bus arrival information for an EMT Madrid stop.",
)
def get_arrivals(stop_id: int):
    try:
        service = get_arrival_service()
        return service.get_stop_arrivals(stop_id)
    except Exception as exc:
        raise HTTPException(
            status_code=502,
            detail=f"Error retrieving EMT data: {exc}",
        )