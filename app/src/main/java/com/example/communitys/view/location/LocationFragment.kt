package com.example.communitys.view.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.communitys.databinding.FragmentLocationBinding
import com.example.communitys.model.data.ReportModel
import com.example.communitys.view.documents.ReportDetailSheet
import com.example.communitys.viewmodel.LocationViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class LocationFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentLocationBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LocationViewModel by viewModels()
    private lateinit var adapter: LocationReportsAdapter

    private var googleMap: GoogleMap? = null
    private var userLatLng: LatLng? = null
    private var currentBarangay: String? = null
    private var allReports: List<ReportModel> = emptyList()

    companion object {
        private val BARANGAY_HALLS = mapOf(
            "Mangin"                    to LatLng(16.0453, 120.3672),
            "Bolosan"                   to LatLng(16.0441, 120.3378),
            "Calmay"                    to LatLng(16.0389, 120.3501),
            "Pantal"                    to LatLng(16.0312, 120.3398),
            "Lucao"                     to LatLng(16.0298, 120.3512),
            "Bonuan Binloc"             to LatLng(16.0201, 120.3489),
            "Bonuan Boquig"             to LatLng(16.0178, 120.3412),
            "Bonuan Gueset"             to LatLng(16.0223, 120.3601),
            "Malued"                    to LatLng(16.0501, 120.3298),
            "Mayombo"                   to LatLng(16.0478, 120.3512),
            "Perez"                     to LatLng(16.0432, 120.3445),
            "Bacayao Norte"             to LatLng(16.0389, 120.3223),
            "Bacayao Sur"               to LatLng(16.0356, 120.3201),
            "Caranglaan"                to LatLng(16.0512, 120.3601),
            "Carael"                    to LatLng(16.0534, 120.3512),
            "Herrero"                   to LatLng(16.0445, 120.3489),
            "Lasip Chico"               to LatLng(16.0567, 120.3423),
            "Lasip Grande"              to LatLng(16.0589, 120.3401),
            "Lomboy"                    to LatLng(16.0312, 120.3601),
            "Mamalingling"              to LatLng(16.0601, 120.3512),
            "Pugaro Suit"               to LatLng(16.0223, 120.3312),
            "Quezon"                    to LatLng(16.0434, 120.3398),
            "Salvador"                  to LatLng(16.0456, 120.3512),
            "Salapingao"                to LatLng(16.0378, 120.3623),
            "Sta. Barbara"              to LatLng(16.0512, 120.3489),
            "Sta. Maria"                to LatLng(16.0489, 120.3445),
            "Tebeng"                    to LatLng(16.0534, 120.3378),
            "Pogo Chico"                to LatLng(16.0267, 120.3534),
            "Pogo Grande"               to LatLng(16.0245, 120.3556),
            "Barangay I (Poblacion)"    to LatLng(16.0432, 120.3335),
            "Barangay II (Poblacion)"   to LatLng(16.0445, 120.3312),
            "Barangay III (Poblacion)"  to LatLng(16.0423, 120.3356),
            "Barangay IV (Poblacion)"   to LatLng(16.0412, 120.3378),
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        if (perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            fetchUserLocation()
        } else {
            showRouteInfo(null, "Location permission denied", null)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // RecyclerView
        adapter = LocationReportsAdapter(onViewDetails = { item ->
            ReportDetailSheet.newInstance(
                problem         = item.problem,
                description     = item.description,
                status          = item.status,
                date            = item.createdAt,
                imageUrl        = item.imageUrl,
                pointsAwarded   = item.pointsAwarded,
                locationLat     = item.locationLat,
                locationLng     = item.locationLng,
                rejectionReason = item.rejectionReason
            ).show(childFragmentManager, "report_detail")
        })
        binding.recyclerReports.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerReports.adapter = adapter

        // Map fragment (nested inside Fragment uses childFragmentManager)
        val mapFrag = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(com.example.communitys.R.id.mapContainer, mapFrag)
            .commit()
        mapFrag.getMapAsync(this)

        // My Location button
        binding.btnMyLocation.setOnClickListener { checkLocationPermission() }

        // Tap map to open full-screen view
        binding.mapClickOverlay.setOnClickListener { openFullMap() }

        // Observers
        viewModel.isLoading.observe(viewLifecycleOwner) { loading ->
            binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        }

        viewModel.reports.observe(viewLifecycleOwner) { list ->
            allReports = list
            adapter.updateList(list.take(3))
            binding.tvEmpty.visibility         = if (list.isEmpty()) View.VISIBLE else View.GONE
            binding.recyclerReports.visibility = if (list.isEmpty()) View.GONE   else View.VISIBLE
            binding.tvViewAllReports.visibility = if (list.size > 3) View.VISIBLE else View.GONE
        }

        binding.tvViewAllReports.setOnClickListener {
            AllReportsSheet.newInstance(allReports).show(childFragmentManager, "all_reports")
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            if (!err.isNullOrBlank()) Toast.makeText(requireContext(), err, Toast.LENGTH_SHORT).show()
        }

        viewModel.barangay.observe(viewLifecycleOwner) { barangay ->
            currentBarangay = barangay
            tryDrawMap()
        }
    }

    // ── Map ready ─────────────────────────────────────────────────────────────

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings.isZoomControlsEnabled = false
        map.uiSettings.isCompassEnabled = true

        // Default center: Dagupan City
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(16.045016, 120.367793), 14f))

        // Request location once map is ready
        checkLocationPermission()
    }

    // ── Location permission + fetch ───────────────────────────────────────────

    private fun checkLocationPermission() {
        val fine   = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            fetchUserLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun fetchUserLocation() {
        val fused = LocationServices.getFusedLocationProviderClient(requireContext())
        fused.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLatLng = LatLng(location.latitude, location.longitude)
                tryDrawMap()
            } else {
                // No cached location — still show barangay hall if available
                tryDrawMap()
            }
        }.addOnFailureListener {
            tryDrawMap()
        }
    }

    // ── Draw map when both barangay and (optionally) location are ready ────────

    private fun tryDrawMap() {
        val map      = googleMap ?: return
        val barangay = currentBarangay ?: return
        val hallPos  = BARANGAY_HALLS[barangay] ?: run {
            // Barangay not found in map — show hall at Dagupan center
            return
        }

        map.clear()

        // Barangay Hall marker (blue)
        map.addMarker(
            MarkerOptions()
                .position(hallPos)
                .title("$barangay Barangay Hall")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        val userPos = userLatLng
        if (userPos != null) {
            // User location marker (red)
            map.addMarker(
                MarkerOptions()
                    .position(userPos)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            // Fit both markers in view
            val bounds = LatLngBounds.Builder()
                .include(hallPos)
                .include(userPos)
                .build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 140))

            // Fetch and draw route
            fetchRoute(userPos, hallPos, barangay)
        } else {
            // Only show barangay hall
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(hallPos, 15f))
            showRouteInfo(barangay, "Enable location to see directions", null)
        }
    }

    // ── OSRM route fetch ──────────────────────────────────────────────────────

    private fun fetchRoute(from: LatLng, to: LatLng, barangay: String) {
        showRouteInfo(barangay, "Calculating route...", null)

        val url = "https://router.project-osrm.org/route/v1/driving/" +
                "${from.longitude},${from.latitude};${to.longitude},${to.latitude}" +
                "?overview=full&geometries=geojson"

        lifecycleScope.launch {
            try {
                val json = withContext(Dispatchers.IO) {
                    val conn = URL(url).openConnection()
                    conn.connectTimeout = 8000
                    conn.readTimeout    = 8000
                    conn.getInputStream().bufferedReader().readText()
                }

                val root = JSONObject(json)
                if (root.getString("code") == "Ok") {
                    val route  = root.getJSONArray("routes").getJSONObject(0)
                    val distM  = route.getDouble("distance")
                    val durS   = route.getDouble("duration")
                    val distKm = "%.1f km".format(distM / 1000.0)
                    val etaMin = "~${(durS / 60).toInt()} min"

                    showRouteInfo(barangay, "to $barangay Barangay Hall  ·  $etaMin", distKm)

                    // Build polyline from GeoJSON coordinates
                    val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
                    val points = ArrayList<LatLng>(coords.length())
                    for (i in 0 until coords.length()) {
                        val pair = coords.getJSONArray(i)
                        points.add(LatLng(pair.getDouble(1), pair.getDouble(0)))
                    }

                    googleMap?.addPolyline(
                        PolylineOptions()
                            .addAll(points)
                            .color(Color.parseColor("#1E3A5F"))
                            .width(9f)
                            .geodesic(true)
                    )
                } else {
                    showRouteInfo(barangay, "Route unavailable", null)
                }
            } catch (e: Exception) {
                showRouteInfo(barangay, "Route unavailable", null)
            }
        }
    }

    // ── Info strip helper ─────────────────────────────────────────────────────

    private fun showRouteInfo(barangay: String?, routeText: String, distKm: String?) {
        val b = _binding ?: return
        b.layoutRouteInfo.visibility = View.VISIBLE
        b.tvBarangayName.text = if (barangay != null) "$barangay Barangay Hall" else "Barangay Hall"
        b.tvRouteInfo.text    = routeText
        b.tvDistance.text     = distKm ?: "--"
    }

    // ── Full map sheet ────────────────────────────────────────────────────────

    private fun openFullMap() {
        val barangay = currentBarangay ?: return
        val userPos  = userLatLng
        FullMapSheet.newInstance(
            barangay  = barangay,
            userLat   = userPos?.latitude,
            userLng   = userPos?.longitude
        ).show(childFragmentManager, "full_map")
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        viewModel.loadReports()
        viewModel.loadBarangay()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
