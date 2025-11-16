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

class RoutePerformanceActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var routeListContainer: LinearLayout
    private lateinit var routeNameInput: EditText
    private lateinit var searchBtn: ImageView
    private lateinit var checkStatsBtn: Button
    private lateinit var routesStatsBtn: Button


    // Stats TextViews
    private lateinit var totalRevenueAmnt: TextView
    private lateinit var totalExpenseAmnt: TextView
    private lateinit var profitMarginText: TextView
    private lateinit var selectedRouteText: TextView

    private var selectedRouteName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.route_performance_page)

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
        selectedRouteText = findViewById(R.id.textView13)
        routesStatsBtn = findViewById(R.id.save_payment_btn1)


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

        // Navigate to Total Routes Performance Activity
        routesStatsBtn.setOnClickListener {
            val intent = Intent(this, TotalRoutesPerformanceActivity::class.java)
            startActivity(intent)
        }

        // Search functionality
        searchBtn.setOnClickListener {
            val routeName = routeNameInput.text.toString().trim()
            if (routeName.isNotEmpty()) {
                searchRoute(routeName)
            } else {
                Toast.makeText(this, "Please enter a route name", Toast.LENGTH_SHORT).show()
            }
        }

        // Check stats button - navigate to DisplayRoutePerformanceActivity
        checkStatsBtn.setOnClickListener {
            if (selectedRouteName != null) {
                val intent = Intent(this, DisplayRoutePerformanceActivity::class.java)
                intent.putExtra("routeNames", arrayOf(selectedRouteName!!))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select a route first", Toast.LENGTH_SHORT).show()
            }
        }

        // Load all routes and calculate metrics on page load
        loadAllRoutesAndMetrics()
    }

    private fun loadAllRoutesAndMetrics() {
        val userId = auth.currentUser?.uid ?: return

        val routesRef = database.getReference("Route_Income").child(userId)
        routesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.exists()) {
                    Toast.makeText(
                        this@RoutePerformanceActivity,
                        "No routes found",
                        Toast.LENGTH_SHORT
                    ).show()
                    return
                }

                val routesList = mutableListOf<String>()
                dataSnapshot.children.forEach { routeSnapshot ->
                    val routeName = routeSnapshot.key
                    if (!routeName.isNullOrEmpty()) {
                        routesList.add(routeName)
                    }
                }

                if (routesList.isNotEmpty()) {
                    // Calculate metrics for all routes
                    calculateMetricsForAllRoutes(routesList)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@RoutePerformanceActivity,
                    "Failed to load routes: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun calculateMetricsForAllRoutes(routesList: List<String>) {
        val userId = auth.currentUser?.uid ?: return

        var totalRevenue = 0.0
        var totalExpenses = 0.0
        var completedCount = 0
        val totalRoutes = routesList.size

        routesList.forEach { routeName ->
            val incomeRef = database.getReference("Route_Income").child(userId).child(routeName)
            val expensesRef = database.getReference("Route_Expenses").child(userId).child(routeName)

            incomeRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(incomeSnapshot: DataSnapshot) {
                    expensesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(expensesSnapshot: DataSnapshot) {
                            val metrics = calculateMetrics(incomeSnapshot, expensesSnapshot)
                            totalRevenue += metrics.totalRevenue
                            totalExpenses += metrics.totalExpenses

                            completedCount++

                            // When all routes are processed, display the totals
                            if (completedCount == totalRoutes) {
                                val netProfit = totalRevenue - totalExpenses
                                val profitMargin = if (totalRevenue > 0) {
                                    (netProfit / totalRevenue) * 100
                                } else {
                                    0.0
                                }

                                val aggregatedMetrics = RouteMetrics(
                                    totalRevenue = totalRevenue,
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

    private fun searchRoute(routeName: String) {
        val userId = auth.currentUser?.uid ?: return
        val normalizedRouteName = routeName.trim().lowercase()

        val routesRef = database.getReference("Route_Income").child(userId)
        routesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                routeListContainer.removeAllViews()

                var matchFound = false
                dataSnapshot.children.forEach { routeSnapshot ->
                    val storedRouteName = routeSnapshot.key ?: return@forEach
                    val normalizedStoredName = storedRouteName.trim().lowercase()

                    if (normalizedStoredName.contains(normalizedRouteName)) {
                        displayRoute(storedRouteName)
                        matchFound = true
                    }
                }

                if (!matchFound) {
                    Toast.makeText(
                        this@RoutePerformanceActivity,
                        "No Route Matches the Name",
                        Toast.LENGTH_SHORT
                    ).show()
                    routeListContainer.removeAllViews()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(
                    this@RoutePerformanceActivity,
                    "Failed to search route: ${databaseError.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun displayRoute(routeName: String) {
        val inflater = LayoutInflater.from(this)
        val routeView: View = inflater.inflate(R.layout.bus_routes_plate_item, routeListContainer, false)

        val routeNameTextView = routeView.findViewById<TextView>(R.id.bus_plate_display)
        routeNameTextView.text = routeName

        routeView.setOnClickListener {
            selectedRouteName = routeName

            // Update the selected route text
            selectedRouteText.text = routeName

            Toast.makeText(this, "Selected: $routeName", Toast.LENGTH_SHORT).show()

            // Highlight selected route
            for (i in 0 until routeListContainer.childCount) {
                routeListContainer.getChildAt(i).setBackgroundColor(
                    resources.getColor(android.R.color.transparent, null)
                )
            }
            routeView.setBackgroundColor(resources.getColor(android.R.color.darker_gray, null))
        }

        routeListContainer.addView(routeView)
    }

    private fun calculateMetrics(
        incomeSnapshot: DataSnapshot,
        expensesSnapshot: DataSnapshot
    ): RouteMetrics {
        var totalRevenue = 0.0
        var totalExpenses = 0.0

        // Parse income data - structure: routeName/dd-MM-yyyy/tripId/{num_trips, income}
        for (dateSnapshot in incomeSnapshot.children) {
            for (tripSnapshot in dateSnapshot.children) {
                val tripData = tripSnapshot.value as? Map<*, *> ?: continue
                val income = (tripData["income"] as? Number)?.toDouble() ?: 0.0
                totalRevenue += income
            }
        }

        // Parse expenses data - structure: routeName/MM-yyyy/total_expenses
        for (monthSnapshot in expensesSnapshot.children) {
            val expenses = (monthSnapshot.child("total_expenses").value as? Number)?.toDouble() ?: 0.0
            totalExpenses += expenses
        }

        val netProfit = totalRevenue - totalExpenses
        val profitMargin = if (totalRevenue > 0) {
            (netProfit / totalRevenue) * 100
        } else {
            0.0
        }

        return RouteMetrics(
            totalRevenue = totalRevenue,
            totalExpenses = totalExpenses,
            profitMargin = profitMargin
        )
    }

    private fun displayMetrics(metrics: RouteMetrics) {
        totalRevenueAmnt.text = "KSh ${String.format("%.2f", metrics.totalRevenue)}"
        totalExpenseAmnt.text = "KSh ${String.format("%.2f", metrics.totalExpenses)}"
        profitMarginText.text = "${String.format("%.2f", metrics.profitMargin)}%"
    }

    data class RouteMetrics(
        val totalRevenue: Double,
        val totalExpenses: Double,
        val profitMargin: Double
    )
}