package com.example.communitys.view.location

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.communitys.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class FullMapSheet : BottomSheetDialogFragment(), OnMapReadyCallback {

    companion object {
        private const val ARG_BARANGAY = "barangay"
        private const val ARG_USER_LAT = "user_lat"
        private const val ARG_USER_LNG = "user_lng"

        fun newInstance(barangay: String, userLat: Double?, userLng: Double?): FullMapSheet {
            return FullMapSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_BARANGAY, barangay)
                    if (userLat != null) putDouble(ARG_USER_LAT, userLat)
                    if (userLng != null) putDouble(ARG_USER_LNG, userLng)
                }
            }
        }
    }

    private val barangayHalls = mapOf(
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

    private var googleMap: GoogleMap? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            if (sheet != null) {
                val behavior = BottomSheetBehavior.from(sheet)
                val screenHeight = resources.displayMetrics.heightPixels
                behavior.peekHeight = screenHeight
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                sheet.layoutParams.height = screenHeight
            }
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.bottom_sheet_full_map, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFrag = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(R.id.fullMapContainer, mapFrag)
            .commit()
        mapFrag.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.mapType = GoogleMap.MAP_TYPE_NORMAL
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false

        map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(16.045016, 120.367793), 14f))

        val barangay = arguments?.getString(ARG_BARANGAY) ?: return
        val hallPos  = barangayHalls[barangay] ?: return

        val userLat = if (arguments?.containsKey(ARG_USER_LAT) == true) arguments?.getDouble(ARG_USER_LAT) else null
        val userLng = if (arguments?.containsKey(ARG_USER_LNG) == true) arguments?.getDouble(ARG_USER_LNG) else null
        val userPos = if (userLat != null && userLng != null) LatLng(userLat, userLng) else null

        map.addMarker(
            MarkerOptions()
                .position(hallPos)
                .title("$barangay Barangay Hall")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        if (userPos != null) {
            map.addMarker(
                MarkerOptions()
                    .position(userPos)
                    .title("Your Location")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            val bounds = LatLngBounds.Builder().include(hallPos).include(userPos).build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 160))
            fetchRoute(userPos, hallPos, barangay)
        } else {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(hallPos, 15f))
            showInfo(barangay, "Enable location to see directions", null)
        }
    }

    private fun fetchRoute(from: LatLng, to: LatLng, barangay: String) {
        showInfo(barangay, "Calculating route...", null)

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
                    val distKm = "%.1f km".format(route.getDouble("distance") / 1000.0)
                    val etaMin = "~${(route.getDouble("duration") / 60).toInt()} min"

                    showInfo(barangay, "to $barangay Barangay Hall  ·  $etaMin", distKm)

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
                            .width(10f)
                            .geodesic(true)
                    )
                } else {
                    showInfo(barangay, "Route unavailable", null)
                }
            } catch (e: Exception) {
                showInfo(barangay, "Route unavailable", null)
            }
        }
    }

    private fun showInfo(barangay: String, routeText: String, distKm: String?) {
        val v = view ?: return
        v.findViewById<View>(R.id.fullLayoutRouteInfo).visibility = View.VISIBLE
        v.findViewById<TextView>(R.id.tvFullBarangayName).text = "$barangay Barangay Hall"
        v.findViewById<TextView>(R.id.tvFullRouteInfo).text    = routeText
        v.findViewById<TextView>(R.id.tvFullDistance).text     = distKm ?: "--"
    }
}
