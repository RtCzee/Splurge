package com.example.splurge.ui.budgets

import com.example.splurge.data.local.BudgetGoalEntity
import com.example.splurge.data.local.CategorySpendTotal
import com.example.splurge.data.local.MonthProgressSummary
import com.example.splurge.data.local.ProgressVisualizationItem
import com.example.splurge.data.local.SpendingStatus

/**
 * Helper class for building progress visualization data from budget and spending information.
 */
object ProgressVisualizationHelper {

    /**
     * Creates a ProgressVisualizationItem for a single category based on its spending and goals.
     */
    fun createProgressItem(
        categorySpend: CategorySpendTotal,
        minimumGoal: Double,
        maximumGoal: Double
    ): ProgressVisualizationItem {
        val status = determineSpendingStatus(
            categorySpend.totalAmount,
            minimumGoal,
            maximumGoal
        )

        return ProgressVisualizationItem(
            categoryId = categorySpend.categoryId,
            categoryName = categorySpend.categoryName,
            currentSpending = categorySpend.totalAmount,
            minimumGoal = minimumGoal,
            maximumGoal = maximumGoal,
            status = status,
            progressPercentage = calculateProgressPercentage(
                categorySpend.totalAmount,
                maximumGoal
            )
        )
    }

    /**
     * Creates a complete MonthProgressSummary from all category spending and budget goals.
     */
    fun createMonthProgressSummary(
        monthKey: String,
        categorySpends: List<CategorySpendTotal>,
        budgetGoal: BudgetGoalEntity?
    ): MonthProgressSummary {
        val minGoal = budgetGoal?.minimumGoal ?: 0.0
        val maxGoal = budgetGoal?.maximumGoal ?: 0.0
        val totalSpent = categorySpends.sumOf { it.totalAmount }

        val progressItems = categorySpends.map { spend ->
            createProgressItem(spend, minGoal, maxGoal)
        }

        val (overCount, underCount, onTrackCount) = countStatuses(progressItems)

        return MonthProgressSummary(
            monthKey = monthKey,
            totalSpent = totalSpent,
            minimumGoal = minGoal,
            maximumGoal = maxGoal,
            categoryItems = progressItems,
            overBudgetCount = overCount,
            underBudgetCount = underCount,
            onTrackCount = onTrackCount
        )
    }

    /**
     * Determines the spending status for a category based on its spending and goals.
     */
    private fun determineSpendingStatus(
        currentSpending: Double,
        minimumGoal: Double,
        maximumGoal: Double
    ): SpendingStatus {
        return when {
            maximumGoal > 0.0 && currentSpending > maximumGoal -> SpendingStatus.OVER_MAX
            minimumGoal > 0.0 && currentSpending < minimumGoal -> SpendingStatus.UNDER_MIN
            else -> SpendingStatus.WITHIN_GOALS
        }
    }

    /**
     * Calculates the progress percentage for a progress bar (0-100).
     */
    private fun calculateProgressPercentage(
        currentSpending: Double,
        maximumGoal: Double
    ): Float {
        return if (maximumGoal > 0.0) {
            ((currentSpending / maximumGoal) * 100).toFloat().coerceIn(0f, 200f)
        } else {
            0f
        }
    }

    /**
     * Counts how many items fall into each status category.
     */
    private fun countStatuses(items: List<ProgressVisualizationItem>): Triple<Int, Int, Int> {
        var overCount = 0
        var underCount = 0
        var onTrackCount = 0

        items.forEach { item ->
            when (item.status) {
                SpendingStatus.OVER_MAX -> overCount++
                SpendingStatus.UNDER_MIN -> underCount++
                SpendingStatus.WITHIN_GOALS -> onTrackCount++
            }
        }

        return Triple(overCount, underCount, onTrackCount)
    }
}
