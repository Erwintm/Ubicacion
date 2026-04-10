package com.example.ubicacion

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.ubicacion.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null
    private var homeLocation: LatLng? = null
    // REEMPLAZA ESTA KEY con la de OpenRouteService
    private val ORS_API_KEY = "TU_API_KEY_AQUI"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Inicializar fragmento del mapa de forma segura
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        findViewById<Button>(R.id.btnSetHome).setOnClickListener {
            mMap?.cameraPosition?.target?.let {
                homeLocation = it
                Toast.makeText(this, "Casa guardada correctamente", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "El mapa no está listo", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        enableMyLocation()

        mMap?.setOnMyLocationButtonClickListener {
            homeLocation?.let { home ->
                getDeviceLocation { myLocation ->
                    drawRoute(myLocation, home)
                }
            } ?: Toast.makeText(this, "Primero fija la ubicación de tu casa", Toast.LENGTH_SHORT).show()
            false
        }
    }

    private fun drawRoute(start: LatLng, end: LatLng) {
        val service = NetworkClient.retrofit.create(RouteService::class.java)
        val startStr = "${start.longitude},${start.latitude}"
        val endStr = "${end.longitude},${end.latitude}"

        service.getRoute(ORS_API_KEY, startStr, endStr).enqueue(object : Callback<RouteResponse> {
            override fun onResponse(call: Call<RouteResponse>, response: Response<RouteResponse>) {
                if (response.isSuccessful) {
                    val points = response.body()?.features?.firstOrNull()?.geometry?.coordinates
                    val polylineOptions = PolylineOptions().color(Color.BLUE).width(12f)

                    points?.forEach {
                        polylineOptions.add(LatLng(it[1], it[0]))
                    }
                    mMap?.addPolyline(polylineOptions)

                    val bounds = LatLngBounds.Builder().include(start).include(end).build()
                    mMap?.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
                } else {
                    Toast.makeText(this@MapsActivity, "Error en la API de rutas: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<RouteResponse>, t: Throwable) {
                Toast.makeText(this@MapsActivity, "Error de red: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun enableMyLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap?.isMyLocationEnabled = true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
    }

    private fun getDeviceLocation(callback: (LatLng) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    callback(LatLng(location.latitude, location.longitude))
                } else {
                    Toast.makeText(this, "No se pudo obtener ubicación actual", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
        }
    }
}