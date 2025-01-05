package com.example.matatupapadminapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class AddPaymentActivity : AppCompatActivity() {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth
    private lateinit var paymentDatabase: com.google.firebase.database.DatabaseReference
    private lateinit var mpesaOptionsSpinner: Spinner
    // List to hold M-Pesa payment options
    private val mpesaOptions = arrayOf("Select a M-Pesa Payment Method", "PayBill", "Buy Goods and Service")

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge display for better UI
        enableEdgeToEdge()
        // Set the layout for this activity
        setContentView(R.layout.add_payment_page)

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        paymentDatabase = FirebaseDatabase.getInstance().getReference("Payments")

        // Initialize UI components from the layout
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        val paymentName = findViewById<EditText>(R.id.bus_code)
        mpesaOptionsSpinner = findViewById(R.id.action_spinner)
        val partyB = findViewById<EditText>(R.id.partyb)
        val addPaymentBtn = findViewById<Button>(R.id.save_payment_btn)

        // Setup the spinner for M-Pesa payment options
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, mpesaOptions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mpesaOptionsSpinner.adapter = spinnerAdapter
        mpesaOptionsSpinner.setSelection(0, false) // No animation for initial selection

        // Handle click for the "Add Payment" button
        addPaymentBtn.setOnClickListener {
            val paymentMethodName = paymentName.text.toString()
            val selectedMpesaOption = mpesaOptionsSpinner.selectedItem.toString()
            val partyBNumber = partyB.text.toString()

            // Validate user input before proceeding
            if (paymentMethodName.isNotEmpty() && selectedMpesaOption != "Select a M-Pesa Payment Method" && partyBNumber.isNotEmpty()) {
                findUserId(paymentMethodName, selectedMpesaOption, partyBNumber)
            } else {
                Toast.makeText(this, "Please fill all fields and select a payment method", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up navigation
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

    /**
     * Check user authentication and proceed to add payment information if authenticated.
     */
    private fun findUserId(paymentMethodName: String, mpesaOption: String, partyBNumber: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            addPaymentInfo(userId, paymentMethodName, mpesaOption, partyBNumber)
        } else {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Add payment data to Firebase under the user's ID with the payment method name as the key.
     */
    private fun addPaymentInfo(userId: String, paymentMethodName: String, mpesaOption: String, partyBNumber: String) {
        val userPaymentRef = paymentDatabase.child(userId).child(paymentMethodName)
        val paymentData = mapOf(
            "mpesa option" to mpesaOption,
            "short code" to partyBNumber
        )

        userPaymentRef.setValue(paymentData)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Payment Added Successfully!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Failed to add payment: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }
}