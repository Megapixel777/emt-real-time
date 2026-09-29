package com.tomasperez.emtrealtime

import com.tomasperez.emtrealtime.data.EmtApi
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {

    private const val BASE_URL = "http://192.168.1.130:8000/"

    val api: EmtApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EmtApi::class.java)
    }
}