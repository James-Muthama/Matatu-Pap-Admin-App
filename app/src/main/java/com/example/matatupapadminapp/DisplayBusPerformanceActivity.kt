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

class DisplayBusPerformanceActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Spinners
    private lateinit var timePeriodSpinner: Spinner
    private lateinit var dateSpinner: Spinner

    // Bus Plate TextView
    private lateinit var bus1PlateTextView: TextView

    // Metric TextViews
    private lateinit var totalTripsTextView: TextView
    private lateinit var totalIncomeTextView: TextView
    private lateinit var totalExpensesTextView: TextView
    private lateinit var netProfitTextView: TextView
    private lateinit var profitMarginTextView: TextView

    // Navigation
    private lateinit var backIcon: ImageView
    private lateinit var homeIconCard: CardView
    private lateinit var receiptsIconCard: CardView
    private lateinit var profileIconCard: CardView

    // Bus Selection Card
    private lateinit var bus1SelectCard: CardView

    // Data variables
    private var busPlate: String? = null
    private var selectedTimePeriod: String = "Day"
    private var selectedDate: String = ""
    private var selectedCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.display_bus_performance_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get bus plate from intent
        val busPlates = intent.getStringArrayExtra("busPlates")
        busPlate = busPlates?.firstOrNull()

        // Initialize all views
        initializeViews()

        // Setup spinners
        setupTimePeriodSpinner()

        // Display bus plate
        bus1PlateTextView.text = busPlate ?: "Select Bus"

        // Setup navigation
        setupNavigation()

        // Setup bus selection card click
        bus1SelectCard.setOnClickListener {
            finish() // Go back to selection screen
        }
    }

    private fun initializeViews() {
        // Spinners
        timePeriodSpinner = findViewById(R.id.timePeriodSpinner)
        dateSpinner = findViewById(R.id.dateSpinner)

        // Bus Plate TextView
        bus1PlateTextView = findViewById(R.id.bus1PlateTextView)

        // Metric TextViews
        totalTripsTextView = findViewById(R.id.bus2TotalTripsTextView)
        totalIncomeTextView = findViewById(R.id.bus2TotalIncomeTextView)
        totalExpensesTextView = findViewById(R.id.bus2TotalExpensesTextView)
        netProfitTextView = findViewById(R.id.bus2NetProfitTextView)
        profitMarginTextView = findViewById(R.id.bus2ProfitMarginTextView)

        // Navigation
        backIcon = findViewById(R.id.backIcon)
        homeIconCard = findViewById(R.id.homeIconCard)
        receiptsIconCard = findViewById(R.id.receiptsIconCard)
        profileIconCard = findViewById(R.id.profileIconCard)

        // Bus Selection Card
        bus1SelectCard = findViewById(R.id.bus1SelectCard)
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
        if (busPlate == null) {
            Toast.makeText(this, "Bus plate not found", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        fetchBusMetrics(userId, busPlate!!)
    }

    private fun fetchBusMetrics(userId: String, busPlate: String) {
        val incomeRef = database.getReference("Bus_Income").child(userId).child(busPlate)
        val expensesRef = database.getReference("Bus_Expenses").child(userId).child(busPlate)

        incomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(incomeSnapshot: DataSnapshot) {
                expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(expensesSnapshot: DataSnapshot) {
                        val metrics = calculateMetrics(incomeSnapshot, expensesSnapshot)
                        displayMetrics(metrics)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@DisplayBusPerformanceActivity,
                            "Failed to load expenses for $busPlate",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@DisplayBusPerformanceActivity,
                    "Failed to load income for $busPlate",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateMetrics(
        incomeSnapshot: DataSnapshot,
        expensesSnapshot: DataSnapshot
    ): BusMetrics {
        var totalTrips = 0
        var totalIncome = 0.0
        var totalExpenses = 0.0
        val activeDaysSet = mutableSetOf<String>()

        // Parse income data - structure: busPlate/dd-MM-yyyy/tripId/{num_trips, income}
        for (dateSnapshot in incomeSnapshot.children) {
            val dateKey = dateSnapshot.key ?: continue

            if (isDateInRangeForIncome(dateKey)) {
                for (tripSnapshot in dateSnapshot.children) {
                    val tripData = tripSnapshot.value as? Map<*, *> ?: continue
                    val numTrips = (tripData["num_trips"] as? Long)?.toInt() ?: 0
                    val income = (tripData["income"] as? Number)?.toDouble() ?: 0.0

                    totalTrips += numTrips
                    totalIncome += income
                    activeDaysSet.add(dateKey)
                }
            }
        }

        // Parse expenses data - structure: busPlate/MM-yyyy/total_expenses
        for (monthSnapshot in expensesSnapshot.children) {
            val monthKey = monthSnapshot.key ?: continue

            if (isDateInRangeForExpenses(monthKey)) {
                val expenses = (monthSnapshot.child("total_expenses").value as? Number)?.toDouble() ?: 0.0
                totalExpenses += expenses
            }
        }

        val netProfit = totalIncome - totalExpenses
        val profitMargin = if (totalIncome > 0) {
            (netProfit / totalIncome) * 100
        } else {
            0.0
        }

        return BusMetrics(
            totalTrips = totalTrips,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            profitMargin = profitMargin
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

    private fun displayMetrics(metrics: BusMetrics) {
        totalTripsTextView.text = "${metrics.totalTrips}"
        totalIncomeTextView.text = "KSh ${String.format("%.2f", metrics.totalIncome)}"
        totalExpensesTextView.text = "KSh ${String.format("%.2f", metrics.totalExpenses)}"
        netProfitTextView.text = "KSh ${String.format("%.2f", metrics.netProfit)}"
        profitMarginTextView.text = "${String.format("%.2f", metrics.profitMargin)}%"
    }

    data class BusMetrics(
        val totalTrips: Int,
        val totalIncome: Double,
        val totalExpenses: Double,
        val netProfit: Double,
        val profitMargin: Double
    )
}