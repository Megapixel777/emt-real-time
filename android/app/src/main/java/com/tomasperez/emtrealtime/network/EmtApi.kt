package com.tomasperez.emtrealtime.network

import com.tomasperez.emtrealtime.data.BusArrival
import retrofit2.http.GET
import retrofit2.http.Path

interface EmtApi {

    @GET("stops/{stopId}/arrivals")
    suspend fun getArrivals(
        @Path("stopId") stopId: Int
    ): List<BusArrival>
}