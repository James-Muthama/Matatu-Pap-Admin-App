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

class TotalRoutesPerformanceActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Spinners
    private lateinit var timePeriodSpinner: Spinner
    private lateinit var dateSpinner: Spinner

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

    // Data variables
    private var selectedTimePeriod: String = "Day"
    private var selectedDate: String = ""
    private var selectedCalendar = Calendar.getInstance()
    private var allRouteNames = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.display_total_route_fleet_performance_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize all views
        initializeViews()

        // Setup spinners
        setupTimePeriodSpinner()

        // Setup navigation
        setupNavigation()

        // Fetch all routes first, then calculate metrics
        fetchAllRoutes()
    }

    private fun initializeViews() {
        // Spinners
        timePeriodSpinner = findViewById(R.id.timePeriodSpinner)
        dateSpinner = findViewById(R.id.dateSpinner)

        // Metric TextViews
        totalRevenueTextView = findViewById(R.id.textView_2)           // Total Revenue
        totalExpensesTextView = findViewById(R.id.textView_4)          // Total Expenses
        profitMarginTextView = findViewById(R.id.textView6)            // Profit Margin
        tripVolumeTextView = findViewById(R.id.textView8_)             // Trip Volume
        revenuePerBusTextView = findViewById(R.id.textView10)          // Revenue per Bus

        // Navigation
        backIcon = findViewById(R.id.back_icon)
        homeIconCard = findViewById(R.id.home_icon_card)
        receiptsIconCard = findViewById(R.id.receipts_icon_card)
        profileIconCard = findViewById(R.id.profile_icon_card)

        android.util.Log.d("TotalRoutesPerf", "Views initialized successfully")
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
                android.util.Log.d("TotalRoutesPerf", "Time period selected: $selectedTimePeriod")
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
                android.util.Log.d("TotalRoutesPerf", "Triggering initial data fetch for Day")
                if (allRouteNames.isNotEmpty()) {
                    fetchAllRoutesMetrics()
                }
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
                        android.util.Log.d("TotalRoutesPerf", "Month selected: $selectedDate")
                        if (allRouteNames.isNotEmpty()) {
                            fetchAllRoutesMetrics()
                        }
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
                        android.util.Log.d("TotalRoutesPerf", "Year selected: $selectedDate")
                        if (allRouteNames.isNotEmpty()) {
                            fetchAllRoutesMetrics()
                        }
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

                android.util.Log.d("TotalRoutesPerf", "Date selected: $selectedDate")
                // Fetch data for selected date
                if (allRouteNames.isNotEmpty()) {
                    fetchAllRoutesMetrics()
                }
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    private fun fetchAllRoutes() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val routesRef = database.getReference("Routes").child(userId)
        routesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allRouteNames.clear()

                if (!snapshot.exists()) {
                    Toast.makeText(
                        this@TotalRoutesPerformanceActivity,
                        "No routes found",
                        Toast.LENGTH_SHORT
                    ).show()
                    displayMetrics(FleetRouteMetrics(0.0, 0.0, 0.0, 0, 0.0))
                    return
                }

                // Structure: Routes/userId/routeId/{name, ...}
                for (routeSnapshot in snapshot.children) {
                    // Try both "name" and "route name" for compatibility
                    var routeName = routeSnapshot.child("name").value as? String
                    if (routeName == null) {
                        routeName = routeSnapshot.child("route name").value as? String
                    }

                    if (routeName != null) {
                        allRouteNames.add(routeName)
                        android.util.Log.d("TotalRoutesPerf", "Added route: $routeName")
                    }
                }

                android.util.Log.d("TotalRoutesPerf", "Found ${allRouteNames.size} routes: $allRouteNames")

                if (allRouteNames.isNotEmpty()) {
                    fetchAllRoutesMetrics()
                } else {
                    displayMetrics(FleetRouteMetrics(0.0, 0.0, 0.0, 0, 0.0))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@TotalRoutesPerformanceActivity,
                    "Failed to load routes: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun fetchAllRoutesMetrics() {
        val userId = auth.currentUser?.uid ?: return

        if (allRouteNames.isEmpty()) {
            displayMetrics(FleetRouteMetrics(0.0, 0.0, 0.0, 0, 0.0))
            return
        }

        android.util.Log.d("TotalRoutesPerf", "Fetching metrics for ${allRouteNames.size} routes")

        var aggregatedRevenue = 0.0
        var aggregatedExpenses = 0.0
        var aggregatedTrips = 0
        var totalBusCount = 0
        var completedRoutes = 0

        allRouteNames.forEach { routeName ->
            fetchSingleRouteMetrics(userId, routeName) { metrics, busCount ->
                aggregatedRevenue += metrics.revenue
                aggregatedExpenses += metrics.expense
                aggregatedTrips += metrics.tripVolume
                totalBusCount += busCount
                completedRoutes++

                android.util.Log.d("TotalRoutesPerf", "Route '$routeName' completed. Progress: $completedRoutes/${allRouteNames.size}")

                // When all routes are processed
                if (completedRoutes == allRouteNames.size) {
                    val profitMargin = if (aggregatedRevenue > 0) {
                        ((aggregatedRevenue - aggregatedExpenses) / aggregatedRevenue) * 100
                    } else {
                        0.0
                    }

                    val revenuePerBus = if (totalBusCount > 0) {
                        aggregatedRevenue / totalBusCount
                    } else {
                        0.0
                    }

                    val finalMetrics = FleetRouteMetrics(
                        revenue = aggregatedRevenue,
                        expense = aggregatedExpenses,
                        profitMargin = profitMargin,
                        tripVolume = aggregatedTrips,
                        revenuePerBus = revenuePerBus
                    )

                    android.util.Log.d("TotalRoutesPerf", "All routes processed. Final metrics: $finalMetrics")
                    displayMetrics(finalMetrics)
                }
            }
        }
    }

    private fun fetchSingleRouteMetrics(
        userId: String,
        routeName: String,
        onComplete: (RouteMetrics, Int) -> Unit
    ) {
        val incomeRef = database.getReference("Route_Income").child(userId).child(routeName)
        val expensesRef = database.getReference("Route_Expenses").child(userId).child(routeName)
        val busesRef = database.getReference("Buses").child(userId)

        // First, fetch buses that belong to this route
        busesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(busesSnapshot: DataSnapshot) {
                val busPlatesForRoute = mutableListOf<String>()

                // Structure: Buses/userId/busId/{number_plate, payment_method, route_name}
                for (busIdSnapshot in busesSnapshot.children) {
                    val routeNameInBus = busIdSnapshot.child("route name").value as? String
                    val numberPlate = busIdSnapshot.child("number plate").value as? String

                    if (routeNameInBus == routeName && numberPlate != null) {
                        busPlatesForRoute.add(numberPlate)
                    }
                }

                val busCount = busPlatesForRoute.size
                android.util.Log.d("TotalRoutesPerf", "Route '$routeName' has $busCount buses")

                // Now fetch income and expenses
                incomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(incomeSnapshot: DataSnapshot) {
                        expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(expensesSnapshot: DataSnapshot) {
                                val metrics = calculateMetrics(incomeSnapshot, expensesSnapshot)
                                android.util.Log.d("TotalRoutesPerf", "Route '$routeName' metrics - Revenue: ${metrics.revenue}, Expense: ${metrics.expense}, Trips: ${metrics.tripVolume}")
                                onComplete(metrics, busCount)
                            }

                            override fun onCancelled(error: DatabaseError) {
                                android.util.Log.e("TotalRoutesPerf", "Error loading expenses for $routeName: ${error.message}")
                                onComplete(RouteMetrics(0.0, 0.0, 0.0, 0, 0.0), busCount)
                            }
                        })
                    }

                    override fun onCancelled(error: DatabaseError) {
                        android.util.Log.e("TotalRoutesPerf", "Error loading income for $routeName: ${error.message}")
                        onComplete(RouteMetrics(0.0, 0.0, 0.0, 0, 0.0), busCount)
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.e("TotalRoutesPerf", "Error loading buses for $routeName: ${error.message}")
                onComplete(RouteMetrics(0.0, 0.0, 0.0, 0, 0.0), 0)
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

        // Parse income data - structure: routeName/dd-MM-yyyy/tripId/{income, num_trips}
        for (dateSnapshot in incomeSnapshot.children) {
            val dateKey = dateSnapshot.key ?: continue

            if (isDateInRangeForIncome(dateKey)) {
                for (tripSnapshot in dateSnapshot.children) {
                    val tripData = tripSnapshot.value as? Map<*, *> ?: continue
                    val income = (tripData["income"] as? Number)?.toDouble() ?: 0.0
                    val numTrips = (tripData["num_trips"] as? Long)?.toInt() ?: 0

                    totalRevenue += income
                    totalTrips += numTrips
                }
            }
        }

        // Parse expenses data - structure: routeName/MM-yyyy/total_expenses
        for (monthSnapshot in expensesSnapshot.children) {
            val monthKey = monthSnapshot.key ?: continue

            if (isDateInRangeForExpenses(monthKey)) {
                val expense = (monthSnapshot.child("total_expenses").value as? Number)?.toDouble() ?: 0.0
                totalExpense += expense
            }
        }

        val profitMargin = if (totalRevenue > 0) {
            ((totalRevenue - totalExpense) / totalRevenue) * 100
        } else {
            0.0
        }

        return RouteMetrics(
            revenue = totalRevenue,
            expense = totalExpense,
            profitMargin = profitMargin,
            tripVolume = totalTrips,
            revenuePerBus = 0.0 // This will be calculated at fleet level
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

    private fun displayMetrics(metrics: FleetRouteMetrics) {
        android.util.Log.d("TotalRoutesPerf", "Displaying fleet metrics: $metrics")

        totalRevenueTextView.text = "KSh ${String.format("%.2f", metrics.revenue)}"
        totalExpensesTextView.text = "KSh ${String.format("%.2f", metrics.expense)}"
        profitMarginTextView.text = "${String.format("%.1f", metrics.profitMargin)}%"
        tripVolumeTextView.text = "${metrics.tripVolume}"
        revenuePerBusTextView.text = "KSh ${String.format("%.2f", metrics.revenuePerBus)}"

        Toast.makeText(this, "Fleet metrics loaded successfully", Toast.LENGTH_SHORT).show()
    }

    data class RouteMetrics(
        val revenue: Double,
        val expense: Double,
        val profitMargin: Double,
        val tripVolume: Int,
        val revenuePerBus: Double
    )

    data class FleetRouteMetrics(
        val revenue: Double,
        val expense: Double,
        val profitMargin: Double,
        val tripVolume: Int,
        val revenuePerBus: Double
    )
}