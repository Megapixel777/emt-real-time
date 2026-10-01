package com.tomasperez.emtrealtime.data

data class Favorite(
    val stopId: Int,
    val line: String,
    val destination: String,

    // null = notificaciones desactivadas
    // 1, 3, 5, 10 = minutos antes del aviso
    val notificationMinutes: Int? = null
)