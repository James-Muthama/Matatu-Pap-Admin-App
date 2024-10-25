package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.cardview.widget.CardView


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        val addBusCard = findViewById<CardView>(R.id.add_bus_card)
        val removeBusCard = findViewById<CardView>(R.id.remove_bus_card)
        val addRouteCard = findViewById<CardView>(R.id.add_route_card)
        val changeRoutePrice = findViewById<CardView>(R.id.change_route_prices_card)
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        addBusCard.setOnClickListener {
            val intent = Intent(this, AddBusPageActivity::class.java)
            startActivity(intent)
        }

        removeBusCard.setOnClickListener {
            val intent = Intent(this, RemoveBusPageActivity::class.java)
            startActivity(intent)
        }

        addRouteCard.setOnClickListener {
            val intent = Intent(this, AddRoutePageActivity::class.java)
            startActivity(intent)
        }

        changeRoutePrice.setOnClickListener {
            val intent = Intent(this, ChangeRoutePageActivity::class.java)
            startActivity(intent)
        }

        homeIcon.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener{
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        receiptsIcon.setOnClickListener{
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }

    }
}
