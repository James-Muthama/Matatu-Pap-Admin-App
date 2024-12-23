package com.example.matatupapadminapp

import ConfirmDeleteRouteFragment
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

class ChangeRoutePricesOrNameActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var routeListContainer: LinearLayout
    private lateinit var routeNameInput: EditText
    private lateinit var searchBtn: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.change_route_prices_or_name_page)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        searchBtn = findViewById(R.id.search_btn)
        routeNameInput = findViewById(R.id.route_name_input)

        routeListContainer = findViewById(R.id.route_list_container)

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

        fetchRoutes()

        searchBtn.setOnClickListener {
            val searchText = routeNameInput.text.toString().trim()
            if (searchText.isNotEmpty()) {
                searchRoute(searchText)
            } else {
                fetchRoutes()
                Toast.makeText(this, "Displaying all routes", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchRoutes() {
        val userId = auth.currentUser?.uid ?: return

        val routesRef = database.getReference("Routes").child(userId)
        routesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                routeListContainer.removeAllViews()
                for (routeSnapshot in dataSnapshot.children) {
                    val routeName = routeSnapshot.child("name").getValue(String::class.java) ?: continue
                    val fare = routeSnapshot.child("fare").getValue(Long::class.java)?.toString() ?: continue

                    displayRoute(routeName, fare)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ChangeRoutePricesOrNameActivity, "Failed to load routes", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayRoute(routeName: String, fare: String) {
        val inflater = LayoutInflater.from(this)
        val routeView: View = inflater.inflate(R.layout.route_item, routeListContainer, false)

        routeView.findViewById<TextView>(R.id.bus_plate_txt).text = routeName
        routeView.findViewById<TextView>(R.id.route_name_text).text = fare

        val updateButton = routeView.findViewById<Button>(R.id.button)
        val deleteImage = routeView.findViewById<ImageView>(R.id.delete_icon)

        updateButton.setOnClickListener {
            val intent = Intent(this, UpdateRouteActivity::class.java)
            intent.putExtra("routeName", routeName)
            intent.putExtra("fare", fare)
            startActivity(intent)
        }

        deleteImage.setOnClickListener {
            val confirmDeleteFragment = ConfirmDeleteRouteFragment()
            confirmDeleteFragment.setOnDeleteConfirmedListener {
                val userId = auth.currentUser?.uid ?: return@setOnDeleteConfirmedListener
                val routeRef = database.getReference("Routes").child(userId).child(routeName)
                val busesRef = database.getReference("Buses").child(userId)

                routeRef.removeValue().addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        busesRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (busSnapshot in dataSnapshot.children) {
                                    val busKey = busSnapshot.key ?: continue
                                    val busData = busSnapshot.value as? Map<*, *> ?: continue
                                    val currentRouteName = busData["route name"] as? String

                                    if (currentRouteName == routeName) {
                                        busesRef.child(busKey).child("route name").setValue(null)
                                    }
                                }
                                routeListContainer.removeView(routeView)

                                // Show the toast message here
                                Toast.makeText(this@ChangeRoutePricesOrNameActivity, "Route $routeName has been deleted", Toast.LENGTH_SHORT).show()
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                Toast.makeText(this@ChangeRoutePricesOrNameActivity, "Failed to update buses: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    } else {
                        Toast.makeText(this, "Failed to delete route", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            confirmDeleteFragment.setOnDeleteCancelledListener {
                // No need to manually dismiss or remove, as it's handled by the DialogFragment
            }

            // Pass the route name to the fragment
            val bundle = Bundle()
            bundle.putString("routeName", routeName)
            confirmDeleteFragment.arguments = bundle

            // Show the fragment as a dialog
            confirmDeleteFragment.show(supportFragmentManager, "ConfirmDeleteDialog")
        }

        routeListContainer.addView(routeView)
    }

    private fun searchRoute(searchText: String) {
        val userId = auth.currentUser?.uid ?: return
        val routesRef = database.getReference("Routes").child(userId)

        routesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                routeListContainer.removeAllViews()
                var matchFound = false

                for (routeSnapshot in dataSnapshot.children) {
                    val routeName = routeSnapshot.child("name").getValue(String::class.java) ?: continue
                    val fare = routeSnapshot.child("fare").getValue(Long::class.java)?.toString() ?: continue

                    if (routeName.contains(searchText, ignoreCase = true)) {
                        displayRoute(routeName, fare)
                        matchFound = true
                    }
                }

                if (!matchFound) {
                    Toast.makeText(this@ChangeRoutePricesOrNameActivity,
                        "No routes found matching '$searchText'",
                        Toast.LENGTH_SHORT).show()
                    fetchRoutes()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Toast.makeText(this@ChangeRoutePricesOrNameActivity,
                    "Search failed: ${databaseError.message}",
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
}