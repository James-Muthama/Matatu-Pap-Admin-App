package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth

class ReceiptsActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.receipts_page)

        auth = FirebaseAuth.getInstance()

        // Initialize UI components
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        // Performance cards
        val busPerformanceCard = findViewById<CardView>(R.id.start_route_card)
        val routePerformanceCard = findViewById<CardView>(R.id.end_route_card)
        val fleetPerformanceCard = findViewById<CardView>(R.id.stage_card)

        // Trip cards
        val busTripsCard = findViewById<CardView>(R.id.add_bus_card)
        val costTripsCard = findViewById<CardView>(R.id.remove_bus_card)

        // Comparison cards
        val busCompareCard = findViewById<CardView>(R.id.add_bus_card_2)
        val routeCompareCard = findViewById<CardView>(R.id.remove_bus_card_2)

        // Bottom navigation
        homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        backIcon.setOnClickListener {
            finish()
        }

        // Receipts icon - already in this activity, no action needed
        receiptsIcon.setOnClickListener {
            // Already in ReceiptsActivity
        }

        // Performance card navigation
        busPerformanceCard.setOnClickListener {
            val intent = Intent(this, BusPerformanceActivity::class.java)
            startActivity(intent)
        }

        routePerformanceCard.setOnClickListener {
            val intent = Intent(this, RoutePerformanceActivity::class.java)
            startActivity(intent)
        }

        fleetPerformanceCard.setOnClickListener {
            // TODO: Navigate to Fleet Performance Activity
            // val intent = Intent(this, FleetPerformanceActivity::class.java)
            // startActivity(intent)
        }

        // Trip cards navigation
        busTripsCard.setOnClickListener {
            val intent = Intent(this, AddBusTripActivity::class.java)
            startActivity(intent)
        }

        costTripsCard.setOnClickListener {
            val intent = Intent(this, AddBusExpenseActivity::class.java)
            startActivity(intent)
        }

        // Comparison cards navigation
        busCompareCard.setOnClickListener {
            val intent = Intent(this, CompareBusActivity::class.java)
            startActivity(intent)
        }

        routeCompareCard.setOnClickListener {
            val intent = Intent(this, CompareRouteActivity::class.java)
            startActivity(intent)
        }
    }
}