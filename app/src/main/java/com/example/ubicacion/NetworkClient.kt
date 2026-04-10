package com.example.ubicacion

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente de red encargado de configurar y proveer la instancia de Retrofit.
 * Se utiliza para realizar peticiones a la API de OpenRouteService.
 */
object NetworkClient {

    // URL base de la API de OpenRouteService para obtener rutas
    private const val BASE_URL = "https://api.openrouteservice.org/"

    // Inicialización perezosa (lazy) de Retrofit
    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            // GsonConverterFactory convierte el JSON de la API automáticamente a nuestras clases de datos
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }
}