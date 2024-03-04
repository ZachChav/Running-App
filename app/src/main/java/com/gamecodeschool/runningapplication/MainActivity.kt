package com.gamecodeschool.runningapplication

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Granularity
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val requestCodeLocationPermission = 1
    private var trackingStarted = false
    private var totalSteps = 0
    private var totalDistance = 0.0
    private var totalCalories = 0
    private var lastLocation: Location? = null

    private var timeInterval: Long = 10000
    private var minimalDistance: Float = 0.0f

    private lateinit var stepsTextView: TextView
    private lateinit var distanceTextView: TextView
    private lateinit var caloriesTextView: TextView
    private lateinit var startTrackingButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        startTrackingButton = findViewById(R.id.startButton)
        startTrackingButton.setOnClickListener {
            if (!trackingStarted) {
                startTracking()
            } else {
                stopTracking()
            }
        }

        // Initialize TextViews
        stepsTextView = findViewById(R.id.stepsTextView)
        distanceTextView = findViewById(R.id.distanceTextView)
        caloriesTextView = findViewById(R.id.caloriesTextView)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        setUpMap()
    }

    @SuppressLint("MissingPermission")
    private fun setUpMap() {
        if (hasLocationPermission()) {
            googleMap.isMyLocationEnabled = true
            googleMap.uiSettings.isMyLocationButtonEnabled = true
            updateMap()
        } else {
            requestLocationPermission()
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION),
            requestCodeLocationPermission
        )
    }

    @SuppressLint("MissingPermission")
    private fun updateMap() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val currentLatLng = LatLng(location.latitude, location.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                if (trackingStarted) {
                    if (lastLocation != null) {
                        val distanceInMeters = lastLocation!!.distanceTo(location).toDouble()
                        val distanceInMiles = metersToMiles(distanceInMeters)
                        totalDistance += distanceInMiles

                        // Update TextViews
                        distanceTextView.text = "Distance: ${"%.2f".format(totalDistance)} miles"

                        // Calculate steps (assuming 1 step per yard)
                        val steps = metersToYards(distanceInMeters).toInt()
                        totalSteps += steps
                        stepsTextView.text = "Steps: $totalSteps"

                        // Calculate calories (assuming a certain calorie burned per step)
                        val caloriesPerStep = 0.05 // arbitrary value for demonstration
                        val caloriesBurned = (steps * caloriesPerStep).toInt()
                        totalCalories += caloriesBurned
                        caloriesTextView.text = "Calories: $totalCalories"
                    }

                    // Update last location
                    lastLocation = location
                }
            }
        }
    }

    private fun metersToMiles(meters: Double): Double {
        return meters * 0.000621371
    }

    private fun metersToYards(meters: Double): Double {
        return meters * 1.09361
    }


    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestCodeLocationPermission) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setUpMap()
            }
            else {
                Toast.makeText(this, "Location permission is denied, please allow the permission", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            p0 ?: return
            for (location in p0.locations) {
                updateMetrics(location)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, timeInterval).apply {
            setMinUpdateDistanceMeters(minimalDistance)
            setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL)
            setWaitForAccurateLocation(true)
        }.build()
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun updateMetrics(location: Location) {
        if (trackingStarted) {
            if (lastLocation != null) {
                val distanceInMeters = lastLocation!!.distanceTo(location).toDouble()
                val distanceInMiles = metersToMiles(distanceInMeters)
                totalDistance += distanceInMiles

                // Update TextViews
                distanceTextView.text = "Distance: ${"%.2f".format(totalDistance)} miles"

                // Calculate steps (assuming 1 step per yard)
                val steps = metersToYards(distanceInMeters).toInt()
                totalSteps += steps
                stepsTextView.text = "Steps: $totalSteps"

                // Calculate calories (assuming a certain calorie burned per step)
                val caloriesPerStep = 0.05 // arbitrary value for demonstration
                val caloriesBurned = (steps * caloriesPerStep).toInt()
                totalCalories += caloriesBurned
                caloriesTextView.text = "Calories: $totalCalories"
            }

            // Update last location
            lastLocation = location
        }
    }

    private fun startTracking() {
        trackingStarted = true
        startTrackingButton.text = "Stop Tracking"
        Toast.makeText(this, "Tracking started", Toast.LENGTH_SHORT).show()
        startLocationUpdates() // Start location updates
    }

    private fun stopTracking() {
        trackingStarted = false
        startTrackingButton.text = "Start Tracking"
        Toast.makeText(this, "Tracking stopped", Toast.LENGTH_SHORT).show()
        stopLocationUpdates() // Stop location updates
    }

}