package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : ComponentActivity() {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Initialize Firebase Authentication and Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Sacco")

        // Find greeting TextView
        val greetingTextView = findViewById<TextView>(R.id.greeting)

        // Get current user ID
        val currentUser = auth.currentUser
        currentUser?.let {
            val userId = it.uid

            // Fetch user data from Firebase Realtime Database
            database.child(userId).get().addOnSuccessListener { dataSnapshot ->
                val userName = dataSnapshot.child("name").getValue(String::class.java)
                if (userName != null) {
                    greetingTextView.text = getString(R.string.greeting_text, userName)
                } else {
                    greetingTextView.text = getString(R.string.greeting_text, "User")
                }
            }.addOnFailureListener {
                greetingTextView.text = getString(R.string.greeting_text, "User")
            }
        }

        // Initialize CardViews for navigation
        val addBusCard = findViewById<CardView>(R.id.add_bus_card)
        val removeBusCard = findViewById<CardView>(R.id.remove_bus_card)
        val addRouteCard = findViewById<CardView>(R.id.add_route_card)
        val changeRoutePrice = findViewById<CardView>(R.id.change_route_prices_card)
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)

        // Set onClickListeners for each card to navigate to the corresponding activities
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

        homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        receiptsIcon.setOnClickListener {
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }
    }
}
