package com.example.matatupapadminapp

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignUpActivity : ComponentActivity() {
    private lateinit var database: DatabaseReference

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

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
        val passwordVisibilityToggle = findViewById<ImageView>(R.id.password_visibility)
        val confirmPasswordVisibilityToggle = findViewById<ImageView>(R.id.confirm_password_visibility)

        // Initialize Firebase Database reference
        database = FirebaseDatabase.getInstance().getReference("Users")

        // Toggle password visibility for password field
        passwordVisibilityToggle.setOnClickListener {
            // Toggle the visibility state
            isPasswordVisible = !isPasswordVisible

            if (isPasswordVisible) {
                // Set the input type to visible password
                saccoPass.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                // Change icon to show that password is visible
                passwordVisibilityToggle.setImageResource(R.drawable.visibility_on_icon)
            } else {
                // Set the input type back to hidden password
                saccoPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                // Change icon to show that password is hidden
                passwordVisibilityToggle.setImageResource(R.drawable.visibility_off_icon)
            }


            // Move the cursor to the end of the text in the password field
            saccoPass.setSelection(saccoPass.text.length)
        }

        // Toggle password visibility for confirm password field
        confirmPasswordVisibilityToggle.setOnClickListener {
            // Toggle the visibility state for confirm password
            isConfirmPasswordVisible = !isConfirmPasswordVisible

            // If confirm password should be visible...
            if (isConfirmPasswordVisible) {
                // ...set the input type to show the text
                saccoConfirmPass.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                // ...update the visibility icon to 'on'
                confirmPasswordVisibilityToggle.setImageResource(R.drawable.visibility_on_icon)
            } else {
                // If confirm password should be hidden...
                // ...set the input type back to hide the password
                saccoConfirmPass.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                // ...update the visibility icon to 'off'
                confirmPasswordVisibilityToggle.setImageResource(R.drawable.visibility_off_icon)
            }

            // Move the cursor to the end of the text in the confirm password field
            saccoConfirmPass.setSelection(saccoConfirmPass.text.length)
        }

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
