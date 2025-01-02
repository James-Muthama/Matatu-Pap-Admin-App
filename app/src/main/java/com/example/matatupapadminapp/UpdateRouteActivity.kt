package com.example.matatupapadminapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UpdateRouteActivity : AppCompatActivity() {
    // Firebase authentication instance
    private lateinit var auth: FirebaseAuth
    // Reference to the "Routes" node in Firebase
    private lateinit var routeDatabase: DatabaseReference

    // UI elements for editing route details
    private lateinit var routeStartEditText: EditText
    private lateinit var routeEndEditText: EditText
    private lateinit var fairPriceEditText: EditText
    private lateinit var updateBusButton: Button
    private lateinit var backIcon: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display for the activity
        enableEdgeToEdge()
        setContentView(R.layout.update_route_page)

        // Initialize Firebase Authentication and Database
        auth = FirebaseAuth.getInstance()
        routeDatabase = FirebaseDatabase.getInstance().getReference("Routes")

        // Binding UI elements from XML layout
        backIcon = findViewById(R.id.back_icon)
        routeStartEditText = findViewById(R.id.route_start_name)
        routeEndEditText = findViewById(R.id.route_end_name)
        fairPriceEditText = findViewById(R.id.fair_price)
        updateBusButton = findViewById(R.id.save_payment_btn)

        // Set up click listener for back icon to close the activity
        backIcon.setOnClickListener {
            finish() // Close the current activity
        }

        // Retrieve route name from intent extras
        var routeName = intent.getStringExtra("routeName") ?: ""

        // Convert to match Firebase database format by replacing spaces with underscores
        routeName = routeName.replace(" ", "_")

        if (routeName.isEmpty()) {
            // Show error if no route name is provided
            Toast.makeText(this, "No route name was found", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else {
            // Split route name into start and end parts
            val parts = routeName.split("-", limit = 2)
            var routeStartName = parts.getOrNull(0) ?: ""
            routeStartName = routeStartName.replace("_", " ") // Convert back to readable format for display
            var routeEndName = parts.getOrNull(1) ?: ""
            routeEndName = routeEndName.replace("_", " ") // Convert back to readable format for display
            routeStartEditText.setText(routeStartName)
            routeEndEditText.setText(routeEndName)
        }

        val fare = intent.getStringExtra("fare") ?: ""
        if (fare.isEmpty()) {
            // Show error if no fare is provided
            Toast.makeText(this, "No fare was found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Fetch existing route data from Firebase
        fetchRouteData(routeName, fare)

        // Disable editing of route start and end names
        routeStartEditText.setOnClickListener {
            Toast.makeText(this, "You cannot change the start name", Toast.LENGTH_SHORT).show()
        }
        routeEndEditText.setOnClickListener {
            Toast.makeText(this, "You cannot change the end name", Toast.LENGTH_SHORT).show()
        }

        // Set up click listener for updating route fare
        updateBusButton.setOnClickListener {
            updateRouteFair(routeName)
        }
    }

    /**
     * Fetches route data from Firebase based on route name.
     * @param routeName The name of the route to fetch data for.
     */
    @SuppressLint("SetTextI18n")
    private fun fetchRouteData(routeName: String, fare: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Reference to the specific route under the user's node
            val routeRef = routeDatabase.child(userId).child(routeName)
            routeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        fairPriceEditText.setText(fare)
                    } else {
                        // Show message if route does not exist in database
                        Toast.makeText(this@UpdateRouteActivity, "Route not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle database read errors
                    Toast.makeText(this@UpdateRouteActivity, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            // User not logged in, show error and close activity
            Toast.makeText(this@UpdateRouteActivity, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * Updates the fare of the route in Firebase.
     * @param routeName The name of the route to update.
     */
    private fun updateRouteFair(routeName: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Reference to the specific route under the user's node
            val routeRef = routeDatabase.child(userId).child(routeName)
            val newFareText = fairPriceEditText.text.toString().trim()

            if (newFareText.isNotEmpty()) {
                val newFare = newFareText.toIntOrNull()
                if (newFare != null) {
                    // Prepare data for update
                    val updatedData = mapOf(
                        "fare" to newFare
                    )
                    // Update fare in Firebase
                    routeRef.updateChildren(updatedData).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Route updated successfully", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent) // Navigate back to the main activity
                            finish() // Close this activity
                        } else {
                            Toast.makeText(this, "Failed to update route: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // Show error for invalid fare input
                    Toast.makeText(this, "Please enter a valid number for fare", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Warn user if fare input is empty
                Toast.makeText(this, "Please enter a fare", Toast.LENGTH_SHORT).show()
            }
        }
    }
}