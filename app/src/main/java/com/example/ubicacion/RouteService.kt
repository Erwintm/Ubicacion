package com.example.ubicacion


import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface RouteService {
    @GET("v2/directions/driving-car")
    fun getRoute(
        @Query("api_key") apiKey: String,
        @Query("start") start: String, // format: "lng,lat"
        @Query("end") end: String     // format: "lng,lat"
    ): Call<RouteResponse>
}

// Clase para parsear la respuesta JSON (Simplificada)
data class RouteResponse(val features: List<Feature>)
data class Feature(val geometry: Geometry)
data class Geometry(val coordinates: List<List<Double>>)