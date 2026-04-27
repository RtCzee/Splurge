package com.example.splurge.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.splurge.R
import com.example.splurge.data.FinanceRepository
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.common.FinanceUiFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

class MainActivity : BaseActivity() {
    private lateinit var repository: FinanceRepository

    private var dashboardOverview = DashboardOverview(
        currentBalance = 12480.0,
        todaySpending = 420.0,
        splurgeMoney = 980.0,
        monthlyBudget = 12000.0,
        budgetRemaining = 8150.0,
        budgetStatus = "On Track",
        spendingNote = "Groceries and transport",
        splurgeNote = "Available for wants this week"
    )
    private var calculatorInput = SplurgeCalculatorInput(
        income = 14500.0,
        expenses = 9800.0,
        savingsGoal = 3720.0
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        repository = FinanceRepository.getInstance(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupCalculatorButton()
        refreshDashboardFromDatabase()
        setupBottomNavigation(R.id.navigation_dashboard)
    }

    override fun onResume() {
        super.onResume()
        refreshDashboardFromDatabase()
    }

    private fun setupCalculatorButton() {
        findViewById<MaterialButton>(R.id.calculate_splurge_button).setOnClickListener {
            showSplurgeCalculatorDialog()
        }
    }

    private fun bindOverview(overview: DashboardOverview) {
        val budgetUsedPercent = if (overview.monthlyBudget > 0.0) {
            ((overview.spentBudget / overview.monthlyBudget) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }

        findViewById<TextView>(R.id.current_balance_value).text = formatCurrency(overview.currentBalance)
        findViewById<TextView>(R.id.balance_note).text =
            getString(R.string.dashboard_balance_note_value, formatCurrency(overview.splurgeMoney))
        findViewById<TextView>(R.id.today_spending_value).text = formatCurrency(overview.todaySpending)
        findViewById<TextView>(R.id.today_spending_note).text = overview.spendingNote
        findViewById<TextView>(R.id.splurge_money_value).text = formatCurrency(overview.splurgeMoney)
        findViewById<TextView>(R.id.splurge_money_note).text = overview.splurgeNote
        findViewById<TextView>(R.id.budget_status_value).text = overview.budgetStatus
        findViewById<TextView>(R.id.budget_status_caption).text = if (overview.monthlyBudget > 0.0) {
            getString(R.string.dashboard_budget_used_percent, budgetUsedPercent)
        } else {
            getString(R.string.dashboard_budget_goal_missing_caption)
        }
        findViewById<TextView>(R.id.budget_status_note).text = if (overview.monthlyBudget > 0.0) {
            getString(
                R.string.dashboard_budget_status_note,
                formatCurrency(overview.budgetRemaining),
                formatCurrency(overview.monthlyBudget)
            )
        } else {
            getString(R.string.dashboard_budget_goal_missing_note)
        }
        findViewById<LinearProgressIndicator>(R.id.budget_status_progress).progress = budgetUsedPercent
    }

    private fun showSplurgeCalculatorDialog() {
        calculatorInput = calculatorInput.copy(expenses = getCurrentMonthSpent())
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_splurge_calculator, null, false)
        val incomeInput = dialogView.findViewById<TextInputEditText>(R.id.income_input)
        val expensesInput = dialogView.findViewById<TextInputEditText>(R.id.expenses_input)
        val savingsGoalInput = dialogView.findViewById<TextInputEditText>(R.id.savings_goal_input)
        val resultView = dialogView.findViewById<TextView>(R.id.calculated_splurge_result)
        val noteView = dialogView.findViewById<TextView>(R.id.calculated_splurge_note)

        incomeInput.setText(formatPlainAmount(calculatorInput.income))
        expensesInput.setText(formatPlainAmount(calculatorInput.expenses))
        savingsGoalInput.setText(formatPlainAmount(calculatorInput.savingsGoal))
        expensesInput.isFocusable = false
        expensesInput.isClickable = false
        expensesInput.isCursorVisible = false

        val updateCalculator = {
            calculatorInput = calculatorInput.copy(
                income = incomeInput.text.toCurrencyInput(),
                savingsGoal = savingsGoalInput.text.toCurrencyInput()
            )

            val calculatedSplurge = applySplurgeCalculation()
            resultView.text = formatCurrency(calculatedSplurge)
            noteView.text = buildSplurgeNote(
                calculatorInput = calculatorInput,
                splurgeMoney = calculatedSplurge
            )
        }

        incomeInput.doAfterTextChanged { updateCalculator() }
        expensesInput.doAfterTextChanged { updateCalculator() }
        savingsGoalInput.doAfterTextChanged { updateCalculator() }
        updateCalculator()

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.calculator_title)
            .setView(dialogView)
            .setPositiveButton(R.string.calculator_done, null)
            .show()
    }

    private fun applySplurgeCalculation(): Double {
        val calculatedSplurge = calculateSplurge(
            income = calculatorInput.income,
            expenses = calculatorInput.expenses,
            savingsGoal = calculatorInput.savingsGoal
        )

        dashboardOverview = dashboardOverview.copy(
            splurgeMoney = calculatedSplurge,
            splurgeNote = buildSplurgeNote(calculatorInput, calculatedSplurge)
        )
        bindOverview(dashboardOverview)
        return calculatedSplurge
    }

    private fun refreshDashboardFromDatabase() {
        val currentMonthSpent = getCurrentMonthSpent()
        val today = FinanceUiFormatter.formatDate(LocalDate.now())
        val todaySpent = repository.getTotalSpentForPeriod(today, today)
        val currentMonth = YearMonth.now()
        val monthlyGoal = repository.getMonthlyGoal(FinanceUiFormatter.monthKey(currentMonth))
        val maximumGoal = monthlyGoal?.maximumGoal ?: 0.0
        val remainingBudget = if (maximumGoal > 0.0) {
            (maximumGoal - currentMonthSpent).coerceAtLeast(0.0)
        } else {
            0.0
        }

        calculatorInput = calculatorInput.copy(expenses = currentMonthSpent)
        dashboardOverview = dashboardOverview.copy(
            todaySpending = todaySpent,
            monthlyBudget = maximumGoal,
            budgetRemaining = remainingBudget,
            budgetStatus = buildBudgetStatus(currentMonthSpent, monthlyGoal?.minimumGoal ?: 0.0, maximumGoal),
            spendingNote = getString(R.string.dashboard_spending_note_value, FinanceUiFormatter.formatMonthLabel(currentMonth))
        )
        applySplurgeCalculation()
    }

    private fun getCurrentMonthSpent(): Double {
        val month = YearMonth.now()
        return repository.getTotalSpentForPeriod(
            FinanceUiFormatter.formatDate(month.atDay(1)),
            FinanceUiFormatter.formatDate(month.atEndOfMonth())
        )
    }

    private fun buildBudgetStatus(
        monthSpent: Double,
        minimumGoal: Double,
        maximumGoal: Double
    ): String {
        return when {
            maximumGoal <= 0.0 && minimumGoal <= 0.0 ->
                getString(R.string.dashboard_budget_goal_missing_title)
            maximumGoal > 0.0 && monthSpent > maximumGoal ->
                getString(R.string.dashboard_budget_status_over)
            minimumGoal > 0.0 && monthSpent < minimumGoal ->
                getString(R.string.dashboard_budget_status_below)
            else ->
                getString(R.string.dashboard_budget_status_good)
        }
    }

    private fun calculateSplurge(income: Double, expenses: Double, savingsGoal: Double): Double {
        return (income - expenses - savingsGoal).coerceAtLeast(0.0)
    }

    private fun buildSplurgeNote(
        calculatorInput: SplurgeCalculatorInput,
        splurgeMoney: Double
    ): String {
        return if (splurgeMoney <= 0.0) {
            getString(R.string.calculator_no_splurge_note)
        } else {
            getString(
                R.string.calculator_result_breakdown,
                formatCurrency(calculatorInput.income),
                formatCurrency(calculatorInput.expenses),
                formatCurrency(calculatorInput.savingsGoal)
            )
        }
    }

    private fun formatCurrency(amount: Double): String {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-ZA")).format(amount)
    }

    private fun formatPlainAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            amount.toString()
        }
    }

    private fun CharSequence?.toCurrencyInput(): Double {
        return this?.toString()?.trim()?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

    private data class DashboardOverview(
        val currentBalance: Double,
        val todaySpending: Double,
        val splurgeMoney: Double,
        val monthlyBudget: Double,
        val budgetRemaining: Double,
        val budgetStatus: String,
        val spendingNote: String,
        val splurgeNote: String
    ) {
        val spentBudget: Double
            get() = monthlyBudget - budgetRemaining
    }

    private data class SplurgeCalculatorInput(
        val income: Double,
        val expenses: Double,
        val savingsGoal: Double
    )
}

