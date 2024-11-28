package com.example.matatupapadminapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AddBusPageActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth
    private lateinit var busDatabase: DatabaseReference
    private lateinit var routeDatabase: DatabaseReference
    // UI component for selecting a route
    private lateinit var routeSpinner: Spinner
    // List to hold the names of routes fetched from Firebase
    private var routeNames = mutableListOf<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display for better UI
        enableEdgeToEdge()
        // Set the layout for this activity
        setContentView(R.layout.add_bus_page)

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        routeDatabase = FirebaseDatabase.getInstance().getReference("Routes")
        busDatabase = FirebaseDatabase.getInstance().getReference("Buses")

        // Initialize UI components from the layout
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        val busNumberPlate = findViewById<EditText>(R.id.bus_number_plate)
        routeSpinner = findViewById(R.id.bus_route_spinner)
        val busCode = findViewById<EditText>(R.id.bus_code)
        val busPaymentMethod = findViewById<EditText>(R.id.bus_payment_method)
        val addBusBtn = findViewById<Button>(R.id.add_bus_btn)

        // Fetch routes from Firebase to populate the spinner
        fetchRoutesFromFirebase()

        // Handle click for the "Add Bus" button
        addBusBtn.setOnClickListener {
            // Remove spaces from number plate
            val numberPlate = busNumberPlate.text.toString().replace("\\s".toRegex(), "")
            val selectedRoute = routeSpinner.selectedItem.toString()
            val code = busCode.text.toString()
            val paymentMethod = busPaymentMethod.text.toString()

            // Validate user input before proceeding
            if (numberPlate.isNotEmpty() && selectedRoute != "Select a route" && code.isNotEmpty() && paymentMethod.isNotEmpty()) {
                findUserId(numberPlate, selectedRoute, code, paymentMethod)
            } else {
                // Show a warning if the route is not selected or any field is empty
                Toast.makeText(this, "Please select a route and fill in all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up navigation
        receiptsIcon.setOnClickListener {
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        backIcon.setOnClickListener {
            finish()
        }

        homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Set up spinner to listen for item selection
        routeSpinner.onItemSelectedListener = this
    }

    /**
     * Fetch route names from Firebase and populate the spinner, including a default item.
     */
    private fun fetchRoutesFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            routeDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    // Clear the existing list to avoid duplicates
                    routeNames.clear()
                    // Add default item
                    routeNames.add(0, "Select a route")
                    // Iterate through all child nodes under the user ID
                    dataSnapshot.children.forEach { routeSnapshot ->
                        val name = routeSnapshot.child("name").getValue(String::class.java)
                        name?.let { routeNames.add(it) } // Add the route name if it exists
                    }
                    // After fetching all routes, set up the spinner
                    setupSpinner()
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Display an error message if the fetch fails
                    Toast.makeText(this@AddBusPageActivity, "Failed to load routes", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * Set up the spinner with the list of route names, including the default item.
     */
    private fun setupSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routeNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        routeSpinner.adapter = adapter
    }

    /**
     * Check user authentication and proceed to add bus information if authenticated.
     */
    private fun findUserId(numberPlate: String, routeName: String, code: String, paymentMethod: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            addBusInfo(userId, numberPlate, routeName, code, paymentMethod)
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Add bus data to Firebase under the user's ID.
     */
    private fun addBusInfo(userId: String, numberPlate: String, routeName: String, code: String, paymentMethod: String) {
        val userBusRef = busDatabase.child(userId).child(numberPlate)
        val busData = mapOf(
            "bus code" to code,
            "payment method" to paymentMethod,
            "route name" to routeName
        )

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

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        // You can provide your implementation here
        if (parent.id == R.id.bus_route_spinner) {
            // Check if the selection is not the default item
            if (position > 0) {
                // Handle the selection of a real route
                val selectedRoute = parent.getItemAtPosition(position).toString()
                // You might want to do something with the selected route here
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // This method is called when nothing is selected in the spinner
        // You can leave it empty or add some logic if necessary
    }
}