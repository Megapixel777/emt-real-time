package com.tomasperez.emtrealtime

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(
    private val context: Context
) {

    companion object {

        private const val CHANNEL_ID = "emt_arrivals"
        private const val CHANNEL_NAME = "EMT Real-Time"

        private const val CHANNEL_DESCRIPTION =
            "Notificaciones de llegada de autobuses"
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {

                    description =
                        CHANNEL_DESCRIPTION
                }

            val notificationManager =
                context.getSystemService(
                    Context.NOTIFICATION_SERVICE
                ) as NotificationManager

            notificationManager.createNotificationChannel(
                channel
            )
        }
    }

    fun showArrivalNotification(
        line: String,
        stopId: Int,
        minutes: Int
    ) {

        // Android 13+
        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU
        ) {

            if (
                context.checkSelfPermission(
                    Manifest.permission.POST_NOTIFICATIONS
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        val title =
            "🚌 Línea $line"

        val message =
            if (minutes <= 1) {

                "Está a punto de llegar · Parada $stopId"

            } else {

                "Llega en $minutes minutos · Parada $stopId"
            }

        val notification =
            NotificationCompat.Builder(
                context,
                CHANNEL_ID
            )
                .setSmallIcon(
                    android.R.drawable.ic_dialog_info
                )
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(
                    NotificationCompat.PRIORITY_HIGH
                )
                .setAutoCancel(true)
                .setCategory(
                    NotificationCompat.CATEGORY_TRANSPORT
                )
                .build()

        /*
         * El ID ahora depende de parada + línea.
         *
         * Así:
         *
         * parada 1503 + línea 49
         *
         * es diferente de:
         *
         * parada 1503 + línea 42
         *
         * y también de:
         *
         * parada 2000 + línea 49
         */
        val notificationId =
            "$stopId-$line".hashCode()

        NotificationManagerCompat
            .from(context)
            .notify(
                notificationId,
                notification
            )
    }
}