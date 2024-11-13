package com.example.matatupapadminapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions

class AddRoutePageActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_route_page)

        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        receiptsIcon.setOnClickListener {
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        backIcon.setOnClickListener {
            // This will close the current activity and navigate back to the previous one
            finish()
        }

        homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Initialize the map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set initial map location to Kenya
        val kenyaLatLng = LatLng(-1.286389, 36.817223) // Coordinates for Nairobi, Kenya
        val kenyaBounds = LatLngBounds(
            LatLng(-4.67677, 33.909821), // Southwest corner of Kenya
            LatLng(4.62, 41.899578) // Northeast corner of Kenya
        )
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(kenyaLatLng, 7f)) // Adjust the zoom level as needed

        // Enable zoom controls
        mMap.uiSettings.isZoomControlsEnabled = true

        // Restrict the user from navigating outside Kenya's boundaries
        mMap.setLatLngBoundsForCameraTarget(kenyaBounds)

        // Check and request location permissions if needed
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request missing permissions from the user
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }

        // Enable the 'My Location' layer on the map if permissions are granted
        mMap.isMyLocationEnabled = true

        // Set a listener for map click
        mMap.setOnMapClickListener { latLng ->
            // When user clicks on the map, place a marker and display a Toast
            if (kenyaBounds.contains(latLng)) {
                val markerOptions = MarkerOptions()
                    .position(latLng)
                    // Here we change the color of the marker to green
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                mMap.clear()
                mMap.addMarker(markerOptions)
                Toast.makeText(this, latLng.toString(), Toast.LENGTH_SHORT).show()
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            } else {
                Toast.makeText(this, "Please click within Kenya's borders", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Request code for location permissions
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}