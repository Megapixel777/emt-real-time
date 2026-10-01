package com.tomasperez.emtrealtime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope

import com.google.firebase.messaging.FirebaseMessaging

import com.tomasperez.emtrealtime.data.BusArrival
import com.tomasperez.emtrealtime.data.Favorite
import com.tomasperez.emtrealtime.storage.FavoriteStorage
import com.tomasperez.emtrealtime.ui.theme.EMTRealTimeTheme

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class MainActivity : ComponentActivity() {

    // ==================================================
    // PARADA / BÚSQUEDA
    // ==================================================

    private var stopId by mutableIntStateOf(72)

    private var stopInput by mutableStateOf("72")

    private var lineInput by mutableStateOf("")


    // ==================================================
    // LLEGADAS DE LA BÚSQUEDA PRINCIPAL
    // ==================================================

    private var arrivals by mutableStateOf<List<BusArrival>>(emptyList())

    private var errorMessage by mutableStateOf("")

    private var lastUpdate by mutableStateOf("")

    private var secondsSinceUpdate by mutableIntStateOf(0)

    private var loading by mutableStateOf(false)


    // ==================================================
    // FIREBASE
    // ==================================================

    private var fcmToken by mutableStateOf("Obteniendo token...")


    // ==================================================
    // FAVORITOS
    // ==================================================

    private var favorites by mutableStateOf<List<Favorite>>(emptyList())

    /*
     * Guardamos las llegadas de cada favorito.
     *
     * La clave es el Favorite y el valor es la lista
     * de autobuses que están llegando a esa parada/línea.
     */
    private var favoriteArrivals by mutableStateOf(
        emptyMap<Favorite, List<BusArrival>>()
    )

    private lateinit var favoriteStorage: FavoriteStorage


    // ==================================================
    // PERMISO NOTIFICACIONES
    // ==================================================

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }


    // ==================================================
    // ON CREATE
    // ==================================================

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        enableEdgeToEdge()


        // ==================================================
        // FAVORITOS
        // ==================================================

        favoriteStorage = FavoriteStorage(this)

        favorites = favoriteStorage.getFavorites()


        // ==================================================
        // NOTIFICACIONES
        // ==================================================

        requestNotificationPermission()

        getFcmToken()


        // ==================================================
        // UI
        // ==================================================

        setContent {

            EMTRealTimeTheme {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {


                    // ==================================================
                    // CABECERA
                    // ==================================================

                    Text(
                        text = "EMT Real-Time",
                        style = MaterialTheme.typography.headlineMedium
                    )


                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )


                    // ==================================================
                    // PARADA
                    // ==================================================

                    OutlinedTextField(
                        value = stopInput,

                        onValueChange = {
                            stopInput = it
                        },

                        label = {
                            Text("Número de parada")
                        },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()
                    )


                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )


                    // ==================================================
                    // LÍNEA
                    // ==================================================

                    OutlinedTextField(
                        value = lineInput,

                        onValueChange = {
                            lineInput = it
                        },

                        label = {
                            Text("Línea (opcional)")
                        },

                        placeholder = {
                            Text("Ejemplo: 27")
                        },

                        singleLine = true,

                        modifier = Modifier.fillMaxWidth()
                    )


                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )


                    // ==================================================
                    // BUSCAR
                    // ==================================================

                    Button(
                        onClick = {

                            val newStop =
                                stopInput.toIntOrNull()


                            if (newStop != null) {

                                stopId = newStop

                                loadArrivals(newStop)
                                loadAllFavoriteArrivals()

                            } else {

                                errorMessage =
                                    "Introduce un número de parada válido"
                            }
                        },

                        modifier = Modifier.fillMaxWidth()
                    ) {

                        Text("Buscar")
                    }


                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )


                    // ==================================================
                    // MIS FAVORITOS
                    // ==================================================

                    if (favorites.isNotEmpty()) {

                        Text(
                            text = "⭐ Mis favoritos",
                            style = MaterialTheme.typography.titleLarge
                        )


                        Spacer(
                            modifier = Modifier.height(6.dp)
                        )


                        /*
                         * Mostramos todos los favoritos.
                         */
                        favorites.forEach { favorite ->

                            FavoriteCard(
                                favorite = favorite,

                                arrivals =
                                    favoriteArrivals[
                                        favorite
                                    ] ?: emptyList(),

                                onClick = {

                                    stopInput =
                                        favorite.stopId.toString()

                                    lineInput =
                                        favorite.line

                                    stopId =
                                        favorite.stopId

                                    loadArrivals(
                                        favorite.stopId
                                    )
                                },

                                onDelete = {

                                    favoriteStorage
                                        .deleteFavorite(
                                            favorite
                                        )

                                    favorites =
                                        favoriteStorage
                                            .getFavorites()

                                    favoriteArrivals =
                                        favoriteArrivals
                                            .toMutableMap()
                                            .apply {
                                                remove(favorite)
                                            }
                                }
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(5.dp)
                            )
                        }


                        Spacer(
                            modifier =
                                Modifier.height(3.dp)
                        )
                    }


                    // ==================================================
                    // PARADA ACTUAL
                    // ==================================================

                    Text(
                        text = "Parada $stopId",

                        style =
                            MaterialTheme.typography.titleMedium
                    )


                    if (lineInput.isNotBlank()) {

                        Text(
                            text =
                                "Línea ${lineInput.trim()}",

                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(4.dp)
                    )


                    // ==================================================
                    // ÚLTIMA ACTUALIZACIÓN
                    // ==================================================

                    if (loading) {

                        Text(
                            text = "⟳ Actualizando...",

                            style =
                                MaterialTheme.typography.bodySmall
                        )

                    } else if (lastUpdate.isNotEmpty()) {

                        Text(
                            text =
                                "● Actualizado hace ${secondsSinceUpdate}s",

                            style =
                                MaterialTheme.typography.bodySmall
                        )
                    }


                    Spacer(
                        modifier =
                            Modifier.height(8.dp)
                    )


                    // ==================================================
                    // FILTRAR LÍNEA
                    // ==================================================

                    val filteredArrivals =

                        if (lineInput.isBlank()) {

                            arrivals

                        } else {

                            arrivals.filter {

                                it.line.equals(
                                    lineInput.trim(),
                                    ignoreCase = true
                                )
                            }
                        }


                    // ==================================================
                    // GUARDAR FAVORITO
                    // ==================================================

                    if (
                        lineInput.isNotBlank() &&
                        filteredArrivals.isNotEmpty()
                    ) {

                        val firstArrival =
                            filteredArrivals.first()


                        /*
                         * Comprobamos si ya existe.
                         */
                        val alreadyFavorite =
                            favorites.any {

                                it.stopId == stopId &&
                                        it.line.equals(
                                            firstArrival.line,
                                            ignoreCase = true
                                        )
                            }


                        if (!alreadyFavorite) {

                            Button(
                                onClick = {

                                    val newFavorite =
                                        Favorite(

                                            stopId =
                                                stopId,

                                            line =
                                                firstArrival.line,

                                            destination =
                                                firstArrival.destination
                                        )


                                    favoriteStorage
                                        .saveFavorite(
                                            newFavorite
                                        )


                                    favorites =
                                        favoriteStorage
                                            .getFavorites()


                                    /*
                                     * Cargar inmediatamente
                                     * las llegadas del nuevo
                                     * favorito.
                                     */
                                    loadFavoriteArrivals(
                                        newFavorite
                                    )
                                },

                                modifier =
                                    Modifier.fillMaxWidth()
                            ) {

                                Text(
                                    "⭐ Guardar como favorito"
                                )
                            }


                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }
                    }


                    // ==================================================
                    // CARGANDO
                    // ==================================================

                    if (
                        loading &&
                        filteredArrivals.isEmpty()
                    ) {

                        Text(
                            text =
                                "Actualizando llegadas..."
                        )

                    } else {


                        // ==================================================
                        // ERROR
                        // ==================================================

                        if (errorMessage.isNotEmpty()) {

                            Text(
                                text =
                                    "⚠ $errorMessage",

                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .error,

                                style =
                                    MaterialTheme
                                        .typography
                                        .bodySmall
                            )


                            Spacer(
                                modifier =
                                    Modifier.height(8.dp)
                            )
                        }


                        // ==================================================
                        // LISTA DE AUTOBUSES
                        // ==================================================

                        LazyColumn(
                            modifier =
                                Modifier.fillMaxSize()
                        ) {

                            items(
                                filteredArrivals
                            ) { arrival ->

                                ArrivalCard(
                                    arrival =
                                        arrival
                                )


                                Spacer(
                                    modifier =
                                        Modifier.height(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }


        // ==================================================
        // PRIMERA CARGA
        // ==================================================

        loadArrivals(stopId)


        // ==================================================
        // CARGAR LLEGADAS DE FAVORITOS
        // ==================================================

        loadAllFavoriteArrivals()


        // ==================================================
        // ACTUALIZACIÓN AUTOMÁTICA
        // ==================================================

        startAutoRefresh()
    }


    // ==================================================
    // CARGAR LLEGADAS PRINCIPALES
    // ==================================================

    private fun loadArrivals(
        stop: Int
    ) {

        lifecycleScope.launch {

            secondsSinceUpdate = 0

            loading = true


            try {

                Log.d(
                    "EMT_API",
                    "1. Solicitando parada $stop"
                )


                val result =
                    RetrofitClient
                        .api
                        .getArrivals(stop)


                Log.d(
                    "EMT_API",
                    "2. Respuesta recibida"
                )


                Log.d(
                    "EMT_API",
                    "3. Número de buses: ${result.size}"
                )


                Log.d(
                    "EMT_API",
                    "4. Datos: $result"
                )


                arrivals = result

                errorMessage = ""

                secondsSinceUpdate = 0


                lastUpdate =
                    SimpleDateFormat(
                        "HH:mm:ss",
                        Locale.getDefault()
                    ).format(Date())


            } catch (e: Exception) {

                Log.e(
                    "EMT_API",
                    "5. ERROR ${e.javaClass.name}: ${e.message}",
                    e
                )


                errorMessage =
                    "Sin conexión"


            } finally {

                loading = false


                Log.d(
                    "EMT_API",
                    "6. loading=false"
                )
            }
        }
    }


    // ==================================================
    // CARGAR UN FAVORITO
    // ==================================================

    private fun loadFavoriteArrivals(
        favorite: Favorite
    ) {

        lifecycleScope.launch {

            try {

                Log.d(
                    "EMT_FAVORITE",
                    "Cargando ${favorite.line} " +
                            "en parada ${favorite.stopId}"
                )


                val result =
                    RetrofitClient
                        .api
                        .getArrivals(
                            favorite.stopId
                        )


                /*
                 * Filtramos únicamente la línea
                 * correspondiente al favorito.
                 */
                val filtered =
                    result
                        .filter {

                            it.line.equals(
                                favorite.line,
                                ignoreCase = true
                            )
                        }
                        .sortedBy {
                            it.minutes
                        }
                        .take(3)


                /*
                 * Actualizamos únicamente
                 * este favorito.
                 */
                favoriteArrivals =
                    favoriteArrivals
                        .toMutableMap()
                        .apply {

                            put(
                                favorite,
                                filtered
                            )
                        }


                Log.d(
                    "EMT_FAVORITE",
                    "${favorite.line}: " +
                            "${filtered.size} llegadas"
                )


            } catch (e: Exception) {

                Log.e(
                    "EMT_FAVORITE",
                    "Error cargando favorito",
                    e
                )


                /*
                 * Si falla la conexión dejamos
                 * los datos anteriores.
                 */
            }
        }
    }


    // ==================================================
    // CARGAR TODOS LOS FAVORITOS
    // ==================================================

    private fun loadAllFavoriteArrivals() {

        favorites.forEach { favorite ->

            loadFavoriteArrivals(
                favorite
            )
        }
    }


    // ==================================================
    // ACTUALIZACIÓN AUTOMÁTICA
    // ==================================================

    private fun startAutoRefresh() {


        // ==================================================
        // CONTADOR
        // ==================================================

        lifecycleScope.launch {

            while (true) {

                delay(1_000)


                if (
                    !loading &&
                    lastUpdate.isNotEmpty()
                ) {

                    secondsSinceUpdate++
                }
            }
        }


        // ==================================================
        // ACTUALIZAR DATOS
        // ==================================================

        lifecycleScope.launch {

            while (true) {

                delay(30_000)


                /*
                 * Actualizar búsqueda principal.
                 */
                loadArrivals(
                    stopId
                )


                /*
                 * Actualizar todos los favoritos.
                 */
                loadAllFavoriteArrivals()
            }
        }
    }


    // ==================================================
    // PERMISO NOTIFICACIONES
    // ==================================================

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&

            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }


    // ==================================================
    // FIREBASE TOKEN
    // ==================================================

    private fun getFcmToken() {

        FirebaseMessaging
            .getInstance()
            .token

            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    fcmToken =
                        task.result


                    Log.d(
                        "FCM",
                        "Token obtenido: $fcmToken"
                    )

                } else {

                    fcmToken =
                        "Error obteniendo FCM token"


                    Log.e(
                        "FCM",
                        "Error obteniendo FCM token",

                        task.exception
                    )
                }
            }
    }
}


// ======================================================
// TARJETA DE FAVORITO — COMPACTA
// ======================================================

@Composable
private fun FavoriteCard(
    favorite: Favorite,
    arrivals: List<BusArrival>,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    )
        ) {


            // ==================================================
            // CABECERA
            // ==================================================

            Row(
                modifier =
                    Modifier.fillMaxWidth(),

                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Column(
                    modifier =
                        Modifier.weight(1f)
                ) {

                    Text(
                        text =
                            "${favorite.line} · " +
                                    favorite.destination,

                        style =
                            MaterialTheme
                                .typography
                                .titleSmall,

                        maxLines = 1
                    )


                    Text(
                        text =
                            "Parada ${favorite.stopId}",

                        style =
                            MaterialTheme
                                .typography
                                .labelSmall
                    )
                }


                // ==================================================
                // PAPELERA
                // ==================================================

                TextButton(
                    onClick = onDelete
                ) {

                    Text(
                        text = "🗑",

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }
            }


            // ==================================================
            // LLEGADAS
            // ==================================================

            if (arrivals.isEmpty()) {

                Text(
                    text =
                        "Sin próximas llegadas",

                    style =
                        MaterialTheme
                            .typography
                            .labelSmall
                )

            } else {

                Row(
                    modifier =
                        Modifier.fillMaxWidth(),

                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    arrivals
                        .take(3)
                        .forEachIndexed {
                                index,
                                arrival ->

                            if (index > 0) {

                                Spacer(
                                    modifier =
                                        Modifier.width(8.dp)
                                )
                            }


                            FavoriteArrivalRow(
                                arrival =
                                    arrival,

                                modifier =
                                    Modifier.weight(1f)
                            )
                        }
                }
            }
        }
    }
}


// ======================================================
// LLEGADA DENTRO DEL FAVORITO — COMPACTA
// ======================================================

@Composable
private fun FavoriteArrivalRow(
    arrival: BusArrival,
    modifier: Modifier = Modifier
) {

    val statusColor =
        when {

            arrival.minutes <= 5 ->
                Color(0xFF2E7D32)

            arrival.minutes <= 10 ->
                Color(0xFFF9A825)

            arrival.minutes <= 20 ->
                Color(0xFFEF6C00)

            else ->
                Color(0xFFC62828)
        }


    Row(
        modifier =
            modifier,

        verticalAlignment =
            Alignment.CenterVertically
    ) {


        // ==================================================
        // SEMÁFORO
        // ==================================================

        Canvas(
            modifier =
                Modifier
                    .width(7.dp)
                    .height(7.dp)
        ) {

            drawCircle(
                color =
                    statusColor
            )
        }


        Spacer(
            modifier =
                Modifier.width(4.dp)
        )


        // ==================================================
        // TIEMPO
        // ==================================================

        Text(
            text =
                "${arrival.minutes} min",

            color =
                statusColor,

            style =
                MaterialTheme
                    .typography
                    .labelMedium,

            maxLines = 1
        )


        Spacer(
            modifier =
                Modifier.width(4.dp)
        )


        // ==================================================
        // DISTANCIA
        // ==================================================

        Text(
            text =
                "${arrival.distance_meters} m",

            style =
                MaterialTheme
                    .typography
                    .labelSmall,

            maxLines = 1
        )
    }
}


// ======================================================
// TARJETA PRINCIPAL DE AUTOBÚS
// ======================================================

@Composable
private fun ArrivalCard(
    arrival: BusArrival
) {

    val statusColor =
        when {

            arrival.minutes <= 5 ->
                Color(0xFF2E7D32)

            arrival.minutes <= 10 ->
                Color(0xFFF9A825)

            arrival.minutes <= 20 ->
                Color(0xFFEF6C00)

            else ->
                Color(0xFFC62828)
        }


    val statusText =
        when {

            arrival.minutes <= 2 ->
                "Llega ahora"

            arrival.minutes <= 5 ->
                "Muy próximo"

            arrival.minutes <= 10 ->
                "Próximo"

            arrival.minutes <= 20 ->
                "En camino"

            else ->
                "Más tarde"
        }


    Card(
        modifier =
            Modifier.fillMaxWidth()
    ) {

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 14.dp
                    ),

            verticalAlignment =
                Alignment.CenterVertically
        ) {


            // ==================================================
            // INFORMACIÓN
            // ==================================================

            Column(
                modifier =
                    Modifier.weight(1f)
            ) {

                Row(
                    verticalAlignment =
                        Alignment.CenterVertically
                ) {

                    Text(
                        text =
                            arrival.line,

                        style =
                            MaterialTheme
                                .typography
                                .titleLarge
                    )


                    Spacer(
                        modifier =
                            Modifier.width(10.dp)
                    )


                    Text(
                        text =
                            arrival.destination,

                        style =
                            MaterialTheme
                                .typography
                                .bodyMedium
                    )
                }


                Spacer(
                    modifier =
                        Modifier.height(6.dp)
                )


                Text(
                    text =
                        "📍 ${arrival.distance_meters} m",

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )


                Spacer(
                    modifier =
                        Modifier.height(4.dp)
                )


                Text(
                    text =
                        statusText,

                    color =
                        statusColor,

                    style =
                        MaterialTheme
                            .typography
                            .bodySmall
                )
            }


            // ==================================================
            // SEMÁFORO + MINUTOS
            // ==================================================

            Row(
                verticalAlignment =
                    Alignment.CenterVertically
            ) {

                Canvas(
                    modifier =
                        Modifier
                            .width(14.dp)
                            .height(14.dp)
                ) {

                    drawCircle(
                        color =
                            statusColor
                    )
                }


                Spacer(
                    modifier =
                        Modifier.width(8.dp)
                )


                Column(
                    horizontalAlignment =
                        Alignment.End
                ) {

                    Text(
                        text =
                            "${arrival.minutes}",

                        color =
                            statusColor,

                        style =
                            MaterialTheme
                                .typography
                                .headlineMedium
                    )


                    Text(
                        text =
                            "min",

                        style =
                            MaterialTheme
                                .typography
                                .bodySmall
                    )
                }
            }
        }
    }
}
