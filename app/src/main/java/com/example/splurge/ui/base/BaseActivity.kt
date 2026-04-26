package com.example.splurge.ui.base

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.example.splurge.R
import com.example.splurge.ui.budgets.Budgets
import com.example.splurge.ui.goals.Goals
import com.example.splurge.ui.main.MainActivity
import com.example.splurge.ui.profile.Profile
import com.example.splurge.ui.transactions.Transactions
import com.google.android.material.bottomnavigation.BottomNavigationView

abstract class BaseActivity : AppCompatActivity() {

    protected fun setupBottomNavigation(selectedItemId: Int) {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.selectedItemId = selectedItemId

        bottomNavigationView.setOnItemSelectedListener { item ->
            if (item.itemId == selectedItemId) {
                return@setOnItemSelectedListener true
            }

            when (item.itemId) {
                R.id.navigation_dashboard -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.navigation_transactions -> {
                    startActivity(Intent(this, Transactions::class.java))
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.navigation_budgets -> {
                    startActivity(Intent(this, Budgets::class.java))
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.navigation_goals -> {
                    startActivity(Intent(this, Goals::class.java))
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, Profile::class.java))
                    finish()
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                    true
                }
                else -> false
            }
        }
    }
}