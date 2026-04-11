package com.example.ubicacion

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente de red encargado de configurar y proveer la instancia de Retrofit.
 * Se utiliza para realizar peticiones a la API de OpenRouteService.
 */
object NetworkClient {

    private const val BASE_URL = "https://api.openrouteservice.org/"


    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)

            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}