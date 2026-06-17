package com.example.splurge.ui.budgets

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
import com.example.splurge.data.PrivacyPreferences
import com.example.splurge.ui.base.BaseActivity
import com.example.splurge.ui.common.FinanceUiFormatter
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.textfield.TextInputEditText
import java.time.YearMonth
import kotlin.math.max

/**
 * Screen that lets the user manage monthly budget goals and inspect spending by category.
 */
class Budgets : BaseActivity() {
    private lateinit var repository: FinanceRepository
    private lateinit var privacyPreferences: PrivacyPreferences
    private lateinit var categoryAdapter: BudgetCategoryAdapter
    // The screen always works against the current month rather than an arbitrary period.
    private val currentMonth: YearMonth = YearMonth.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_budgets)
        repository = FinanceRepository.getInstance(this)
        privacyPreferences = PrivacyPreferences(this)
        categoryAdapter = BudgetCategoryAdapter()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.budget_categories_recycler).apply {
            layoutManager = LinearLayoutManager(this@Budgets)
            adapter = categoryAdapter
        }

        setupTabs()
        setupActions()
        bindBudgetScreen()
        setupBottomNavigation(R.id.navigation_budgets)
    }

    // Refresh again when returning from another screen in case shared data changed.
    override fun onResume() {
        super.onResume()
        bindBudgetScreen()
    }

    /** Switches between the goal summary tab and the category totals tab. */
    private fun setupTabs() {
        val goalSection = findViewById<View>(R.id.goal_section)
        val categorySection = findViewById<View>(R.id.category_section)
        findViewById<TabLayout>(R.id.budget_tab_layout).addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    val showGoals = tab.position == 0
                    goalSection.visibility = if (showGoals) View.VISIBLE else View.GONE
                    categorySection.visibility = if (showGoals) View.GONE else View.VISIBLE
                }

                override fun onTabUnselected(tab: TabLayout.Tab) = Unit

                override fun onTabReselected(tab: TabLayout.Tab) = Unit
            }
        )
        goalSection.visibility = View.VISIBLE
        categorySection.visibility = View.GONE
    }

    /** Wires the edit and add actions shown on this screen. */
    private fun setupActions() {
        findViewById<MaterialButton>(R.id.edit_goals_button).setOnClickListener {
            showGoalDialog()
        }
        findViewById<MaterialButton>(R.id.add_category_button).setOnClickListener {
            showAddCategoryDialog()
        }
        findViewById<FloatingActionButton>(R.id.add_budget_fab).setOnClickListener {
            showAddCategoryDialog()
        }
    }

    /** Reads the latest month data and binds it into the budget summary views. */
    private fun bindBudgetScreen() {
        val monthKey = FinanceUiFormatter.monthKey(currentMonth)
        val goal = repository.getMonthlyGoal(monthKey)
        val periodStart = FinanceUiFormatter.formatDate(currentMonth.atDay(1))
        val periodEnd = FinanceUiFormatter.formatDate(currentMonth.atEndOfMonth())
        val categoryTotals = repository.getCategoryTotalsForPeriod(periodStart, periodEnd)
        val monthlySpent = categoryTotals.sumOf { it.totalAmount }
        val minimumGoal = goal?.minimumGoal ?: 0.0
        val maximumGoal = goal?.maximumGoal ?: 0.0
        val remainingBeforeMax = max(maximumGoal - monthlySpent, 0.0)

        findViewById<TextView>(R.id.goal_month_label).text =
            FinanceUiFormatter.formatMonthLabel(currentMonth)
        findViewById<TextView>(R.id.total_budget_amount).text = privacyPreferences.formatCurrency(maximumGoal)
        findViewById<TextView>(R.id.minimum_goal_amount).text = if (privacyPreferences.isHideBalancesEnabled()) {
            getString(R.string.privacy_hidden_value)
        } else {
            getString(R.string.minimum_goal_value, privacyPreferences.formatCurrency(minimumGoal))
        }
        findViewById<TextView>(R.id.remaining_budget).text = if (maximumGoal > 0.0) {
            if (privacyPreferences.isHideBalancesEnabled()) {
                getString(R.string.privacy_hidden_value)
            } else {
                getString(
                    R.string.remaining_before_max_value,
                    privacyPreferences.formatCurrency(remainingBeforeMax)
                )
            }
        } else {
            getString(R.string.no_maximum_goal_message)
        }
        findViewById<TextView>(R.id.budget_goal_status).text =
            buildBudgetStatusMessage(monthlySpent, minimumGoal, maximumGoal)
        findViewById<TextView>(R.id.category_section_subtitle).text = getString(
            R.string.category_summary_subtitle,
            FinanceUiFormatter.formatMonthLabel(currentMonth)
        )

        categoryAdapter.submitList(categoryTotals)
        findViewById<TextView>(R.id.empty_categories_text).visibility =
            if (categoryTotals.isEmpty()) View.VISIBLE else View.GONE
    }

    /** Converts the current month spend into a short status message for the UI. */
    private fun buildBudgetStatusMessage(
        monthlySpent: Double,
        minimumGoal: Double,
        maximumGoal: Double
    ): String {
        if (privacyPreferences.isHideBalancesEnabled()) {
            return getString(R.string.privacy_hidden_message)
        }
        return when {
            maximumGoal <= 0.0 && minimumGoal <= 0.0 ->
                getString(R.string.set_budget_goals_prompt)
            maximumGoal > 0.0 && monthlySpent > maximumGoal ->
                getString(R.string.budget_status_over_maximum)
            minimumGoal > 0.0 && monthlySpent < minimumGoal ->
                getString(R.string.budget_status_below_minimum)
            else ->
                getString(R.string.budget_status_on_track)
        }
    }

    /** Opens the dialog used to create or update the current month's goals. */
    private fun showGoalDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_budget_goal, null, false)
        val minimumInput = dialogView.findViewById<TextInputEditText>(R.id.minimum_goal_input)
        val maximumInput = dialogView.findViewById<TextInputEditText>(R.id.maximum_goal_input)
        val existingGoal = repository.getMonthlyGoal(FinanceUiFormatter.monthKey(currentMonth))

        minimumInput.setText(existingGoal?.minimumGoal?.takeIf { it > 0.0 }?.toString().orEmpty())
        maximumInput.setText(existingGoal?.maximumGoal?.takeIf { it > 0.0 }?.toString().orEmpty())

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.monthly_goals_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save_budget_goals, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val minimumGoal = minimumInput.text.toCurrencyValue()
            val maximumGoal = maximumInput.text.toCurrencyValue()

            // A maximum smaller than the minimum would make the goal impossible to satisfy.
            if (maximumGoal > 0.0 && minimumGoal > maximumGoal) {
                Toast.makeText(this, R.string.invalid_goal_range_message, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            repository.saveMonthlyGoal(
                monthKey = FinanceUiFormatter.monthKey(currentMonth),
                minimumGoal = minimumGoal,
                maximumGoal = maximumGoal
            )
            bindBudgetScreen()
            dialog.dismiss()
        }
    }

    /** Opens the dialog used to save a new expense category. */
    private fun showAddCategoryDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_category, null, false)
        val categoryInput = dialogView.findViewById<TextInputEditText>(R.id.category_name_input)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.add_category_dialog_title)
            .setView(dialogView)
            .setPositiveButton(R.string.save_category_button, null)
            .setNegativeButton(android.R.string.cancel, null)
            .show()

        dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val saved = repository.addCategory(categoryInput.text?.toString().orEmpty())
            if (!saved) {
                Toast.makeText(this, R.string.category_save_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            bindBudgetScreen()
            dialog.dismiss()
        }
    }

    /** Parses a numeric text field into a non-negative currency value. */
    private fun CharSequence?.toCurrencyValue(): Double {
        return this?.toString()?.trim()?.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    }
}
