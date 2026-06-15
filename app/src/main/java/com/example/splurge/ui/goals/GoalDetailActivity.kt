package com.example.splurge.ui.goals

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.splurge.R
import com.example.splurge.data.AuthSessionManager
import com.example.splurge.data.GoalRepository
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.data.ProgressUpdateResult
import com.example.splurge.data.local.GoalEntity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class GoalDetailActivity : AppCompatActivity() {

    private lateinit var repository: GoalRepository
    private lateinit var authSessionManager: AuthSessionManager
    private lateinit var progressBar: ProgressBar
    private lateinit var tvGoalName: TextView
    private lateinit var tvTargetAmount: TextView
    private lateinit var tvCurrentAmount: TextView
    private lateinit var tvProgressPercentage: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvDeadline: TextView
    private lateinit var tvNotes: TextView
    private lateinit var btnAddProgress: Button
    private lateinit var btnEditGoal: Button
    private lateinit var btnDeleteGoal: Button

    private var goalId: Long = -1
    private var currentUserId: Long = -1L
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    private lateinit var privacyPreferences: PrivacyPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goal_detail)

        repository = GoalRepository(this)
        authSessionManager = AuthSessionManager(this)
        privacyPreferences = PrivacyPreferences(this)

        currentUserId = authSessionManager.getSignedInUserId() ?: -1L
        goalId = intent.getLongExtra("goal_id", -1)

        if (goalId == -1L) {
            Toast.makeText(this, "Error: Goal not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        if (currentUserId == -1L) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        loadGoalDetails()
        setupClickListeners()
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        tvGoalName = findViewById(R.id.tvGoalName)
        tvTargetAmount = findViewById(R.id.tvTargetAmount)
        tvCurrentAmount = findViewById(R.id.tvCurrentAmount)
        tvProgressPercentage = findViewById(R.id.tvProgressPercentage)
        tvCategory = findViewById(R.id.tvCategory)
        tvDeadline = findViewById(R.id.tvDeadline)
        tvNotes = findViewById(R.id.tvNotes)
        btnAddProgress = findViewById(R.id.btnAddProgress)
        btnEditGoal = findViewById(R.id.btnEditGoal)
        btnDeleteGoal = findViewById(R.id.btnDeleteGoal)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun loadGoalDetails() {
        lifecycleScope.launch {
            val goal = repository.getGoalById(goalId, currentUserId)
            goal?.let { displayGoalDetails(it) }
                ?: run {
                    Toast.makeText(this@GoalDetailActivity, "Goal not found", Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
    }

    private fun displayGoalDetails(goal: GoalEntity) {
        val progress = if (goal.targetAmount > 0) {
            ((goal.currentAmount / goal.targetAmount) * 100).toFloat()
        } else 0f
        val privacyEnabled = privacyPreferences.isHideBalancesEnabled()

        tvGoalName.text = goal.name
        tvTargetAmount.text = privacyPreferences.formatCurrency(goal.targetAmount)
        tvCurrentAmount.text = privacyPreferences.formatCurrency(goal.currentAmount)
        tvProgressPercentage.text = if (privacyEnabled) {
            getString(R.string.privacy_hidden_value)
        } else {
            String.format(Locale.US, "%.1f%%", progress)
        }
        tvCategory.text = goal.category
        tvDeadline.text = dateFormat.format(goal.deadline)
        tvNotes.text = goal.notes.ifEmpty { "No notes added" }

        progressBar.max = 100
        progressBar.progress = if (privacyEnabled) 0 else progress.toInt()

        val progressColor = when {
            progress >= 90f -> android.R.color.holo_green_dark
            progress >= 60f -> android.R.color.holo_blue_dark
            progress >= 30f -> android.R.color.holo_orange_dark
            else -> android.R.color.holo_red_dark
        }
        progressBar.progressTintList = ContextCompat.getColorStateList(this, progressColor)

        val categoryColor = when (goal.category) {
            "SAVING" -> android.R.color.holo_green_dark
            "SPENDING_LIMIT" -> android.R.color.holo_orange_dark
            "INVESTMENT" -> android.R.color.holo_blue_dark
            "DEBT_PAYMENT" -> android.R.color.holo_red_dark
            else -> android.R.color.holo_purple
        }
        tvCategory.setBackgroundColor(ContextCompat.getColor(this, categoryColor))
    }

    private fun setupClickListeners() {
        btnAddProgress.setOnClickListener {
            showAddProgressDialog()
        }

        btnEditGoal.setOnClickListener {
            val intent = Intent(this, AddEditGoalActivity::class.java)
            intent.putExtra("goal_id", goalId)
            intent.putExtra("is_edit", true)
            startActivity(intent)
        }

        btnDeleteGoal.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun showAddProgressDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_progress, null)  // Your actual filename with typo
        val tvGoalNameDialog = dialogView.findViewById<TextView>(R.id.tvGoalName)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAmount)

        // Get goal name to display in dialog
        lifecycleScope.launch {
            val goal = repository.getGoalById(goalId, currentUserId)
            tvGoalNameDialog.text = goal?.name ?: "this goal"
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Progress")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val amountText = etAmount.text.toString()
                if (amountText.isNotEmpty()) {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        updateProgress(amount)
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

    private fun updateProgress(amount: Double) {
        lifecycleScope.launch {
            val result = repository.updateProgress(goalId, currentUserId, amount)
            when (result) {
                is ProgressUpdateResult.Updated -> {
                    Toast.makeText(this@GoalDetailActivity, "Progress updated! ${result.message}", Toast.LENGTH_SHORT).show()
                    loadGoalDetails()

                    if (result.percentage >= 90) {
                        showMotivationalMessage(result.message)
                    }
                }
                is ProgressUpdateResult.Completed -> {
                    showCelebrationMessage()
                    Toast.makeText(this@GoalDetailActivity, "🎉 Goal Completed! 🎉", Toast.LENGTH_LONG).show()
                    finish()
                }
                is ProgressUpdateResult.GoalNotFound -> {
                    Toast.makeText(this@GoalDetailActivity, "Goal not found", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showMotivationalMessage(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Great Progress!")
            .setMessage(message)
            .setPositiveButton("Continue", null)
            .show()
    }

    private fun showCelebrationMessage() {
        AlertDialog.Builder(this)
            .setTitle("Congratulations!")
            .setMessage("You've achieved your goal! Keep up the great work!")
            .setPositiveButton("Awesome!", null)
            .show()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Delete Goal")
            .setMessage("Are you sure you want to delete this goal? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteGoal()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteGoal() {
        lifecycleScope.launch {
            repository.deleteGoal(goalId, currentUserId)
            Toast.makeText(this@GoalDetailActivity, "Goal deleted", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
