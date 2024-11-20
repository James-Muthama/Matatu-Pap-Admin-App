package com.example.matatupapadminapp

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : AppCompatActivity() {
    // Firebase authentication reference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.splash_screen)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Launch a coroutine for the delay and user check
        lifecycleScope.launch {
            delay(5000) // Delay for 5 seconds

            // Check if the user is already signed in
            val currentUser = auth.currentUser
            if (currentUser != null) {
                // User is signed in, navigate to MainActivity
                val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
                startActivity(intent)
            } else {
                // No user is signed in, navigate to LoginActivity
                val intent = Intent(this@SplashScreenActivity, LoginActivity::class.java)
                startActivity(intent)
            }
            finish() // Close SplashScreenActivity
        }
    }
}
