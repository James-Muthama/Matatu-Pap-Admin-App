package com.example.matatupapadminapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
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
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL
import com.google.maps.android.PolyUtil
import kotlinx.coroutines.DelicateCoroutinesApi

class AddRoutePageActivity : AppCompatActivity(), OnMapReadyCallback {

    // Google Map instance for this activity
    private lateinit var mMap: GoogleMap

    // Current color for marker placement based on route stage
    private var markerColor: Float = BitmapDescriptorFactory.HUE_GREEN

    // UI components for selecting different stages of the route
    private lateinit var startRouteCard: CardView
    private lateinit var endRouteCard: CardView
    private lateinit var stageRouteCard: CardView

    // List to manage all markers placed on the map
    private var markers = mutableListOf<Marker>()

    // Stores the last clicked location on the map
    private var lastClickedLatLng: LatLng? = null

    // Markers for the start and end points of the route
    private var startMarker: LatLng? = null
    private var endMarker: LatLng? = null

    // List to manage stage markers (intermediate points in the route)
    private val stageMarkers = mutableListOf<LatLng>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display for better UI on devices with gesture navigation
        enableEdgeToEdge()
        // Set the layout for this activity
        setContentView(R.layout.add_route_page)

        // Initialize UI components
        startRouteCard = findViewById(R.id.start_route_card)
        endRouteCard = findViewById(R.id.end_route_card)
        stageRouteCard = findViewById(R.id.stage_card)

        // Navigation icons setup
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        // Set up click listeners for navigation and route planning
        receiptsIcon.setOnClickListener { startActivity(Intent(this, ReceiptsActivity::class.java)) }
        profileIcon.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        backIcon.setOnClickListener { finish() }
        homeIcon.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        // Set up click listeners for route stage selection
        startRouteCard.setOnClickListener { changeToStartRoute() }
        endRouteCard.setOnClickListener { changeToEndRoute() }
        stageRouteCard.setOnClickListener { changeToStageRoute() }

        // Button to initiate route planning
        val addRouteBtn = findViewById<View>(R.id.add_route_btn)
        addRouteBtn.setOnClickListener {
            // Clear only the polylines before planning a new route
            clearPolylines()
            planRoute()
        }

        // Asynchronously load the Google Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // Set initial state to start route if it's a new instance
        if (savedInstanceState == null) {
            changeToStartRoute()
        }
    }

    /**
     * Removes Route everytime to allow new route to be drawn each time the user clicks add route.
     */
    private fun clearPolylines() {
        polylines.forEach { it.remove() }
        polylines.clear()
    }

    /**
     * Switches to stage route mode where blue markers are used for intermediate stops.
     */
    private fun changeToStageRoute() {
        markerColor = BitmapDescriptorFactory.HUE_BLUE
        updateMapInstruction(R.string.instruction_route_stage)
        updateCardBackgrounds(stageRouteCard, endRouteCard, startRouteCard)
    }

    /**
     * Switches to start route mode where a green marker is used for the starting point.
     */
    private fun changeToStartRoute() {
        markerColor = BitmapDescriptorFactory.HUE_GREEN
        updateMapInstruction(R.string.instruction_route_start)
        updateCardBackgrounds(startRouteCard, endRouteCard, stageRouteCard)
    }

    /**
     * Switches to end route mode where a red marker is used for the endpoint.
     */
    private fun changeToEndRoute() {
        markerColor = BitmapDescriptorFactory.HUE_RED
        updateMapInstruction(R.string.instruction_route_end)
        updateCardBackgrounds(endRouteCard, startRouteCard, stageRouteCard)
    }

    /**
     * Updates the instruction text on the map to guide the user.
     * @param stringRes Resource ID of the string to display.
     */
    private fun updateMapInstruction(stringRes: Int) {
        findViewById<TextView>(R.id.map_instruction)?.text = getString(stringRes)
    }

    /**
     * Updates the background color of route stage cards to highlight the selected one.
     * @param selectedCard The card currently selected.
     * @param otherCard Other card to reset color.
     * @param otherCard2 Another card to reset color.
     */
    private fun updateCardBackgrounds(selectedCard: CardView, otherCard: CardView, otherCard2: CardView) {
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        otherCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.my_purple))
        otherCard2.setCardBackgroundColor(ContextCompat.getColor(this, R.color.my_purple))
    }


    override fun onMapReady(googleMap: GoogleMap) {
        // Assign the passed GoogleMap instance to our class property
        mMap = googleMap

        // Define a central point in Kenya to initially center the map
        val kenyaLatLng = LatLng(-1.286389, 36.817223)

        // Define the geographical bounds for Kenya to restrict map interactions within the country
        val kenyaBounds = LatLngBounds(
            LatLng(-4.67677, 33.909821), // Southwest corner of Kenya
            LatLng(4.62, 41.899578)       // Northeast corner of Kenya
        )

        // Move the camera to center on Kenya with a zoom level of 7
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kenyaLatLng, 7f))

        // Enable zoom controls for the map UI
        mMap.uiSettings.isZoomControlsEnabled = true

        // Restrict the camera movement to within Kenya's geographical bounds
        mMap.setLatLngBoundsForCameraTarget(kenyaBounds)

        // Check for location permissions
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // If permissions are not granted, request them
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Enable the user's current location on the map
        mMap.isMyLocationEnabled = true

        // Set up a listener for map clicks to add markers
        mMap.setOnMapClickListener { latLng ->
            // Check if the clicked location is within Kenya's bounds
            if (kenyaBounds.contains(latLng)) {
                lastClickedLatLng = latLng

                // Create marker options with the current color and position
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))

                // Depending on the current marker color, handle the marker placement
                when (markerColor) {
                    BitmapDescriptorFactory.HUE_GREEN -> {
                        // Remove any existing start marker and set the new one
                        removeMarkerOfType(markerColor)
                        startMarker = latLng
                    }
                    BitmapDescriptorFactory.HUE_RED -> {
                        // Remove any existing end marker and set the new one
                        removeMarkerOfType(markerColor)
                        endMarker = latLng
                    }
                    BitmapDescriptorFactory.HUE_BLUE -> {
                        // Add the clicked location as a stage in the route
                        stageMarkers.add(latLng)
                    }
                }

                // Add the marker to the map, set its tag for later identification, and add to our list
                val marker = mMap.addMarker(markerOptions)!!
                marker.tag = markerColor
                markers.add(marker)

                // Display the coordinates of the clicked location
                Toast.makeText(this, latLng.toString(), Toast.LENGTH_SHORT).show()

                // Animate the camera to focus on the placed marker
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                // Inform user to click within Kenya's borders
                Toast.makeText(this, "Please click within Kenya's borders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Retrieves the Google Maps API key from the application's metadata.
     * @return The API key string or null if not found or an error occurred.
     */
    private fun getGoogleMapsApiKey(): String? {
        return try {
            // Retrieve application info with meta-data
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val bundle = ai.metaData

            // Extract the API key from the meta-data
            bundle.getString("com.example.matatupapadminapp.DIRECTIONS_API_KEY")
        } catch (e: PackageManager.NameNotFoundException) {
            // Log error if the package name is not found
            Log.e("API_KEY", "Failed to load meta-data, NameNotFoundException", e)
            null
        }
    }

    /**
     * Plans the route based on the start, end, and stage markers placed on the map.
     */
    private fun planRoute() {
        when {
            // Check if start marker is set, if not, prompt user
            startMarker == null -> Toast.makeText(this, "Add route start", Toast.LENGTH_SHORT).show()

            // Check if end marker is set, if not, prompt user
            endMarker == null -> Toast.makeText(this, "Add route end", Toast.LENGTH_SHORT).show()

            // Check if at least one stage marker is set, if not, prompt user
            stageMarkers.size < 5 -> Toast.makeText(this, "You should have at least five stages", Toast.LENGTH_SHORT).show()

            else -> {
                // Retrieve the API key or return if not available
                val apiKey = getGoogleMapsApiKey() ?: return

                // Launch coroutine in the main thread for UI updates
                GlobalScope.launch(Dispatchers.Main) {
                    try {
                        // List to store all points in the route
                        val routePoints = mutableListOf<LatLng>()

                        // Find the nearest road to the start marker or use the marker if no road found
                        val startRoad = getNearestRoad(startMarker!!, apiKey) ?: startMarker!!
                        routePoints.add(startRoad)

                        // For each stage marker, find the nearest transit stop
                        for (stage in stageMarkers) {
                            val stageStop = getNearestTransitStop(stage, apiKey)
                            if (stageStop != null) {
                                routePoints.add(stageStop)
                            }
                        }

                        // Find the nearest road to the end marker or use the marker if no road found
                        val endRoad = getNearestRoad(endMarker!!, apiKey) ?: endMarker!!
                        routePoints.add(endRoad)

                        // Loop through adjacent points to fetch directions
                        for (i in 0 until routePoints.size - 1) {
                            val from = routePoints[i]
                            val to = routePoints[i + 1]

                            // Construct URL for Google Directions API
                            val directionsUrl = "https://maps.googleapis.com/maps/api/directions/json?" +
                                    "origin=${URLEncoder.encode("${from.latitude},${from.longitude}", "UTF-8")}&" +
                                    "destination=${URLEncoder.encode("${to.latitude},${to.longitude}", "UTF-8")}&" +
                                    "mode=driving&" +
                                    "key=$apiKey"

                            // Fetch the route points from Google Directions API
                            val routeResult = fetchDirections(directionsUrl)

                            if (routeResult != null) {
                                // Draw the route segment on the map, check if it's the last segment
                                drawRouteOnMap(routeResult)
                            } else {
                                // Show error message if fetching route segment fails
                                Toast.makeText(this@AddRoutePageActivity, "Failed to fetch route segment", Toast.LENGTH_SHORT).show()
                                break
                            }
                        }

                    } catch (e: Exception) {
                        // Log and show any exceptions during route planning
                        Log.e("RoutePlanning", "Route planning failed", e)
                        Toast.makeText(this@AddRoutePageActivity, "Route planning failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Fetches route directions from Google Maps API for given origin and destination.
     * @param url The URL for the Directions API request.
     * @return List of LatLng points representing the route, or null if an error occurs.
     */
    @OptIn(DelicateCoroutinesApi::class)
    private suspend fun fetchDirections(url: String): List<LatLng>? {
        return withContext(Dispatchers.IO) {
            try {
                // Fetch and parse the JSON response from the Directions API
                val result = URL(url).readText()
                JSONObject(result).let { jsonObject ->
                    if (jsonObject.getString("status") == "OK") {
                        val routes = jsonObject.getJSONArray("routes")
                        if (routes.length() > 0) {
                            val route = routes.getJSONObject(0)
                            val poly = route.getJSONObject("overview_polyline").getString("points")
                            // Decode the polyline to get actual geographic coordinates
                            PolyUtil.decode(poly)
                        } else {
                            null
                        }
                    } else {
                        // Log error if API returns an error status
                        Log.e("RoutePlanning", "API returned error: ${jsonObject.getString("status")}")
                        null
                    }
                }
            } catch (e: Exception) {
                // Log any exception during the API call
                Log.e("RoutePlanning", "Failed to fetch directions", e)
                null
            }
        }
    }

    // Declare a list to hold your polylines
    private val polylines = mutableListOf<Polyline>()

    /**
     * Draws a route segment on the map using the provided points.
     * @param routePoints List of points defining the route segment.
     */
    private fun drawRouteOnMap(routePoints: List<LatLng>) {
        // Create options for drawing the polyline
        val lineOptions = PolylineOptions()
        routePoints.forEach { lineOptions.add(it) }

        // Set the color of the route, default is blue
        lineOptions.color(Color.BLUE)

        // Add the polyline to the map and store it in the polylines list
        val polyline = mMap.addPolyline(lineOptions)
        polylines.add(polyline)
    }

    /**
     * Searches for the nearest transit stop to the given location.
     * @param latLng The location to search from.
     * @param apiKey API key for Google Places API.
     * @return LatLng of the nearest transit stop or null if not found or an error occurred.
     */
    private suspend fun getNearestTransitStop(latLng: LatLng, apiKey: String): LatLng? = withContext(Dispatchers.IO) {
        val placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${latLng.latitude},${latLng.longitude}&" +
                "radius=500&" + // 500 meters search radius
                "type=transit_station&" + // Looking specifically for transit stations
                "key=$apiKey"
        try {
            val response = URL(placesUrl).readText()
            val placesJson = JSONObject(response)
            val results = placesJson.getJSONArray("results")
            if (results.length() > 0) {
                // If results found, get the first (nearest) transit stop
                val nearestStop = results.getJSONObject(0)
                val location = nearestStop.getJSONObject("geometry").getJSONObject("location")
                LatLng(location.getDouble("lat"), location.getDouble("lng"))
            } else null // No transit stops found within the radius
        } catch (e: Exception) {
            Log.e("RoutePlanning", "Failed to find transit stop", e)
            null
        }
    }

    /**
     * Searches for the nearest road to the given location.
     * @param latLng The location to search from.
     * @param apiKey API key for Google Places API.
     * @return LatLng of the nearest road or null if not found or an error occurred.
     */
    private suspend fun getNearestRoad(latLng: LatLng, apiKey: String): LatLng? = withContext(Dispatchers.IO) {
        val placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${latLng.latitude},${latLng.longitude}&" +
                "radius=100&" + // 100 meters search radius
                "type=route&" + // Looking specifically for roads
                "key=$apiKey"
        try {
            val response = URL(placesUrl).readText()
            val placesJson = JSONObject(response)
            val results = placesJson.getJSONArray("results")
            if (results.length() > 0) {
                // If results found, get the first (nearest) road
                val nearestRoad = results.getJSONObject(0)
                val location = nearestRoad.getJSONObject("geometry").getJSONObject("location")
                LatLng(location.getDouble("lat"), location.getDouble("lng"))
            } else null // No roads found within the radius
        } catch (e: Exception) {
            Log.e("RoutePlanning", "Failed to find a road near Route End or Route Start", e)
            null
        }
    }

    /**
     * Handles the result of permission requests for location access.
     * @param requestCode The request code originally passed to requestPermissions().
     * @param permissions The requested permissions.
     * @param grantResults The grant results for the corresponding permissions which is either PERMISSION_GRANTED or PERMISSION_DENIED.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, enable location on the map
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        mMap.isMyLocationEnabled = true
                    }
                } else {
                    // Permission denied, show a message to the user
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Removes a marker from the map based on its color.
     * @param color The color of the marker to be removed.
     */
    private fun removeMarkerOfType(color: Float) {
        // Find the marker with the specified color
        val markerToRemove = markers.find { it.tag == color }
        markerToRemove?.let {
            // Remove the marker from the map and from our list
            it.remove()
            markers.remove(it)
        }
    }

    companion object {
        // Constant for identifying location permission request
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

