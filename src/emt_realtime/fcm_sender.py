import os

from dotenv import load_dotenv

import firebase_admin
from firebase_admin import credentials, messaging


load_dotenv()


FCM_DEVICE_TOKEN = os.getenv("FCM_DEVICE_TOKEN")

FIREBASE_CREDENTIALS = (
    "credentials/firebase-service-account.json"
)


def initialize_firebase() -> None:

    if not os.path.exists(FIREBASE_CREDENTIALS):
        raise RuntimeError(
            "No se encuentra el archivo de credenciales Firebase: "
            f"{FIREBASE_CREDENTIALS}"
        )

    if not firebase_admin._apps:

        cred = credentials.Certificate(
            FIREBASE_CREDENTIALS
        )

        firebase_admin.initialize_app(
            cred
        )


def send_notification(
    line: str,
    destination: str,
    minutes: float,
    distance: int,
) -> str:

    if not FCM_DEVICE_TOKEN:
        raise RuntimeError(
            "Falta FCM_DEVICE_TOKEN en .env"
        )

    initialize_firebase()

    message = messaging.Message(

        notification=messaging.Notification(

            title=f"🚌 Línea {line}",

            body=(
                f"{destination}\n"
                f"⏱️ {minutes:.1f} min · "
                f"📍 {distance} m"
            ),
        ),

        token=FCM_DEVICE_TOKEN,
    )

    response = messaging.send(
        message
    )

    return response


def main() -> None:

    print(
        "Enviando notificación FCM..."
    )

    response = send_notification(

        line="27",

        destination="PLAZA CASTILLA",

        minutes=2.0,

        distance=601,
    )

    print()
    print(
        "✅ Notificación enviada correctamente"
    )

    print(
        f"Firebase message ID: {response}"
    )


if __name__ == "__main__":
    main()