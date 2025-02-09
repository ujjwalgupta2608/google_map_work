package com.example.googlemapintegration.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.googlemapintegration.R
import com.example.googlemapintegration.model.DirectionsResponse
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import com.google.gson.Gson
import com.google.maps.android.PolyUtil
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private var routeType: String = "shortest"
    private lateinit var currentMarker: Marker
    private lateinit var polyline: Polyline
    private var destinationLatLng = LatLng(0.0, 0.0)
    private lateinit var currentLatLng: LatLng
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var createPolylineButton: Button
    private lateinit var currentLocation: ImageView
    private lateinit var nextButton: ImageView
    private var autoCompleteSupportFragment: AutocompleteSupportFragment? = null

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
//        NEED TO SAVE A MAP API KEY IN STRINGS FILE WITH  MAPS SDK, DISTANCE API AND PLACES API SHOULD BE ENABLED FROM PLAY CONSOLE, WITHOUT THIS MAP WILL NOT BE WORKING

        setContentView(R.layout.activity_main)
        initViews()

        // Request location permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        setUpClicks()
        // Check if GPS is enabled
        checkGpsEnabled()
    }

    private fun setUpClicks() {
        createPolylineButton.setOnClickListener {
            if (::currentLatLng.isInitialized && destinationLatLng!=LatLng(0.0, 0.0)) {
                googleMap.clear()
                fetchDirections()

            } else {
                if (!::currentLatLng.isInitialized) Toast.makeText(this, "Fetching current location", Toast.LENGTH_SHORT).show() else Toast.makeText(this, "Please select a destination location.", Toast.LENGTH_SHORT).show()
            }
//            createPolyline()
        }
        currentLocation.setOnClickListener {
            setCurrentLocation()
        }
        nextButton.setOnClickListener {
            startActivity(Intent(this, ShowPinPointsActivity::class.java))
        }
    }

    private fun initViews() {
        autoCompleteSupportFragment =
            fragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment?
        createPolylineButton = findViewById(R.id.createPolyline)
        currentLocation = findViewById(R.id.currentLocation)
        nextButton = findViewById(R.id.next_button)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        initPlaces()
        // Initialize the FusedLocationProviderClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    private fun setCurrentLocation() {
        if (::currentLatLng.isInitialized && ::currentMarker.isInitialized){
            getCurrentLocation()
        }else{
            Toast.makeText(this, "Fetching current location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initPlaces() {
        if (!Places.isInitialized()) {
            Places.initialize(this, getString(R.string.map_key))
        }
        autoCompleteSupportFragment =
            supportFragmentManager.findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment
        autoCompleteSupportFragment?.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_search_button)?.visibility =
            View.GONE

        (autoCompleteSupportFragment?.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_search_input) as EditText).textSize =
            13f
        (autoCompleteSupportFragment?.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_search_input) as EditText).setCompoundDrawablesWithIntrinsicBounds(
            R.drawable.search_icon,
            0,
            0,
            0
        )
        (autoCompleteSupportFragment?.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_search_input) as EditText).compoundDrawablePadding =
            20

        (autoCompleteSupportFragment?.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_search_input) as EditText).typeface =
            Typeface.DEFAULT

        (autoCompleteSupportFragment?.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_search_input) as EditText).setTextColor(
            ContextCompat.getColor(
                this, R.color.black
            )

        )
        autoCompleteSupportFragment?.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG
            )
        )
        autoCompleteSupportFragment?.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_clear_button)?.visibility =
            View.GONE
        autoCompleteSupportFragment?.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_clear_button)?.setOnClickListener {
            destinationLatLng = LatLng(0.0,0.0)
            autoCompleteSupportFragment?.setText("")
            autoCompleteSupportFragment?.view?.findViewById<View>(com.google.android.libraries.places.R.id.places_autocomplete_clear_button)?.visibility =
                View.GONE
//            googleMap.clear()
        }
        autoCompleteSupportFragment?.setOnPlaceSelectedListener(object : PlaceSelectionListener {

            override fun onPlaceSelected(place: Place) {
//                if (isAdded){
                    CoroutineScope(Dispatchers.IO).launch {
                        val geocoder = Geocoder(this@MainActivity)
                        var addressList: List<Address> = emptyList()
                        try {
                            addressList = geocoder.getFromLocationName(place.toString(), 2) ?: mutableListOf()
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        withContext(Dispatchers.Main) {
                            if (addressList.isNotEmpty()) {
                                val address: Address = addressList[0]
                                autoCompleteSupportFragment?.setText(address.getAddressLine(0))
                                destinationLatLng = LatLng(address.latitude, address.longitude)

                            }
                        }
                    }
//                }
            }

            override fun onError(status: Status) {
                Log.e("TAG", "onError: $status")
            }
        })
    }

    private fun createPolyline() {
        // It creates only a straight line between two points even without enabling distance API at google play console
        if (::currentLatLng.isInitialized && destinationLatLng!=LatLng(0.0, 0.0)) {
            googleMap.clear()
            // Create a polyline
            // Add a marker at the destination
            googleMap.addMarker(MarkerOptions().position(destinationLatLng).title("Destination"))
            googleMap.addMarker(MarkerOptions().position(currentLatLng).title("Destination"))
            polyline = googleMap.addPolyline(
                PolylineOptions()
                    .add(currentLatLng, destinationLatLng)
                    .width(5f)
                    .color(android.graphics.Color.RED)
            )

            // Create a LatLngBounds that includes both points
            val bounds = LatLngBounds.builder()
                .include(currentLatLng) // Add current location
                .include(destinationLatLng) // Add destination
                .build()

            // Animate the camera to show both points with padding
            val padding = 100 // Padding in pixels
            googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } else {
            if (!::currentLatLng.isInitialized) Toast.makeText(this, "Fetching current location", Toast.LENGTH_SHORT).show() else Toast.makeText(this, "Please select a destination location.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Enable zoom controls
        googleMap.uiSettings.isZoomControlsEnabled = true

        // Wait for the map to be fully loaded
        googleMap.setOnMapLoadedCallback {
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
                    // Stop further location updates
                    fusedLocationClient.removeLocationUpdates(this)

                    // Get the current location coordinates
                    Log.i("TAG", "getCurrentLocation: $location")
                    currentLatLng = LatLng(location.latitude, location.longitude)

                    // Move the camera to the current location
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Add a marker at the current location
                    if (::currentMarker.isInitialized) currentMarker.remove()
                    currentMarker = googleMap.addMarker(MarkerOptions().position(currentLatLng).title("You are here"))!!

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
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            Log.e("TAG", "Location permission not granted.")
        }
    }

    private fun checkGpsEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            // Prompt the user to enable GPS
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permission granted, get the location
                getCurrentLocation()
            } else {
                Log.e("TAG", "Location permission denied.")
            }
        }
    }
    /*private fun fetchDirections() {

        val apiKey = getString(R.string.map_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?origin=${currentLatLng.latitude},${currentLatLng.longitude}&destination=${destinationLatLng.latitude},${destinationLatLng.longitude}&key=$apiKey"

        thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                response.body()?.use { responseBody ->
                    val responseData = responseBody.string()
                    val directionsResponse = Gson().fromJson(responseData, DirectionsResponse::class.java)

                    runOnUiThread {
                        if (directionsResponse.routes.isNotEmpty()) {
                            val selectedRoute = when (routeType) {
                                "shortest" -> directionsResponse.routes.minByOrNull { it.legs[0].distance.value }
                                "fastest" -> directionsResponse.routes.minByOrNull { it.legs[0].duration.value }
                                "longest" -> directionsResponse.routes.maxByOrNull { it.legs[0].distance.value }
                                else -> directionsResponse.routes.firstOrNull()
                            }

                            selectedRoute?.let {
                                drawPolyline(it.overview_polyline.points)
                                Toast.makeText(this, "$routeType route selected", Toast.LENGTH_SHORT).show()
                            } ?: Toast.makeText(this, "No valid route found!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "No routes available!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to get directions!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }*/
    private fun fetchDirections() {
        val apiKey = getString(R.string.map_key)
        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${currentLatLng.latitude},${currentLatLng.longitude}" +
                "&destination=${destinationLatLng.latitude},${destinationLatLng.longitude}" +
                "&alternatives=true" +
                "&mode=driving" +
                "&avoid=tolls|highways|ferries" + // Force multiple routes
                "&departure_time=now" + // Get real-time fastest route
                "&key=${apiKey}"

        thread {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                response.body()?.use { responseBody ->
                    val responseData = responseBody.string()
                    val directionsResponse = Gson().fromJson(responseData, DirectionsResponse::class.java)

                    Log.d("DirectionsAPI", "Number of routes: ${directionsResponse.routes.size}")

                    runOnUiThread {
                        if (directionsResponse.routes.isNotEmpty()) {
                            val selectedRoute = when (routeType) {
                                "shortest" -> directionsResponse.routes.minByOrNull { it.legs[0].distance.value }
                                "fastest" -> directionsResponse.routes.minByOrNull { it.legs[0].duration.value }
                                "longest" -> directionsResponse.routes.maxByOrNull { it.legs[0].distance.value }
                                else -> directionsResponse.routes.firstOrNull()
                            }

                            selectedRoute?.let {
                                drawPolyline(it.overview_polyline.points)
                                Toast.makeText(this, "$routeType route selected", Toast.LENGTH_SHORT).show()
                            } ?: Toast.makeText(this, "No valid route found!", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this, "No routes available!", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Failed to get directions!", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    private fun drawPolyline(encodedPath: String) {
        val polylineOptions = PolylineOptions()
            .addAll(PolyUtil.decode(encodedPath))
            .color(Color.BLUE)
            .width(5f)

        googleMap.addPolyline(polylineOptions)
        currentMarker.remove()
        currentMarker = googleMap.addMarker(MarkerOptions().position(currentLatLng).title("Origin"))!!
        googleMap.addMarker(MarkerOptions().position(destinationLatLng).title("Destination"))

        val bounds = LatLngBounds.builder()
            .include(currentLatLng) // Add current location
            .include(destinationLatLng) // Add destination
            .build()

        // Animate the camera to show both points with padding
        val padding = 100 // Padding in pixels
        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
    }
}