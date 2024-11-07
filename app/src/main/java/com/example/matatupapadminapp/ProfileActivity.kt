package com.example.matatupapadminapp
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.profile)

        val profileIcon = findViewById<CardView>(R.id.profile_icon_card)
        val homeIcon = findViewById<CardView>(R.id.home_icon_card)
        val backIcon = findViewById<ImageView>(R.id.back_icon)
        val receiptsIcon = findViewById<CardView>(R.id.receipts_icon_card)
        val logoutIcon = findViewById<ImageView>(R.id.logout_icon)

        // Set click listener for logout icon
        logoutIcon.setOnClickListener {
            // Clear user session or perform logout logic
            // Redirect to the login activity
            val intent = Intent(this@ProfileActivity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish() // Ensure the profile activity is finished after logout
        }

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

    }
}
