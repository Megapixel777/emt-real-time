package com.tomasperez.emtrealtime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    private var stopId by mutableIntStateOf(72)

    private var stopInput by mutableStateOf("72")

    private var lineInput by mutableStateOf("")

    private var arrivals by mutableStateOf<List<BusArrival>>(emptyList())

    private var errorMessage by mutableStateOf("")

    private var lastUpdate by mutableStateOf("")

    private var loading by mutableStateOf(false)

    private var fcmToken by mutableStateOf("Obteniendo token...")

    private var favorite by mutableStateOf<Favorite?>(null)

    private lateinit var favoriteStorage: FavoriteStorage

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        // Inicializar almacenamiento de favoritos
        favoriteStorage = FavoriteStorage(this)

        // Recuperar favorito guardado
        favorite = favoriteStorage.getFavorite()

        requestNotificationPermission()

        getFcmToken()

        setContent {
            EMTRealTimeTheme {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp)
                ) {

                    Text(
                        text = "EMT Real-Time",
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Spacer(
                        modifier = Modifier.height(12.dp)
                    )

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

                    Button(
                        onClick = {

                            val newStop = stopInput.toIntOrNull()

                            if (newStop != null) {

                                stopId = newStop

                                loadArrivals(newStop)

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

                    // ============================
                    // FAVORITO
                    // ============================

                    if (favorite != null) {

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {

                                val savedFavorite = favorite!!

                                stopInput = savedFavorite.stopId.toString()
                                lineInput = savedFavorite.line
                                stopId = savedFavorite.stopId

                                loadArrivals(savedFavorite.stopId)
                            }
                        ) {

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {

                                Text(
                                    text = "⭐ Mi favorito",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                Spacer(
                                    modifier = Modifier.height(4.dp)
                                )

                                Text(
                                    text = "Parada ${favorite!!.stopId} · Línea ${favorite!!.line}"
                                )

                                Text(
                                    text = favorite!!.destination,
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )

                                OutlinedButton(
                                    onClick = {
                                        favoriteStorage.deleteFavorite()
                                        favorite = null
                                    }
                                ) {
                                    Text("Eliminar favorito")
                                }
                            }
                        }

                        Spacer(
                            modifier = Modifier.height(12.dp)
                        )
                    }

                    Text(
                        text = "Parada $stopId",
                        style = MaterialTheme.typography.titleMedium
                    )

                    if (lineInput.isNotBlank()) {

                        Text(
                            text = "Línea ${lineInput.trim()}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(4.dp)
                    )

                    if (lastUpdate.isNotEmpty()) {

                        Text(
                            text = "Última actualización: $lastUpdate",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )

                    // ============================
                    // GUARDAR FAVORITO
                    // ============================

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

                    if (lineInput.isNotBlank() && filteredArrivals.isNotEmpty()) {

                        val firstArrival = filteredArrivals.first()

                        Button(
                            onClick = {

                                val newFavorite = Favorite(
                                    stopId = stopId,
                                    line = firstArrival.line,
                                    destination = firstArrival.destination
                                )

                                favoriteStorage.saveFavorite(newFavorite)

                                favorite = newFavorite
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("⭐ Guardar como favorito")
                        }

                        Spacer(
                            modifier = Modifier.height(8.dp)
                        )
                    }

                    if (loading && filteredArrivals.isEmpty()) {

                        Text(
                            text = "Actualizando llegadas..."
                        )

                    } else {

                        if (errorMessage.isNotEmpty()) {

                            Text(
                                text = "⚠ Sin conexión · mostrando los últimos datos disponibles",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )

                            Spacer(
                                modifier = Modifier.height(8.dp)
                            )
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {

                            items(filteredArrivals) { arrival ->

                                ArrivalCard(
                                    arrival = arrival
                                )

                                Spacer(
                                    modifier = Modifier.height(8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        loadArrivals(stopId)

        startAutoRefresh()
    }

    private fun loadArrivals(stop: Int) {

        lifecycleScope.launch {

            loading = true

            try {

                val result = RetrofitClient.api
                    .getArrivals(stop)

                arrivals = result

                errorMessage = ""

                lastUpdate = SimpleDateFormat(
                    "HH:mm:ss",
                    Locale.getDefault()
                ).format(Date())

            } catch (exception: Exception) {

                errorMessage =
                    "No se pudo actualizar. Mostrando los últimos datos disponibles."

            } finally {

                loading = false
            }
        }
    }

    private fun startAutoRefresh() {

        lifecycleScope.launch {

            while (true) {

                delay(30_000)

                loadArrivals(stopId)
            }
        }
    }

    private fun requestNotificationPermission() {

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
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

    private fun getFcmToken() {

        FirebaseMessaging.getInstance()
            .token
            .addOnCompleteListener { task ->

                if (task.isSuccessful) {

                    fcmToken = task.result

                } else {

                    fcmToken = "Error obteniendo FCM token"
                }
            }
    }
}

@androidx.compose.runtime.Composable
private fun ArrivalCard(
    arrival: BusArrival
) {

    Card(
        modifier = Modifier.fillMaxWidth()
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {

                Text(
                    text = "Línea ${arrival.line}",
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = arrival.destination,
                    style = MaterialTheme.typography.bodyMedium
                )

                Text(
                    text = "${arrival.distance_meters} m",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Text(
                text = "${arrival.minutes} min",
                style = MaterialTheme.typography.headlineSmall
            )
        }
    }
}