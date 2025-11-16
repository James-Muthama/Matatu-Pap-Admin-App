package com.example.matatupapadminapp

import android.app.DatePickerDialog
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
import java.util.Calendar

class CompareBusActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var routeListContainer: LinearLayout
    private lateinit var routeNameInput: EditText
    private lateinit var searchBtn: ImageView
    private lateinit var textView13: TextView
    private lateinit var stageTextView: TextView
    private lateinit var savePaymentBtn: Button

    // Array to store selected bus plates (maximum 2)
    private val busesCompare = arrayOfNulls<String>(2)
    private var nextInsertIndex = 0 // Tracks which position to insert next (0 or 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.compare_bus_page)

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
            val busPlate = routeNameInput.text.toString().trim()
            if (busPlate.isNotEmpty()) {
                searchBus(busPlate)
            } else {
                Toast.makeText(this, "Please enter a bus plate number", Toast.LENGTH_SHORT).show()
            }
        }

        // Compare button
        savePaymentBtn.setOnClickListener {
            if (busesCompare[0] != null && busesCompare[1] != null) {
                val intent = Intent(this, Compare2BussesActivity::class.java)
                intent.putExtra("busPlate1", busesCompare[0])
                intent.putExtra("busPlate2", busesCompare[1])
                startActivity(intent)
            } else {
                Toast.makeText(this, "Please select two buses to compare", Toast.LENGTH_SHORT).show()
            }
        }

        // Keep the list empty on initial load
        // Don't call fetchBuses() here
    }

    private fun searchBus(busPlate: String) {
        val userId = auth.currentUser?.uid ?: return
        val normalizedBusPlate = busPlate.replace("\\s".toRegex(), "").uppercase()

        val busesRef = database.getReference("Buses").child(userId)
        busesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                routeListContainer.removeAllViews() // Clear existing views before displaying search results

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
                    Toast.makeText(this@CompareBusActivity, "No Bus Matches the Plate Number", Toast.LENGTH_SHORT).show()
                    routeListContainer.removeAllViews() // Keep list empty if no match
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@CompareBusActivity, "Failed to search bus: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayBus(numberPlate: String) {
        val inflater = LayoutInflater.from(this)
        val busView: View = inflater.inflate(R.layout.bus_routes_plate_item, routeListContainer, false)

        val busPlateTextView = busView.findViewById<TextView>(R.id.bus_plate_display)
        busPlateTextView.text = numberPlate

        // Make the entire view clickable
        busView.setOnClickListener {
            // Add bus to the array using alternating indices
            busesCompare[nextInsertIndex] = numberPlate

            // Update the corresponding TextView
            if (nextInsertIndex == 0) {
                textView13.text = numberPlate
                Toast.makeText(this, "Bus 1 selected: $numberPlate", Toast.LENGTH_SHORT).show()
            } else {
                stageTextView.text = numberPlate
                Toast.makeText(this, "Bus 2 selected: $numberPlate", Toast.LENGTH_SHORT).show()
            }

            // Toggle to the next position (0 -> 1 -> 0 -> 1...)
            nextInsertIndex = if (nextInsertIndex == 0) 1 else 0
        }

        routeListContainer.addView(busView)
    }
}