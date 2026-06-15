package com.example.splurge.ui.goals

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.splurge.R
import com.example.splurge.data.AuthSessionManager
import com.example.splurge.data.GoalRepository
import com.example.splurge.data.ProgressUpdateResult
import com.example.splurge.data.local.GoalEntity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

class GoalsActivity : AppCompatActivity() {

    private lateinit var repository: GoalRepository
    private lateinit var authSessionManager: AuthSessionManager
    private lateinit var goalAdapter: GoalAdapter
    private lateinit var rvGoals: RecyclerView
    private lateinit var fabAddGoal: FloatingActionButton
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var cardMotivational: MaterialCardView
    private lateinit var tvMotivationalTitle: TextView
    private lateinit var tvMotivationalMessage: TextView
    private lateinit var btnDismissMotivational: Button
    private lateinit var btnHistory: Button
    private lateinit var cardMonthlySummary: MaterialCardView
    private lateinit var tvAchievedCount: TextView
    private lateinit var tvMissedCount: TextView
    private lateinit var tvCompletionRate: TextView
    private lateinit var tvSummaryMessage: TextView

    private var currentUserId: Long = -1L
    private val currencyFormat = NumberFormat.getCurrencyInstance(Locale("en", "ZA"))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activty_monthly_goal)  // Your actual filename with typo

        repository = GoalRepository(this)
        authSessionManager = AuthSessionManager(this)

        currentUserId = authSessionManager.getSignedInUserId() ?: -1L

        if (currentUserId == -1L) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupRecyclerView()
        setupClickListeners()
        loadGoals()
        loadMonthlySummary()
    }

    private fun initViews() {
        rvGoals = findViewById(R.id.rvGoals)
        fabAddGoal = findViewById(R.id.fabAddGoal)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        cardMotivational = findViewById(R.id.cardMotivational)
        tvMotivationalTitle = findViewById(R.id.tvMotivationalTitle)
        tvMotivationalMessage = findViewById(R.id.tvMotivationalMessage)
        btnDismissMotivational = findViewById(R.id.btnDismissMotivational)
        btnHistory = findViewById(R.id.btnHistory)
        cardMonthlySummary = findViewById(R.id.cardMonthlySummary)
        tvAchievedCount = findViewById(R.id.tvAchievedCount)
        tvMissedCount = findViewById(R.id.tvMissedCount)
        tvCompletionRate = findViewById(R.id.tvCompletionRate)
        tvSummaryMessage = findViewById(R.id.tvSummaryMessage)
    }

    private fun setupRecyclerView() {
        goalAdapter = GoalAdapter { goal ->
            showAddProgressDialog(goal)
        }
        rvGoals.layoutManager = LinearLayoutManager(this)
        rvGoals.adapter = goalAdapter
    }

    private fun setupClickListeners() {
        fabAddGoal.setOnClickListener {
            showCreateGoalDialog()
        }

        btnDismissMotivational.setOnClickListener {
            cardMotivational.visibility = android.view.View.GONE
        }

        btnHistory.setOnClickListener {
            startActivity(Intent(this, GoalHistoryActivity::class.java))
        }
    }

    private fun loadGoals() {
        lifecycleScope.launch {
            repository.getActiveGoals(currentUserId).collect { goals ->
                if (goals.isEmpty()) {
                    rvGoals.visibility = android.view.View.GONE
                    layoutEmpty.visibility = android.view.View.VISIBLE
                } else {
                    rvGoals.visibility = android.view.View.VISIBLE
                    layoutEmpty.visibility = android.view.View.GONE
                    goalAdapter.submitList(goals)
                }
            }
        }
    }

    private fun loadMonthlySummary() {
        lifecycleScope.launch {
            val review = repository.getMonthlyReview(currentUserId)
            if (review.totalGoals > 0) {
                cardMonthlySummary.visibility = android.view.View.VISIBLE
                tvAchievedCount.text = review.completed.toString()
                tvMissedCount.text = review.missed.toString()
                tvCompletionRate.text = String.format(Locale.US, "%.0f%%", review.successRate)
                tvSummaryMessage.text = review.message
            }
        }
    }

    private fun showCreateGoalDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_edit_goal, null)  // Your actual filename
        val etName = dialogView.findViewById<EditText>(R.id.etGoalName)
        val etAmount = dialogView.findViewById<EditText>(R.id.etTargetAmount)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Create Monthly Goal")
            .setView(dialogView)
            .setPositiveButton("Create") { _, _ ->
                val name = etName.text.toString().trim()
                val amountText = etAmount.text.toString().trim()

                if (name.isEmpty()) {
                    Toast.makeText(this, "Please enter a goal name", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (amountText.isEmpty()) {
                    Toast.makeText(this, "Please enter a target amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val amount = amountText.toDoubleOrNull()
                if (amount == null || amount <= 0) {
                    Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                lifecycleScope.launch {
                    repository.createGoal(
                        userId = currentUserId,
                        name = name,
                        targetAmount = amount,
                        category = "SAVING"
                    )
                    Toast.makeText(this@GoalsActivity, "Goal created!", Toast.LENGTH_SHORT).show()
                    loadGoals()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun showAddProgressDialog(goal: GoalEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_progress, null)  // Your actual filename
        val tvGoalName = dialogView.findViewById<TextView>(R.id.tvGoalName)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)

        tvGoalName.text = goal.name

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Progress")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val amountText = etAmount.text.toString()
                if (amountText.isNotEmpty()) {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        updateGoalProgress(goal, amount)
                    } else {
                        Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please enter an amount", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
    }

    private fun updateGoalProgress(goal: GoalEntity, amount: Double) {
        lifecycleScope.launch {
            val result = repository.updateProgress(goal.id, currentUserId, amount)
            when (result) {
                is ProgressUpdateResult.Updated -> {
                    Toast.makeText(this@GoalsActivity, result.message, Toast.LENGTH_SHORT).show()
                    loadGoals()
                    loadMonthlySummary()

                    if (result.percentage >= 90) {
                        showMotivationalMessage("Almost there!", result.message)
                    }
                }
                is ProgressUpdateResult.Completed -> {
                    showCelebrationDialog(goal)
                    loadGoals()
                    loadMonthlySummary()
                }
                is ProgressUpdateResult.GoalNotFound -> {
                    Toast.makeText(this@GoalsActivity, "Goal not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showMotivationalMessage(title: String, message: String) {
        tvMotivationalTitle.text = title
        tvMotivationalMessage.text = message
        cardMotivational.visibility = android.view.View.VISIBLE

        Handler(Looper.getMainLooper()).postDelayed({
            cardMotivational.visibility = android.view.View.GONE
        }, 5000)
    }

    private fun showCelebrationDialog(goal: GoalEntity) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_celebration, null)  // Your actual filename
        val tvGoalName = dialogView.findViewById<TextView>(R.id.tvGoalName)
        val tvAmount = dialogView.findViewById<TextView>(R.id.tvAmount)
        val tvEmoji = dialogView.findViewById<TextView>(R.id.tvEmoji)

        tvGoalName.text = goal.name
        tvAmount.text = currencyFormat.format(goal.targetAmount)

        val emojis = listOf("🎉", "🏆", "🌟", "💪", "🎊", "✨")
        var index = 0
        val handler = Handler(Looper.getMainLooper())
        val runnable = object : Runnable {
            override fun run() {
                tvEmoji.text = emojis[index % emojis.size]
                index++
                handler.postDelayed(this, 200)
            }
        }
        handler.post(runnable)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .setPositiveButton("Awesome!") { _, _ ->
                handler.removeCallbacks(runnable)
            }
            .create()

        dialog.show()
    }
}