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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : ComponentActivity() {
    // Firebase database reference to store user data
    private lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Enables edge-to-edge display on devices with gesture navigation
        setContentView(R.layout.signup_page) // Sets the layout file for this activity

        // Initializing UI elements from the layout
        val loginBtn = findViewById<Button>(R.id.login_button)
        val saccoName = findViewById<EditText>(R.id.sacco_name)
        val saccoEmail = findViewById<EditText>(R.id.sacco_email)
        val saccoPass = findViewById<EditText>(R.id.password)
        val saccoConfirmPass = findViewById<EditText>(R.id.confirm_password)
        val signUpBtn = findViewById<Button>(R.id.sign_up_btn)

        // Initialize Firebase Database reference to "Users" node
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Set up password visibility toggle functionality for the password fields
        setupPasswordToggle(saccoPass, R.drawable.visibility_off_icon, R.drawable.visibility_on_icon)
        setupPasswordToggle(saccoConfirmPass, R.drawable.visibility_off_icon, R.drawable.visibility_on_icon)

        // Set up the Sign Up button's functionality
        signUpBtn.setOnClickListener {
            val name = saccoName.text.toString()
            val email = saccoEmail.text.toString()
            val pass = saccoPass.text.toString()
            val confirmPass = saccoConfirmPass.text.toString()

            // Check if passwords match
            if (pass == confirmPass) {
                val user = UserClass(name, email, pass) // Create a new user object

                // Add user data to Firebase Database under the user name key
                database.child(name).setValue(user).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "User registered successfully!", Toast.LENGTH_SHORT).show()
                        // Navigate to MainActivity after successful registration
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                    } else {
                        // Show error message if registration fails
                        Toast.makeText(this, "Failed to register user: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                // Show message if passwords do not match
                Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show()
            }
        }

        // Set up the Login button to navigate to LoginActivity
        loginBtn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    // Function to set up a password visibility toggle
    @SuppressLint("ClickableViewAccessibility")
    private fun setupPasswordToggle(
        editText: EditText,
        visibilityOffIcon: Int,
        visibilityOnIcon: Int
    ) {
        var isPasswordVisible = false // Tracks if password is currently visible

        // Set up a touch listener on the password field
        editText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) { // Check for touch release action
                val drawableEnd = editText.compoundDrawables[2] // Get drawable at the end (right side)
                // Check if touch is within drawable bounds
                if (drawableEnd != null && event.rawX >= (editText.right - drawableEnd.bounds.width())) {
                    isPasswordVisible = !isPasswordVisible // Toggle visibility state
                    togglePasswordVisibility(editText, isPasswordVisible, visibilityOffIcon, visibilityOnIcon)
                    return@setOnTouchListener true // Indicate event was handled
                }
            }
            false
        }
    }

    // Function to toggle password visibility and icon
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
