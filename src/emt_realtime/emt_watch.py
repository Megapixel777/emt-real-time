import os
import time

from dotenv import load_dotenv

from emt_realtime.emt_client import EMTClient
from emt_realtime.fcm_sender import send_notification


load_dotenv()


STOP_ID = 1503
MAX_MINUTES = 3
POLL_SECONDS = 30


def main():
    client = EMTClient(
        os.getenv("EMT_CLIENT_ID"),
        os.getenv("EMT_PASSKEY"),
    )

    client.login()

    # Líneas para las que ya hemos enviado una notificación
    notified_lines = set()

    print()
    print("🚌 EMT Real-Time Watch")
    print(f"📍 Parada: {STOP_ID}")
    print(f"🔔 Avisar cuando queden <= {MAX_MINUTES} minutos")
    print(f"🔄 Consulta cada {POLL_SECONDS} segundos")
    print()

    while True:

        try:
            response = client.get_arrivals(STOP_ID)

            arrivals = response["data"][0]["Arrive"]

            print("\n" + "=" * 50)

            # Líneas que actualmente tienen algún autobús
            # dentro del límite de aviso
            lines_with_alert = set()

            for bus in arrivals:

                line = bus["line"]
                destination = bus["destination"]
                bus_id = bus["bus"]
                estimate_seconds = bus["estimateArrive"]
                distance = bus["DistanceBus"]

                minutes = estimate_seconds / 60

                print(
                    f"🚌 Línea {line:<4} "
                    f"{minutes:>4.1f} min  "
                    f"📍 {distance:>4} m  "
                    f"→ {destination}"
                )

                # ¿Este autobús está dentro del intervalo de aviso?
                if minutes <= MAX_MINUTES:

                    lines_with_alert.add(line)

                    # Solo una notificación por línea
                    if line not in notified_lines:

                        print(
                            f"🔔 AVISO: línea {line} "
                            f"llega en {minutes:.1f} minutos"
                        )

                        send_notification(
                            line=line,
                            destination=destination,
                            minutes=minutes,
                            distance=distance,
                        )

                        notified_lines.add(line)

            # Si una línea ya NO tiene ningún autobús dentro
            # del intervalo de 3 minutos, desbloqueamos esa línea.
            #
            # Así podremos volver a avisar cuando llegue
            # otro autobús de esa misma línea.
            notified_lines.intersection_update(lines_with_alert)

            time.sleep(POLL_SECONDS)

        except KeyboardInterrupt:

            print("\nPrograma detenido.")
            break

        except Exception as exc:

            print(f"❌ Error: {exc}")

            time.sleep(POLL_SECONDS)


if __name__ == "__main__":
    main()