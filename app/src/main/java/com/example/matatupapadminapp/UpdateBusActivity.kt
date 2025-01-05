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
    private lateinit var busDatabase: DatabaseReference
    private lateinit var routeDatabase: DatabaseReference
    private lateinit var paymentDatabase: DatabaseReference

    private lateinit var busNumberPlateEditText: EditText
    private lateinit var busCodeEditText: EditText
    private lateinit var busPaymentMethodEditText: EditText
    private lateinit var updateBusButton: Button
    private lateinit var backIcon: ImageView

    // UI component for selecting a route and payment method
    private lateinit var routeSpinner: Spinner
    private lateinit var paymentSpinner: Spinner

    // Lists to hold the names of routes and payments fetched from Firebase
    private var routeNames = mutableListOf<String>()
    private var paymentNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.update_bus_page)

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        busDatabase = FirebaseDatabase.getInstance().getReference("Buses")
        routeDatabase = FirebaseDatabase.getInstance().getReference("Routes")
        paymentDatabase = FirebaseDatabase.getInstance().getReference("Payments")

        // Get references to the views
        backIcon = findViewById(R.id.back_icon)
        busNumberPlateEditText = findViewById(R.id.bus_number_plate)
        routeSpinner = findViewById(R.id.action_spinner)
        busCodeEditText = findViewById(R.id.bus_code)
        busPaymentMethodEditText = findViewById(R.id.partyb)
        updateBusButton = findViewById(R.id.save_payment_btn)
        paymentSpinner = findViewById(R.id.action_spinner_2) // Assuming there's another spinner for payments

        backIcon.setOnClickListener { finish() }

        // Get the bus code from the intent
        val busCode = intent.getStringExtra("busCode") ?: ""
        val busNumberPlate = intent.getStringExtra("numberPlate") ?: ""

        if (busCode.isEmpty()) {
            Toast.makeText(this, "No bus code provided", Toast.LENGTH_SHORT).show()
            finish()
            return
        } else {
            // Set the number plate in the EditText
            val formattedNumberPlate = busNumberPlate.replace("(?<=\\G.{3})".toRegex(), " ")
            busNumberPlateEditText.setText(formattedNumberPlate)
        }

        // Fetch the bus data from the database with the bus code
        fetchBusData(busCode)

        // Fetch routes from Firebase to populate the route spinner
        fetchRoutesFromFirebase()

        // Fetch payments from Firebase to populate the payment spinner
        fetchPaymentsFromFirebase()

        // Set an onClickListener to show a toast message
        busNumberPlateEditText.setOnClickListener {
            Toast.makeText(this, "You cannot change the number plate", Toast.LENGTH_SHORT).show()
        }

        // Handle the update button click
        updateBusButton.setOnClickListener {
            updateBusData(busCode)
        }
    }

    private fun fetchBusData(busCode: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val busRef = busDatabase.child(userId).child(busCode)
            busRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // Fill the fields with the retrieved data
                        val routeName = snapshot.child("route name").getValue(String::class.java)
                        val numberPlate = snapshot.child("number plate").getValue(String::class.java)
                        val paymentMethod = snapshot.child("payment method").getValue(String::class.java)

                        // Populate the spinners
                        routeSpinner.setSelection(getSpinnerIndex(routeSpinner, routeName ?: ""))
                        paymentSpinner.setSelection(getSpinnerIndex(paymentSpinner, paymentMethod ?: ""))

                        // Set text for other fields
                        busNumberPlateEditText.setText(numberPlate)
                        busCodeEditText.setText(busCode)
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
     * Fetch route names from Firebase and populate the spinner, including a default item.
     */
    private fun fetchRoutesFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            routeDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    routeNames.clear()
                    routeNames.add(0, "Select a route")
                    dataSnapshot.children.forEach { routeSnapshot ->
                        val name = routeSnapshot.child("name").getValue(String::class.java)
                        name?.let { routeNames.add(it) }
                    }
                    setupSpinner(routeSpinner, routeNames)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@UpdateBusActivity, "Failed to load routes", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * Fetch payment method names from Firebase and populate the spinner, including a default item.
     */
    private fun fetchPaymentsFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            paymentDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    paymentNames.clear()
                    paymentNames.add(0, "Select a payment")
                    dataSnapshot.children.forEach { paymentSnapshot ->
                        paymentNames.add(paymentSnapshot.key ?: "Unknown Payment")
                    }
                    setupSpinner(paymentSpinner, paymentNames)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@UpdateBusActivity, "Failed to load payments", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * Set up the spinner with the list of items, including the default item.
     */
    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    private fun updateBusData(busCode: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val busRef = busDatabase.child(userId).child(busCode)

            // Prepare the updated data
            val updatedData = mapOf(
                "route name" to routeSpinner.selectedItem.toString(),
                "payment method" to paymentSpinner.selectedItem.toString()
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