package com.example.matatupapadminapp
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class AddPaymentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.add_payment_page)

        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        val mpesaOptionsSpinner = findViewById<Spinner>(R.id.action_spinner)
        val addBusBtn = findViewById<Button>(R.id.save_payment_btn)


        homeIcon.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        profileIcon.setOnClickListener{
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
        }

        backIcon.setOnClickListener {
            // This will close the current activity and navigate back to the previous one
            finish()
        }

        receiptsIcon.setOnClickListener{
            val intent = Intent(this, ReceiptsActivity::class.java)
            startActivity(intent)
        }

        // Setup the spinner
        val actions = arrayOf("Select a M-Pesa Payment Method","PayBill", "Buy Goods and Service")
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, actions)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mpesaOptionsSpinner.adapter = spinnerAdapter

        // Set the initial position to the instruction item
        mpesaOptionsSpinner.setSelection(0, false) // false here means no animation

        // Optionally, you can add a listener for item selection
        mpesaOptionsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                if (position != 0) { // Check if the selection is not the instruction
                    val selectedAction = parent.getItemAtPosition(position).toString()
                    Toast.makeText(this@AddPaymentActivity, "Selected: $selectedAction", Toast.LENGTH_SHORT).show()
                } else {
                    // Clear any previous selection or reset UI if needed
                }
            }


            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }


    }
}
