package com.example.splurge.ui.profile

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.splurge.R
import com.example.splurge.data.AuthSessionManager
import com.example.splurge.data.FinanceRepository
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.welcome.Welcome
import com.google.android.material.button.MaterialButton

/**
 * Placeholder profile screen that currently just hosts the shared bottom navigation.
 */
class Profile : BaseActivity() {
    private lateinit var repository: FinanceRepository
    private lateinit var sessionManager: AuthSessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = FinanceRepository.getInstance(this)
        sessionManager = AuthSessionManager(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bindProfileSummary()
        setupSessionAction()
        setupBottomNavigation(R.id.navigation_profile)
    }

    override fun onResume() {
        super.onResume()
        bindProfileSummary()
    }

    private fun bindProfileSummary() {
        val sessionUserId = sessionManager.getSignedInUserId()
        val user = sessionUserId?.let(repository::getUserById)
        findViewById<TextView>(R.id.profile_name).text = user?.fullName ?: getString(R.string.profile_guest_name)
        findViewById<TextView>(R.id.profile_email).text = user?.email ?: getString(R.string.profile_guest_email)

        findViewById<MaterialButton>(R.id.buttonProfileSessionAction).text =
            if (user == null) getString(R.string.profile_login_button) else getString(R.string.profile_logout_button)
    }

    private fun setupSessionAction() {
        findViewById<MaterialButton>(R.id.buttonProfileSessionAction).setOnClickListener {
            sessionManager.clearUserSession()
            startActivity(
                Intent(this, Welcome::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
            )
            finish()
        }
    }
}
