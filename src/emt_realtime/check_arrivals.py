import os
import sys

from dotenv import load_dotenv

from emt_client import EMTClient
from fcm_sender import send_notification


load_dotenv()

EMT_CLIENT_ID = os.getenv("EMT_CLIENT_ID")
EMT_PASSKEY = os.getenv("EMT_PASSKEY")


# ============================================================
# CONTROL DE NOTIFICACIONES
# ============================================================

# Guarda el identificador del último autobús
# para el que ya hemos enviado una notificación.
last_notified_bus_id = None


def check_arrival(
    stop_id: int,
    line: str,
    threshold: int,
) -> None:

    global last_notified_bus_id

    print("=" * 60)
    print("EMT ARRIVAL CHECK")
    print("=" * 60)

    print(f"Parada:    {stop_id}")
    print(f"Línea:     {line}")
    print(f"Umbral:    {threshold} minutos")
    print()

    # --------------------------------------------------
    # VALIDAR CREDENCIALES
    # --------------------------------------------------

    if not EMT_CLIENT_ID:
        raise RuntimeError(
            "Falta EMT_CLIENT_ID en .env"
        )

    if not EMT_PASSKEY:
        raise RuntimeError(
            "Falta EMT_PASSKEY en .env"
        )

    # --------------------------------------------------
    # CLIENTE EMT
    # --------------------------------------------------

    client = EMTClient(
        client_id=EMT_CLIENT_ID,
        passkey=EMT_PASSKEY,
    )

    print("Autenticando con EMT...")

    client.login()

    print("Autenticación EMT correcta.")
    print()

    # --------------------------------------------------
    # CONSULTAR PARADA
    # --------------------------------------------------

    print(
        f"Consultando parada {stop_id}..."
    )

    response = client.get_arrivals(
        stop_id
    )

    # --------------------------------------------------
    # EXTRAER DATA
    # --------------------------------------------------

    data = response.get(
        "data",
        []
    )

    if not data:

        print(
            "No se han recibido datos de EMT."
        )

        return

    print(
        f"Registros recibidos: {len(data)}"
    )

    # --------------------------------------------------
    # ESTRUCTURA EMT
    # --------------------------------------------------

    stop_data = data[0]

    arrivals = stop_data.get(
        "Arrive",
        []
    )

    if not arrivals:

        print(
            "No hay llegadas disponibles."
        )

        return

    print(
        f"Llegadas recibidas: "
        f"{len(arrivals)}"
    )

    print()

    # --------------------------------------------------
    # BUSCAR LA LÍNEA
    # --------------------------------------------------

    matching = []

    for arrival in arrivals:

        arrival_line = str(
            arrival.get(
                "line",
                ""
            )
        ).strip()

        if arrival_line.lower() == line.lower():

            matching.append(
                arrival
            )

    # --------------------------------------------------
    # LÍNEA NO ENCONTRADA
    # --------------------------------------------------

    if not matching:

        print(
            f"No se ha encontrado la línea "
            f"{line} en la parada {stop_id}."
        )

        print()

        available_lines = sorted(
            {
                str(
                    arrival.get(
                        "line",
                        "?"
                    )
                )
                for arrival in arrivals
            }
        )

        print(
            "Líneas encontradas:"
        )

        for available_line in available_lines:

            print(
                f"- {available_line}"
            )

        return

    # --------------------------------------------------
    # CONVERTIR SEGUNDOS → MINUTOS
    # --------------------------------------------------

    parsed_arrivals = []

    for arrival in matching:

        estimate_seconds = arrival.get(
            "estimateArrive"
        )

        try:

            estimate_seconds = int(
                estimate_seconds
            )

        except (
            TypeError,
            ValueError,
        ):

            continue

        minutes = estimate_seconds / 60

        parsed_arrivals.append(
            (
                minutes,
                arrival
            )
        )

    if not parsed_arrivals:

        print(
            "No se han encontrado tiempos "
            "de llegada válidos."
        )

        return

    # --------------------------------------------------
    # ORDENAR
    # --------------------------------------------------

    parsed_arrivals.sort(
        key=lambda item: item[0]
    )

    print(
        f"Llegadas de la línea {line}:"
    )

    print()

    for minutes, arrival in parsed_arrivals:

        destination = arrival.get(
            "destination",
            "Desconocido"
        )

        distance = arrival.get(
            "DistanceBus",
            "?"
        )

        bus_id = arrival.get(
            "bus",
            "?"
        )

        print(
            f"Bus {bus_id} → "
            f"{destination} → "
            f"{minutes:.1f} min → "
            f"{distance} m"
        )

    # --------------------------------------------------
    # PRÓXIMO AUTOBÚS
    # --------------------------------------------------

    nearest_minutes, nearest_bus = (
        parsed_arrivals[0]
    )

    nearest_bus_id = nearest_bus.get(
        "bus",
        "?"
    )

    destination = nearest_bus.get(
        "destination",
        "Desconocido"
    )

    distance = nearest_bus.get(
        "DistanceBus",
        "?"
    )

    # --------------------------------------------------
    # REDONDEO
    # --------------------------------------------------

    notification_minutes = max(
        0,
        round(nearest_minutes)
    )

    # --------------------------------------------------
    # RESULTADO
    # --------------------------------------------------

    print()
    print("-" * 60)

    print(
        f"Próximo {line}: "
        f"{nearest_minutes:.1f} minutos"
    )

    print(
        f"Bus:       {nearest_bus_id}"
    )

    print(
        f"Destino:   {destination}"
    )

    print(
        f"Distancia: {distance} m"
    )

    print(
        f"Umbral:    {threshold} minutos"
    )

    print("-" * 60)

    # --------------------------------------------------
    # COMPROBAR UMBRAL
    # --------------------------------------------------

    if nearest_minutes <= threshold:

        print()
        print(
            "🔔 CONDICIÓN CUMPLIDA"
        )

        print(
            f"La línea {line} llega "
            f"en aproximadamente "
            f"{notification_minutes} minutos."
        )

        # ==================================================
        # EVITAR NOTIFICACIONES DUPLICADAS
        # ==================================================

        if str(nearest_bus_id) == str(
            last_notified_bus_id
        ):

            print()
            print(
                "🔕 NOTIFICACIÓN NO ENVIADA"
            )

            print(
                f"El bus {nearest_bus_id} "
                "ya ha generado una notificación."
            )

            print(
                "Esperando a que aparezca "
                "un nuevo autobús."
            )

            return

        # --------------------------------------------------
        # NUEVO AUTOBÚS → NOTIFICAR
        # --------------------------------------------------

        print()
        print(
            "🆕 Nuevo autobús dentro del umbral."
        )

        print(
            f"Bus: {nearest_bus_id}"
        )

        print()
        print(
            "📱 NOTIFICACIÓN QUE SE VA A ENVIAR"
        )

        print("-" * 60)

        notification_title = (
            f"🚌 Línea {line}"
        )

        notification_body = (
            f"El autobús {line} hacia "
            f"{destination} llega en "
            f"aproximadamente "
            f"{notification_minutes} minutos. "
            f"Distancia: {distance} m."
        )

        print(
            f"Título:  {notification_title}"
        )

        print(
            f"Mensaje: {notification_body}"
        )

        print("-" * 60)

        print()
        print(
            "📱 Enviando notificación FCM..."
        )

        try:

            response = send_notification(
                line=line,
                destination=destination,
                minutes=nearest_minutes,
                distance=int(distance),
            )

            # ------------------------------------------
            # MARCAR COMO NOTIFICADO
            # ------------------------------------------

            last_notified_bus_id = (
                nearest_bus_id
            )

            print()
            print(
                "✅ Notificación FCM "
                "enviada correctamente"
            )

            print(
                f"Firebase message ID: "
                f"{response}"
            )

            print(
                f"Bus marcado como notificado: "
                f"{last_notified_bus_id}"
            )

        except Exception as e:

            print()
            print(
                "❌ Error enviando "
                "notificación FCM:"
            )

            print(
                f"{type(e).__name__}: {e}"
            )

    else:

        print()
        print(
            "⏳ NO ENVIAR NOTIFICACIÓN"
        )

        print(
            f"La línea {line} llega "
            f"en aproximadamente "
            f"{notification_minutes} minutos."
        )

        print(
            "Todavía no ha alcanzado "
            "el umbral."
        )


def main() -> None:

    if len(sys.argv) != 4:

        print(
            "Uso:"
        )

        print(
            "python src/emt_realtime/"
            "check_arrivals.py "
            "<parada> <línea> <minutos>"
        )

        print()

        print(
            "Ejemplo:"
        )

        print(
            "python src/emt_realtime/"
            "check_arrivals.py 72 27 3"
        )

        sys.exit(1)

    try:

        stop_id = int(
            sys.argv[1]
        )

        line = sys.argv[2]

        threshold = int(
            sys.argv[3]
        )

    except ValueError:

        print(
            "La parada y el umbral "
            "deben ser números."
        )

        sys.exit(1)

    check_arrival(
        stop_id=stop_id,
        line=line,
        threshold=threshold,
    )


if __name__ == "__main__":

    main()

