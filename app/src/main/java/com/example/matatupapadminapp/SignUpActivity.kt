package com.example.matatupapadminapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.InputType
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : ComponentActivity() {
    // Firebase authentication and database references
    private lateinit var auth: FirebaseAuth // Reference to Firebase Authentication for user sign-up
    private lateinit var database: DatabaseReference // Reference to Firebase Realtime Database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables edge-to-edge display for devices with gesture navigation
        setContentView(R.layout.signup_page) // Sets the layout file for this activity

        // Initialize Firebase Authentication and Realtime Database
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Sacco") // Points to "Users" node in Realtime Database

        // UI elements from the layout
        val loginBtn = findViewById<Button>(R.id.login_button) // Button to navigate to login screen
        val saccoName = findViewById<EditText>(R.id.sacco_name) // Input field for user's name
        val saccoEmail = findViewById<EditText>(R.id.bus_route_start) // Input field for user's email
        val saccoPass = findViewById<EditText>(R.id.password) // Input field for user's password
        val saccoConfirmPass = findViewById<EditText>(R.id.confirm_password) // Input field for confirming password
        val signUpBtn = findViewById<Button>(R.id.sign_up_btn) // Button to trigger sign-up process

        // Set up password visibility toggle
        setupPasswordToggle(saccoPass, R.drawable.visibility_off_icon, R.drawable.visibility_on_icon)
        setupPasswordToggle(saccoConfirmPass, R.drawable.visibility_off_icon, R.drawable.visibility_on_icon)

        // Sign Up button functionality
        signUpBtn.setOnClickListener {
            val name = saccoName.text.toString() // Retrieve the entered name
            val email = saccoEmail.text.toString() // Retrieve the entered email
            val pass = saccoPass.text.toString() // Retrieve the entered password
            val confirmPass = saccoConfirmPass.text.toString() // Retrieve the confirmed password

            if (pass == confirmPass) {
                // Proceed with user registration if passwords match
                registerUser(name, email, pass) // Register user and store additional data
            } else {
                // Show a toast message if passwords do not match
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
            }
        }

        // Login button functionality
        loginBtn.setOnClickListener {
            // Navigate to LoginActivity when login button is clicked
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Register user with Firebase Authentication and store additional data
    private fun registerUser(name: String, email: String, password: String) {
        // Create a new user in Firebase Authentication using email and password
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // If registration is successful, get the current user's UID
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        // Store additional user data in Realtime Database
                        storeUserData(userId, name, email)
                    }
                } else {
                    // Show error message if registration fails
                    Toast.makeText(this, "Registration Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Store additional user data in Realtime Database
    private fun storeUserData(userId: String, name: String, email: String) {
        // Create a user data map to store name and email
        val user = mapOf(
            "name" to name,
            "email" to email
        )

        // Store user data under "Users/userId" in Realtime Database
        database.child(userId).setValue(user)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // If data is stored successfully, show a success message
                    Toast.makeText(this, "User registered successfully!", Toast.LENGTH_SHORT).show()
                    // Navigate to MainActivity after successful registration
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Ensure this activity is removed from the back stack
                } else {
                    // Show error message if data storage fails
                    Toast.makeText(this, "Failed to store user data: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // Password visibility toggle setup
    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle(
        editText: EditText,
        visibilityOffIcon: Int,
        visibilityOnIcon: Int
    ) {
        var isPasswordVisible = false // Tracks the visibility state of the password

        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2] // Get drawable at the end (right side)
                if (drawableEnd != null && event.rawX >= (editText.right - drawableEnd.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible // Toggle visibility state
                    togglePasswordVisibility(editText, isPasswordVisible, visibilityOffIcon, visibilityOnIcon)
                    return@setOnTouchListener true // Indicate event was handled
                }
            }
            false
        }
    }

    // Toggle password visibility and icon
    private fun togglePasswordVisibility(
        editText: EditText,
        isVisible: Boolean,
        visibilityOffIcon: Int,
        visibilityOnIcon: Int
    ) {
        // Set input type based on visibility state
        val newInputType = if (isVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD // Show text
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD // Mask text with dots
        }
        editText.inputType = newInputType // Apply the new input type
        editText.setSelection(editText.text.length) // Move cursor to end of text

        // Update icon based on visibility state
        val icon = if (isVisible) visibilityOnIcon else visibilityOffIcon
        val drawableEnd: Drawable? = ContextCompat.getDrawable(this, icon)
        editText.setCompoundDrawablesWithIntrinsicBounds(editText.compoundDrawables[0], null, drawableEnd, null)
    }
}
