package com.example.matatupapadminapp
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_bus_page)

        // Initialize Firebase Authentication and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Buses")

        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        val busNumberPlate = findViewById<EditText>(R.id.bus_number_plate)
        val busRouteStart = findViewById<EditText>(R.id.bus_route_start)
        val busRouteEnd = findViewById<EditText>(R.id.bus_route_end)
        val addBusBtn = findViewById<Button>(R.id.add_bus_btn)

        addBusBtn.setOnClickListener {
            val numberPlate = busNumberPlate.text.toString()
            val routeStart = busRouteStart.text.toString()
            val routeEnd = busRouteEnd.text.toString()

            findUser(numberPlate, routeStart, routeEnd)
        }

        receiptsIcon.setOnClickListener{
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener{
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        backIcon.setOnClickListener {
            // This will close the current activity and navigate back to the previous one
            finish()
        }

        homeIcon.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    // Register user with Firebase Authentication and store additional data
    private fun findUser(numberPlate: String, routeStart: String, routeEnd: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Store additional user data in Realtime Database
            addBus(userId, numberPlate, routeStart, routeEnd)
        }
    }

    // Store additional user data in Realtime Database
    private fun addBus(userId: String, numberPlate: String, routeStart: String, routeEnd: String) {
        // Create a user data map to store name and email
        val bus = mapOf(
            "number plate" to numberPlate,
            "route start" to routeStart,
            "route end" to routeEnd
        )

        // Store user data under "Users/userId" in Realtime Database
        database.child(userId).setValue(bus)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // If data is stored successfully, show a success message
                    Toast.makeText(this, "Bus Added Successfully!", Toast.LENGTH_SHORT).show()
                    // Navigate to MainActivity after successful registration
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Ensure this activity is removed from the back stack
                } else {
                    // Show error message if data storage fails
                    Toast.makeText(this, "Failed to add bus: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}