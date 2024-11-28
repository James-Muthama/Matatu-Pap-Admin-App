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
                findUserId(numberPlate, routeStart, routeEnd, code, paymentMethod)
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
            finish()
        }

        // Set up navigation to the home/main activity
        homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // Check if the user is authenticated and call the function to add a bus
    private fun findUserId(numberPlate: String, routeStart: String, routeEnd: String, code: String, paymentMethod: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            addBusInfo(userId, numberPlate, routeStart, routeEnd, code, paymentMethod)
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    // Add bus data to Firebase Realtime Database with the simplified structure
    private fun addBusInfo(userId: String, numberPlate: String, routeStart: String, routeEnd: String, code: String, paymentMethod: String) {
        // Reference to the specific user's buses node in the database
        val userBusRef = database.child(userId).child(numberPlate)

        // Create a map to hold the bus data
        val busData = mapOf(
            "bus code" to code,
            "payment method" to paymentMethod,
            "route start" to routeStart,
            "route end" to routeEnd
        )

        // Store the bus data directly under the number plate
        userBusRef.setValue(busData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Bus Added Successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to add bus: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}