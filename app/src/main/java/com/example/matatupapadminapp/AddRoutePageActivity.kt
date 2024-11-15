package com.example.matatupapadminapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
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
        // Enable edge-to-edge display for immersive experience
        enableEdgeToEdge()
        // Set the layout for this activity
        setContentView(R.layout.add_route_page)

        // Initialize UI components
        startRouteCard = findViewById(R.id.start_route_card)
        endRouteCard = findViewById(R.id.end_route_card)
        stageRouteCard = findViewById(R.id.stage_card)

        // Navigation setup
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        // Set click listeners for navigation
        receiptsIcon.setOnClickListener { startActivity(Intent(this, ReceiptsActivity::class.java)) }
        profileIcon.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        backIcon.setOnClickListener { finish() }
        homeIcon.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        // Set click listeners for route selection
        startRouteCard.setOnClickListener { changeToStartRoute() }
        endRouteCard.setOnClickListener { changeToEndRoute() }
        stageRouteCard.setOnClickListener { changeToStageRoute() }

        // Button to plan route
        val addRouteBtn = findViewById<View>(R.id.add_route_btn)
        addRouteBtn.setOnClickListener {
            // This function will be called when the user clicks to plan a route
            planRoute()
        }

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // If it's a new instance of the activity, start in the start route selection mode
        if (savedInstanceState == null) {
            changeToStartRoute()
        }
    }

    // Functions to change UI for different marker types (start, end, stage)
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

    // Utility to update map instructions
    private fun updateMapInstruction(stringRes: Int) {
        findViewById<TextView>(R.id.map_instruction)?.text = getString(stringRes)
    }

    // Utility to update card backgrounds
    private fun updateCardBackgrounds(selectedCard: CardView, otherCard: CardView, otherCard2: CardView) {
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        otherCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.my_purple))
        otherCard2.setCardBackgroundColor(ContextCompat.getColor(this, R.color.my_purple))
    }

    // Called when Google Map is ready to be used
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set initial map view to Kenya
        val kenyaLatLng = LatLng(-1.286389, 36.817223)
        val kenyaBounds = LatLngBounds(
            LatLng(-4.67677, 33.909821),
            LatLng(4.62, 41.899578)
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kenyaLatLng, 7f))

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.setLatLngBoundsForCameraTarget(kenyaBounds)

        // Check for location permissions before enabling current location
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

        // Enable current location on the map
        mMap.isMyLocationEnabled = true

        // Set map click listener to add markers
        mMap.setOnMapClickListener { latLng ->
            if (kenyaBounds.contains(latLng)) {
                lastClickedLatLng = latLng
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

                val marker = mMap.addMarker(markerOptions)!!
                marker.tag = markerColor
                markers.add(marker)

                // Assign the clicked position to the appropriate marker type
                when (markerColor) {
                    BitmapDescriptorFactory.HUE_GREEN -> startMarker = latLng
                    BitmapDescriptorFactory.HUE_RED -> endMarker = latLng
                    BitmapDescriptorFactory.HUE_BLUE -> stageMarkers.add(latLng)
                }

                // Show coordinates of the clicked point
                Toast.makeText(this, latLng.toString(), Toast.LENGTH_SHORT).show()
                // Zoom in on the clicked location
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "Please click within Kenya's borders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Function to retrieve the Google Maps API key from the manifest
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

    // Plan the route based on the markers placed on the map
    private fun planRoute() {
        when {
            startMarker == null -> Toast.makeText(this, "Add route start", Toast.LENGTH_SHORT).show()
            endMarker == null -> Toast.makeText(this, "Add route end", Toast.LENGTH_SHORT).show()
            stageMarkers.size < 3 -> Toast.makeText(this, "You should have at least three stages", Toast.LENGTH_SHORT).show() // Reduced to 3 from 5
            else -> {
                startMarker?.let { start ->
                    endMarker?.let { end ->
                        // Clear previous markers and polylines if any
                        mMap.clear()

                        // Create waypoint string from stage markers
                        val apiKey = getGoogleMapsApiKey()

                        // URL Encoding for start, end, and waypoints
                        val encodedStart = URLEncoder.encode("${start.latitude},${start.longitude}", "UTF-8")
                        val encodedEnd = URLEncoder.encode("${end.latitude},${end.longitude}", "UTF-8")
                        val encodedWaypoints = stageMarkers.map { URLEncoder.encode("${it.latitude},${it.longitude}", "UTF-8") }.joinToString("|")

                        // Construct URL for Google Directions API with the dynamic API key
                        val url = "https://maps.googleapis.com/maps/api/directions/json?" +
                                "origin=$encodedStart&" +
                                "destination=$encodedEnd&" +
                                "waypoints=$encodedWaypoints&" +
                                "mode=transit&transit_mode=bus&" +
                                "key=$apiKey"

                        fetchDirection(url)

                        // Add start marker
                        val startMarkerOptions = MarkerOptions().position(start).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        mMap.addMarker(startMarkerOptions)

                        // Add end marker
                        val endMarkerOptions = MarkerOptions().position(end).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        mMap.addMarker(endMarkerOptions)

                        // Add stage markers
                        for (stage in stageMarkers) {
                            val stageMarkerOptions = MarkerOptions().position(stage).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                            mMap.addMarker(stageMarkerOptions)
                        }
                    }
                } ?: run {
                    // This will run if startMarker or endMarker is null
                    Toast.makeText(this, "Start or end marker not set", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Fetch route information from Google Directions API
    @OptIn(DelicateCoroutinesApi::class)
    private fun fetchDirection(url: String) {
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val result = URL(url).readText()
                withContext(Dispatchers.Main) {
                    handleDirectionResult(result)
                }
            } catch (e: Exception) {
                Log.e("RoutePlanning", "Error fetching route", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@AddRoutePageActivity, "Failed to fetch route", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Handle the route data returned by the API
    private fun handleDirectionResult(result: String) {
        try {
            val jsonObject = JSONObject(result)
            val status = jsonObject.getString("status")
            if (status == "OK") {
                val routes = jsonObject.getJSONArray("routes")
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val poly = route.getJSONObject("overview_polyline").getString("points")
                    val polyline = PolyUtil.decode(poly)
                    val lineOptions = PolylineOptions()
                    for (point in polyline) {
                        lineOptions.add(point)
                    }
                    // Add the route to the map
                    mMap.addPolyline(lineOptions)
                } else {
                    Toast.makeText(this, "No routes found", Toast.LENGTH_SHORT).show()
                }
            } else {
                val errorDetails = jsonObject.optString("error_message", "No error message provided")
                Log.e("RoutePlanning", "API Error: $status, Details: $errorDetails")
                Toast.makeText(this, "API Error: $status", Toast.LENGTH_LONG).show()
            }
        } catch (e: JSONException) {
            Log.e("RoutePlanning", "Error parsing route response", e)
            Toast.makeText(this, "Error parsing route data", Toast.LENGTH_SHORT).show()
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

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}