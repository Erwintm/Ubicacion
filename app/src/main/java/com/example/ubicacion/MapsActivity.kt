package com.example.ubicacion

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    // ✅ TU LLAVE DE OPENROUTE SERVICE
    private val ORS_API_KEY = "eyJvcmciOiI1YjNjZTM1OTc4NTExMTAwMDFjZjYyNDgiLCJpZCI6IjBlZDk0ZjYyNTYxYjQ0MDY4NWZmMmQ3NmU5MThiMWFkIiwiaCI6Im11cm11cjY0In0="

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configuración necesaria para OSM
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        setContentView(R.layout.activity_maps)

        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        val mapController = map.controller
        mapController.setZoom(15.0)

        // Punto inicial (puedes poner las coordenadas de tu ciudad)
        val startPoint = GeoPoint(20.21, -101.18)
        mapController.setCenter(startPoint)

        checkPermissions()

        findViewById<Button>(R.id.btnSetHome).setOnClickListener {
            val center = map.mapCenter as GeoPoint
            homeLocation = center

            // Poner un marcador en la casa
            map.overlays.clear()
            val homeMarker = Marker(map)
            homeMarker.position = center
            homeMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            homeMarker.title = "Mi Casa"
            map.overlays.add(homeMarker)
            map.invalidate()

            Toast.makeText(this, "Casa guardada", Toast.LENGTH_SHORT).show()

            // Simulamos que al guardar la casa, trazamos ruta desde un punto cercano
            // En una app real, usarías la ubicación del GPS
            drawRoute(GeoPoint(20.22, -101.19), center)
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
                    val line = Polyline()
                    line.outlinePaint.color = Color.BLUE
                    line.outlinePaint.strokeWidth = 10f

                    val routePoints = mutableListOf<GeoPoint>()
                    points?.forEach {
                        routePoints.add(GeoPoint(it[1], it[0]))
                    }
                    line.setPoints(routePoints)
                    map.overlays.add(line)
                    map.invalidate()
                }
            }
            override fun onFailure(call: Call<RouteResponse>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "Error de ruta", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun checkPermissions() {
        val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE)
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