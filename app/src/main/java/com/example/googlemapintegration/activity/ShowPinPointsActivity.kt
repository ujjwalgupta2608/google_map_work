package com.example.googlemapintegration.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.googlemapintegration.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.widget.AutocompleteSupportFragment

class ShowPinPointsActivity : AppCompatActivity(), OnMapReadyCallback {
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var currentLatLng: LatLng
    private var currentMarker: Marker? = null
    private lateinit var nextButton: ImageView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_show_pin_points)
        nextButton = findViewById(R.id.next_button)
        clickListener()
        initViews()
    }
    private fun clickListener() {
        nextButton.setOnClickListener {
            startActivity(Intent(this, LiveTrackingActivity::class.java))
        }
    }
    private fun initViews() {
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    @SuppressLint("PotentialBehaviorOverride")
    private fun drawRadiusCircle(center: LatLng, radius: Double) {
        // Draw a transparent circle of 5 km radius
        googleMap?.addCircle(
            CircleOptions()
                .center(center)
                .radius(radius) // 5 km in meters
                .strokeColor(Color.BLUE)
                .strokeWidth(3f)
                .fillColor(0x5500A5FF) // Transparent Blue (0x55 for 33% transparency)
        )

        // Replace with actual predefined points
        val greenPoints = listOf(
            LatLng(28.6354, 77.3951),
            LatLng(28.6238, 77.4102),
            LatLng(28.6117, 77.3794),
            LatLng(28.6301, 77.3856),
            LatLng(28.6205, 77.4023)
        )

        val redPoints = listOf(
            LatLng(28.6082, 77.3936),
            LatLng(28.6171, 77.3754),
            LatLng(28.6290, 77.4008),
            LatLng(28.6156, 77.4129),
            LatLng(28.6224, 77.3801)
        )


        // Add green markers
        for (point in greenPoints) {
            googleMap?.addMarker(
                MarkerOptions()
                    .position(point)
                    .title("Green Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
        }

        // Add red markers
        for (point in redPoints) {
            googleMap?.addMarker(
                MarkerOptions()
                    .position(point)
                    .title("Red Point")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }
        // Set a click listener for markers
        googleMap?.setOnMarkerClickListener { marker ->
            val latLng = marker.position
            Toast.makeText(
                this,
                "Clicked: ${latLng.latitude}, ${latLng.longitude}",
                Toast.LENGTH_SHORT
            ).show()
            false // Return false to allow default behavior (e.g., camera centering)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable zoom controls
        googleMap?.uiSettings?.isZoomControlsEnabled = true

        // Wait for the map to be fully loaded
        googleMap?.setOnMapLoadedCallback {
            // Get the current location
            getCurrentLocation()
        }
    }

    private fun getCurrentLocation() {
        val locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // 10 seconds
            fastestInterval = 5000 // 5 seconds
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation
                if (location != null) {
                    fusedLocationClient.removeLocationUpdates(this)
//                    val currentLatLng = LatLng(28.6189632, 77.390971) // Temporarily set for New Delhi

                    Log.i("TAG", "getCurrentLocation: ${location.latitude}  ${location.longitude}")
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 13f))

                    if (currentMarker!=null) currentMarker?.remove()
                    currentMarker = googleMap?.addMarker(
                        MarkerOptions().position(currentLatLng).title("You are here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                    )

                    drawRadiusCircle(currentLatLng, 5000.0) // 5 km radius
                    // Define a destination (replace with your desired destination)
                } else {
                    Log.e("TAG", "Location is still null. Ensure GPS is enabled and try again.")
                }
            }
        }

        // Request location updates
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper()
            )
        } else {
            Log.e("TAG", "Location permission not granted.")
        }
    }

}