package com.example.splurge.ui.main

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.splurge.R
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.budgets.Budgets
import com.example.splurge.ui.goals.Goals
import com.example.splurge.ui.profile.Profile
import com.example.splurge.ui.transactions.Transactions
import com.google.android.material.card.MaterialCardView

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNavigation(R.id.navigation_dashboard)
    }

    private fun setupClickableNavigation() {
        // Budget Status Card Links
        val viewAllBudgetsLink = findViewById<TextView>(R.id.view_all_budgets_link)
        viewAllBudgetsLink?.setOnClickListener { navigateToBudgets() }

        val budgetStatusCard = findViewById<MaterialCardView>(R.id.budget_status_card)
        budgetStatusCard?.setOnClickListener { navigateToBudgets() }

        // Goals Card Links
        val viewAllGoalsLink = findViewById<TextView>(R.id.view_all_goals_link)
        viewAllGoalsLink?.setOnClickListener { navigateToGoals() }

        val goalsCard = findViewById<MaterialCardView>(R.id.goals_card)
        goalsCard?.setOnClickListener { navigateToGoals() }

        // Transactions Link
        val viewAllTransactionsLink = findViewById<TextView>(R.id.view_all_transactions_link)
        viewAllTransactionsLink?.setOnClickListener { navigateToTransactions() }
    }

    private fun navigateToBudgets() {
        val intent = Intent(this, Budgets::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToGoals() {
        val intent = Intent(this, Goals::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToProfile() {
        val intent = Intent(this, Profile::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToTransactions() {
        val intent = Intent(this, Transactions::class.java)
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}