package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UpdateBusActivity : AppCompatActivity() {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var busDatabase: DatabaseReference
    private lateinit var routeDatabase: DatabaseReference


    private lateinit var busNumberPlateEditText: EditText
    private lateinit var busCodeEditText: EditText
    private lateinit var busPaymentMethodEditText: EditText
    private lateinit var updateBusButton: Button
    private lateinit var backIcon: ImageView

    // UI component for selecting a route
    private lateinit var routeSpinner: Spinner

    // List to hold the names of routes fetched from Firebase
    private var routeNames = mutableListOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display for better UI
        enableEdgeToEdge()
        // Set the layout for this activity
        setContentView(R.layout.update_bus_page)

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        busDatabase = FirebaseDatabase.getInstance().getReference("Buses")
        routeDatabase = FirebaseDatabase.getInstance().getReference("Routes")


        // Get references to the views
        backIcon = findViewById(R.id.back_icon)
        busNumberPlateEditText = findViewById(R.id.bus_number_plate)
        routeSpinner = findViewById(R.id.action_spinner)
        busCodeEditText = findViewById(R.id.bus_code)
        busPaymentMethodEditText = findViewById(R.id.bus_payment_method)
        updateBusButton = findViewById(R.id.save_payment_btn)

        backIcon.setOnClickListener {
            finish()
        }


        // Get the number plate from the intent
        val busNumberPlate = intent.getStringExtra("numberPlate") ?: ""
        if (busNumberPlate.isEmpty()) {
            Toast.makeText(this, "No bus number plate provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else{
            // Set the number plate in the EditText
            val formattedNumberPlate = busNumberPlate.replace("(?<=\\G.{3})".toRegex(), " ")
            busNumberPlateEditText.setText(formattedNumberPlate)
        }


        // Fetch the bus data from the database
        fetchBusData(busNumberPlate)

        // Set an onClickListener to show a toast message
        busNumberPlateEditText.setOnClickListener {
            Toast.makeText(this, "You cannot change the number plate", Toast.LENGTH_SHORT).show()
        }

        // Handle the update button click
        updateBusButton.setOnClickListener {
            updateBusData(busNumberPlate)
        }
    }

    private fun fetchBusData(busNumberPlate: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val busRef = busDatabase.child(userId).child(busNumberPlate)
            busRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Fill the fields with the retrieved data
                        val routeName = snapshot.child("route name").getValue(String::class.java)
                        val code = snapshot.child("bus code").getValue(String::class.java)
                        val paymentMethod = snapshot.child("payment method").getValue(String::class.java)

                        routeDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                // Clear the existing list to avoid duplicates
                                routeNames.clear()
                                // Add default item
                                if (routeName != null) {
                                    routeNames.add(0, routeName)
                                }
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
                                Toast.makeText(this@UpdateBusActivity, "Failed to load routes", Toast.LENGTH_SHORT).show()
                            }
                        })

                        // Populate the fields
                        routeSpinner.setSelection(getSpinnerIndex(routeSpinner, routeName ?: ""))
                        busCodeEditText.setText(code)
                        busPaymentMethodEditText.setText(paymentMethod)
                    } else {
                        Toast.makeText(this@UpdateBusActivity, "Bus not found", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@UpdateBusActivity, "Failed to load data: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this@UpdateBusActivity, "User not authenticated", Toast.LENGTH_SHORT).show()
            finish()
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

    private fun updateBusData(busNumberPlate: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val busRef = busDatabase.child(userId).child(busNumberPlate)

            // Prepare the updated data
            val updatedData = mapOf(
                "route name" to routeSpinner.selectedItem.toString(),
                "bus code" to busCodeEditText.text.toString(),
                "payment method" to busPaymentMethodEditText.text.toString()
            )

            // Update the database
            busRef.updateChildren(updatedData).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Bus updated successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to update bus", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun getSpinnerIndex(spinner: Spinner, value: String): Int {
        for (i in 0 until spinner.count) {
            if (spinner.getItemAtPosition(i).toString() == value) {
                return i
            }
        }
        return 0 // Default to the first item if not found
    }
}
