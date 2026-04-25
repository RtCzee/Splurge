package com.example.splurge

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickableNavigation()
    }

    private fun setupClickableNavigation() {
        // Top Navigation Bar Links
        val navBudgets = findViewById<TextView>(R.id.nav_budgets)
        navBudgets?.setOnClickListener { navigateToBudgets() }

        val navGoals = findViewById<TextView>(R.id.nav_goals)
        navGoals?.setOnClickListener { navigateToGoals() }

        val navSettings = findViewById<TextView>(R.id.nav_settings)
        navSettings?.setOnClickListener { navigateToProfile() }

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

        // Splurge Card Links
        val adjustBudgetLink = findViewById<TextView>(R.id.adjust_budget_link)
        adjustBudgetLink?.setOnClickListener { navigateToBudgets() }

        val splurgeCard = findViewById<MaterialCardView>(R.id.splurge_card)
        splurgeCard?.setOnClickListener { navigateToBudgets() }

        // Transactions Link
        val viewAllTransactionsLink = findViewById<TextView>(R.id.view_all_transactions_link)
        viewAllTransactionsLink?.setOnClickListener { navigateToTransactions() }
    }

    private fun navigateToBudgets() {
        val intent = Intent(this, Budgets::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToGoals() {
        val intent = Intent(this, Goals::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToProfile() {
        val intent = Intent(this, Profile::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToTransactions() {
        val intent = Intent(this, Transactions::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}