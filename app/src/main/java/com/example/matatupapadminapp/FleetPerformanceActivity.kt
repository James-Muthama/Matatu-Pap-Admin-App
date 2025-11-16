package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
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

class FleetPerformanceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var busDatabase: DatabaseReference
    private lateinit var routeDatabase: DatabaseReference

    // UI elements
    private lateinit var totalRevenueAmnt: TextView
    private lateinit var totalExpenseAmnt: TextView
    private lateinit var profitMarginText: TextView
    private lateinit var routesCountTextView: TextView
    private lateinit var busesCountTextView: TextView
    private lateinit var busStatsBtn: Button
    private lateinit var routesStatsBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.fleet_performance_page)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            routeDatabase = database.getReference("Routes").child(currentUser.uid)
            busDatabase = database.getReference("Buses").child(currentUser.uid)
        } else {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Initialize views
        initializeViews()

        // Setup navigation
        setupNavigation()

        // Load data
        loadRoutesCount()
        loadBusesCount()
        loadFleetMetrics()
    }

    private fun initializeViews() {
        totalRevenueAmnt = findViewById(R.id.total_revenue_amnt)
        totalExpenseAmnt = findViewById(R.id.textView9)
        profitMarginText = findViewById(R.id.stage)
        routesCountTextView = findViewById(R.id.routes_count)
        busesCountTextView = findViewById(R.id.buses_count)
        busStatsBtn = findViewById(R.id.bus_stats_btn)
        routesStatsBtn = findViewById(R.id.routes_stats_btn)
    }

    private fun setupNavigation() {
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        homeIcon.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }

        backIcon.setOnClickListener {
            finish()
        }

        profileIcon.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        receiptsIcon.setOnClickListener {
            startActivity(Intent(this, ReceiptsActivity::class.java))
        }

        // Navigate to Total Bus Performance Activity
        busStatsBtn.setOnClickListener {
            val intent = Intent(this, TotalBusPerformanceActivity::class.java)
            startActivity(intent)
        }

        // Navigate to Total Routes Performance Activity
        routesStatsBtn.setOnClickListener {
            val intent = Intent(this, TotalRoutesPerformanceActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadRoutesCount() {
        routeDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val count = dataSnapshot.childrenCount
                routesCountTextView.text = count.toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@FleetPerformanceActivity,
                    "Failed to load routes count",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadBusesCount() {
        busDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val count = dataSnapshot.childrenCount
                busesCountTextView.text = count.toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@FleetPerformanceActivity,
                    "Failed to load buses count",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun loadFleetMetrics() {
        val userId = auth.currentUser?.uid ?: return

        // First, get all buses
        busDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    displayMetrics(FleetMetrics(0.0, 0.0, 0.0))
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
                    calculateFleetMetrics(busList)
                } else {
                    displayMetrics(FleetMetrics(0.0, 0.0, 0.0))
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@FleetPerformanceActivity,
                    "Failed to load fleet data",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateFleetMetrics(busList: List<String>) {
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

                                val aggregatedMetrics = FleetMetrics(
                                    totalIncome = totalIncome,
                                    totalExpenses = totalExpenses,
                                    profitMargin = profitMargin
                                )
                                displayMetrics(aggregatedMetrics)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            completedCount++
                            if (completedCount == totalBuses) {
                                val netProfit = totalIncome - totalExpenses
                                val profitMargin = if (totalIncome > 0) {
                                    (netProfit / totalIncome) * 100
                                } else {
                                    0.0
                                }
                                displayMetrics(FleetMetrics(totalIncome, totalExpenses, profitMargin))
                            }
                        }
                    })
                }

                override fun onCancelled(error: DatabaseError) {
                    completedCount++
                    if (completedCount == totalBuses) {
                        val netProfit = totalIncome - totalExpenses
                        val profitMargin = if (totalIncome > 0) {
                            (netProfit / totalIncome) * 100
                        } else {
                            0.0
                        }
                        displayMetrics(FleetMetrics(totalIncome, totalExpenses, profitMargin))
                    }
                }
            })
        }
    }

    private fun calculateMetrics(
        incomeSnapshot: DataSnapshot,
        expensesSnapshot: DataSnapshot
    ): FleetMetrics {
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

        return FleetMetrics(
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            profitMargin = profitMargin
        )
    }

    private fun displayMetrics(metrics: FleetMetrics) {
        totalRevenueAmnt.text = "KSh ${String.format("%.2f", metrics.totalIncome)}"
        totalExpenseAmnt.text = "KSh ${String.format("%.2f", metrics.totalExpenses)}"
        profitMarginText.text = "${String.format("%.2f", metrics.profitMargin)}%"
    }

    data class FleetMetrics(
        val totalIncome: Double,
        val totalExpenses: Double,
        val profitMargin: Double
    )
}