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

class Compare2BussesActivity : AppCompatActivity() {

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    // Spinners
    private lateinit var timePeriodSpinner: Spinner
    private lateinit var dateSpinner: Spinner

    // Bus Plate TextViews
    private lateinit var bus1PlateTextView: TextView
    private lateinit var bus2PlateTextView: TextView

    // Bus 1 Metric TextViews
    private lateinit var bus1TotalTripsTextView: TextView
    private lateinit var bus1TotalIncomeTextView: TextView
    private lateinit var bus1TotalExpensesTextView: TextView
    private lateinit var bus1NetProfitTextView: TextView
    private lateinit var bus1TripsPerDayTextView: TextView

    // Bus 2 Metric TextViews
    private lateinit var bus2TotalTripsTextView: TextView
    private lateinit var bus2TotalIncomeTextView: TextView
    private lateinit var bus2TotalExpensesTextView: TextView
    private lateinit var bus2NetProfitTextView: TextView
    private lateinit var bus2TripsPerDayTextView: TextView

    // Navigation
    private lateinit var backIcon: ImageView
    private lateinit var homeIconCard: CardView
    private lateinit var receiptsIconCard: CardView
    private lateinit var profileIconCard: CardView

    // Data variables
    private var busPlate1: String? = null
    private var busPlate2: String? = null
    private var selectedTimePeriod: String = "Day"
    private var selectedDate: String = ""
    private var selectedCalendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.compare_2_busses_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Get bus plates from intent
        busPlate1 = intent.getStringExtra("busPlate1")
        busPlate2 = intent.getStringExtra("busPlate2")

        // Initialize all views
        initializeViews()

        // Setup spinners
        setupTimePeriodSpinner()

        // Display bus plates
        bus1PlateTextView.text = busPlate1 ?: "Bus 1"
        bus2PlateTextView.text = busPlate2 ?: "Bus 2"

        // Setup navigation
        setupNavigation()
    }

    private fun initializeViews() {
        // Spinners
        timePeriodSpinner = findViewById(R.id.timePeriodSpinner)
        dateSpinner = findViewById(R.id.dateSpinner)

        // Bus Plate TextViews
        bus1PlateTextView = findViewById(R.id.bus1PlateTextView)
        bus2PlateTextView = findViewById(R.id.bus2PlateTextView)

        // Bus 1 Metric TextViews
        bus1TotalTripsTextView = findViewById(R.id.bus1TotalTripsTextView)
        bus1TotalIncomeTextView = findViewById(R.id.bus1TotalIncomeTextView)
        bus1TotalExpensesTextView = findViewById(R.id.bus1TotalExpensesTextView)
        bus1NetProfitTextView = findViewById(R.id.bus1NetProfitTextView)
        bus1TripsPerDayTextView = findViewById(R.id.bus1ProfitMarginTextView)

        // Bus 2 Metric TextViews
        bus2TotalTripsTextView = findViewById(R.id.bus2TotalTripsTextView)
        bus2TotalIncomeTextView = findViewById(R.id.bus2TotalIncomeTextView)
        bus2TotalExpensesTextView = findViewById(R.id.bus2TotalExpensesTextView)
        bus2NetProfitTextView = findViewById(R.id.bus2NetProfitTextView)
        bus2TripsPerDayTextView = findViewById(R.id.bus2ProfitMarginTextView)

        // Navigation
        backIcon = findViewById(R.id.backIcon)
        homeIconCard = findViewById(R.id.homeIconCard)
        receiptsIconCard = findViewById(R.id.receiptsIconCard)
        profileIconCard = findViewById(R.id.profileIconCard)
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
        if (busPlate1 == null || busPlate2 == null) {
            Toast.makeText(this, "Bus plates not found", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = auth.currentUser?.uid ?: return

        // Fetch data for both buses
        fetchBusMetrics(userId, busPlate1!!, true)
        fetchBusMetrics(userId, busPlate2!!, false)
    }

    private fun fetchBusMetrics(userId: String, busPlate: String, isBus1: Boolean) {
        val incomeRef = database.getReference("Bus_Income").child(userId).child(busPlate)
        val expensesRef = database.getReference("Bus_Expenses").child(userId).child(busPlate)

        incomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(incomeSnapshot: DataSnapshot) {
                expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(expensesSnapshot: DataSnapshot) {
                        val metrics = calculateMetrics(incomeSnapshot, expensesSnapshot)
                        displayMetrics(metrics, isBus1)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(
                            this@Compare2BussesActivity,
                            "Failed to load expenses for $busPlate",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@Compare2BussesActivity,
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

        // Calculate trips per day based on time period
        val tripsPerDay = when (selectedTimePeriod) {
            "Day" -> {
                // For a specific day, just show the total trips for that day
                totalTrips.toDouble()
            }
            "Month", "Year" -> {
                // For month or year, calculate average trips per day
                if (activeDaysSet.isNotEmpty()) {
                    totalTrips.toDouble() / activeDaysSet.size
                } else {
                    0.0
                }
            }
            else -> 0.0
        }

        return BusMetrics(
            totalTrips = totalTrips,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            netProfit = netProfit,
            tripsPerDay = tripsPerDay
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

    private fun displayMetrics(metrics: BusMetrics, isBus1: Boolean) {
        if (isBus1) {
            bus1TotalTripsTextView.text = "${metrics.totalTrips}"
            bus1TotalIncomeTextView.text = "KSh ${String.format("%.2f", metrics.totalIncome)}"
            bus1TotalExpensesTextView.text = "KSh ${String.format("%.2f", metrics.totalExpenses)}"
            bus1NetProfitTextView.text = "KSh ${String.format("%.2f", metrics.netProfit)}"

            // Display trips per day based on time period
            if (selectedTimePeriod == "Day") {
                bus1TripsPerDayTextView.text = "${metrics.tripsPerDay.toInt()}"
            } else {
                bus1TripsPerDayTextView.text = "${String.format("%.1f", metrics.tripsPerDay)}"
            }
        } else {
            bus2TotalTripsTextView.text = "${metrics.totalTrips}"
            bus2TotalIncomeTextView.text = "KSh ${String.format("%.2f", metrics.totalIncome)}"
            bus2TotalExpensesTextView.text = "KSh ${String.format("%.2f", metrics.totalExpenses)}"
            bus2NetProfitTextView.text = "KSh ${String.format("%.2f", metrics.netProfit)}"

            // Display trips per day based on time period
            if (selectedTimePeriod == "Day") {
                bus2TripsPerDayTextView.text = "${metrics.tripsPerDay.toInt()}"
            } else {
                bus2TripsPerDayTextView.text = "${String.format("%.1f", metrics.tripsPerDay)}"
            }
        }
    }

    data class BusMetrics(
        val totalTrips: Int,
        val totalIncome: Double,
        val totalExpenses: Double,
        val netProfit: Double,
        val tripsPerDay: Double
    )
}