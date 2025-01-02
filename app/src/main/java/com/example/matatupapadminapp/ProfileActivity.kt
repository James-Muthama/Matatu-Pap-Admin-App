package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ProfileActivity : AppCompatActivity() {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth
    private lateinit var busDatabase: DatabaseReference
    private lateinit var routeDatabase: DatabaseReference
    private lateinit var saccoDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profile_page)

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            routeDatabase = FirebaseDatabase.getInstance().getReference("Routes").child(currentUser.uid)
            busDatabase = FirebaseDatabase.getInstance().getReference("Buses").child(currentUser.uid)
            saccoDatabase = FirebaseDatabase.getInstance().getReference("Sacco").child(currentUser.uid)
        } else {
            // Handle the case where no user is logged in, perhaps by showing an error or redirecting to login
            return
        }


        // Find greeting TextView
        val greetingTextView = findViewById<TextView>(R.id.greeting)

        currentUser.let {
            val userId = it.uid

            // Fetch user data from Firebase Realtime Database
            saccoDatabase.get().addOnSuccessListener { dataSnapshot ->
                // Directly get the username from the dataSnapshot since we're already at the correct node
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

        // UI elements
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        val logoutIcon = findViewById<ImageView>(R.id.logout_icon)
        val routesCountTextView = findViewById<TextView>(R.id.routes_count)
        val busesCountTextView = findViewById<TextView>(R.id.buses_count)
        val addPaymentBtn = findViewById<Button>(R.id.add_payment_btn)
        val editAccountInfo = findViewById<Button>(R.id.edit_info_btn)

        // Fetch routes count for specific user
        routeDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val count = dataSnapshot.childrenCount
                routesCountTextView.text = count.toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Failed to read value from Routes: ${databaseError.toException()}")
            }
        })

        // Fetch buses count for specific user
        busDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val count = dataSnapshot.childrenCount
                busesCountTextView.text = count.toString()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("Failed to read value from Buses: ${databaseError.toException()}")
            }
        })

        // Set onClickListeners for each card to navigate to the corresponding activities
        addPaymentBtn.setOnClickListener {
            val intent = Intent(this, AddPaymentActivity::class.java)
            startActivity(intent)
        }

        // editAccountInfo.setOnClickListener {
            //val intent = Intent(this, EditAccountInfoActivity::class.java)
            //startActivity(intent)
        //}


        // Rest of the click listeners remain the same

        logoutIcon.setOnClickListener {
            auth.signOut()
            val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        homeIcon.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener{
            // This might be redundant if already in ProfileActivity
            // Consider removing or redirecting to another profile view if needed
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        backIcon.setOnClickListener {
            finish()
        }

        receiptsIcon.setOnClickListener{
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }
    }
}