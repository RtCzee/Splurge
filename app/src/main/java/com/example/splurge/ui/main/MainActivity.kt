package com.example.splurge.ui.main

import android.os.Bundle
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.core.widget.doAfterTextChanged
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.card.MaterialCardView
import com.example.splurge.R
import com.example.splurge.data.FinanceRepository
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.notifications.BillReminderPreferences
import com.example.splurge.ui.bills.Bills
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.common.FinanceUiFormatter
import com.example.splurge.ui.graph.CategorySpendingGraphActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import java.time.LocalDate
import java.time.YearMonth

/**
 * Dashboard screen that summarizes the user's current finances and splurge allowance.
 */
class MainActivity : BaseActivity() {
    private lateinit var repository: FinanceRepository
    private lateinit var reminderPreferences: BillReminderPreferences
    private lateinit var privacyPreferences: PrivacyPreferences

    // These defaults let the dashboard render immediately before live values are loaded.
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
        reminderPreferences = BillReminderPreferences(this)
        privacyPreferences = PrivacyPreferences(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupCalculatorButton()
        setupBillsShortcut()
        refreshDashboardFromDatabase()
        setupBottomNavigation(R.id.navigation_dashboard)
        setupAnalyticsNavigation()
    }

    // Refresh when returning from other screens because goals and expenses may have changed.
    override fun onResume() {
        super.onResume()
        refreshDashboardFromDatabase()
    }

    /** Wires the dashboard button that opens the splurge calculator dialog. */
    private fun setupCalculatorButton() {
        findViewById<MaterialButton>(R.id.calculate_splurge_button).setOnClickListener {
            showSplurgeCalculatorDialog()
        }
    }

    /** Opens the bills screen from the dashboard summary card. */
    private fun setupBillsShortcut() {
        findViewById<MaterialButton>(R.id.open_bills_button).setOnClickListener {
            startActivity(Intent(this, Bills::class.java))
        }
    }

    /** Pushes the latest dashboard model values into the visible widgets. */
    private fun bindOverview(overview: DashboardOverview) {
        val budgetUsedPercent = if (overview.monthlyBudget > 0.0) {
            ((overview.spentBudget / overview.monthlyBudget) * 100).toInt().coerceIn(0, 100)
        } else {
            0
        }
        val privacyEnabled = privacyPreferences.isHideBalancesEnabled()
        val currentBalanceValue = privacyPreferences.formatCurrency(overview.currentBalance)
        val todaySpendingValue = privacyPreferences.formatCurrency(overview.todaySpending)
        val splurgeMoneyValue = privacyPreferences.formatCurrency(overview.splurgeMoney)
        val budgetRemainingValue = privacyPreferences.formatCurrency(overview.budgetRemaining)
        val monthlyBudgetValue = privacyPreferences.formatCurrency(overview.monthlyBudget)

        findViewById<TextView>(R.id.current_balance_value).text = currentBalanceValue
        findViewById<TextView>(R.id.balance_note).text =
            if (privacyEnabled) {
                getString(R.string.privacy_hidden_message)
            } else {
                getString(R.string.dashboard_balance_note_value, splurgeMoneyValue)
            }
        findViewById<TextView>(R.id.today_spending_value).text = todaySpendingValue
        findViewById<TextView>(R.id.today_spending_note).text = overview.spendingNote
        findViewById<TextView>(R.id.splurge_money_value).text = splurgeMoneyValue
        findViewById<TextView>(R.id.splurge_money_note).text =
            if (privacyEnabled) getString(R.string.privacy_hidden_message) else overview.splurgeNote
        findViewById<TextView>(R.id.budget_status_value).text =
            if (privacyEnabled) getString(R.string.privacy_hidden_value) else overview.budgetStatus
        findViewById<TextView>(R.id.budget_status_caption).text = if (privacyEnabled) {
            getString(R.string.privacy_hidden_message)
        } else if (overview.monthlyBudget > 0.0) {
            getString(R.string.dashboard_budget_used_percent, budgetUsedPercent)
        } else {
            getString(R.string.dashboard_budget_goal_missing_caption)
        }
        findViewById<TextView>(R.id.budget_status_note).text = if (overview.monthlyBudget > 0.0) {
            if (privacyEnabled) {
                getString(R.string.privacy_hidden_message)
            } else {
                getString(
                    R.string.dashboard_budget_status_note,
                    budgetRemainingValue,
                    monthlyBudgetValue
                )
            }
        } else {
            getString(R.string.dashboard_budget_goal_missing_note)
        }
        findViewById<LinearProgressIndicator>(R.id.budget_status_progress).apply {
            visibility = if (privacyEnabled) View.GONE else View.VISIBLE
            progress = if (privacyEnabled) 0 else budgetUsedPercent
        }
    }

    /** Opens the calculator dialog and keeps its result synced as inputs change. */
    private fun showSplurgeCalculatorDialog() {
        // Always pull fresh month spending before showing the calculator.
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
            // Only income and savings are user editable; expenses are sourced from transactions.
            calculatorInput = calculatorInput.copy(
                income = incomeInput.text.toCurrencyInput(),
                savingsGoal = savingsGoalInput.text.toCurrencyInput()
            )

            val calculatedSplurge = applySplurgeCalculation()
            if (privacyPreferences.isHideBalancesEnabled()) {
                resultView.text = getString(R.string.privacy_hidden_value)
                noteView.text = getString(R.string.privacy_hidden_message)
            } else {
                resultView.text = privacyPreferences.formatCurrency(calculatedSplurge)
                noteView.text = buildSplurgeNote(
                    calculatorInput = calculatorInput,
                    splurgeMoney = calculatedSplurge
                )
            }
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

    /** Recomputes splurge money and reflects the result back on the dashboard. */
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

    /** Reads the latest totals from the repository and rebuilds the dashboard state. */
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
        bindBillSummary()
    }

    /** Returns how much has been spent from the first to the last day of the current month. */
    private fun getCurrentMonthSpent(): Double {
        val month = YearMonth.now()
        return repository.getTotalSpentForPeriod(
            FinanceUiFormatter.formatDate(month.atDay(1)),
            FinanceUiFormatter.formatDate(month.atEndOfMonth())
        )
    }

    /** Produces a short budget status label from the month's spend versus the saved goal range. */
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

    /** Calculates how much money is left for discretionary spending. */
    private fun calculateSplurge(income: Double, expenses: Double, savingsGoal: Double): Double {
        return (income - expenses - savingsGoal).coerceAtLeast(0.0)
    }

    /** Explains how the splurge number was derived for the user. */
    private fun buildSplurgeNote(
        calculatorInput: SplurgeCalculatorInput,
        splurgeMoney: Double
    ): String {
        return if (splurgeMoney <= 0.0) {
            getString(R.string.calculator_no_splurge_note)
        } else {
            getString(
                R.string.calculator_result_breakdown,
                privacyPreferences.formatCurrency(calculatorInput.income),
                privacyPreferences.formatCurrency(calculatorInput.expenses),
                privacyPreferences.formatCurrency(calculatorInput.savingsGoal)
            )
        }
    }

    /** Keeps numeric fields tidy by dropping the decimal when the value is a whole number. */
    private fun formatPlainAmount(amount: Double): String {
        return if (amount % 1.0 == 0.0) {
            amount.toInt().toString()
        } else {
            amount.toString()
        }
    }

    /** Mirrors the bill reminder state on the dashboard so due dates stay visible. */
    private fun bindBillSummary() {
        val activeBills = repository.getActiveBills()
        findViewById<TextView>(R.id.bills_summary_value).text = resources.getQuantityString(
            R.plurals.active_bill_count,
            activeBills.size,
            activeBills.size
        )

        val nextBill = activeBills.mapNotNull { bill ->
            com.example.splurge.ui.common.FinanceUiFormatter.parseDateOrNull(bill.dueDate)?.let { dueDate ->
                bill to dueDate
            }
        }.minByOrNull { (_, dueDate) ->
            java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), dueDate)
        }?.first
        findViewById<TextView>(R.id.bills_summary_note).text = nextBill?.let {
            com.example.splurge.ui.bills.BillUiFormatter.buildReminderPreview(this, it, reminderPreferences.getReminderIntervals())
        } ?: getString(R.string.bill_next_due_empty_message)
    }

    /** Parses text input into a non-negative number for calculator use. */
    private fun CharSequence?.toCurrencyInput(): Double {
        return this?.toString()?.trim()?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }

    /** Snapshot of all values rendered by the dashboard summary cards. */
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
        /** Derived amount already used from the configured monthly budget. */
        val spentBudget: Double
            get() = monthlyBudget - budgetRemaining
    }

    /** Inputs required to calculate the user's discretionary "splurge" amount. */
    private data class SplurgeCalculatorInput(
        val income: Double,
        val expenses: Double,
        val savingsGoal: Double
    )

    private fun setupAnalyticsNavigation() {
        val analyticsCard = findViewById<MaterialCardView>(R.id.analytics_card)
        analyticsCard?.setOnClickListener {
            val intent = Intent(this, CategorySpendingGraphActivity::class.java)
            startActivity(intent)
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }
}

