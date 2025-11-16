package com.example.matatupapadminapp

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.*

class Compare2RoutesActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Spinner
    private lateinit var timePeriodSpinner: Spinner
    private lateinit var dateSpinner: Spinner

    // Route Name TextViews
    private lateinit var route1NameTextView: TextView
    private lateinit var route2NameTextView: TextView

    // Route 1 Metric TextViews
    private lateinit var route1RevenueTextView: TextView
    private lateinit var route1ExpenseTextView: TextView
    private lateinit var route1ProfitMarginTextView: TextView
    private lateinit var route1TripVolumeTextView: TextView
    private lateinit var route1RevenuePerBusTextView: TextView

    // Route 2 Metric TextViews
    private lateinit var route2RevenueTextView: TextView
    private lateinit var route2ExpenseTextView: TextView
    private lateinit var route2ProfitMarginTextView: TextView
    private lateinit var route2TripVolumeTextView: TextView
    private lateinit var route2RevenuePerBusTextView: TextView

    // Navigation
    private lateinit var backIcon: ImageView
    private lateinit var homeIconCard: CardView
    private lateinit var receiptsIconCard: CardView
    private lateinit var profileIconCard: CardView

    // Data variables
    private var routeName1: String? = null
    private var routeName2: String? = null
    private var selectedTimePeriod: String = "Day"
    private var selectedDate: String = ""
    private var selectedCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.compare_2_routes_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get route names from intent
        routeName1 = intent.getStringExtra("routeName1")
        routeName2 = intent.getStringExtra("routeName2")

        // Initialize all views
        initializeViews()

        // Setup spinner
        setupTimePeriodSpinner()

        // Display route names
        route1NameTextView.text = routeName1 ?: "Route 1"
        route2NameTextView.text = routeName2 ?: "Route 2"

        // Setup navigation
        setupNavigation()
    }

    private fun initializeViews() {
        // Spinners
        timePeriodSpinner = findViewById(R.id.timePeriodSpinner)
        dateSpinner = findViewById(R.id.dateSpinner)

        // Route Name TextViews
        route1NameTextView = findViewById(R.id.bus_1)
        route2NameTextView = findViewById(R.id.bus_2)

        // Route 1 Metric TextViews
        route1RevenueTextView = findViewById(R.id.textView1_)
        route1ExpenseTextView = findViewById(R.id.textView3)
        route1ProfitMarginTextView = findViewById(R.id.textView_5)
        route1TripVolumeTextView = findViewById(R.id.textView7)
        route1RevenuePerBusTextView = findViewById(R.id.textView9)

        // Route 2 Metric TextViews
        route2RevenueTextView = findViewById(R.id.textView_2)
        route2ExpenseTextView = findViewById(R.id.textView_4)
        route2ProfitMarginTextView = findViewById(R.id.textView6)
        route2TripVolumeTextView = findViewById(R.id.textView8_)
        route2RevenuePerBusTextView = findViewById(R.id.textView10)

        // Navigation
        backIcon = findViewById(R.id.back_icon)
        homeIconCard = findViewById(R.id.home_icon_card)
        receiptsIconCard = findViewById(R.id.receipts_icon_card)
        profileIconCard = findViewById(R.id.profile_icon_card)

        // Add logging to verify views are found
        android.util.Log.d("Compare2Routes", "Views initialized - Revenue1: ${route1RevenueTextView != null}, Revenue2: ${route2RevenueTextView != null}")
    }

    private fun setupNavigation() {
        receiptsIconCard.setOnClickListener {
            startActivity(Intent(this, ReceiptsActivity::class.java))
        }

        profileIconCard.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        backIcon.setOnClickListener {
            finish()
        }

        homeIconCard.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }

    private fun setupTimePeriodSpinner() {
        val timePeriods = arrayOf("Day", "Month", "Year")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, timePeriods)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        timePeriodSpinner.adapter = adapter

        timePeriodSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedTimePeriod = timePeriods[position]
                setupDateSpinner(selectedTimePeriod)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDateSpinner(timePeriod: String) {
        when (timePeriod) {
            "Day" -> {
                // For Day, set up a clickable spinner that opens DatePicker
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(selectedCalendar.time)

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf(selectedDate))
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                dateSpinner.adapter = adapter

                dateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        showDatePicker()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // Trigger initial data fetch
                fetchAndCalculateMetrics()
            }
            "Month" -> {
                val calendar = Calendar.getInstance()
                val dateOptions = mutableListOf<String>()

                // Generate last 12 months
                val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                for (i in 0..11) {
                    calendar.time = Date()
                    calendar.add(Calendar.MONTH, -i)
                    dateOptions.add(monthFormat.format(calendar.time))
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dateOptions)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                dateSpinner.adapter = adapter

                dateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedDate = dateOptions[position]
                        fetchAndCalculateMetrics()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
            "Year" -> {
                val dateOptions = mutableListOf<String>()

                // Generate years from 2025 to current year + 1
                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                for (year in 2025..currentYear + 1) {
                    dateOptions.add(year.toString())
                }

                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dateOptions)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                dateSpinner.adapter = adapter

                dateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedDate = dateOptions[position]
                        fetchAndCalculateMetrics()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedCalendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(selectedCalendar.time)

                // Update spinner to show selected date
                val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, arrayOf(selectedDate))
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                dateSpinner.adapter = adapter

                // Fetch data for selected date
                fetchAndCalculateMetrics()
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun fetchAndCalculateMetrics() {
        if (routeName1 == null || routeName2 == null) {
            Toast.makeText(this, "Route names not found", Toast.LENGTH_SHORT).show()
            android.util.Log.e("Compare2Routes", "Route names are null - Route1: $routeName1, Route2: $routeName2")
            return
        }

        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            android.util.Log.e("Compare2Routes", "User ID is null")
            return
        }

        android.util.Log.d("Compare2Routes", "Fetching metrics for routes: $routeName1 and $routeName2, User: $userId, Period: $selectedTimePeriod, Date: $selectedDate")

        // Fetch data for both routes
        fetchRouteMetrics(userId, routeName1!!, true)
        fetchRouteMetrics(userId, routeName2!!, false)
    }

    private fun fetchRouteMetrics(userId: String, routeName: String, isRoute1: Boolean) {
        android.util.Log.d("Compare2Routes", "Fetching metrics for route: $routeName (isRoute1: $isRoute1)")
        android.util.Log.d("Compare2Routes", "Using userId: $userId")

        val incomeRef = database.getReference("Route_Income").child(userId).child(routeName)
        val expensesRef = database.getReference("Route_Expenses").child(userId).child(routeName)
        val busesRef = database.getReference("Buses").child(userId)

        // First, fetch buses that belong to this route
        busesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(busesSnapshot: DataSnapshot) {
                val busPlatesForRoute = mutableListOf<String>()

                android.util.Log.d("Compare2Routes", "Buses snapshot exists: ${busesSnapshot.exists()}, Children count: ${busesSnapshot.childrenCount}")

                // Structure: Buses/userId/busId/{number_plate, payment_method, route_name}
                for (busIdSnapshot in busesSnapshot.children) {
                    val busId = busIdSnapshot.key
                    val routeNameInBus = busIdSnapshot.child("route name").value as? String
                    val numberPlate = busIdSnapshot.child("number plate").value as? String

                    android.util.Log.d("Compare2Routes", "Bus ID: $busId, Route in DB: '$routeNameInBus', Plate: $numberPlate, Looking for: '$routeName'")

                    if (routeNameInBus == routeName && numberPlate != null) {
                        busPlatesForRoute.add(numberPlate)
                        android.util.Log.d("Compare2Routes", "✓ MATCH! Added bus plate: $numberPlate to route $routeName")
                    } else if (routeNameInBus != null && numberPlate != null) {
                        android.util.Log.d("Compare2Routes", "✗ No match. Route '$routeNameInBus' != '$routeName'")
                    }
                }

                android.util.Log.d("Compare2Routes", "===== SUMMARY: Found ${busPlatesForRoute.size} buses for route '$routeName': $busPlatesForRoute =====")

                // Now fetch income and expenses
                incomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(incomeSnapshot: DataSnapshot) {
                        android.util.Log.d("Compare2Routes", "Income snapshot exists: ${incomeSnapshot.exists()}, Children: ${incomeSnapshot.childrenCount}")

                        expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(expensesSnapshot: DataSnapshot) {
                                android.util.Log.d("Compare2Routes", "Expenses snapshot exists: ${expensesSnapshot.exists()}, Children: ${expensesSnapshot.childrenCount}")

                                val metrics = calculateMetrics(incomeSnapshot, expensesSnapshot)
                                android.util.Log.d("Compare2Routes", "Calculated metrics for $routeName - Revenue: ${metrics.revenue}, Expense: ${metrics.expense}, Trips: ${metrics.tripVolume}")

                                // Calculate revenue per bus
                                calculateRevenuePerBus(userId, busPlatesForRoute, metrics, isRoute1)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@Compare2RoutesActivity,
                                    "Failed to load expenses for $routeName",
                                    Toast.LENGTH_SHORT
                                ).show()
                                android.util.Log.e("Compare2Routes", "Error loading expenses: ${error.message}")
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@Compare2RoutesActivity,
                            "Failed to load income for $routeName",
                            Toast.LENGTH_SHORT
                        ).show()
                        android.util.Log.e("Compare2Routes", "Error loading income: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@Compare2RoutesActivity,
                    "Failed to load buses for $routeName",
                    Toast.LENGTH_SHORT
                ).show()
                android.util.Log.e("Compare2Routes", "Error loading buses: ${error.message}")
            }
        })
    }

    private fun calculateMetrics(
        incomeSnapshot: DataSnapshot,
        expensesSnapshot: DataSnapshot
    ): RouteMetrics {
        var totalRevenue = 0.0
        var totalExpense = 0.0
        var totalTrips = 0

        android.util.Log.d("Compare2Routes", "Calculating metrics - Income children: ${incomeSnapshot.childrenCount}, Expense children: ${expensesSnapshot.childrenCount}")

        // Parse income data - ACTUAL structure: routeName/date/tripId/{income, num_trips}
        // NOT: routeName/busPlate/date/tripId (no busPlate level in Route_Income!)
        for (dateSnapshot in incomeSnapshot.children) {
            val dateKey = dateSnapshot.key ?: continue
            android.util.Log.d("Compare2Routes", "Processing income date: $dateKey")

            if (isDateInRangeForIncome(dateKey)) {
                android.util.Log.d("Compare2Routes", "Date $dateKey is in range")
                for (tripSnapshot in dateSnapshot.children) {
                    val tripData = tripSnapshot.value as? Map<*, *> ?: continue
                    val income = (tripData["income"] as? Number)?.toDouble() ?: 0.0
                    val numTrips = (tripData["num_trips"] as? Long)?.toInt() ?: 0

                    totalRevenue += income
                    totalTrips += numTrips
                    android.util.Log.d("Compare2Routes", "Added income: $income, trips: $numTrips")
                }
            } else {
                android.util.Log.d("Compare2Routes", "Date $dateKey is NOT in range")
            }
        }

        // Parse expenses data - structure: routeName/MM-yyyy/total_expenses
        for (monthSnapshot in expensesSnapshot.children) {
            val monthKey = monthSnapshot.key ?: continue
            android.util.Log.d("Compare2Routes", "Processing expense month: $monthKey")

            if (isDateInRangeForExpenses(monthKey)) {
                android.util.Log.d("Compare2Routes", "Month $monthKey is in range")
                val expense = (monthSnapshot.child("total_expenses").value as? Number)?.toDouble() ?: 0.0
                totalExpense += expense
                android.util.Log.d("Compare2Routes", "Added expense: $expense")
            } else {
                android.util.Log.d("Compare2Routes", "Month $monthKey is NOT in range")
            }
        }

        val profitMargin = if (totalRevenue > 0) {
            ((totalRevenue - totalExpense) / totalRevenue) * 100
        } else {
            0.0
        }

        android.util.Log.d("Compare2Routes", "Final metrics - Revenue: $totalRevenue, Expense: $totalExpense, Trips: $totalTrips, Profit: $profitMargin%")

        // Revenue per bus will be calculated after fetching bus plates from Buses node
        val revenuePerBus = 0.0 // Placeholder

        return RouteMetrics(
            revenue = totalRevenue,
            expense = totalExpense,
            profitMargin = profitMargin,
            tripVolume = totalTrips,
            revenuePerBus = revenuePerBus
        )
    }

    private fun isDateInRangeForIncome(dateKey: String): Boolean {
        // dateKey format: dd-MM-yyyy
        try {
            val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val keyDate = dateFormat.parse(dateKey) ?: return false
            val keyCal = Calendar.getInstance().apply { time = keyDate }

            when (selectedTimePeriod) {
                "Day" -> {
                    val targetFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val selectedDateParsed = targetFormat.parse(selectedDate) ?: return false
                    val selectedCal = Calendar.getInstance().apply { time = selectedDateParsed }

                    return keyCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR) &&
                            keyCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
                }
                "Month" -> {
                    val targetFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val selectedDateParsed = targetFormat.parse(selectedDate) ?: return false
                    val selectedCal = Calendar.getInstance().apply { time = selectedDateParsed }

                    return keyCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
                            keyCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
                }
                "Year" -> {
                    val selectedYear = selectedDate.toIntOrNull() ?: return false
                    return keyCal.get(Calendar.YEAR) == selectedYear
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun isDateInRangeForExpenses(monthKey: String): Boolean {
        // monthKey format: MM-yyyy
        try {
            val monthFormat = SimpleDateFormat("MM-yyyy", Locale.getDefault())
            val keyDate = monthFormat.parse(monthKey) ?: return false
            val keyCal = Calendar.getInstance().apply { time = keyDate }

            when (selectedTimePeriod) {
                "Day" -> {
                    val targetFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val selectedDateParsed = targetFormat.parse(selectedDate) ?: return false
                    val selectedCal = Calendar.getInstance().apply { time = selectedDateParsed }

                    return keyCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
                            keyCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
                }
                "Month" -> {
                    val targetFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                    val selectedDateParsed = targetFormat.parse(selectedDate) ?: return false
                    val selectedCal = Calendar.getInstance().apply { time = selectedDateParsed }

                    return keyCal.get(Calendar.MONTH) == selectedCal.get(Calendar.MONTH) &&
                            keyCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
                }
                "Year" -> {
                    val selectedYear = selectedDate.toIntOrNull() ?: return false
                    return keyCal.get(Calendar.YEAR) == selectedYear
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun isSpecificDateInRange(dateStr: String): Boolean {
        // dateStr format: dd/MM/yyyy
        if (selectedTimePeriod != "Day") return true

        try {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val expenseDate = dateFormat.parse(dateStr) ?: return false
            val expenseCal = Calendar.getInstance().apply { time = expenseDate }

            val selectedDateParsed = dateFormat.parse(selectedDate) ?: return false
            val selectedCal = Calendar.getInstance().apply { time = selectedDateParsed }

            return expenseCal.get(Calendar.DAY_OF_YEAR) == selectedCal.get(Calendar.DAY_OF_YEAR) &&
                    expenseCal.get(Calendar.YEAR) == selectedCal.get(Calendar.YEAR)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun calculateRevenuePerBus(
        userId: String,
        busPlates: List<String>,
        metrics: RouteMetrics,
        isRoute1: Boolean
    ) {
        android.util.Log.d("Compare2Routes", "Calculating revenue per bus - Bus count: ${busPlates.size}, Total Revenue: ${metrics.revenue}")

        if (busPlates.isEmpty()) {
            android.util.Log.d("Compare2Routes", "No buses found for this route, setting revenue per bus to 0")
            displayMetrics(metrics.copy(revenuePerBus = 0.0), isRoute1)
            return
        }

        // Simply divide total revenue by number of buses
        val revenuePerBus = metrics.revenue / busPlates.size

        android.util.Log.d("Compare2Routes", "Revenue per bus calculated: $revenuePerBus (${metrics.revenue} / ${busPlates.size})")
        android.util.Log.d("Compare2Routes", "Buses for this route: $busPlates")

        displayMetrics(metrics.copy(revenuePerBus = revenuePerBus), isRoute1)
    }

    private fun displayMetrics(metrics: RouteMetrics, isRoute1: Boolean) {
        android.util.Log.d("Compare2Routes", "Displaying metrics for Route${if(isRoute1) "1" else "2"} - Revenue: ${metrics.revenue}, Expense: ${metrics.expense}, Profit: ${metrics.profitMargin}%, Trips: ${metrics.tripVolume}, RevPerBus: ${metrics.revenuePerBus}")

        if (isRoute1) {
            route1RevenueTextView.text = "KSh ${String.format("%.2f", metrics.revenue)}"
            route1ExpenseTextView.text = "KSh ${String.format("%.2f", metrics.expense)}"
            route1ProfitMarginTextView.text = "${String.format("%.1f", metrics.profitMargin)}%"
            route1TripVolumeTextView.text = "${metrics.tripVolume}"
            route1RevenuePerBusTextView.text = "KSh ${String.format("%.2f", metrics.revenuePerBus)}"
        } else {
            route2RevenueTextView.text = "KSh ${String.format("%.2f", metrics.revenue)}"
            route2ExpenseTextView.text = "KSh ${String.format("%.2f", metrics.expense)}"
            route2ProfitMarginTextView.text = "${String.format("%.1f", metrics.profitMargin)}%"
            route2TripVolumeTextView.text = "${metrics.tripVolume}"
            route2RevenuePerBusTextView.text = "KSh ${String.format("%.2f", metrics.revenuePerBus)}"
        }

        android.util.Log.d("Compare2Routes", "Display completed for Route${if(isRoute1) "1" else "2"}")
    }

    data class RouteMetrics(
        val revenue: Double,
        val expense: Double,
        val profitMargin: Double,
        val tripVolume: Int,
        val revenuePerBus: Double
    )
}