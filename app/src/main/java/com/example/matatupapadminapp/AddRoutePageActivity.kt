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

/**
 *  This interface defines methods that the fragment will use to communicate with this activity.
 * It includes methods for planning a route and removing stage markers.
 */
interface RouteActions {
    fun planRoute(): Boolean  // Returns true if the route planning was successful, false otherwise.
    fun removeStageMarkers()  // Removes all stage markers from the map.
}

/**
 * This interface is used for listening to events related to removing stage markers.
 */
interface RemoveStageListener {
    fun removeStageMarkers()  // Method to be implemented to handle the removal of stage markers.
}

/**
 * The AddRoutePageActivity class manages the UI for adding a new route. It implements:
 * - AppCompatActivity for lifecycle management,
 * - OnMapReadyCallback for when the Google Map is ready,
 * - RouteActions for route-related actions,
 * - FragmentAddRoute.RemoveStageListener for listening to remove stage marker events.
 */
class AddRoutePageActivity : AppCompatActivity(), OnMapReadyCallback, RouteActions, FragmentAddRoute.RemoveStageListener  {

    /**
     * Holds the GoogleMap instance once it's initialized.
     */
    private lateinit var mMap: GoogleMap

    /**
     * The color used for new markers. Initially set to green for the starting point.
     */
    private var markerColor: Float = BitmapDescriptorFactory.HUE_GREEN

    /**
     * CardViews used for selecting different stages of route creation:
     * - startRouteCard for setting the start point,
     * - endRouteCard for setting the end point,
     * - stageRouteCard for adding intermediate stages.
     */
    private lateinit var startRouteCard: CardView
    private lateinit var endRouteCard: CardView
    private lateinit var stageRouteCard: CardView

    /**
     * A list to manage all markers placed on the map.
     */
    private var markers = mutableListOf<Marker>()

    /**
     * Stores the last location clicked on the map for potential reuse.
     */
    private var lastClickedLatLng: LatLng? = null

    /**
     * Holds the coordinates for the route's start and end points.
     */
    private var startMarker: LatLng? = null
    private var endMarker: LatLng? = null

    /**
     * List to hold all intermediate stage markers of the route.
     */
    private val stageMarkers = mutableListOf<LatLng>()

    /**
     * Flag to indicate whether the route planning process was successful.
     */
    var isRoutePlanningSuccessful = false

    /**
     * List to store all polylines drawn on the map representing routes.
     */
    private val polylines = mutableListOf<Polyline>()

    /**
     * List to store coordinates of nearby transit stops.
     * Also, a list to keep markers for these stops for easy removal.
     */
    val nearbyStops = mutableListOf<LatLng>()
    private val nearbyStopMarkers = mutableListOf<Marker>()

    /**
     * Job for managing coroutines, particularly useful when using GlobalScope for operations.
     */
    var globalScopeJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Edge-to-edge enables a modern UI design that extends to the edges of the screen.
        enableEdgeToEdge()
        // Set the layout for this activity from the XML layout file.
        setContentView(R.layout.add_route_page)

        // Initialize UI components from the layout.
        startRouteCard = findViewById(R.id.start_route_card)
        endRouteCard = findViewById(R.id.end_route_card)
        stageRouteCard = findViewById(R.id.stage_card)

        // Setup navigation icons for user interaction with different parts of the app.
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        // Set click listeners for navigation options.
        receiptsIcon.setOnClickListener { startActivity(Intent(this, ReceiptsActivity::class.java)) }
        profileIcon.setOnClickListener { startActivity(Intent(this, ProfileActivity::class.java)) }
        backIcon.setOnClickListener { finish() } // Close the current activity.
        homeIcon.setOnClickListener { startActivity(Intent(this, MainActivity::class.java)) }

        // Set click listeners for route creation stages.
        startRouteCard.setOnClickListener { changeToStartRoute() }
        endRouteCard.setOnClickListener { changeToEndRoute() }
        stageRouteCard.setOnClickListener { changeToStageRoute() }

        // Asynchronously initialize the Google Map.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // If the activity is being created for the first time, set up the initial state.
        if (savedInstanceState == null) {
            changeToStartRoute()
            // Add the FragmentAddRoute to display route creation options.
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_route, FragmentAddRoute())
                .commit()
        }
    }

    /**
     * Switches the application to a mode where users can add intermediate stops on the route.
     * - Changes the marker color to blue, indicating intermediate stages.
     * - Updates the map instructions to reflect this mode.
     * - Highlights the stage card by changing its background, de-emphasizes others.
     */
    private fun changeToStageRoute() {
        // Set the marker color to blue for intermediate stops
        markerColor = BitmapDescriptorFactory.HUE_BLUE
        // Update the instruction text shown to the user
        updateMapInstruction(R.string.instruction_route_stage)
        // Change card backgrounds to visually indicate the current mode
        updateCardBackgrounds(stageRouteCard, endRouteCard, startRouteCard)
    }

    /**
     * Switches the application to a mode for selecting the start of the route.
     * - Changes the marker color to green, signaling the start point.
     * - Updates instructions for starting route selection.
     * - Highlights the start route card, dims others.
     */
    private fun changeToStartRoute() {
        // Set marker color to green, indicating the route start
        markerColor = BitmapDescriptorFactory.HUE_GREEN
        // Update the user instruction text for starting route
        updateMapInstruction(R.string.instruction_route_start)
        // Change the visual state of UI cards to highlight this mode
        updateCardBackgrounds(startRouteCard, endRouteCard, stageRouteCard)
    }

    /**
     * Switches the application to a mode where the end point of the route can be set.
     * - Changes the marker color to red for the end point.
     * - Updates the instructions for ending the route.
     * - Visually distinguishes the end route card from others.
     */
    private fun changeToEndRoute() {
        // Set the marker color to red, marking the route's end
        markerColor = BitmapDescriptorFactory.HUE_RED
        // Update the instructional text for the end of route selection
        updateMapInstruction(R.string.instruction_route_end)
        // Highlight the end route card, reset others' backgrounds
        updateCardBackgrounds(endRouteCard, startRouteCard, stageRouteCard)
    }

    /**
     * Updates the text on the map which guides the user on what to do next.
     * @param stringRes An integer representing the resource ID of the string to be shown.
     * This function assumes there's a TextView with id 'map_instruction' in the layout.
     */
    private fun updateMapInstruction(stringRes: Int) {
        // Find the TextView for displaying instructions and set its text
        findViewById<TextView>(R.id.map_instruction)?.text = getString(stringRes)
    }

    /**
     * Modifies the background color of the CardViews to indicate which stage of route creation is active.
     * @param selectedCard The CardView representing the currently active stage.
     * @param otherCard A CardView that isn't currently active.
     * @param otherCard2 Another CardView that isn't currently active.
     * This function uses white for the selected card and a custom color (my_purple) for others.
     */
    private fun updateCardBackgrounds(selectedCard: CardView, otherCard: CardView, otherCard2: CardView) {
        // Set the background color of the selected card to white to highlight it
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        // Set the background color of other cards to my_purple to indicate they're not selected
        otherCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.my_purple))
        otherCard2.setCardBackgroundColor(ContextCompat.getColor(this, R.color.my_purple))
    }


    override fun onMapReady(googleMap: GoogleMap) {
        // Assign the GoogleMap object to our class property for later use
        mMap = googleMap

        // Set an initial center point; we're using coordinates for Kenya
        val kenyaLatLng = LatLng(-1.286389, 36.817223)

        // Define geographical bounds for Kenya to limit map interactions
        val kenyaBounds = LatLngBounds(
            LatLng(-4.67677, 33.909821), // Southwest corner
            LatLng(4.62, 41.899578)       // Northeast corner
        )

        // Initially center and zoom the map to show Kenya
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kenyaLatLng, 7f))

        // Enable zoom controls for better user interaction
        mMap.uiSettings.isZoomControlsEnabled = true

        // Restrict map movement to stay within Kenya's bounds
        mMap.setLatLngBoundsForCameraTarget(kenyaBounds)

        // Check for location permissions before proceeding
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions not granted, prompt the user for permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return  // Exits the function if permissions are not yet granted
        }

        // Enable the 'My Location' layer on the map if permissions are granted
        mMap.isMyLocationEnabled = true

        mMap.setOnMapClickListener { latLng ->
            if (kenyaBounds.contains(latLng)) {
                lastClickedLatLng = latLng
                lifecycleScope.launch(Dispatchers.Main) {
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

                    val marker = mMap.addMarker(markerOptions)
                    marker?.tag = markerColor
                    marker?.let { markers.add(it) }

                    Toast.makeText(this@AddRoutePageActivity, latLng.toString(), Toast.LENGTH_SHORT).show()
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            } else {
                Toast.makeText(this, "Please click within Kenya's borders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Retrieves the Google Maps API key from the application's metadata.
     * @return The API key string or null if it's not found or an error occurs.
     */
    private fun getGoogleMapsApiKey(): String? {
        return try {
            // Get the application info which includes metadata
            val ai = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
            val bundle = ai.metaData

            // Retrieve the API key from the meta-data using the specific key name
            bundle.getString("com.example.matatupapadminapp.DIRECTIONS_API_KEY")
        } catch (e: PackageManager.NameNotFoundException) {
            // If the package name is not found, log the error and return null
            Log.e("API_KEY", "Failed to load meta-data, NameNotFoundException", e)
            null
        }
    }

    /**
     * Plans the route based on the start, end, and stage markers placed on the map.
     * @return Boolean indicating if the route planning process was initiated successfully.
     */
    @OptIn(DelicateCoroutinesApi::class)
    override fun planRoute(): Boolean {
        if (startMarker == null || endMarker == null || !::mMap.isInitialized) {
            Toast.makeText(this, "Please set both start and end points or wait for map to initialize", Toast.LENGTH_SHORT).show()
            return false
        }

        clearNearbyStopsMarkers()
        clearPolylines()

        val apiKey = getGoogleMapsApiKey() ?: run {
            Toast.makeText(this, "API Key not found", Toast.LENGTH_SHORT).show()
            return false
        }

        globalScopeJob = lifecycleScope.launch(Dispatchers.Main) {
            try {
                val routePoints = mutableListOf<LatLng>().apply {
                    add(getNearestRoad(startMarker!!, apiKey) ?: startMarker!!)
                    addAll(stageMarkers)
                    add(getNearestRoad(endMarker!!, apiKey) ?: endMarker!!)
                }

                // Handle transit stops in background
                withContext(Dispatchers.IO) {
                    findTransitStopsAlongRoute(routePoints, apiKey)
                }

                val routeSegments = mutableListOf<List<LatLng>>()
                for (i in 0 until routePoints.size - 1) {
                    val segment = fetchDirections(constructDirectionsUrl(routePoints[i], routePoints[i + 1], apiKey))
                    if (segment == null) {
                        handleRoutePlanningError(NullPointerException("Failed to fetch directions for segment"))
                        return@launch
                    }
                    routeSegments.add(segment)
                }

                // Add polylines on the main thread
                routeSegments.forEach { drawRouteOnMap(it) }

                // Update UI on the main thread
                routePlanningCompleted(true)
                isRoutePlanningSuccessful = true
            } catch (e: Exception) {
                handleRoutePlanningError(e)
            }
        }
        return true  // Planning started
    }


    /**
     * Clears all transit stop markers from the map and resets the related lists.
     */
    private fun clearNearbyStopsMarkers() {
        // Iterate through each marker in the nearbyStopMarkers list and remove it from the map
        nearbyStopMarkers.forEach { it.remove() }
        // Clear the list that holds the markers to prevent memory leaks
        nearbyStopMarkers.clear()
        // Also clear the list of nearby stops to ensure we start with a fresh list
        nearbyStops.clear() // This clears the list where we store the coordinates of transit stops
    }

    /**
     * Determines if a given transit stop is close enough to be considered on the route.
     * @param stop The LatLng of the transit stop to check.
     * @param routePoints A list of LatLng points defining the route.
     * @return Boolean indicating if the stop is within the threshold distance from any segment of the route.
     */
    private fun isStopOnRoute(stop: LatLng, routePoints: List<LatLng>): Boolean {
        val thresholdDistance = 20.0 // meters, this is how close a stop must be to be considered on the route

        // Use windowed(2) to create pairs of consecutive points, representing route segments
        return routePoints.windowed(2).any { (start, end) ->
            val segment = listOf(start, end)
            // Check if the distance from the stop to the segment is less than or equal to the threshold
            getDistanceToPolyline(stop, segment) <= thresholdDistance
        }
    }

    /**
     * Searches for transit stops along the entire route defined by routePoints.
     * This function uses the Google Places API to find transit stations.
     * @param routePoints List of LatLng points representing the route.
     * @param apiKey API key for Google Places API access.
     */
    @DelicateCoroutinesApi
    private suspend fun findTransitStopsAlongRoute(routePoints: List<LatLng>, apiKey: String) {
        // Before finding new stops, ensure we clear any existing markers and data
        clearNearbyStopsMarkers()

        for (i in 0 until routePoints.size - 1) {
            val start = routePoints[i]
            val end = routePoints[i + 1]

            // For each segment of the route, get the transit stops within that area
            val segmentStops = getTransitStopsAlongSegment(listOf(start, end), apiKey)

            // Filter the stops to include only those that are actually on or very near the route segment
            segmentStops.forEach { stop ->
                if (isStopOnRoute(stop, listOf(start, end))) {
                    nearbyStops.add(stop) // Add valid stops to our list for later use or display
                }
            }
        }

        // Once all stops are collected, add markers for them on the map
        markNearbyStops()
    }

    /**
     * Searches for transit stops along a specific segment of the route using the Google Places API.
     * @param segmentPoints List containing exactly two LatLng points (start and end of segment).
     * @param apiKey API key for accessing Google Places API.
     * @return A list of LatLng representing the transit stops found along the segment.
     */
    private suspend fun getTransitStopsAlongSegment(segmentPoints: List<LatLng>, apiKey: String): List<LatLng> {
        // Early return if there aren't enough points to define a segment
        if (segmentPoints.size < 2) return emptyList()

        val radius = 1000 // meters, this value determines how far from the segment we'll look for stops
        val stops = mutableListOf<LatLng>()

        // Calculate the midpoint of the segment to use as a center for the API search
        val midpoint = LatLng(
            (segmentPoints[0].latitude + segmentPoints[1].latitude) / 2,
            (segmentPoints[0].longitude + segmentPoints[1].longitude) / 2
        )

        val placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${midpoint.latitude},${midpoint.longitude}&" +
                "radius=$radius&" +
                "type=transit_station&" +
                "key=$apiKey"

        try {
            withContext(Dispatchers.IO) { // Use IO dispatcher for network operations
                val response = URL(placesUrl).readText()
                val placesJson = JSONObject(response)
                val results = placesJson.getJSONArray("results")

                // Iterate over the results from Google Places API to find transit stations
                for (i in 0 until results.length()) {
                    val stop = results.getJSONObject(i)
                    val location = stop.getJSONObject("geometry").getJSONObject("location")
                    val latLng = LatLng(location.getDouble("lat"), location.getDouble("lng"))

                    // Ensure the stop is actually on or near the route before adding it
                    if (isStopOnRoute(latLng, segmentPoints)) {
                        stops.add(latLng)
                    }
                }
            }
        } catch (e: Exception) {
            // Log any errors encountered during the API call or JSON parsing
            Log.e("RoutePlanning", "Failed to get transit stops along segment", e)
        }

        return stops
    }

    /**
     * Constructs the URL for the Google Directions API to fetch walking or driving directions.
     * @param from The starting LatLng of the route segment.
     * @param to The ending LatLng of the route segment.
     * @param apiKey The API key for authenticating with the Google Maps Directions API.
     * @return A string containing the complete URL for the API call.
     */
    private fun constructDirectionsUrl(from: LatLng, to: LatLng, apiKey: String): String {
        return "https://maps.googleapis.com/maps/api/directions/json?" +
                "origin=${URLEncoder.encode("${from.latitude},${from.longitude}", "UTF-8")}&" +
                "destination=${URLEncoder.encode("${to.latitude},${to.longitude}", "UTF-8")}&" +
                "mode=driving&" +  // Driving mode is set here, could be changed to 'walking' or 'transit'
                "key=$apiKey"
    }

    /**
     * Calculates the distance from a given point to the closest point on a polyline (route segment).
     * @param point The point (LatLng) from which to measure the distance.
     * @param polyline List of LatLng points defining the segment of the route.
     * @return The minimum distance in meters from the point to the polyline.
     */
    private fun getDistanceToPolyline(point: LatLng, polyline: List<LatLng>): Double {
        var minDistance = Double.MAX_VALUE

        // Loop through consecutive pairs of points in the polyline to check distances
        for (i in 0 until polyline.size - 1) {
            val segmentStart = polyline[i]
            val segmentEnd = polyline[i + 1]

            // Check distance to start and end of the current segment
            val distanceStart = SphericalUtil.computeDistanceBetween(point, segmentStart)
            val distanceEnd = SphericalUtil.computeDistanceBetween(point, segmentEnd)

            // Update minimum distance if closer than current minimum
            if (distanceStart < minDistance) minDistance = distanceStart
            if (distanceEnd < minDistance) minDistance = distanceEnd

            // Vector calculations for determining the closest point on the segment
            val v = LatLng(segmentEnd.latitude - segmentStart.latitude, segmentEnd.longitude - segmentStart.longitude)
            val w = LatLng(point.latitude - segmentStart.latitude, point.longitude - segmentStart.longitude)

            val c1 = w.latitude * v.longitude - w.longitude * v.latitude
            if (c1 > 0) continue; // Point is not on the left side of the segment, so we continue

            if (c1 < 0) {
                // Now we check if the point lies between the start and end of the segment
                val dot = w.latitude * v.latitude + w.longitude * v.longitude
                if (dot < 0) continue; // Point is before the segment
                val len2 = v.latitude * v.latitude + v.longitude * v.longitude
                if (dot > len2) continue; // Point is beyond the segment

                // Project the point onto the line segment
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

    /**
     * Handles the completion of route planning, updating the UI on the main thread.
     * @param success A boolean indicating whether the route planning was successful.
     */
    private fun routePlanningCompleted(success: Boolean) {
        isRoutePlanningSuccessful = success
        println("Route planning completed with success: $success")

        if (success) {
            lifecycleScope.launch(Dispatchers.Main) {
                // Log the transit stops for debugging or informational purposes
                logNearbyStops()
                // Add markers for nearby transit stops to the map
                markNearbyStops()

                // Prepare data to pass to the next fragment
                val bundle = Bundle().apply {
                    startMarker?.let { putParcelable("routeStart", it) }
                    endMarker?.let { putParcelable("routeEnd", it) }
                    putParcelableArrayList("nearbyStops", ArrayList(nearbyStops))
                }

                // Create and configure the next fragment with route data
                val fragment = FragmentNameRoute().apply { arguments = bundle }

                // Navigate to the route naming fragment, adding the transaction to the back stack
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_route, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }


    /**
     * Adds markers for nearby transit stops to the map. This operation is performed on the main thread
     * to ensure smooth UI updates.
     */
    private fun markNearbyStops() {
        lifecycleScope.launch(Dispatchers.Main) {
            nearbyStops.forEach { stop ->
                // Create a marker for each transit stop with a yellow icon
                val marker = mMap.addMarker(MarkerOptions()
                    .position(stop)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW))
                    .title("Transit Stop"))

                // Tag the marker for identification and add it to the list for management
                marker?.tag = "nearby_stop"
                marker?.let { nearbyStopMarkers.add(it) }
            }
        }
    }


    /**
     * Logs the coordinates of all nearby transit stops to Logcat for debugging or informational purposes.
     */
    private fun logNearbyStops() {
        // Check if there are any stops to log
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
     * Manages error handling when route planning fails.
     * Logs the error, displays it to the user, and updates the UI accordingly.
     * @param exception The Exception object containing details of the error.
     */
    private fun handleRoutePlanningError(exception: Exception) {
        // Log the error for debugging
        Log.e("RoutePlanning", "Route planning failed", exception)
        // Inform the user about the failure with a toast message
        Toast.makeText(this, "Route planning failed: ${exception.message}", Toast.LENGTH_SHORT).show()
        // Update UI on the main thread to reflect the failed route planning
        runOnUiThread {
            routePlanningCompleted(false)
            isRoutePlanningSuccessful = false
        }
    }

    /**
     * Retrieves route directions from Google Maps Directions API.
     * @param url The fully constructed URL for the API request.
     * @return A list of LatLng points representing the route or null if there was an issue.
     */
    private suspend fun fetchDirections(url: String): List<LatLng>? {
        return withContext(Dispatchers.IO) {  // Switch to IO context for network operations
            try {
                // Send request to Google Directions API and read the response
                val result = URL(url).readText()
                val jsonObject = JSONObject(result)
                if (jsonObject.getString("status") == "OK") {
                    val routes = jsonObject.getJSONArray("routes")
                    if (routes.length() > 0) {
                        // Extract the polyline from the first route in the response
                        val route = routes.getJSONObject(0)
                        val poly = route.getJSONObject("overview_polyline").getString("points")
                        // Decode polyline to get actual LatLng coordinates
                        PolyUtil.decode(poly)
                    } else {
                        null  // No route found
                    }
                } else {
                    // Log API error status
                    Log.e("RoutePlanning", "API returned error: ${jsonObject.getString("status")}")
                    null
                }
            } catch (e: Exception) {
                // Log any exception that occurred during the API call or parsing
                Log.e("RoutePlanning", "Failed to fetch directions", e)
                null
            }
        }
    }


    /**
     * Draws the route on the map by adding a polyline that represents the path between route points.
     * This function ensures that the drawing operation is performed on the main thread to update the UI.
     * @param routePoints A list of LatLng objects representing the path of the route.
     */
    private fun drawRouteOnMap(routePoints: List<LatLng>) {
        lifecycleScope.launch(Dispatchers.Main) {
            // Configure the PolylineOptions with route points and set the line color to blue
            val lineOptions = PolylineOptions().apply {
                routePoints.forEach { add(it) }
                color(Color.BLUE)
            }
            // Add the polyline to the map and store it for later manipulation
            val polyline = mMap.addPolyline(lineOptions)
            polylines.add(polyline)
        }
    }

    /**
     * Clears all drawn routes from the map, preparing it for new route plotting.
     */
    private fun clearPolylines() {
        // Iterate through all polylines and remove them from the map
        polylines.forEach { it.remove() }
        // Clear the polylines list to ensure no stale references remain
        polylines.clear()
    }

    /**
     * Queries Google Maps Places API to find the nearest road to a specified point.
     * @param latLng The geographical point to start the search from.
     * @param apiKey Google Maps API key for authentication.
     * @return The LatLng of the nearest road, or null if no road is found or an error occurs.
     */
    private suspend fun getNearestRoad(latLng: LatLng, apiKey: String): LatLng? = withContext(Dispatchers.IO) {
        // Construct the API request URL with location parameters
        val placesUrl = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?" +
                "location=${latLng.latitude},${latLng.longitude}&" +
                "radius=50&" + // Search within a 50 meter radius
                "type=route&" + // Filter results to only roads
                "key=$apiKey"
        try {
            // Fetch the JSON response from the API
            val response = URL(placesUrl).readText()
            val placesJson = JSONObject(response)
            val results = placesJson.getJSONArray("results")

            // If there are results, use the first (nearest) road found
            if (results.length() > 0) {
                val nearestRoad = results.getJSONObject(0)
                val location = nearestRoad.getJSONObject("geometry").getJSONObject("location")
                LatLng(location.getDouble("lat"), location.getDouble("lng"))
            } else null // No results found within the specified radius
        } catch (e: Exception) {
            // Log any errors that occur during API call or JSON parsing
            Log.e("RoutePlanning", "Failed to find a road near Route End or Route Start", e)
            null
        }
    }

    /**
     * Manages the response from the Android permission system regarding location access.
     * @param requestCode Code used to identify which permission request this is a result for.
     * @param permissions Array of permissions we requested.
     * @param grantResults Array of grant results for the corresponding permissions.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                // Check if permission was granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Ensure we still have permission before enabling location features
                    if (ActivityCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        mMap.isMyLocationEnabled = true // Enable the "My Location" layer on the map
                    }
                } else {
                    // Notify the user about the permission denial
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Identifies and removes a marker from the map based on its color tag.
     * @param color The hue color of the marker to remove.
     */
    private fun removeMarkerOfType(color: Float) {
        // Look for a marker with the specified color tag
        val markerToRemove = markers.find { it.tag == color }
        markerToRemove?.let {
            // If found, remove it from the map and from our local list
            it.remove()
            markers.remove(it)
        }
    }


    /**
     * Removes the most recently added stage marker from the map and the list.
     */
    override fun removeStageMarkers() {
        if (stageMarkers.isNotEmpty()) {
            // Remove the last LatLng from stageMarkers
            val lastStage = stageMarkers.removeAt(stageMarkers.size - 1)

            // Find the corresponding marker on the map with the HUE_BLUE tag
            val markerToRemove = markers.findLast {
                it.position == lastStage && it.tag == BitmapDescriptorFactory.HUE_BLUE
            }
            markerToRemove?.let {
                // Remove the marker from the map
                it.remove()
                // Also remove it from the list of markers
                markers.remove(it)
            }
        }
        // If you want to ensure that 'stageMarkers' remains consistent with map state:
        // stageMarkers.clear()  // Uncomment this line if you want to clear all stage markers instead of just the last one
    }

    companion object {
        /**
         * This constant is used as a unique identifier for the location permission request.
         * It allows the activity to respond to the correct permission request.
         */
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}

