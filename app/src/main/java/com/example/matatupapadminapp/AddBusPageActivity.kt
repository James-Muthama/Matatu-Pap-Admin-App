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
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AddBusPageActivity : AppCompatActivity() {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables edge-to-edge display for UI
        setContentView(R.layout.add_bus_page)

        // Initialize Firebase Authentication and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Buses")

        // Initialize UI components
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        val busNumberPlate = findViewById<EditText>(R.id.bus_number_plate)
        val busRouteStart = findViewById<EditText>(R.id.bus_route_start)
        val busRouteEnd = findViewById<EditText>(R.id.bus_route_end)
        val busCode = findViewById<EditText>(R.id.bus_code)
        val busPaymentMethod = findViewById<EditText>(R.id.bus_payment_method)
        val addBusBtn = findViewById<Button>(R.id.add_bus_btn)

        // Handle "Add Bus" button click
        addBusBtn.setOnClickListener {
            // Get user input from the fields
            val numberPlate = busNumberPlate.text.toString()
            val routeStart = busRouteStart.text.toString()
            val routeEnd = busRouteEnd.text.toString()
            val code = busCode.text.toString()
            val paymentMethod = busPaymentMethod.text.toString()

            // Validate user input before proceeding
            if (numberPlate.isNotEmpty() && routeStart.isNotEmpty() && routeEnd.isNotEmpty() && code.isNotEmpty() && paymentMethod.isNotEmpty()) {
                findUserId(numberPlate, routeStart, routeEnd, code, paymentMethod) // Call function to add bus if input is valid
            } else {
                // Show a warning if any field is empty
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up navigation to the receipts page
        receiptsIcon.setOnClickListener {
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }

        // Set up navigation to the profile page
        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        // Set up "back" navigation to the previous activity
        backIcon.setOnClickListener {
            finish() // Close current activity and go back
        }

        // Set up navigation to the home/main activity
        homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // Check if the user is authenticated and call the function to add a bus
    private fun findUserId(numberPlate: String, routeStart: String, routeEnd: String, code: String, paymentMethod: String) {
        val userId = auth.currentUser?.uid // Get the current user ID
        if (userId != null) {
            // Call the function to add the bus data
            addBusInfo(userId, numberPlate, routeStart, routeEnd, code, paymentMethod)
        } else {
            // Show a warning if the user is not authenticated
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    // Add bus data to Firebase Realtime Database
    private fun addBusInfo(userId: String, numberPlate: String, routeStart: String, routeEnd: String, code: String, paymentMethod: String) {
        // Reference to the specific user's buses node in the database
        val userBusRef = database.child(userId)

        // Fetch existing data to determine the next bus ID dynamically
        userBusRef.get().addOnSuccessListener { dataSnapshot ->
            // Calculate the next bus index based on the number of existing child nodes
            val nextBusIndex = (dataSnapshot.childrenCount + 1).toInt() // Increment index for a new bus
            val newBusId = "bus$nextBusIndex" // Create a new bus ID (e.g., "bus1", "bus2")

            // Create a map to hold the new bus data
            val newBusData = mapOf(
                "number plate" to numberPlate,
                "route start" to routeStart,
                "route end" to routeEnd,
                "bus code" to code,
                "payment method" to paymentMethod
            )

            // Store the new bus data under the generated bus ID
            userBusRef.child(newBusId).setValue(newBusData)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Show success message and navigate to the home activity
                        Toast.makeText(this, "Bus Added Successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish() // Remove this activity from the back stack
                    } else {
                        // Show error message if storing data fails
                        Toast.makeText(this, "Failed to add bus: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
        }.addOnFailureListener { e ->
            // Handle errors that occur while fetching data
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
