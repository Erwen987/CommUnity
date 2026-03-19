package com.example.communitys.view.reportissue

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.communitys.databinding.ActivityMapPickerBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.communitys.R

class MapPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMapPickerBinding
    private lateinit var googleMap: GoogleMap

    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    // Default: Dagupan City, Philippines
    private val dagupanCenter = LatLng(16.045016, 120.367793)

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            moveToMyLocation()
        } else {
            Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        binding.btnBack.setOnClickListener { finish() }

        binding.btnMyLocation.setOnClickListener {
            val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
                moveToMyLocation()
            } else {
                locationPermissionLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                )
            }
        }

        binding.btnConfirm.setOnClickListener {
            if (selectedLat != null && selectedLng != null) {
                setResult(RESULT_OK, Intent().apply {
                    putExtra("lat", selectedLat!!)
                    putExtra("lng", selectedLng!!)
                })
                finish()
            }
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        googleMap.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
        }
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(dagupanCenter, 17f))

        // Tap to place marker
        googleMap.setOnMapClickListener { latLng ->
            placeMarker(latLng)
        }
    }

    private fun placeMarker(latLng: LatLng) {
        googleMap.clear()
        googleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title("Selected Location")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
        )
        selectedLat = latLng.latitude
        selectedLng = latLng.longitude
        binding.tvCoords.text = "%.5f, %.5f".format(latLng.latitude, latLng.longitude)
        binding.tvCoords.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        binding.btnConfirm.isEnabled = true
    }

    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(this)
        binding.btnMyLocation.isEnabled = false
        binding.btnMyLocation.text = "Locating..."

        fusedClient.lastLocation.addOnSuccessListener { location ->
            binding.btnMyLocation.isEnabled = true
            binding.btnMyLocation.text = "My Location"
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                placeMarker(latLng)
            } else {
                Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            binding.btnMyLocation.isEnabled = true
            binding.btnMyLocation.text = "My Location"
            Toast.makeText(this, "Location error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
