package com.example.splurge.ui.goals

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.splurge.R
import com.example.splurge.data.FinanceRepository
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.common.FinanceUiFormatter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate

/**
 * Screen that tracks the user's savings goals and combined progress toward them.
 */
class Goals : BaseActivity() {
    private lateinit var repository: FinanceRepository
    private lateinit var goalsAdapter: SavingsGoalsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_goals)
        repository = FinanceRepository.getInstance(this)
        goalsAdapter = SavingsGoalsAdapter()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.goals_recycler).apply {
            layoutManager = LinearLayoutManager(this@Goals)
            adapter = goalsAdapter
        }

        findViewById<FloatingActionButton>(R.id.add_goal_fab).setOnClickListener {
            showAddGoalDialog()
        }

        bindGoalsScreen()
        setupBottomNavigation(R.id.navigation_goals)
    }

    // Refresh the list after navigation because goals can be changed elsewhere.
    override fun onResume() {
        super.onResume()
        bindGoalsScreen()
    }

    /** Loads savings goal data and updates the summary widgets. */
    private fun bindGoalsScreen() {
        val goals = repository.getSavingsGoals()
        val totalSaved = repository.getTotalSavedAcrossGoals()
        val totalTarget = repository.getTotalGoalTarget()
        val progressPercent = if (totalTarget > 0.0) {
            ((totalSaved / totalTarget) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val halfwayGoals = goals.count { it.targetAmount > 0.0 && it.currentAmount / it.targetAmount >= 0.5 }

        findViewById<TextView>(R.id.total_saved_amount).text = FinanceUiFormatter.formatCurrency(totalSaved)
        findViewById<TextView>(R.id.total_goal_amount).text = getString(
            R.string.total_goal_amount_value,
            FinanceUiFormatter.formatCurrency(totalTarget)
        )
        findViewById<android.widget.ProgressBar>(R.id.overall_progress).apply {
            max = 100
            progress = progressPercent
        }
        findViewById<TextView>(R.id.goals_summary_note).text = if (goals.isEmpty()) {
            getString(R.string.no_savings_goals_summary)
        } else {
            getString(
                R.string.savings_goals_summary_value,
                halfwayGoals,
                goals.size
            )
        }

        goalsAdapter.submitList(goals)
        findViewById<TextView>(R.id.empty_goals_text).visibility =
            if (goals.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Opens the dialog for creating a new savings goal. */
    private fun showAddGoalDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_savings_goal, null, false)
        val nameInput = dialogView.findViewById<TextInputEditText>(R.id.savings_goal_name_input)
        val targetInput = dialogView.findViewById<TextInputEditText>(R.id.savings_goal_target_input)
        val currentInput = dialogView.findViewById<TextInputEditText>(R.id.savings_goal_current_input)
        val dateInput = dialogView.findViewById<TextInputEditText>(R.id.savings_goal_date_input)
        val selectedDate = arrayOf(LocalDate.now().plusMonths(1))

        // Default the target date to next month so the dialog starts with a realistic goal horizon.
        dateInput.setText(
            FinanceUiFormatter.formatDisplayDate(FinanceUiFormatter.formatDate(selectedDate[0]))
        )
        dateInput.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate[0] = LocalDate.of(year, month + 1, dayOfMonth)
                    dateInput.setText(
                        FinanceUiFormatter.formatDisplayDate(FinanceUiFormatter.formatDate(selectedDate[0]))
                    )
                },
                selectedDate[0].year,
                selectedDate[0].monthValue - 1,
                selectedDate[0].dayOfMonth
            ).show()
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_savings_goal_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save_goal_button, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val title = nameInput.text?.toString()?.trim().orEmpty()
            val targetAmount = targetInput.text.toCurrencyValue()
            val currentAmount = currentInput.text.toCurrencyValue()

            // Validate user input before anything is written to the database.
            when {
                title.isEmpty() ->
                    Toast.makeText(this, R.string.invalid_savings_goal_name, Toast.LENGTH_SHORT).show()
                targetAmount <= 0.0 ->
                    Toast.makeText(this, R.string.invalid_savings_goal_target, Toast.LENGTH_SHORT).show()
                currentAmount < 0.0 || currentAmount > targetAmount ->
                    Toast.makeText(this, R.string.invalid_savings_goal_current, Toast.LENGTH_SHORT).show()
                else -> {
                    repository.addSavingsGoal(
                        title = title,
                        targetAmount = targetAmount,
                        currentAmount = currentAmount,
                        targetDate = FinanceUiFormatter.formatDate(selectedDate[0])
                    )
                    bindGoalsScreen()
                    dialog.dismiss()
                }
            }
        }
    }

    /** Parses the dialog's numeric fields into a double value. */
    private fun CharSequence?.toCurrencyValue(): Double {
        return this?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
    }
}
