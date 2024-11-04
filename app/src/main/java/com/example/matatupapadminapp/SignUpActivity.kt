package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.signup_page)

        val loginBtn = findViewById<Button>(R.id.login_button)
        val saccoName = findViewById<EditText>(R.id.sacco_name)
        val saccoEmail = findViewById<EditText>(R.id.sacco_email)
        val saccoPass = findViewById<EditText>(R.id.password)
        val saccoConfirmPass = findViewById<EditText>(R.id.confirm_password)
        val signUpBtn = findViewById<Button>(R.id.sign_up_btn)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("Users")

        signUpBtn.setOnClickListener {
            val name = saccoName.text.toString()
            val email = saccoEmail.text.toString()
            val pass = saccoPass.text.toString()
            val confirmPass = saccoConfirmPass.text.toString()

            // Check if passwords match
            if (pass == confirmPass) {
                // Create UserClass object
                val user = UserClass(name, email, pass)

                // Store in Firebase Database
                database.child(name).setValue(user).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "User registered successfully!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Display detailed error message
                        Toast.makeText(this, "Failed to register user: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // Display a message if passwords do not match
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
            }
        }

        loginBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}
