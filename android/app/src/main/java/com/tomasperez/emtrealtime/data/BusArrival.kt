package com.tomasperez.emtrealtime.data

data class BusArrival(
    val line: String,
    val destination: String,
    val minutes: Int,
    val distance_meters: Int
)