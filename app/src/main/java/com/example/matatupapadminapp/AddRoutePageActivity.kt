package com.example.matatupapadminapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class AddRoutePageActivity : AppCompatActivity(), OnMapReadyCallback {
    // GoogleMap instance for map operations
    private lateinit var mMap: GoogleMap
    // Marker color, green is for start route, red for end route
    private var markerColor: Float = BitmapDescriptorFactory.HUE_GREEN
    // CardViews for selecting start and end of the route
    private lateinit var startRouteCard: CardView
    private lateinit var endRouteCard: CardView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display (Android 12+)
        enableEdgeToEdge()
        // Set the layout for this activity
        setContentView(R.layout.add_route_page)

        // Initialize UI components
        startRouteCard = findViewById(R.id.start_route_card)
        endRouteCard = findViewById(R.id.end_route_card)

        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        val mapInstruction = findViewById<TextView>(R.id.map_instruction)

        // Set up click listeners for navigation icons
        receiptsIcon.setOnClickListener {
            // Start Receipts activity
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener {
            // Start Profile activity
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        backIcon.setOnClickListener {
            // Close the current activity
            finish()
        }

        homeIcon.setOnClickListener {
            // Navigate to the main activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Set click listeners for route selection
        startRouteCard.setOnClickListener {
            changeToStartRoute()
        }

        endRouteCard.setOnClickListener {
            changeToEndRoute()
        }

        // Initialize the Google Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        // If this is the first time the activity is created, set up the start route by default
        if (savedInstanceState == null) {
            changeToStartRoute()
        }
    }

    // Switch to start route mode
    private fun changeToStartRoute() {
        markerColor = BitmapDescriptorFactory.HUE_GREEN
        updateMapInstruction(R.string.instruction_route_start)
        updateCardBackgrounds(startRouteCard, endRouteCard)
        replaceFragment(RouteStartFragment())
    }

    // Switch to end route mode
    private fun changeToEndRoute() {
        markerColor = BitmapDescriptorFactory.HUE_RED
        updateMapInstruction(R.string.instruction_route_end)
        updateCardBackgrounds(endRouteCard, startRouteCard)
        replaceFragment(RouteEndFragment())
    }

    // Update the map instruction text
    private fun updateMapInstruction(stringRes: Int) {
        findViewById<TextView>(R.id.map_instruction)?.text = getString(stringRes)
    }

    // Update the colors of the CardViews based on selection
    private fun updateCardBackgrounds(selectedCard: CardView, otherCard: CardView) {
        // White for selected, purple for unselected
        selectedCard.setCardBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
        otherCard.setCardBackgroundColor(ContextCompat.getColor(this, R.color.my_purple))
    }

    // Dynamically replace the current fragment with a new one
    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.route_fragment_container, fragment)
            .commitNow() // Commit immediately for instant change
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Set up initial map view to Kenya
        val kenyaLatLng = LatLng(-1.286389, 36.817223) // Nairobi coordinates
        val kenyaBounds = LatLngBounds(
            LatLng(-4.67677, 33.909821), // SW corner
            LatLng(4.62, 41.899578) // NE corner
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kenyaLatLng, 7f))

        // Enable map zoom controls
        mMap.uiSettings.isZoomControlsEnabled = true

        // Restrict map to Kenyan borders
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
            // Request permissions if not already granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Enable current location on the map
        mMap.isMyLocationEnabled = true

        // Set listener for map clicks to place markers
        mMap.setOnMapClickListener { latLng ->
            if (kenyaBounds.contains(latLng)) {
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
                mMap.clear()
                mMap.addMarker(markerOptions)
                Toast.makeText(this, latLng.toString(), Toast.LENGTH_SHORT).show()
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "Please click within Kenya's borders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    // Fragment for displaying route start information
    class RouteStartFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.route_start_fragment, container, false)
        }
    }

    // Fragment for displaying route end information
    class RouteEndFragment : Fragment() {
        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            return inflater.inflate(R.layout.route_end_fragment, container, false)
        }
    }
}