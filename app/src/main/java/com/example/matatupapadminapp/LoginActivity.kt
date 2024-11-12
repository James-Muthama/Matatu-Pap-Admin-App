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

class LoginActivity : ComponentActivity() {
    // Firebase authentication reference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables edge-to-edge display for immersive user experience
        setContentView(R.layout.login_page) // Sets the layout for the login page

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialize UI elements
        val signupBtn = findViewById<Button>(R.id.sign_up_btn) // Button to navigate to the sign-up page
        val loginBtn = findViewById<Button>(R.id.login_button) // Button for logging in
        val saccoEmail = findViewById<EditText>(R.id.bus_route_start) // Email input field
        val saccoPass = findViewById<EditText>(R.id.password) // Password input field

        // Set up password visibility toggle for the password field
        setupPasswordToggle(saccoPass, R.drawable.visibility_off_icon, R.drawable.visibility_on_icon)

        // Redirects to the sign-up activity when the sign-up button is clicked
        signupBtn.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Handle login button click
        loginBtn.setOnClickListener {
            val email = saccoEmail.text.toString()
            val password = saccoPass.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Log in user with Firebase Authentication
    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Successful login, direct to MainActivity
                    Toast.makeText(this, "Login successful!", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish() // Close LoginActivity so user can't return with back button
                } else {
                    // Show error message if registration fails
                    Toast.makeText(this, "Log In Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle(
        editText: EditText,
        visibilityOffIcon: Int,
        visibilityOnIcon: Int
    ) {
        var isPasswordVisible = false // Tracks the current visibility state of the password

        // Sets a touch listener on the password field to handle toggle on icon click
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                val drawableEnd = editText.compoundDrawables[2] // Retrieves the end drawable (eye icon)

                // Checks if the end drawable (icon) was tapped
                if (drawableEnd != null && event.rawX >= (editText.right - drawableEnd.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible // Toggles the visibility state
                    // Updates the password field's input type and icon based on visibility state
                    togglePasswordVisibility(editText, isPasswordVisible, visibilityOffIcon, visibilityOnIcon)
                    return@setOnTouchListener true
                }
            }
            false
        }
    }

    private fun togglePasswordVisibility(
        editText: EditText,
        isVisible: Boolean,
        visibilityOffIcon: Int,
        visibilityOnIcon: Int
    ) {
        // Sets input type based on password visibility state
        val newInputType = if (isVisible) {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD // Shows password
        } else {
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD // Hides password
        }
        editText.inputType = newInputType // Applies new input type
        editText.setSelection(editText.text.length) // Maintains cursor position

        // Sets the appropriate eye icon based on visibility state
        val icon = if (isVisible) visibilityOnIcon else visibilityOffIcon
        val drawableEnd: Drawable? = ContextCompat.getDrawable(this, icon)
        // Adds the new icon to the password field
        editText.setCompoundDrawablesWithIntrinsicBounds(editText.compoundDrawables[0], null, drawableEnd, null)
    }
}
