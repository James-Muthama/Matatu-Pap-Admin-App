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

class CompareRouteActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var routeListContainer: LinearLayout
    private lateinit var routeNameInput: EditText
    private lateinit var searchBtn: ImageView
    private lateinit var textView13: TextView
    private lateinit var stageTextView: TextView
    private lateinit var savePaymentBtn: Button

    // Array to store selected route names (maximum 2)
    private val routesCompare = arrayOfNulls<String>(2)
    private var nextInsertIndex = 0 // Tracks which position to insert next (0 or 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.compare_route_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        searchBtn = findViewById(R.id.search_btn)
        routeListContainer = findViewById(R.id.route_list_container)
        routeNameInput = findViewById(R.id.route_name_input)
        textView13 = findViewById(R.id.textView13)
        stageTextView = findViewById(R.id.stage)
        savePaymentBtn = findViewById(R.id.save_payment_btn)

        receiptsIcon.setOnClickListener {
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        backIcon.setOnClickListener {
            finish()
        }

        homeIcon.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Set up the search functionality
        searchBtn.setOnClickListener {
            val routeName = routeNameInput.text.toString().trim()
            if (routeName.isNotEmpty()) {
                searchRoute(routeName)
            } else {
                Toast.makeText(this, "Please enter a route name", Toast.LENGTH_SHORT).show()
            }
        }

        // Compare button
        savePaymentBtn.setOnClickListener {
            if (routesCompare[0] != null && routesCompare[1] != null) {
                val intent = Intent(this, Compare2RoutesActivity::class.java)
                intent.putExtra("routeName1", routesCompare[0])
                intent.putExtra("routeName2", routesCompare[1])
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select two routes to compare", Toast.LENGTH_SHORT).show()
            }
        }

        // Keep the list empty on initial load
    }

    private fun searchRoute(routeName: String) {
        val userId = auth.currentUser?.uid ?: return
        val normalizedRouteName = routeName.trim().lowercase()

        val routesRef = database.getReference("Route_Income").child(userId)
        routesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                routeListContainer.removeAllViews() // Clear existing views before displaying search results

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
                    Toast.makeText(this@CompareRouteActivity, "No Route Matches the Name", Toast.LENGTH_SHORT).show()
                    routeListContainer.removeAllViews() // Keep list empty if no match
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@CompareRouteActivity, "Failed to search route: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayRoute(routeName: String) {
        val inflater = LayoutInflater.from(this)
        val routeView: View = inflater.inflate(R.layout.bus_routes_plate_item, routeListContainer, false)

        val routeNameTextView = routeView.findViewById<TextView>(R.id.bus_plate_display)
        routeNameTextView.text = routeName

        // Make the entire view clickable
        routeView.setOnClickListener {
            // Add route to the array using alternating indices
            routesCompare[nextInsertIndex] = routeName

            // Update the corresponding TextView
            if (nextInsertIndex == 0) {
                textView13.text = routeName
                Toast.makeText(this, "Route 1 selected: $routeName", Toast.LENGTH_SHORT).show()
            } else {
                stageTextView.text = routeName
                Toast.makeText(this, "Route 2 selected: $routeName", Toast.LENGTH_SHORT).show()
            }

            // Toggle to the next position (0 -> 1 -> 0 -> 1...)
            nextInsertIndex = if (nextInsertIndex == 0) 1 else 0
        }

        routeListContainer.addView(routeView)
    }
}