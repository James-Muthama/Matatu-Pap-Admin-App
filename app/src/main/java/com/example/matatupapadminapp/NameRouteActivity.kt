package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.cardview.widget.CardView
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class NameRouteActivity : ComponentActivity() {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Applies edge-to-edge display for better UI
        setContentView(R.layout.name_route_page) // Set the layout for this activity

        // Initialize Firebase Authentication and Database
        auth = FirebaseAuth.getInstance() // Get Firebase authentication instance
        database = FirebaseDatabase.getInstance().getReference("Routes") // Reference to "Routes" node in Firebase

        // Retrieve route start, end, and nearby stops from Intent
        val routeStart = intent.getParcelableExtra<LatLng>("routeStart")
        val routeEnd = intent.getParcelableExtra<LatLng>("routeEnd")
        val nearbyStops = intent.getParcelableArrayListExtra<LatLng>("nearbyStops")

        // Initialize UI components for input
        val routeStartEditText = findViewById<EditText>(R.id.bus_number_plate)
        val routeEndEditText = findViewById<EditText>(R.id.bus_code)
        val fareEditText = findViewById<EditText>(R.id.bus_payment_method)
        val addRouteButton = findViewById<Button>(R.id.add_bus_btn)

        // Set click listener for adding route to Firebase
        addRouteButton.setOnClickListener {
            val routeStartName = routeStartEditText.text.toString()
            val routeEndName = routeEndEditText.text.toString()
            val routeFare = fareEditText.text.toString().toIntOrNull() ?: 0

            // Check if all necessary data is present before saving to Firebase
            if (routeStart != null && routeEnd != null && routeStartName.isNotBlank() && routeEndName.isNotBlank()) {
                saveRouteToFirebase(routeStartName, routeEndName, routeStart, routeEnd, routeFare, nearbyStops)
            } else {
                // Handle the case where data is missing or invalid, perhaps show an error message
            }
        }

        // Initialize CardViews for navigation
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        homeIcon.setOnClickListener {
            navigateToMainActivity()
        }

        profileIcon.setOnClickListener {
            navigateToProfileActivity()
        }

        receiptsIcon.setOnClickListener {
            navigateToReceiptsActivity()
        }
    }

    /**
     * Saves the route details into Firebase under the user's ID with a unique route name.
     * @param startName Name of the starting location.
     * @param endName Name of the ending location.
     * @param start LatLng of the route start.
     * @param end LatLng of the route end.
     * @param fare The fare for this route.
     * @param stops List of transit stops along the route.
     */
    private fun saveRouteToFirebase(startName: String, endName: String, start: LatLng, end: LatLng, fare: Int, stops: List<LatLng>?) {
        val userId = auth.currentUser?.uid ?: return  // Ensure user is logged in, return if not

        // Create the route name
        val routeName = "$startName-$endName"

        // Reference to the user's node
        val userRoutesRef = database.child(userId)

        // Prepare the route data
        val routeData = mutableMapOf<String, Any>(
            "name" to routeName,
            "start" to mapOf("lat" to start.latitude, "lng" to start.longitude),
            "end" to mapOf("lat" to end.latitude, "lng" to end.longitude),
            "fare" to fare
        )

        // Add transit stops if they exist
        if (stops != null) {
            routeData["transitStops"] = stops.map {
                mapOf("lat" to it.latitude, "lng" to it.longitude)
            }
        }

        // Save the route directly under the user's ID
        // We'll use the route name as the key, but this approach assumes route names are unique per user
        // If route names might not be unique, you'd need to use push().key or create a unique identifier
        val routeKey = routeName.replace(" ", "_") // Replace spaces with underscores or another safe character

        userRoutesRef.child(routeKey).setValue(routeData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Route $routeName was successfully saved", Toast.LENGTH_SHORT).show()
                navigateToMainActivityWithClearingStack()
            } else {
                Toast.makeText(this, "Route $routeName wasn't saved successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Navigation helper methods to reduce code duplication
    private fun navigateToMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
    }

    private fun navigateToProfileActivity() {
        startActivity(Intent(this, ProfileActivity::class.java))
    }

    private fun navigateToReceiptsActivity() {
        startActivity(Intent(this, ReceiptsActivity::class.java))
    }

    private fun navigateToMainActivityWithClearingStack() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK) // Clear top activities and start new task
        startActivity(intent)
        finish() // Close this activity to prevent back navigation
    }
}