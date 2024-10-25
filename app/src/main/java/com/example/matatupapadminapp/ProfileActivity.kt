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
