package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
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

class BusPerformanceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var routeListContainer: LinearLayout
    private lateinit var routeNameInput: EditText
    private lateinit var searchBtn: ImageView
    private lateinit var checkStatsBtn: Button
    private lateinit var busStatsBtn: Button

    // Stats TextViews
    private lateinit var totalRevenueAmnt: TextView
    private lateinit var totalExpenseAmnt: TextView
    private lateinit var profitMarginText: TextView
    private lateinit var selectedBusText: TextView

    private var selectedBusPlate: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.bus_performance_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize views
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        searchBtn = findViewById(R.id.search_btn)
        routeListContainer = findViewById(R.id.route_list_container)
        routeNameInput = findViewById(R.id.route_name_input)
        checkStatsBtn = findViewById(R.id.save_payment_btn)

        // Stats TextViews
        totalRevenueAmnt = findViewById(R.id.total_revenue_amnt)
        totalExpenseAmnt = findViewById(R.id.textView9)
        profitMarginText = findViewById(R.id.stage)
        selectedBusText = findViewById(R.id.textView13)
        busStatsBtn = findViewById(R.id.save_payment_btn1)


        // Navigation
        receiptsIcon.setOnClickListener {
            startActivity(Intent(this, ReceiptsActivity::class.java))
        }

        profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        backIcon.setOnClickListener {
            finish()
        }

        homeIcon.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        // Navigate to Total Bus Performance Activity
        busStatsBtn.setOnClickListener {
            val intent = Intent(this, TotalBusPerformanceActivity::class.java)
            startActivity(intent)
        }

        // Search functionality
        searchBtn.setOnClickListener {
            val busPlate = routeNameInput.text.toString().trim()
            if (busPlate.isNotEmpty()) {
                searchBus(busPlate)
            } else {
                Toast.makeText(this, "Please enter a bus plate number", Toast.LENGTH_SHORT).show()
            }
        }

        // Check stats button - navigate to DisplayBusPerformanceActivity
        checkStatsBtn.setOnClickListener {
            if (selectedBusPlate != null) {
                val intent = Intent(this, DisplayBusPerformanceActivity::class.java)
                intent.putExtra("busPlates", arrayOf(selectedBusPlate!!))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a bus first", Toast.LENGTH_SHORT).show()
            }
        }

        // Load all buses and calculate metrics on page load
        loadAllBusesAndMetrics()
    }

    private fun loadAllBusesAndMetrics() {
        val userId = auth.currentUser?.uid ?: return

        val busesRef = database.getReference("Buses").child(userId)
        busesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(
                        this@BusPerformanceActivity,
                        "No buses found",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val busList = mutableListOf<String>()
                dataSnapshot.children.forEach { busSnapshot ->
                    val busData = busSnapshot.value as? Map<*, *> ?: return@forEach
                    val numberPlate = busData["number plate"] as? String
                    if (!numberPlate.isNullOrEmpty()) {
                        busList.add(numberPlate)
                    }
                }

                if (busList.isNotEmpty()) {
                    // Calculate metrics for all buses
                    calculateMetricsForAllBuses(busList)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@BusPerformanceActivity,
                    "Failed to load buses: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateMetricsForAllBuses(busList: List<String>) {
        val userId = auth.currentUser?.uid ?: return

        var totalIncome = 0.0
        var totalExpenses = 0.0
        var completedCount = 0
        val totalBuses = busList.size

        busList.forEach { busPlate ->
            val incomeRef = database.getReference("Bus_Income").child(userId).child(busPlate)
            val expensesRef = database.getReference("Bus_Expenses").child(userId).child(busPlate)

            incomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(incomeSnapshot: DataSnapshot) {
                    expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(expensesSnapshot: DataSnapshot) {
                            val metrics = calculateMetrics(incomeSnapshot, expensesSnapshot)
                            totalIncome += metrics.totalIncome
                            totalExpenses += metrics.totalExpenses

                            completedCount++

                            // When all buses are processed, display the totals
                            if (completedCount == totalBuses) {
                                val netProfit = totalIncome - totalExpenses
                                val profitMargin = if (totalIncome > 0) {
                                    (netProfit / totalIncome) * 100
                                } else {
                                    0.0
                                }

                                val aggregatedMetrics = BusMetrics(
                                    totalIncome = totalIncome,
                                    totalExpenses = totalExpenses,
                                    profitMargin = profitMargin
                                )
                                displayMetrics(aggregatedMetrics)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            completedCount++
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    completedCount++
                }
            })
        }
    }

    private fun searchBus(busPlate: String) {
        val userId = auth.currentUser?.uid ?: return
        val normalizedBusPlate = busPlate.replace("\\s".toRegex(), "").uppercase()

        val busesRef = database.getReference("Buses").child(userId)
        busesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                routeListContainer.removeAllViews()

                var matchFound = false
                dataSnapshot.children.forEach { busSnapshot ->
                    val busData = busSnapshot.value as? Map<*, *> ?: return@forEach
                    val numberPlate = busData["number plate"] as? String ?: return@forEach
                    val normalizedStoredPlate = numberPlate.replace("\\s".toRegex(), "").uppercase()

                    if (normalizedStoredPlate.contains(normalizedBusPlate)) {
                        displayBus(numberPlate)
                        matchFound = true
                    }
                }

                if (!matchFound) {
                    Toast.makeText(
                        this@BusPerformanceActivity,
                        "No Bus Matches the Plate Number",
                        Toast.LENGTH_SHORT
                    ).show()
                    routeListContainer.removeAllViews()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@BusPerformanceActivity,
                    "Failed to search bus: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun displayBus(numberPlate: String) {
        val inflater = LayoutInflater.from(this)
        val busView: View = inflater.inflate(R.layout.bus_routes_plate_item, routeListContainer, false)

        val busPlateTextView = busView.findViewById<TextView>(R.id.bus_plate_display)
        busPlateTextView.text = numberPlate

        busView.setOnClickListener {
            selectedBusPlate = numberPlate

            // Update the selected bus text
            selectedBusText.text = numberPlate

            Toast.makeText(this, "Selected: $numberPlate", Toast.LENGTH_SHORT).show()

            // Highlight selected bus
            for (i in 0 until routeListContainer.childCount) {
                routeListContainer.getChildAt(i).setBackgroundColor(
                    resources.getColor(android.R.color.transparent, null)
                )
            }
            busView.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
        }

        routeListContainer.addView(busView)
    }

    private fun fetchAndCalculateMetrics(busPlate: String) {
        val userId = auth.currentUser?.uid ?: return

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
                            this@BusPerformanceActivity,
                            "Failed to load expenses",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                })
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@BusPerformanceActivity,
                    "Failed to load income",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateMetrics(
        incomeSnapshot: DataSnapshot,
        expensesSnapshot: DataSnapshot
    ): BusMetrics {
        var totalIncome = 0.0
        var totalExpenses = 0.0

        // Parse income data - structure: busPlate/dd-MM-yyyy/tripId/{num_trips, income}
        for (dateSnapshot in incomeSnapshot.children) {
            for (tripSnapshot in dateSnapshot.children) {
                val tripData = tripSnapshot.value as? Map<*, *> ?: continue
                val income = (tripData["income"] as? Number)?.toDouble() ?: 0.0
                totalIncome += income
            }
        }

        // Parse expenses data - structure: busPlate/MM-yyyy/total_expenses
        for (monthSnapshot in expensesSnapshot.children) {
            val expenses = (monthSnapshot.child("total_expenses").value as? Number)?.toDouble() ?: 0.0
            totalExpenses += expenses
        }

        val netProfit = totalIncome - totalExpenses
        val profitMargin = if (totalIncome > 0) {
            (netProfit / totalIncome) * 100
        } else {
            0.0
        }

        return BusMetrics(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            profitMargin = profitMargin
        )
    }

    private fun displayMetrics(metrics: BusMetrics) {
        totalRevenueAmnt.text = "KSh ${String.format("%.2f", metrics.totalIncome)}"
        totalExpenseAmnt.text = "KSh ${String.format("%.2f", metrics.totalExpenses)}"
        profitMarginText.text = "${String.format("%.2f", metrics.profitMargin)}%"
    }

    data class BusMetrics(
        val totalIncome: Double,
        val totalExpenses: Double,
        val profitMargin: Double
    )
}