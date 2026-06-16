package com.example.splurge.ui.login

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
import com.example.splurge.ui.main.MainActivity
import com.example.splurge.ui.signup.Signup
import com.example.splurge.ui.welcome.Welcome
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class Login : AppCompatActivity() {
    private lateinit var repository: FinanceRepository
    private lateinit var sessionManager: AuthSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FinanceRepository.getInstance(this)
        sessionManager = AuthSessionManager(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<TextView>(R.id.textLoginBack).setOnClickListener {
            openWelcome()
        }

        findViewById<TextView>(R.id.textLoginCreateAccount).setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
            finish()
        }

        findViewById<TextView>(R.id.textLoginGuest).setOnClickListener {
            openDashboard()
        }

        findViewById<MaterialButton>(R.id.buttonLoginContinue).setOnClickListener {
            if (validateForm()) {
                signIn()
            }
        }

        prefillEmailFromSignup()
    }

    private fun prefillEmailFromSignup() {
        val email = intent.getStringExtra(EXTRA_PREFILL_EMAIL)?.trim().orEmpty()
        if (email.isBlank()) {
            return
        }

        findViewById<TextInputEditText>(R.id.inputLoginEmail).setText(email)
        findViewById<TextInputEditText>(R.id.inputLoginPassword).requestFocus()
        Toast.makeText(this, R.string.login_account_created_message, Toast.LENGTH_SHORT).show()
    }

    private fun validateForm(): Boolean {
        val emailLayout = findViewById<TextInputLayout>(R.id.layoutLoginEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.layoutLoginPassword)
        val email = findViewById<TextInputEditText>(R.id.inputLoginEmail).text?.toString()?.trim().orEmpty()
        val password = findViewById<TextInputEditText>(R.id.inputLoginPassword).text?.toString().orEmpty()

        emailLayout.error = null
        passwordLayout.error = null

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

        return true
    }

    private fun signIn() {
        val emailLayout = findViewById<TextInputLayout>(R.id.layoutLoginEmail)
        val passwordLayout = findViewById<TextInputLayout>(R.id.layoutLoginPassword)
        val email = findViewById<TextInputEditText>(R.id.inputLoginEmail).text?.toString().orEmpty()
        val password = findViewById<TextInputEditText>(R.id.inputLoginPassword).text?.toString().orEmpty()
        val rememberDevice = findViewById<MaterialCheckBox>(R.id.checkLoginRememberDevice).isChecked
        val user = repository.authenticateUser(email, password)

        if (user == null) {
            passwordLayout.error = getString(R.string.auth_login_invalid_credentials)
            Toast.makeText(this, R.string.auth_login_invalid_credentials, Toast.LENGTH_SHORT).show()
            return
        }

        emailLayout.error = null
        passwordLayout.error = null

        sessionManager.saveUserSession(user.id, keepSignedIn = rememberDevice)
        openDashboard()
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

    companion object {
        const val EXTRA_PREFILL_EMAIL = "extra_prefill_email"
    }
}
