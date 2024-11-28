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

class RemoveBusPageActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var busListContainer: LinearLayout
    private lateinit var busNumberPlateInput: EditText
    private lateinit var searchBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.remove_bus_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        searchBtn = findViewById(R.id.search_btn)
        busNumberPlateInput = findViewById(R.id.bus_number_plate_input)

        busListContainer = findViewById(R.id.bus_list_container)

        setupNavigation(homeIcon, backIcon, profileIcon, receiptsIcon)

        fetchBuses()

        // Set up the search functionality
        searchBtn.setOnClickListener {
            val numberPlate = busNumberPlateInput.text.toString().replace("\\s".toRegex(), "")
            if (numberPlate.isNotEmpty()) {
                searchBus(numberPlate)
            } else {
                fetchBuses()
                Toast.makeText(this, "Displaying all buses", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupNavigation(
        homeIcon: CardView,
        backIcon: ImageView,
        profileIcon: CardView,
        receiptsIcon: CardView
    ) {
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
    }

    private fun fetchBuses() {
        val userId = auth.currentUser?.uid ?: return

        val busRef = database.getReference("Buses").child(userId)
        busRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                busListContainer.removeAllViews()
                for (busSnapshot in dataSnapshot.children) {
                    val busNumberPlate = busSnapshot.key ?: continue
                    val busData = busSnapshot.value as? Map<*, *> ?: continue
                    val routeName = busData["route name"] as? String ?: "Unknown"

                    displayBus(busNumberPlate, routeName)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors here
            }
        })
    }

    private fun searchBus(numberPlate: String) {
        val userId = auth.currentUser?.uid ?: return

        val busRef = database.getReference("Buses").child(userId).child(numberPlate)
        busRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                busListContainer.removeAllViews()

                if (dataSnapshot.exists()) {
                    val busData = dataSnapshot.value as? Map<*, *> ?: return
                    val routeName = busData["route name"] as? String ?: "Unknown"
                    displayBus(numberPlate, routeName)
                } else {
                    Toast.makeText(this@RemoveBusPageActivity, "No Bus Matches the Number Plate", Toast.LENGTH_SHORT).show()
                    fetchBuses()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@RemoveBusPageActivity, "Failed to search bus: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayBus(numberPlate: String, routeName: String) {
        val inflater = LayoutInflater.from(this)
        val busView: View = inflater.inflate(R.layout.bus_item, busListContainer, false)

        val formattedNumberPlate = numberPlate.replace("(?<=\\G.{3})".toRegex(), " ")

        busView.findViewById<TextView>(R.id.bus_plate_txt).text = formattedNumberPlate
        busView.findViewById<TextView>(R.id.route_name_text).text = routeName

        val updateButton = busView.findViewById<Button>(R.id.button)
        val deleteImage = busView.findViewById<ImageView>(R.id.delete_icon)  // Changed ID to match XML

        // Update Button functionality
        updateButton.setOnClickListener {
            // Placeholder for update logic
            Toast.makeText(this, "Update functionality not implemented", Toast.LENGTH_SHORT).show()
        }

        // Delete functionality
        deleteImage.setOnClickListener {
            val userId = auth.currentUser?.uid ?: return@setOnClickListener
            val busRef = database.getReference("Buses").child(userId).child(numberPlate)

            busRef.removeValue().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    busListContainer.removeView(busView)
                    Toast.makeText(this, "Bus $numberPlate deleted successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to delete bus", Toast.LENGTH_SHORT).show()
                }
            }
        }

        busListContainer.addView(busView)
    }
}