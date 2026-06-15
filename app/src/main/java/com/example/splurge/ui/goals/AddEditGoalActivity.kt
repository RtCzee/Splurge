package com.example.splurge.ui.goals

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.splurge.R
import com.example.splurge.data.AuthSessionManager
import com.example.splurge.data.GoalRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AddEditGoalActivity : AppCompatActivity() {

    private lateinit var repository: GoalRepository
    private lateinit var authSessionManager: AuthSessionManager
    private lateinit var etGoalName: EditText
    private lateinit var etTargetAmount: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var etNotes: EditText
    private lateinit var btnSaveGoal: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDeadline: Button

    private var selectedDeadline: Calendar = Calendar.getInstance()
    private var isEditMode = false
    private var goalId: Long = -1
    private var currentUserId: Long = -1L
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_goal)

        repository = GoalRepository(this)
        authSessionManager = AuthSessionManager(this)

        currentUserId = authSessionManager.getSignedInUserId() ?: -1L
        isEditMode = intent.getBooleanExtra("is_edit", false)
        goalId = intent.getLongExtra("goal_id", -1)

        if (currentUserId == -1L) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupSpinner()
        setupClickListeners()

        if (isEditMode && goalId != -1L) {
            supportActionBar?.title = "Edit Goal"
            loadGoalForEditing()
        } else {
            supportActionBar?.title = "Create Goal"
            selectedDeadline.time = getEndOfMonth()
            updateDeadlineButtonText()
        }

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun initViews() {
        etGoalName = findViewById(R.id.etGoalName)
        etTargetAmount = findViewById(R.id.etTargetAmount)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        etNotes = findViewById(R.id.etNotes)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)
        btnCancel = findViewById(R.id.btnCancel)
        btnDeadline = findViewById(R.id.btnDeadline)
    }

    private fun setupSpinner() {
        val categories = listOf("SAVING", "SPENDING_LIMIT", "INVESTMENT", "DEBT_PAYMENT", "OTHER")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupClickListeners() {
        btnSaveGoal.setOnClickListener {
            saveGoal()
        }

        btnCancel.setOnClickListener {
            finish()
        }

        btnDeadline.setOnClickListener {
            showDatePicker()
        }
    }

    private fun loadGoalForEditing() {
        lifecycleScope.launch {
            val goal = repository.getGoalById(goalId, currentUserId)
            if (goal != null) {
                etGoalName.setText(goal.name)
                etTargetAmount.setText(goal.targetAmount.toString())
                etNotes.setText(goal.notes)

                val categories = listOf("SAVING", "SPENDING_LIMIT", "INVESTMENT", "DEBT_PAYMENT", "OTHER")
                val position = categories.indexOf(goal.category)
                if (position >= 0) {
                    spinnerCategory.setSelection(position)
                }

                selectedDeadline.time = goal.deadline
                updateDeadlineButtonText()
            } else {
                Toast.makeText(this@AddEditGoalActivity, "Goal not found", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun saveGoal() {
        val name = etGoalName.text.toString().trim()
        val targetAmountText = etTargetAmount.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val notes = etNotes.text.toString().trim()

        if (name.isEmpty()) {
            etGoalName.error = "Goal name is required"
            return
        }

        if (targetAmountText.isEmpty()) {
            etTargetAmount.error = "Target amount is required"
            return
        }

        val targetAmount = targetAmountText.toDoubleOrNull()
        if (targetAmount == null || targetAmount <= 0) {
            etTargetAmount.error = "Please enter a valid amount"
            return
        }

        lifecycleScope.launch {
            try {
                if (isEditMode) {
                    // For edit, we need to delete and recreate or implement update
                    // For now, show message
                    Toast.makeText(
                        this@AddEditGoalActivity,
                        "Edit feature coming soon - please delete and recreate",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    repository.createGoal(
                        userId = currentUserId,
                        name = name,
                        targetAmount = targetAmount,
                        category = category,
                        notes = notes,
                        deadline = selectedDeadline.time
                    )
                    Toast.makeText(this@AddEditGoalActivity, "Goal created!", Toast.LENGTH_SHORT).show()
                }
                finish()
            } catch (e: Exception) {
                Toast.makeText(
                    this@AddEditGoalActivity,
                    "Error: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDeadline.set(year, month, dayOfMonth, 23, 59, 59)
                updateDeadlineButtonText()
            },
            selectedDeadline.get(Calendar.YEAR),
            selectedDeadline.get(Calendar.MONTH),
            selectedDeadline.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDeadlineButtonText() {
        btnDeadline.text = "Deadline: ${dateFormat.format(selectedDeadline.time)}"
    }

    private fun getEndOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar.time
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