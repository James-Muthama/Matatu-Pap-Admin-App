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

class DisplayRoutePerformanceActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Spinners
    private lateinit var timePeriodSpinner: Spinner
    private lateinit var dateSpinner: Spinner

    // Route Name TextView
    private lateinit var route1NameTextView: TextView

    // Metric TextViews
    private lateinit var totalRevenueTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var profitMarginTextView: TextView
    private lateinit var tripVolumeTextView: TextView
    private lateinit var revenuePerBusTextView: TextView

    // Navigation
    private lateinit var backIcon: ImageView
    private lateinit var homeIconCard: CardView
    private lateinit var receiptsIconCard: CardView
    private lateinit var profileIconCard: CardView

    // Route Selection Card
    private lateinit var route1SelectCard: CardView

    // Data variables
    private var routeName: String? = null
    private var selectedTimePeriod: String = "Day"
    private var selectedDate: String = ""
    private var selectedCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.display_route_performance_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get route name from intent
        val routeNames = intent.getStringArrayExtra("routeNames")
        routeName = routeNames?.firstOrNull()

        // Initialize all views
        initializeViews()

        // Setup spinners
        setupTimePeriodSpinner()

        // Display route name
        route1NameTextView.text = routeName ?: "Select Route"

        // Setup navigation
        setupNavigation()

        // Setup route selection card click
        route1SelectCard.setOnClickListener {
            finish() // Go back to selection screen
        }
    }

    private fun initializeViews() {
        // Spinners
        timePeriodSpinner = findViewById(R.id.timePeriodSpinner)
        dateSpinner = findViewById(R.id.dateSpinner)

        // Route Name TextView
        route1NameTextView = findViewById(R.id.bus1PlateTextView)

        // Metric TextViews (mapped to the display_bus_performance_page layout)
        totalRevenueTextView = findViewById(R.id.bus2TotalTripsTextView)        // Total Revenue
        totalExpensesTextView = findViewById(R.id.bus2TotalIncomeTextView)      // Total Expenses
        profitMarginTextView = findViewById(R.id.bus2TotalExpensesTextView)     // Profit Margin
        tripVolumeTextView = findViewById(R.id.bus2NetProfitTextView)           // Trip Volume
        revenuePerBusTextView = findViewById(R.id.bus2ProfitMarginTextView)     // Revenue per Bus

        // Navigation
        backIcon = findViewById(R.id.backIcon)
        homeIconCard = findViewById(R.id.homeIconCard)
        receiptsIconCard = findViewById(R.id.receiptsIconCard)
        profileIconCard = findViewById(R.id.profileIconCard)

        // Route Selection Card
        route1SelectCard = findViewById(R.id.bus1SelectCard)
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
        if (routeName == null) {
            Toast.makeText(this, "Route name not found", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        fetchRouteMetrics(userId, routeName!!)
    }

    private fun fetchRouteMetrics(userId: String, routeName: String) {
        android.util.Log.d("DisplayRoutePerf", "Fetching metrics for route: $routeName")

        val incomeRef = database.getReference("Route_Income").child(userId).child(routeName)
        val expensesRef = database.getReference("Route_Expenses").child(userId).child(routeName)
        val busesRef = database.getReference("Buses").child(userId)

        // First, fetch buses that belong to this route
        busesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(busesSnapshot: DataSnapshot) {
                val busPlatesForRoute = mutableListOf<String>()

                android.util.Log.d("DisplayRoutePerf", "Buses snapshot exists: ${busesSnapshot.exists()}, Children count: ${busesSnapshot.childrenCount}")

                // Structure: Buses/userId/busId/{number_plate, payment_method, route_name}
                for (busIdSnapshot in busesSnapshot.children) {
                    val routeNameInBus = busIdSnapshot.child("route name").value as? String
                    val numberPlate = busIdSnapshot.child("number plate").value as? String

                    if (routeNameInBus == routeName && numberPlate != null) {
                        busPlatesForRoute.add(numberPlate)
                        android.util.Log.d("DisplayRoutePerf", "✓ Added bus plate: $numberPlate to route $routeName")
                    }
                }

                android.util.Log.d("DisplayRoutePerf", "Found ${busPlatesForRoute.size} buses for route '$routeName': $busPlatesForRoute")

                // Now fetch income and expenses
                incomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(incomeSnapshot: DataSnapshot) {
                        android.util.Log.d("DisplayRoutePerf", "Income snapshot exists: ${incomeSnapshot.exists()}, Children: ${incomeSnapshot.childrenCount}")

                        expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(expensesSnapshot: DataSnapshot) {
                                android.util.Log.d("DisplayRoutePerf", "Expenses snapshot exists: ${expensesSnapshot.exists()}, Children: ${expensesSnapshot.childrenCount}")

                                val metrics = calculateMetrics(incomeSnapshot, expensesSnapshot, busPlatesForRoute.size)
                                android.util.Log.d("DisplayRoutePerf", "Calculated metrics - Revenue: ${metrics.revenue}, Expense: ${metrics.expense}, Trips: ${metrics.tripVolume}")

                                displayMetrics(metrics)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                Toast.makeText(
                                    this@DisplayRoutePerformanceActivity,
                                    "Failed to load expenses for $routeName",
                                    Toast.LENGTH_SHORT
                                ).show()
                                android.util.Log.e("DisplayRoutePerf", "Error loading expenses: ${error.message}")
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@DisplayRoutePerformanceActivity,
                            "Failed to load income for $routeName",
                            Toast.LENGTH_SHORT
                        ).show()
                        android.util.Log.e("DisplayRoutePerf", "Error loading income: ${error.message}")
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DisplayRoutePerformanceActivity,
                    "Failed to load buses for $routeName",
                    Toast.LENGTH_SHORT
                ).show()
                android.util.Log.e("DisplayRoutePerf", "Error loading buses: ${error.message}")
            }
        })
    }

    private fun calculateMetrics(
        incomeSnapshot: DataSnapshot,
        expensesSnapshot: DataSnapshot,
        busCount: Int
    ): RouteMetrics {
        var totalRevenue = 0.0
        var totalExpense = 0.0
        var totalTrips = 0

        android.util.Log.d("DisplayRoutePerf", "Calculating metrics - Income children: ${incomeSnapshot.childrenCount}, Expense children: ${expensesSnapshot.childrenCount}")

        // Parse income data - structure: routeName/dd-MM-yyyy/tripId/{income, num_trips}
        for (dateSnapshot in incomeSnapshot.children) {
            val dateKey = dateSnapshot.key ?: continue
            android.util.Log.d("DisplayRoutePerf", "Processing income date: $dateKey")

            if (isDateInRangeForIncome(dateKey)) {
                android.util.Log.d("DisplayRoutePerf", "Date $dateKey is in range")
                for (tripSnapshot in dateSnapshot.children) {
                    val tripData = tripSnapshot.value as? Map<*, *> ?: continue
                    val income = (tripData["income"] as? Number)?.toDouble() ?: 0.0
                    val numTrips = (tripData["num_trips"] as? Long)?.toInt() ?: 0

                    totalRevenue += income
                    totalTrips += numTrips
                    android.util.Log.d("DisplayRoutePerf", "Added income: $income, trips: $numTrips")
                }
            } else {
                android.util.Log.d("DisplayRoutePerf", "Date $dateKey is NOT in range")
            }
        }

        // Parse expenses data - structure: routeName/MM-yyyy/total_expenses
        for (monthSnapshot in expensesSnapshot.children) {
            val monthKey = monthSnapshot.key ?: continue
            android.util.Log.d("DisplayRoutePerf", "Processing expense month: $monthKey")

            if (isDateInRangeForExpenses(monthKey)) {
                android.util.Log.d("DisplayRoutePerf", "Month $monthKey is in range")
                val expense = (monthSnapshot.child("total_expenses").value as? Number)?.toDouble() ?: 0.0
                totalExpense += expense
                android.util.Log.d("DisplayRoutePerf", "Added expense: $expense")
            } else {
                android.util.Log.d("DisplayRoutePerf", "Month $monthKey is NOT in range")
            }
        }

        val profitMargin = if (totalRevenue > 0) {
            ((totalRevenue - totalExpense) / totalRevenue) * 100
        } else {
            0.0
        }

        // Calculate revenue per bus
        val revenuePerBus = if (busCount > 0) {
            totalRevenue / busCount
        } else {
            0.0
        }

        android.util.Log.d("DisplayRoutePerf", "Final metrics - Revenue: $totalRevenue, Expense: $totalExpense, Trips: $totalTrips, Profit: $profitMargin%, RevPerBus: $revenuePerBus")

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
                    // For a specific day, check if it belongs to this month
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

    private fun displayMetrics(metrics: RouteMetrics) {
        android.util.Log.d("DisplayRoutePerf", "Displaying metrics - Revenue: ${metrics.revenue}, Expense: ${metrics.expense}, Profit: ${metrics.profitMargin}%, Trips: ${metrics.tripVolume}, RevPerBus: ${metrics.revenuePerBus}")

        totalRevenueTextView.text = "KSh ${String.format("%.2f", metrics.revenue)}"
        totalExpensesTextView.text = "KSh ${String.format("%.2f", metrics.expense)}"
        profitMarginTextView.text = "${String.format("%.1f", metrics.profitMargin)}%"
        tripVolumeTextView.text = "${metrics.tripVolume}"
        revenuePerBusTextView.text = "KSh ${String.format("%.2f", metrics.revenuePerBus)}"
    }

    data class RouteMetrics(
        val revenue: Double,
        val expense: Double,
        val profitMargin: Double,
        val tripVolume: Int,
        val revenuePerBus: Double
    )
}