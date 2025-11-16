package com.example.matatupapadminapp

import android.annotation.SuppressLint
import android.app.DatePickerDialog
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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddBusTripActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth
    private lateinit var busDatabase: DatabaseReference
    private lateinit var routeDatabase: DatabaseReference
    private lateinit var incomeDatabase: DatabaseReference
    private lateinit var routeIncomeDatabase: DatabaseReference

    // UI components
    private lateinit var busSpinner: Spinner
    private lateinit var tripDateEditText: EditText
    private lateinit var numTripsEditText: EditText
    private lateinit var addTripBtn: Button

    // Lists to hold bus data
    private var busDisplayNames = mutableListOf<String>()
    private var busNumberPlates = mutableListOf<String>()
    private var routeNames = mutableListOf<String>()

    // Calendar for date picker
    private val calendar = Calendar.getInstance()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_bus_trips_page)

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        busDatabase = FirebaseDatabase.getInstance().getReference("Buses")
        routeDatabase = FirebaseDatabase.getInstance().getReference("Routes")
        incomeDatabase = FirebaseDatabase.getInstance().getReference("Bus_Income")
        routeIncomeDatabase = FirebaseDatabase.getInstance().getReference("Route_Income")

        // Initialize UI components from the layout
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        busSpinner = findViewById(R.id.action_spinner)
        tripDateEditText = findViewById(R.id.trip_date)
        numTripsEditText = findViewById(R.id.bus_code)
        addTripBtn = findViewById(R.id.save_payment_btn)

        // Fetch buses from Firebase to populate the spinner
        fetchBusesFromFirebase()

        // Set up date picker for trip date
        setupDatePicker()

        // Handle click for the "Add Trip" button
        addTripBtn.setOnClickListener {
            val selectedBusPosition = busSpinner.selectedItemPosition
            val tripDate = tripDateEditText.text.toString()
            val numTripsStr = numTripsEditText.text.toString()

            // Validate user input before proceeding
            if (selectedBusPosition > 0 && tripDate.isNotEmpty() && numTripsStr.isNotEmpty()) {
                val selectedBusPlate = busNumberPlates[selectedBusPosition - 1] // -1 because first item is "Select a bus"
                val selectedRouteName = routeNames[selectedBusPosition - 1]
                val numTrips = numTripsStr.toIntOrNull()

                if (numTrips != null && numTrips > 0) {
                    // Fetch fare price from Routes using route name
                    fetchFarePriceAndAddIncome(selectedBusPlate, selectedRouteName, tripDate, numTrips)
                } else {
                    Toast.makeText(this, "Please enter a valid number of trips", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please select a bus and fill in all fields", Toast.LENGTH_SHORT).show()
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
        busSpinner.onItemSelectedListener = this
    }

    /**
     * Fetch bus data from Firebase and populate the spinner with number plates
     */
    private fun fetchBusesFromFirebase() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            busDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    busDisplayNames.clear()
                    busNumberPlates.clear()
                    routeNames.clear()
                    busDisplayNames.add(0, "Select a bus")

                    dataSnapshot.children.forEach { busSnapshot ->
                        val numberPlate = busSnapshot.child("number plate").getValue(String::class.java)
                        val routeName = busSnapshot.child("route name").getValue(String::class.java)

                        if (numberPlate != null && routeName != null) {
                            // Display ONLY the number plate in spinner
                            busDisplayNames.add(numberPlate)
                            busNumberPlates.add(numberPlate)
                            routeNames.add(routeName)
                        }
                    }
                    setupSpinner(busSpinner, busDisplayNames)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(this@AddBusTripActivity, "Failed to load buses", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * Fetch fare price from Routes database using route name
     */
    private fun fetchFarePriceAndAddIncome(busPlate: String, routeName: String, tripDate: String, numTrips: Int) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            routeDatabase.child(userId).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var farePrice: Long? = null

                    // Search for the route by matching the "name" field
                    dataSnapshot.children.forEach { routeSnapshot ->
                        val routeData = routeSnapshot.value as? Map<*, *>
                        val name = routeData?.get("name") as? String

                        // Match by the name field in the database
                        if (name != null && name.equals(routeName, ignoreCase = true)) {
                            farePrice = try {
                                // Try to get fare as Long first, then as Double
                                routeData["fare"] as? Long ?: (routeData["fare"] as? Double)?.toLong()
                            } catch (e: Exception) {
                                null
                            }
                            return@forEach
                        }
                    }

                    if (farePrice != null) {
                        // Add trip income with the fetched fare price
                        addTripIncome(busPlate, tripDate, numTrips, farePrice!!.toDouble())
                    } else {
                        Toast.makeText(
                            this@AddBusTripActivity,
                            "Fare price not found for route: $routeName",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Toast.makeText(
                        this@AddBusTripActivity,
                        "Failed to fetch fare price: ${databaseError.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        }
    }

    /**
     * Set up the date picker for the trip date field
     */
    private fun setupDatePicker() {
        val dateSetListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        tripDateEditText.setOnClickListener {
            DatePickerDialog(
                this,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    /**
     * Update the trip date EditText with selected date
     */
    private fun updateDateInView() {
        val format = "dd/MM/yyyy"
        val sdf = SimpleDateFormat(format, Locale.getDefault())
        tripDateEditText.setText(sdf.format(calendar.time))
    }

    /**
     * Set up the spinner with the list of items
     */
    private fun setupSpinner(spinner: Spinner, items: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner.adapter = adapter
    }

    /**
     * Add trip income data to Firebase for both Bus_Income and Route_Income
     * Bus_Income Structure: Bus_Income/userId/busPlate/dd-MM-yyyy/tripId/{num_trips, income}
     * Route_Income Structure: Route_Income/userId/routeName/dd-MM-yyyy/tripId/{num_trips, income}
     */
    private fun addTripIncome(busPlate: String, tripDate: String, numTrips: Int, farePrice: Double) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Convert date from dd/MM/yyyy to dd-MM-yyyy for database key
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val dbDateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())

            val date = dateFormat.parse(tripDate)
            val dbDateKey = if (date != null) {
                dbDateFormat.format(date)
            } else {
                Toast.makeText(this, "Invalid date format", Toast.LENGTH_SHORT).show()
                return
            }

            // Calculate income: num_trips * fare_price
            val income = numTrips * farePrice

            // Get the route name for the selected bus
            val selectedBusPosition = busSpinner.selectedItemPosition
            val routeName = if (selectedBusPosition > 0) {
                routeNames[selectedBusPosition - 1]
            } else {
                Toast.makeText(this, "Invalid bus selection", Toast.LENGTH_SHORT).show()
                return
            }

            // Generate a unique trip ID for Bus_Income
            val busTripId = incomeDatabase.child(userId).child(busPlate).child(dbDateKey).push().key

            // Generate a unique trip ID for Route_Income
            val routeTripId = routeIncomeDatabase.child(userId).child(routeName).child(dbDateKey).push().key

            if (busTripId != null && routeTripId != null) {
                val tripData = mapOf(
                    "num_trips" to numTrips,
                    "income" to income
                )

                // Save to Bus_Income: Bus_Income/userId/busPlate/dd-MM-yyyy/tripId
                val busIncomeTask = incomeDatabase.child(userId)
                    .child(busPlate)
                    .child(dbDateKey)
                    .child(busTripId)
                    .setValue(tripData)

                // Save to Route_Income: Route_Income/userId/routeName/dd-MM-yyyy/tripId
                val routeIncomeTask = routeIncomeDatabase.child(userId)
                    .child(routeName)
                    .child(dbDateKey)
                    .child(routeTripId)
                    .setValue(tripData)

                // Wait for both tasks to complete
                busIncomeTask.addOnCompleteListener { busTask ->
                    routeIncomeTask.addOnCompleteListener { routeTask ->
                        if (busTask.isSuccessful && routeTask.isSuccessful) {
                            Toast.makeText(
                                this,
                                "Trip Income Added Successfully!\n$numTrips trips × KSh $farePrice = KSh $income\nSaved for Bus: $busPlate & Route: $routeName",
                                Toast.LENGTH_LONG
                            ).show()
                            val intent = Intent(this, ReceiptsActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            val errorMsg = when {
                                !busTask.isSuccessful && !routeTask.isSuccessful ->
                                    "Failed to add to both Bus Income and Route Income"
                                !busTask.isSuccessful ->
                                    "Failed to add Bus Income: ${busTask.exception?.message}"
                                else ->
                                    "Failed to add Route Income: ${routeTask.exception?.message}"
                            }
                            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        if (parent.id == R.id.action_spinner) {
            if (position > 0) {
                // Show the route name when a bus is selected
                val routeName = routeNames[position - 1]
                Toast.makeText(this, "Route: $routeName", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // This method is called when nothing is selected in the spinner
    }
}