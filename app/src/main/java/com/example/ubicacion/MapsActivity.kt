package com.example.ubicacion

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapsActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private var homeLocation: GeoPoint? = null

    // Tu API Key de OpenRouteService
    private val ORS_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjBlZDk0ZjYyNTYxYjQ0MDY4NWZmMmQ3NmU5MThiMWFkIiwiaCI6Im11cm11cjY0In0="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- OPTIMIZACIÓN DE CARGA ---
        // Identificar la app ayuda a que los servidores de OSM respondan más rápido
        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        setContentView(R.layout.activity_maps)

        // Inicializar Mapa
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK) // Servidor estándar más estable
        map.setMultiTouchControls(true)

        // --- MÁS OPTIMIZACIÓN ---
        map.isTilesScaledToDpi = true // Mejora la nitidez y carga según la resolución
        map.setHasTransientState(true)

        val mapController = map.controller
        mapController.setZoom(17.0)

        val btnSetHome = findViewById<Button>(R.id.btnSetHome)
        val imgCursor = findViewById<CardView>(R.id.imgCursor)

        loadHomeLocation()
        checkPermissions()

        if (homeLocation != null) {
            // MODO NAVEGACIÓN: Ya hay casa, ocultamos UI de configuración
            btnSetHome.visibility = View.GONE
            imgCursor.visibility = View.GONE
            addMarker(homeLocation!!)

            // Trazar ruta automática al abrir
            getDeviceLocation { myLocation ->
                drawRoute(myLocation, homeLocation!!)
            }
        } else {
            // MODO CONFIGURACIÓN: Mostrar bolita azul y botón
            btnSetHome.visibility = View.VISIBLE
            imgCursor.visibility = View.VISIBLE
            // Centrar en un punto inicial (ITSUR)
            mapController.setCenter(GeoPoint(20.1412, -101.1775))
        }

        btnSetHome.setOnClickListener {
            val center = map.mapCenter as GeoPoint
            saveHomeLocation(center.latitude, center.longitude)

            // Ocultar elementos de configuración
            btnSetHome.visibility = View.GONE
            imgCursor.visibility = View.GONE

            addMarker(center)

            getDeviceLocation { myLocation ->
                drawRoute(myLocation, center)
            }
            Toast.makeText(this, "¡Casa guardada!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun drawRoute(start: GeoPoint, end: GeoPoint) {
        val service = NetworkClient.retrofit.create(RouteService::class.java)
        val startStr = "${start.longitude},${start.latitude}"
        val endStr = "${end.longitude},${end.latitude}"

        service.getRoute(ORS_API_KEY, startStr, endStr).enqueue(object : Callback<RouteResponse> {
            override fun onResponse(call: Call<RouteResponse>, response: Response<RouteResponse>) {
                if (response.isSuccessful) {
                    val points = response.body()?.features?.firstOrNull()?.geometry?.coordinates
                    val line = Polyline(map)
                    line.outlinePaint.color = Color.BLUE
                    line.outlinePaint.strokeWidth = 14f

                    val routePoints = mutableListOf<GeoPoint>()
                    points?.forEach {
                        routePoints.add(GeoPoint(it[1], it[0]))
                    }

                    // Limpiar polilíneas viejas
                    map.overlays.removeAll { it is Polyline }

                    line.setPoints(routePoints)
                    map.overlays.add(line)
                    map.invalidate()

                    // Zoom automático para ver toda la ruta
                    map.zoomToBoundingBox(line.bounds, true, 160)
                }
            }

            override fun onFailure(call: Call<RouteResponse>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getDeviceLocation(callback: (GeoPoint) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    callback(GeoPoint(location.latitude, location.longitude))
                } else {
                    Toast.makeText(this, "Buscando señal GPS...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun addMarker(point: GeoPoint) {
        // Limpiar marcadores anteriores
        map.overlays.removeAll { it is Marker }

        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Mi Casa"
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun saveHomeLocation(lat: Double, lon: Double) {
        val sharedPref = getSharedPreferences("MiAppConfig", Context.MODE_PRIVATE)
        sharedPref.edit().putFloat("home_lat", lat.toFloat()).putFloat("home_lon", lon.toFloat()).apply()
        homeLocation = GeoPoint(lat, lon)
    }

    private fun loadHomeLocation() {
        val sharedPref = getSharedPreferences("MiAppConfig", Context.MODE_PRIVATE)
        val lat = sharedPref.getFloat("home_lat", 0f).toDouble()
        val lon = sharedPref.getFloat("home_lon", 0f).toDouble()
        if (lat != 0.0) {
            homeLocation = GeoPoint(lat, lon)
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permissions.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, permissions, 1)
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        super.onPause()
        map.onPause()
    }
}