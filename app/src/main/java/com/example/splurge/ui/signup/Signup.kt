package com.example.splurge.ui.signup

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.splurge.R
import com.example.splurge.data.AuthSessionManager
import com.example.splurge.data.FinanceRepository
import com.example.splurge.ui.login.Login
import com.example.splurge.ui.main.MainActivity
import com.example.splurge.ui.welcome.Welcome
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Signup : AppCompatActivity() {
    private lateinit var repository: FinanceRepository
    private lateinit var sessionManager: AuthSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FinanceRepository.getInstance(this)
        sessionManager = AuthSessionManager(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.textSignupBack).setOnClickListener {
            openWelcome()
        }

        findViewById<TextView>(R.id.textSignupHaveAccount).setOnClickListener {
            startActivity(Intent(this, Login::class.java))
            finish()
        }

        findViewById<TextView>(R.id.textSignupGuest).setOnClickListener {
            openDashboard()
        }

        findViewById<MaterialButton>(R.id.buttonSignupContinue).setOnClickListener {
            if (validateForm()) {
                createAccount()
            }
        }
    }

    private fun validateForm(): Boolean {
        val nameLayout = findViewById<TextInputLayout>(R.id.layoutSignupName)
        val emailLayout = findViewById<TextInputLayout>(R.id.layoutSignupEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.layoutSignupPassword)
        val confirmLayout = findViewById<TextInputLayout>(R.id.layoutSignupConfirmPassword)

        val name = findViewById<TextInputEditText>(R.id.inputSignupName).text?.toString()?.trim().orEmpty()
        val email = findViewById<TextInputEditText>(R.id.inputSignupEmail).text?.toString()?.trim().orEmpty()
        val password = findViewById<TextInputEditText>(R.id.inputSignupPassword).text?.toString().orEmpty()
        val confirmPassword = findViewById<TextInputEditText>(R.id.inputSignupConfirmPassword).text?.toString().orEmpty()

        nameLayout.error = null
        emailLayout.error = null
        passwordLayout.error = null
        confirmLayout.error = null

        if (name.isBlank()) {
            nameLayout.error = getString(R.string.auth_validation_name)
            return false
        }

        if (email.isBlank()) {
            emailLayout.error = getString(R.string.auth_validation_email_empty)
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailLayout.error = getString(R.string.auth_validation_email_invalid)
            return false
        }

        if (password.isBlank()) {
            passwordLayout.error = getString(R.string.auth_validation_password_empty)
            return false
        }

        if (password.length < 6) {
            passwordLayout.error = getString(R.string.auth_validation_password_short)
            return false
        }

        if (confirmPassword.isBlank()) {
            confirmLayout.error = getString(R.string.auth_validation_confirm_password)
            return false
        }

        if (password != confirmPassword) {
            confirmLayout.error = getString(R.string.auth_validation_password_mismatch)
            return false
        }

        return true
    }

    private fun createAccount() {
        val nameLayout = findViewById<TextInputLayout>(R.id.layoutSignupName)
        val emailLayout = findViewById<TextInputLayout>(R.id.layoutSignupEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.layoutSignupPassword)
        val fullName = findViewById<TextInputEditText>(R.id.inputSignupName).text?.toString().orEmpty()
        val email = findViewById<TextInputEditText>(R.id.inputSignupEmail).text?.toString().orEmpty()
        val password = findViewById<TextInputEditText>(R.id.inputSignupPassword).text?.toString().orEmpty()

        when (val result = repository.registerUser(fullName, email, password)) {
            is FinanceRepository.RegistrationResult.Success -> {
                nameLayout.error = null
                emailLayout.error = null
                passwordLayout.error = null
                sessionManager.saveUserSession(result.userId)
                openDashboard()
            }
            FinanceRepository.RegistrationResult.EmailAlreadyExists -> {
                emailLayout.error = getString(R.string.auth_signup_email_exists)
                Toast.makeText(this, R.string.auth_signup_email_exists, Toast.LENGTH_SHORT).show()
            }
            FinanceRepository.RegistrationResult.InvalidInput -> {
                Toast.makeText(this, R.string.auth_signup_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openWelcome() {
        startActivity(
            Intent(this, Welcome::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
        )
        finish()
    }

    private fun openDashboard() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
