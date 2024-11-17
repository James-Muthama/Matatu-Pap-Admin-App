package com.example.matatupapadminapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.DelicateCoroutinesApi

class AddRoutePageActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap
    private var markerColor: Float = BitmapDescriptorFactory.HUE_GREEN
    private lateinit var startRouteCard: CardView
    private lateinit var endRouteCard: CardView
    private lateinit var stageRouteCard: CardView
    private var markers = mutableListOf<Marker>()
    private var lastClickedLatLng: LatLng? = null
    // Start and End markers for route planning
    private var startMarker: LatLng? = null
    private var endMarker: LatLng? = null
    // List to hold the stage markers
    private val stageMarkers = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_route_page)

        startRouteCard = findViewById(R.id.start_route_card)
        endRouteCard = findViewById(R.id.end_route_card)
        stageRouteCard = findViewById(R.id.stage_card)

        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        receiptsIcon.setOnClickListener { startActivity(Intent(this, ReceiptsActivity::class.java)) }
        profileIcon.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        backIcon.setOnClickListener { finish() }
        homeIcon.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        startRouteCard.setOnClickListener { changeToStartRoute() }
        endRouteCard.setOnClickListener { changeToEndRoute() }
        stageRouteCard.setOnClickListener { changeToStageRoute() }

        val addRouteBtn = findViewById<View>(R.id.add_route_btn)
        addRouteBtn.setOnClickListener { planRoute() }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        if (savedInstanceState == null) {
            changeToStartRoute()
        }
    }

    private fun changeToStageRoute() {
        markerColor = BitmapDescriptorFactory.HUE_BLUE
        updateMapInstruction(R.string.instruction_route_stage)
        updateCardBackgrounds(stageRouteCard, endRouteCard, startRouteCard)
    }

    private fun changeToStartRoute() {
        markerColor = BitmapDescriptorFactory.HUE_GREEN
        updateMapInstruction(R.string.instruction_route_start)
        updateCardBackgrounds(startRouteCard, endRouteCard, stageRouteCard)
    }

    private fun changeToEndRoute() {
        markerColor = BitmapDescriptorFactory.HUE_RED
        updateMapInstruction(R.string.instruction_route_end)
        updateCardBackgrounds(endRouteCard, startRouteCard, stageRouteCard)
    }

    private fun updateMapInstruction(stringRes: Int) {
        findViewById<TextView>(R.id.map_instruction)?.text = getString(stringRes)
    }

    private fun updateCardBackgrounds(selectedCard: CardView, otherCard: CardView, otherCard2: CardView) {
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        otherCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.my_purple))
        otherCard2.setCardBackgroundColor(ContextCompat.getColor(this, R.color.my_purple))
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val kenyaLatLng = LatLng(-1.286389, 36.817223)
        val kenyaBounds = LatLngBounds(
            LatLng(-4.67677, 33.909821),
            LatLng(4.62, 41.899578)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kenyaLatLng, 7f))

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setLatLngBoundsForCameraTarget(kenyaBounds)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        mMap.isMyLocationEnabled = true

        mMap.setOnMapClickListener { latLng ->
            if (kenyaBounds.contains(latLng)) {
                lastClickedLatLng = latLng
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

                when (markerColor) {
                    BitmapDescriptorFactory.HUE_GREEN -> {
                        removeMarkerOfType(markerColor)
                        startMarker = latLng
                    }
                    BitmapDescriptorFactory.HUE_RED -> {
                        removeMarkerOfType(markerColor)
                        endMarker = latLng
                    }
                    BitmapDescriptorFactory.HUE_BLUE -> {
                        stageMarkers.add(latLng)
                    }
                }

                val marker = mMap.addMarker(markerOptions)!!
                marker.tag = markerColor
                markers.add(marker)
                Toast.makeText(this, latLng.toString(), Toast.LENGTH_SHORT).show()
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "Please click within Kenya's borders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getGoogleMapsApiKey(): String? {
        return try {
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val bundle = ai.metaData
            bundle.getString("com.example.matatupapadminapp.DIRECTIONS_API_KEY")
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e("API_KEY", "Failed to load meta-data, NameNotFoundException", e)
            null
        }
    }

    // New planRoute function
    private fun planRoute() {
        when {
            startMarker == null -> Toast.makeText(this, "Add route start", Toast.LENGTH_SHORT).show()
            endMarker == null -> Toast.makeText(this, "Add route end", Toast.LENGTH_SHORT).show()
            stageMarkers.size < 1 -> Toast.makeText(this, "You should have at least one stage", Toast.LENGTH_SHORT).show()
            else -> {
                val apiKey = getGoogleMapsApiKey() ?: return
                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        val routePoints = mutableListOf<LatLng>()

                        val startRoad = getNearestRoad(startMarker!!, apiKey) ?: startMarker!!
                        routePoints.add(startRoad)

                        for (stage in stageMarkers) {
                            val stageStop = getNearestTransitStop(stage, apiKey)
                            if (stageStop != null) {
                                routePoints.add(stageStop)
                            }
                        }

                        val endRoad = getNearestRoad(endMarker!!, apiKey) ?: endMarker!!
                        routePoints.add(endRoad)

                        for (i in 0 until routePoints.size - 1) {
                            val from = routePoints[i]
                            val to = routePoints[i + 1]

                            val directionsUrl = "https://maps.googleapis.com/maps/api/directions/json?" +
                                    "origin=${URLEncoder.encode("${from.latitude},${from.longitude}", "UTF-8")}&" +
                                    "destination=${URLEncoder.encode("${to.latitude},${to.longitude}", "UTF-8")}&" +
                                    "mode=driving&" +
                                    "key=$apiKey"

                            val routeResult = fetchDirections(directionsUrl)

                            if (routeResult != null) {
                                drawRouteOnMap(routeResult, i == routePoints.size - 2)
                            } else {
                                Toast.makeText(this@AddRoutePageActivity, "Failed to fetch route segment", Toast.LENGTH_SHORT).show()
                                break
                            }
                        }

                        addMarkersToMap(routePoints)

                    } catch (e: Exception) {
                        Log.e("RoutePlanning", "Route planning failed", e)
                        Toast.makeText(this@AddRoutePageActivity, "Route planning failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun fetchDirections(url: String): List<LatLng>? {
        return withContext(Dispatchers.IO) {
            try {
                val result = URL(url).readText()
                JSONObject(result).let { jsonObject ->
                    if (jsonObject.getString("status") == "OK") {
                        val routes = jsonObject.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val poly = route.getJSONObject("overview_polyline").getString("points")
                            PolyUtil.decode(poly)
                        } else {
                            null
                        }
                    } else {
                        Log.e("RoutePlanning", "API returned error: ${jsonObject.getString("status")}")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("RoutePlanning", "Failed to fetch directions", e)
                null
            }
        }
    }

    private fun drawRouteOnMap(routePoints: List<LatLng>, isLastSegment: Boolean) {
        val lineOptions = PolylineOptions()
        routePoints.forEach { lineOptions.add(it) }
        lineOptions.color(Color.BLUE)
        if (isLastSegment) lineOptions.color(Color.GRAY)
        mMap.addPolyline(lineOptions)
    }

    private fun addMarkersToMap(routePoints: List<LatLng>) {
        routePoints.forEachIndexed { index, latLng ->
            val markerOptions = MarkerOptions().position(latLng)
            when (index) {
                0 -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                routePoints.size - 1 -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                else -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            }
            mMap.addMarker(markerOptions)
        }
    }

    private suspend fun getNearestTransitStop(latLng: LatLng, apiKey: String): LatLng? = withContext(Dispatchers.IO) {
        val placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${latLng.latitude},${latLng.longitude}&" +
                "radius=500&" +
                "type=transit_station&" +
                "key=$apiKey"
        try {
            val response = URL(placesUrl).readText()
            val placesJson = JSONObject(response)
            val results = placesJson.getJSONArray("results")
            if (results.length() > 0) {
                val nearestStop = results.getJSONObject(0)
                val location = nearestStop.getJSONObject("geometry").getJSONObject("location")
                LatLng(location.getDouble("lat"), location.getDouble("lng"))
            } else null
        } catch (e: Exception) {
            Log.e("RoutePlanning", "Failed to find transit stop", e)
            null
        }
    }

    private suspend fun getNearestRoad(latLng: LatLng, apiKey: String): LatLng? = withContext(Dispatchers.IO) {
        val placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${latLng.latitude},${latLng.longitude}&" +
                "radius=500&" +
                "type=route&" +
                "key=$apiKey"
        try {
            val response = URL(placesUrl).readText()
            val placesJson = JSONObject(response)
            val results = placesJson.getJSONArray("results")
            if (results.length() > 0) {
                val nearestRoad = results.getJSONObject(0)
                val location = nearestRoad.getJSONObject("geometry").getJSONObject("location")
                LatLng(location.getDouble("lat"), location.getDouble("lng"))
            } else null
        } catch (e: Exception) {
            Log.e("RoutePlanning", "Failed to find nearest road", e)
            null
        }
    }

    // Handle permission results
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        mMap.isMyLocationEnabled = true
                    }
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Helper function to remove markers by color
    private fun removeMarkerOfType(color: Float) {
        val markerToRemove = markers.find { it.tag == color }
        markerToRemove?.let {
            it.remove()
            markers.remove(it)
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}