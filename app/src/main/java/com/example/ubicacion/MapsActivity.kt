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
import com.google.android.gms.location.*
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
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val ORS_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjBlZDk0ZjYyNTYxYjQ0MDY4NWZmMmQ3NmU5MThiMWFkIiwiaCI6Im11cm11cjY0In0="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue = packageName
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        setContentView(R.layout.activity_maps)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.isTilesScaledToDpi = true

        val btnSetHome = findViewById<Button>(R.id.btnSetHome)
        val imgCursor = findViewById<CardView>(R.id.imgCursor)

        loadHomeLocation()
        checkPermissions()

        if (homeLocation != null) {
            btnSetHome.visibility = View.GONE
            imgCursor.visibility = View.GONE

            // Primero mostramos la casa para que no se vea el mapa vacío
            map.controller.setZoom(16.0)
            map.controller.setCenter(homeLocation)
            addMarker(homeLocation!!)

            // Pedimos la ubicación real para trazar la ruta
            obtenerUbicacionActualYTraza()
        } else {
            btnSetHome.visibility = View.VISIBLE
            imgCursor.visibility = View.VISIBLE
            map.controller.setZoom(17.0)
            map.controller.setCenter(GeoPoint(20.1412, -101.1775)) // ITSUR
        }

        btnSetHome.setOnClickListener {
            val center = map.mapCenter as GeoPoint
            saveHomeLocation(center.latitude, center.longitude)
            btnSetHome.visibility = View.GONE
            imgCursor.visibility = View.GONE
            addMarker(center)
            obtenerUbicacionActualYTraza()
            Toast.makeText(this, "Casa guardada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun obtenerUbicacionActualYTraza() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setMaxUpdates(1) // Solo queremos una actualización rápida para la ruta
            .build()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    val lastLocation = locationResult.lastLocation
                    if (lastLocation != null) {
                        val myPoint = GeoPoint(lastLocation.latitude, lastLocation.longitude)
                        homeLocation?.let { home ->
                            drawRoute(myPoint, home)
                        }
                    }
                }
            }, mainLooper)
        }
    }

    private fun drawRoute(start: GeoPoint, end: GeoPoint) {
        val service = NetworkClient.retrofit.create(RouteService::class.java)
        service.getRoute(ORS_API_KEY, "${start.longitude},${start.latitude}", "${end.longitude},${end.latitude}")
            .enqueue(object : Callback<RouteResponse> {
                override fun onResponse(call: Call<RouteResponse>, response: Response<RouteResponse>) {
                    if (response.isSuccessful) {
                        val points = response.body()?.features?.firstOrNull()?.geometry?.coordinates
                        val line = Polyline(map)
                        line.outlinePaint.color = Color.BLUE
                        line.outlinePaint.strokeWidth = 14f

                        val routePoints = mutableListOf<GeoPoint>()
                        points?.forEach { routePoints.add(GeoPoint(it[1], it[0])) }

                        map.overlays.removeAll { it is Polyline }
                        line.setPoints(routePoints)
                        map.overlays.add(line)
                        map.invalidate()

                        // Zoom para ver ambos puntos
                        map.zoomToBoundingBox(line.bounds, true, 180)
                    }
                }
                override fun onFailure(call: Call<RouteResponse>, t: Throwable) {
                    Toast.makeText(this@MapsActivity, "Error de conexión", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun addMarker(point: GeoPoint) {
        map.overlays.removeAll { it is Marker }
        val marker = Marker(map)
        marker.position = point
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        marker.title = "Mi Casa"
        map.overlays.add(marker)
        map.invalidate()
    }

    private fun saveHomeLocation(lat: Double, lon: Double) {
        val sp = getSharedPreferences("MiAppConfig", Context.MODE_PRIVATE)
        sp.edit().putFloat("home_lat", lat.toFloat()).putFloat("home_lon", lon.toFloat()).apply()
        homeLocation = GeoPoint(lat, lon)
    }

    private fun loadHomeLocation() {
        val sp = getSharedPreferences("MiAppConfig", Context.MODE_PRIVATE)
        val lat = sp.getFloat("home_lat", 0f).toDouble()
        val lon = sp.getFloat("home_lon", 0f).toDouble()
        if (lat != 0.0) homeLocation = GeoPoint(lat, lon)
    }

    private fun checkPermissions() {
        val p = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (p.any { ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED }) {
            ActivityCompat.requestPermissions(this, p, 1)
        }
    }

    override fun onResume() { super.onResume(); map.onResume() }
    override fun onPause() { super.onPause(); map.onPause() }
}