package com.example.splurge.ui.welcome

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.splurge.R
import com.example.splurge.data.AuthSessionManager
import com.example.splurge.data.FinanceRepository
import com.example.splurge.ui.login.Login
import com.example.splurge.ui.main.MainActivity
import com.example.splurge.ui.signup.Signup
import com.google.android.material.button.MaterialButton

class Welcome : AppCompatActivity() {
    private lateinit var sessionManager: AuthSessionManager
    private lateinit var repository: FinanceRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sessionManager = AuthSessionManager(this)
        repository = FinanceRepository.getInstance(this)
        val signedInUserId = sessionManager.getSignedInUserId()
        if (
            signedInUserId != null &&
            sessionManager.shouldKeepUserSignedIn() &&
            repository.getUserById(signedInUserId) != null
        ) {
            openDashboard()
            finish()
            return
        } else if (signedInUserId != null) {
            sessionManager.clearUserSession()
        }
        enableEdgeToEdge()
        setContentView(R.layout.activity_welcome)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<MaterialButton>(R.id.buttonWelcomeLogin).setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }

        findViewById<MaterialButton>(R.id.buttonWelcomeSignup).setOnClickListener {
            startActivity(Intent(this, Signup::class.java))
        }

        findViewById<TextView>(R.id.textWelcomeGuest).setOnClickListener {
            openDashboard()
        }
    }

    private fun openDashboard() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
}
