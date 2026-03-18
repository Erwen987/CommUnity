package com.example.communitys.view.reportissue

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.communitys.databinding.ActivityMapPickerBinding
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.OnlineTileSourceBase
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.MapTileIndex
import org.osmdroid.views.overlay.Marker

class MapPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapPickerBinding
    private var selectedMarker: Marker? = null
    private var selectedLat: Double? = null
    private var selectedLng: Double? = null

    // Default center: Dagupan City, Philippines
    private val dagupanCenter = GeoPoint(16.045016, 120.367793)

    // CartoDB Voyager tiles — free, no API key, no restrictions
    private val cartoTiles = object : OnlineTileSourceBase(
        "CartoDB", 0, 19, 256, ".png",
        arrayOf(
            "https://a.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://b.basemaps.cartocdn.com/rastertiles/voyager/",
            "https://c.basemaps.cartocdn.com/rastertiles/voyager/"
        )
    ) {
        override fun getTileURLString(pMapTileIndex: Long): String {
            return baseUrl +
                MapTileIndex.getZoom(pMapTileIndex) + "/" +
                MapTileIndex.getX(pMapTileIndex) + "/" +
                MapTileIndex.getY(pMapTileIndex) + mImageFilenameEnding
        }
    }

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

        // OSMDroid config — must be set before setContentView
        Configuration.getInstance().userAgentValue = packageName

        binding = ActivityMapPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        setupButtons()
    }

    private fun setupMap() {
        binding.mapView.apply {
            setTileSource(cartoTiles)
            setMultiTouchControls(true)
            controller.setZoom(17.0)
            controller.setCenter(dagupanCenter)
        }

        // Tap on map to drop a pin
        binding.mapView.overlays.add(object : org.osmdroid.views.overlay.Overlay() {
            override fun onSingleTapConfirmed(
                e: android.view.MotionEvent,
                mapView: org.osmdroid.views.MapView
            ): Boolean {
                val projection = mapView.projection
                val geoPoint = projection.fromPixels(e.x.toInt(), e.y.toInt()) as GeoPoint
                placeMarker(geoPoint)
                return true
            }
        })
    }

    private fun placeMarker(geoPoint: GeoPoint) {
        // Remove old marker
        selectedMarker?.let { binding.mapView.overlays.remove(it) }

        // Add new marker
        val marker = Marker(binding.mapView).apply {
            position = geoPoint
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            title = "Selected Location"
        }
        binding.mapView.overlays.add(marker)
        binding.mapView.invalidate()

        selectedMarker = marker
        selectedLat = geoPoint.latitude
        selectedLng = geoPoint.longitude

        // Update UI
        binding.tvCoords.text = "%.5f, %.5f".format(geoPoint.latitude, geoPoint.longitude)
        binding.tvCoords.setTextColor(ContextCompat.getColor(this, android.R.color.white))
        binding.btnConfirm.isEnabled = true
    }

    private fun setupButtons() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnMyLocation.setOnClickListener {
            val fineGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            if (fineGranted || coarseGranted) moveToMyLocation()
            else locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }

        binding.btnConfirm.setOnClickListener {
            if (selectedLat != null && selectedLng != null) {
                val result = Intent().apply {
                    putExtra("lat", selectedLat!!)
                    putExtra("lng", selectedLng!!)
                }
                setResult(RESULT_OK, result)
                finish()
            }
        }
    }

    private fun moveToMyLocation() {
        try {
            val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val isGpsEnabled     = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                Toast.makeText(this, "Please enable location/GPS", Toast.LENGTH_SHORT).show()
                return
            }

            binding.btnMyLocation.isEnabled = false
            binding.btnMyLocation.text = "Locating..."

            val locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    locationManager.removeUpdates(this)
                    binding.btnMyLocation.isEnabled = true
                    binding.btnMyLocation.text = "My Location"
                    val geoPoint = GeoPoint(location.latitude, location.longitude)
                    binding.mapView.controller.animateTo(geoPoint)
                    binding.mapView.controller.setZoom(18.0)
                    placeMarker(geoPoint)
                }
                @Deprecated("Deprecated in Java")
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            }

            // Try last known first for instant result
            val lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

            if (lastKnown != null) {
                binding.btnMyLocation.isEnabled = true
                binding.btnMyLocation.text = "My Location"
                val geoPoint = GeoPoint(lastKnown.latitude, lastKnown.longitude)
                binding.mapView.controller.animateTo(geoPoint)
                binding.mapView.controller.setZoom(18.0)
                placeMarker(geoPoint)
                return
            }

            // No cached location — request a fresh fix
            if (isGpsEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 0L, 0f, locationListener
                )
            }
            if (isNetworkEnabled) {
                locationManager.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 0L, 0f, locationListener
                )
            }

            // Timeout after 10 seconds
            binding.root.postDelayed({
                try { locationManager.removeUpdates(locationListener) } catch (_: Exception) {}
                if (binding.btnMyLocation.text == "Locating...") {
                    binding.btnMyLocation.isEnabled = true
                    binding.btnMyLocation.text = "My Location"
                    Toast.makeText(this, "Could not get location. Try again.", Toast.LENGTH_SHORT).show()
                }
            }, 10_000)

        } catch (e: SecurityException) {
            binding.btnMyLocation.isEnabled = true
            binding.btnMyLocation.text = "My Location"
            Toast.makeText(this, "Location permission required", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
}
