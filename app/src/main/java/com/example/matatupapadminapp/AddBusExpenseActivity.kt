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
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddBusExpenseActivity : AppCompatActivity(), AdapterView.OnItemSelectedListener {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth
    private lateinit var busDatabase: DatabaseReference
    private lateinit var expenseDatabase: DatabaseReference
    private lateinit var routeExpenseDatabase: DatabaseReference

    // UI components
    private lateinit var busSpinner: Spinner
    private lateinit var monthYearSpinner: Spinner
    private lateinit var amountEditText: EditText
    private lateinit var addExpenseBtn: Button

    // Lists to hold bus data
    private var busDisplayNames = mutableListOf<String>()
    private var busNumberPlates = mutableListOf<String>()
    private var routeNames = mutableListOf<String>()

    // List to hold month-year options
    private var monthYearOptions = mutableListOf<String>()
    private var monthYearKeys = mutableListOf<String>()

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_bus_expenses_page)

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        busDatabase = FirebaseDatabase.getInstance().getReference("Buses")
        expenseDatabase = FirebaseDatabase.getInstance().getReference("Bus_Expenses")
        routeExpenseDatabase = FirebaseDatabase.getInstance().getReference("Route_Expenses")

        // Initialize UI components from the layout
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        busSpinner = findViewById(R.id.action_spinner)
        monthYearSpinner = findViewById(R.id.action_spinner_2)
        amountEditText = findViewById(R.id.trip_date)
        addExpenseBtn = findViewById(R.id.save_payment_btn)

        // Fetch buses from Firebase to populate the first spinner
        fetchBusesFromFirebase()

        // Setup month-year spinner
        setupMonthYearSpinner()

        // Handle click for the "Add Expense" button
        addExpenseBtn.setOnClickListener {
            val selectedBusPosition = busSpinner.selectedItemPosition
            val selectedMonthYearPosition = monthYearSpinner.selectedItemPosition
            val amount = amountEditText.text.toString()

            // Validate user input before proceeding
            if (selectedBusPosition > 0 && selectedMonthYearPosition > 0 && amount.isNotEmpty()) {
                val selectedBusPlate = busNumberPlates[selectedBusPosition - 1] // -1 because first item is "Select a bus"
                val selectedRouteName = routeNames[selectedBusPosition - 1]
                val selectedMonthYearKey = monthYearKeys[selectedMonthYearPosition - 1]
                val amountDouble = amount.toDoubleOrNull()

                if (amountDouble != null && amountDouble > 0) {
                    addExpenseInfo(selectedBusPlate, selectedRouteName, selectedMonthYearKey, amountDouble)
                } else {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please select a bus, month-year and fill in the amount", Toast.LENGTH_SHORT).show()
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

        // Set up spinners to listen for item selection
        busSpinner.onItemSelectedListener = this
        monthYearSpinner.onItemSelectedListener = this
    }

    /**
     * Fetch bus data from Firebase and populate the spinner
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
                    Toast.makeText(this@AddBusExpenseActivity, "Failed to load buses", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    /**
     * Setup the month-year spinner with options in MM-yyyy format
     * Generates last 12 months and next 12 months
     */
    private fun setupMonthYearSpinner() {
        monthYearOptions.clear()
        monthYearKeys.clear()
        monthYearOptions.add("Select a month-year")

        val calendar = Calendar.getInstance()
        val monthYearFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
        val displayFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())

        // Generate last 12 months
        for (i in 12 downTo 1) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.MONTH, -i)
            monthYearKeys.add(monthYearFormat.format(tempCal.time))
            monthYearOptions.add(displayFormat.format(tempCal.time))
        }

        // Add current month
        monthYearKeys.add(monthYearFormat.format(calendar.time))
        monthYearOptions.add(displayFormat.format(calendar.time))

        // Generate next 12 months
        for (i in 1..12) {
            val tempCal = Calendar.getInstance()
            tempCal.add(Calendar.MONTH, i)
            monthYearKeys.add(monthYearFormat.format(tempCal.time))
            monthYearOptions.add(displayFormat.format(tempCal.time))
        }

        setupSpinner(monthYearSpinner, monthYearOptions)
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
     * Add expense data to Firebase for both Bus_Expenses and Route_Expenses
     * Bus_Expenses Structure: Bus_Expenses/userId/busPlate/MM-yyyy/total_expenses
     * Route_Expenses Structure: Route_Expenses/userId/routeName/MM-yyyy/total_expenses
     */
    private fun addExpenseInfo(busPlate: String, routeName: String, monthYearKey: String, amount: Double) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            // Path: Bus_Expenses/userId/busPlate/MM-yyyy
            val busExpenseRef = expenseDatabase.child(userId)
                .child(busPlate)
                .child(monthYearKey)

            // Path: Route_Expenses/userId/routeName/MM-yyyy
            val routeExpenseRef = routeExpenseDatabase.child(userId)
                .child(routeName)
                .child(monthYearKey)

            // Check if bus expense already exists for this month
            busExpenseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(busSnapshot: DataSnapshot) {
                    val existingBusExpense = busSnapshot.child("total_expenses").getValue(Double::class.java) ?: 0.0
                    val newBusTotalExpense = existingBusExpense + amount

                    // Check if route expense already exists for this month
                    routeExpenseRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(routeSnapshot: DataSnapshot) {
                            val existingRouteExpense = routeSnapshot.child("total_expenses").getValue(Double::class.java) ?: 0.0
                            val newRouteTotalExpense = existingRouteExpense + amount

                            val busExpenseData = mapOf("total_expenses" to newBusTotalExpense)
                            val routeExpenseData = mapOf("total_expenses" to newRouteTotalExpense)

                            // Save to Bus_Expenses
                            val busExpenseTask = busExpenseRef.setValue(busExpenseData)

                            // Save to Route_Expenses
                            val routeExpenseTask = routeExpenseRef.setValue(routeExpenseData)

                            // Wait for both tasks to complete
                            busExpenseTask.addOnCompleteListener { busTask ->
                                routeExpenseTask.addOnCompleteListener { routeTask ->
                                    if (busTask.isSuccessful && routeTask.isSuccessful) {
                                        val message = if (existingBusExpense > 0 || existingRouteExpense > 0) {
                                            "Expense Updated Successfully!\n" +
                                                    "Bus: $busPlate - Previous: KSh $existingBusExpense, New Total: KSh $newBusTotalExpense\n" +
                                                    "Route: $routeName - Previous: KSh $existingRouteExpense, New Total: KSh $newRouteTotalExpense"
                                        } else {
                                            "Expense Added Successfully!\n" +
                                                    "Bus: $busPlate - KSh $newBusTotalExpense\n" +
                                                    "Route: $routeName - KSh $newRouteTotalExpense"
                                        }
                                        Toast.makeText(this@AddBusExpenseActivity, message, Toast.LENGTH_LONG).show()
                                        val intent = Intent(this@AddBusExpenseActivity, MainActivity::class.java)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        val errorMsg = when {
                                            !busTask.isSuccessful && !routeTask.isSuccessful ->
                                                "Failed to add to both Bus Expenses and Route Expenses"
                                            !busTask.isSuccessful ->
                                                "Failed to add Bus Expense: ${busTask.exception?.message}"
                                            else ->
                                                "Failed to add Route Expense: ${routeTask.exception?.message}"
                                        }
                                        Toast.makeText(this@AddBusExpenseActivity, errorMsg, Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Toast.makeText(
                                this@AddBusExpenseActivity,
                                "Database error (Route): ${error.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@AddBusExpenseActivity,
                        "Database error (Bus): ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
        when (parent.id) {
            R.id.action_spinner -> {
                if (position > 0) {
                    // Show the route name when a bus is selected
                    val routeName = routeNames[position - 1]
                    Toast.makeText(this, "Route: $routeName", Toast.LENGTH_SHORT).show()
                }
            }
            R.id.action_spinner_2 -> {
                if (position > 0) {
                    // Handle month-year selection if needed
                    val selectedMonthYear = monthYearOptions[position]
                    Toast.makeText(this, "Selected: $selectedMonthYear", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        // This method is called when nothing is selected in the spinner
    }
}