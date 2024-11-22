package com.example.matatupapadminapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
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
import com.google.maps.android.SphericalUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Job

// This interface defines methods that the fragment will use to communicate with this activity.
interface RouteActions {
    fun planRoute(): Boolean
    fun removeStageMarkers()
}

interface RemoveStageListener {
    fun removeStageMarkers()
}

// The AddRoutePageActivity is an AppCompatActivity that also implements Google Map callbacks and RouteActions for interaction with a map fragment.
class AddRoutePageActivity : AppCompatActivity(), OnMapReadyCallback, RouteActions, FragmentAddRoute.RemoveStageListener  {

    // This will store the GoogleMap instance once it's loaded.
    private lateinit var mMap: GoogleMap

    // Color for new markers, changes based on what part of the route we're adding.
    private var markerColor: Float = BitmapDescriptorFactory.HUE_GREEN

    // CardViews used as buttons for selecting different stages of route creation.
    private lateinit var startRouteCard: CardView
    private lateinit var endRouteCard: CardView
    private lateinit var stageRouteCard: CardView

    // List to keep track of all markers on the map for management or removal.
    private var markers = mutableListOf<Marker>()

    // Keeps the last clicked location for potential use in marker placement or route planning.
    private var lastClickedLatLng: LatLng? = null

    // LatLng objects to store the start and end points of the route.
    private var startMarker: LatLng? = null
    private var endMarker: LatLng? = null

    // Mutable list to store intermediate stage markers for the route.
    private val stageMarkers = mutableListOf<LatLng>()

    // Add this property to keep track of route planning success
    var isRoutePlanningSuccessful = false

    // Declare a list to hold your polylines
    private val polylines = mutableListOf<Polyline>()

    // List to store nearby transit stops
    val nearbyStops = mutableListOf<LatLng>()

    // If you're using GlobalScope, consider using a Job to manage the coroutine lifecycle
    var globalScopeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge enables a modern UI design that extends to the edges of the screen.
        enableEdgeToEdge()
        // This sets the layout for this activity from XML.
        setContentView(R.layout.add_route_page)

        // Initialize UI components by finding them in the layout.
        startRouteCard = findViewById(R.id.start_route_card)
        endRouteCard = findViewById(R.id.end_route_card)
        stageRouteCard = findViewById(R.id.stage_card)

        // Setup navigation icons which are likely UI elements for navigation between activities.
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        // Set click listeners for navigation. These will start new activities or close the current one.
        receiptsIcon.setOnClickListener { startActivity(Intent(this, ReceiptsActivity::class.java)) }
        profileIcon.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        backIcon.setOnClickListener { finish() } // Closes this activity
        homeIcon.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        // Set click listeners for selecting different parts of the route (start, end, stages).
        startRouteCard.setOnClickListener { changeToStartRoute() }
        endRouteCard.setOnClickListener { changeToEndRoute() }
        stageRouteCard.setOnClickListener { changeToStageRoute() }

        // Asynchronously load the Google Map. This callback will be called when the map is ready.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // If this is a new instance of the activity, set the initial state to start route selection.
        if (savedInstanceState == null) {
            changeToStartRoute()
            // Add the FragmentAddRoute to the activity's layout.
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_route, FragmentAddRoute())
                .commit()
        }
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
    override fun planRoute(): Boolean {
        if (startMarker == null || endMarker == null || !::mMap.isInitialized) {
            Toast.makeText(this, "Please set both start and end points or wait for map to initialize", Toast.LENGTH_SHORT).show()
            return false
        }

        clearPolylines()
        val apiKey = getGoogleMapsApiKey() ?: run {
            Toast.makeText(this, "API Key not found", Toast.LENGTH_SHORT).show()
            return false
        }

        globalScopeJob = lifecycleScope.launch(Dispatchers.Main) {
            try {
                val routePoints = mutableListOf<LatLng>()
                routePoints.add(getNearestRoad(startMarker!!, apiKey) ?: startMarker!!)
                stageMarkers.forEach { stageMarker ->
                    val stageStop = getNearestTransitStop(stageMarker, apiKey)
                    if (stageStop != null) {
                        routePoints.add(stageStop)
                    }
                }
                routePoints.add(getNearestRoad(endMarker!!, apiKey) ?: endMarker!!)

                val routeSegments = mutableListOf<List<LatLng>>()
                for (i in 0 until routePoints.size - 1) {
                    val segment = fetchDirections(constructDirectionsUrl(routePoints[i], routePoints[i + 1], apiKey))
                    if (segment == null) {
                        handleRoutePlanningError(NullPointerException("Failed to fetch directions for segment"))
                        return@launch
                    }
                    routeSegments.add(segment)
                }

                findTransitStopsOnRoute(routeSegments, apiKey)
                routeSegments.forEach { drawRouteOnMap(it) }

                val result = true
                (this@AddRoutePageActivity).runOnUiThread {
                    routePlanningCompleted(result)
                    isRoutePlanningSuccessful = result
                }
            } catch (e: Exception) {
                handleRoutePlanningError(e)
            }
        }
        return true
    }

    /**
     * Constructs the URL for the Google Directions API.
     * @param from The starting LatLng of the segment.
     * @param to The ending LatLng of the segment.
     * @param apiKey The API key for authentication.
     * @return String URL for the Directions API.
     */
    private fun constructDirectionsUrl(from: LatLng, to: LatLng, apiKey: String): String {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${URLEncoder.encode("${from.latitude},${from.longitude}", "UTF-8")}&" +
                "destination=${URLEncoder.encode("${to.latitude},${to.longitude}", "UTF-8")}&" +
                "mode=driving&" +
                "key=$apiKey"
    }

    /**
     * Searches for transit stops that lie on the route segments.
     * @param segments List of route segments where each segment is a list of LatLng points.
     * @param apiKey The Google Maps API key.
     */
    private suspend fun findTransitStopsOnRoute(segments: List<List<LatLng>>, apiKey: String) {
        segments.forEach { segment ->
            val stops = getNearbyTransitStops(segment, apiKey)
            nearbyStops.addAll(stops)
        }
    }

    /**
     * Finds transit stops within 10 meters of the route segment.
     * @param segment List of points representing a route segment.
     * @param apiKey The Google Maps API key.
     * @return List of LatLng points representing transit stops near the segment.
     */
    private suspend fun getNearbyTransitStops(segment: List<LatLng>, apiKey: String): List<LatLng> {
        return withContext(Dispatchers.IO) {
            segment.filter { point ->
                getNearestTransitStop(point, apiKey)?.let { stop ->
                    getDistanceToPolyline(stop, segment) <= 10.0  // 10 meters threshold
                } ?: false
            }
        }
    }

    /**
     * Calculates the distance from a point to the closest point on the polyline.
     * @param point The point to check (LatLng).
     * @param polyline List of points representing the route segment.
     * @return Distance in meters.
     */
    /**
     * Calculates the distance from a point to the closest point on the polyline.
     * @param point The point to check (LatLng).
     * @param polyline List of points representing the route segment.
     * @return Distance in meters.
     */
    private fun getDistanceToPolyline(point: LatLng, polyline: List<LatLng>): Double {
        var minDistance = Double.MAX_VALUE
        for (i in 0 until polyline.size - 1) {
            val segmentStart = polyline[i]
            val segmentEnd = polyline[i + 1]

            // Distance from point to segmentStart
            val distanceStart = SphericalUtil.computeDistanceBetween(point, segmentStart)

            // Distance from point to segmentEnd
            val distanceEnd = SphericalUtil.computeDistanceBetween(point, segmentEnd)

            // If the point is close to the start or end of the segment, use this distance
            if (distanceStart < minDistance) minDistance = distanceStart
            if (distanceEnd < minDistance) minDistance = distanceEnd

            // Now check if the point is closest to a point on the line segment itself
            val v = LatLng(segmentEnd.latitude - segmentStart.latitude, segmentEnd.longitude - segmentStart.longitude)
            val w = LatLng(point.latitude - segmentStart.latitude, point.longitude - segmentStart.longitude)

            val c1 = w.latitude * v.longitude - w.longitude * v.latitude
            if (c1 > 0) continue;  // The point is not on the left side of the segment

            if (c1 < 0) {
                // Point is on the left side, but check if it's within the segment limits
                val dot = w.latitude * v.latitude + w.longitude * v.longitude
                if (dot < 0) continue;  // Point is before the segment

                // Point is beyond the end of the segment
                val len2 = v.latitude * v.latitude + v.longitude * v.longitude
                if (dot > len2) continue;

                // Project the point onto the line
                val proj = dot / len2
                val linePoint = LatLng(
                    segmentStart.latitude + proj * v.latitude,
                    segmentStart.longitude + proj * v.longitude
                )
                val distanceToLine = SphericalUtil.computeDistanceBetween(point, linePoint)
                if (distanceToLine < minDistance) {
                    minDistance = distanceToLine
                }
            }
        }
        return minDistance
    }

    // This function will be called on the UI thread with the result of route planning
    private fun routePlanningCompleted(success: Boolean) {
        // Here you can handle what to do once the route planning is finished
        // For example, update UI or store success status in a variable
        // If you want to use this result elsewhere, you might want to use a shared variable or LiveData
        isRoutePlanningSuccessful = success
        println("Route planning completed with success: $success")

        if (success) {
            // Log nearby stops
            logNearbyStops()

            // Add markers for nearby transit stops
            markNearbyStops()

            // Navigate to FragmentNameRoute if route planning was successful
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_route, FragmentNameRoute())
                .addToBackStack(null)
                .commit()
        }
    }

    /**
     * Marks nearby transit stops on the map with yellow pins.
     */
    private fun markNearbyStops() {
        nearbyStops.forEach { stop ->
            mMap.addMarker(MarkerOptions()
                .position(stop)
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                .title("Transit Stop"))
        }
    }

    /**
     * Logs the coordinates of all nearby transit stops to Logcat.
     */
    private fun logNearbyStops() {
        if (nearbyStops.isNotEmpty()) {
            Log.d("RoutePlanning", "Nearby Transit Stops:")
            nearbyStops.forEachIndexed { index, stop ->
                Log.d("RoutePlanning", "Stop ${index + 1}: Latitude ${stop.latitude}, Longitude ${stop.longitude}")
            }
        } else {
            Log.d("RoutePlanning", "No transit stops were found near the route.")
        }
    }

    /**
     * Handles errors that occur during route planning by logging them and updating the UI.
     * @param exception The Exception that occurred during route planning.
     */
    private fun handleRoutePlanningError(exception: Exception) {
        Log.e("RoutePlanning", "Route planning failed", exception)
        Toast.makeText(this, "Route planning failed: ${exception.message}", Toast.LENGTH_SHORT).show()
        runOnUiThread {
            routePlanningCompleted(false)
            isRoutePlanningSuccessful = false
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
     * Removes Route everytime to allow new route to be drawn each time the user clicks add route.
     */
    private fun clearPolylines() {
        polylines.forEach { it.remove() }
        polylines.clear()
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

    override fun removeStageMarkers() {
        // Remove all markers of type HUE_BLUE
        removeMarkerOfType(BitmapDescriptorFactory.HUE_BLUE)
        // Clear the stage markers list if necessary
        stageMarkers.clear()
    }

    companion object {
        // Constant for identifying location permission request
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }


}

