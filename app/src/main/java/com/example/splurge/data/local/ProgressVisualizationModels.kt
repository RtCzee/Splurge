package com.example.splurge.data.local

/**
 * Represents the spending status for a single category over a period.
 */
enum class SpendingStatus {
    OVER_MAX,      // Exceeded maximum goal
    UNDER_MIN,     // Below minimum goal
    WITHIN_GOALS   // Between min and max goals
}

/**
 * Data model for displaying progress visualization for a category.
 */
data class ProgressVisualizationItem(
    val categoryId: Long,
    val categoryName: String,
    val currentSpending: Double,
    val minimumGoal: Double,
    val maximumGoal: Double,
    val status: SpendingStatus,
    val progressPercentage: Float // 0-100 for progress bar display
) {
    /**
     * Calculates how much has been spent relative to the maximum goal.
     */
    fun getProgressPercent(): Float {
        return if (maximumGoal > 0.0) {
            ((currentSpending / maximumGoal) * 100).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }
    }

    /**
     * Returns a formatted string describing the spending status.
     */
    fun getStatusLabel(): String {
        return when (status) {
            SpendingStatus.OVER_MAX -> "Over Budget"
            SpendingStatus.UNDER_MIN -> "Under Minimum"
            SpendingStatus.WITHIN_GOALS -> "On Track"
        }
    }

    /**
     * Returns the amount remaining before exceeding max (if within goals or under min).
     */
    fun getRemainingAmount(): Double {
        return when {
            maximumGoal > 0.0 && currentSpending < maximumGoal -> maximumGoal - currentSpending
            else -> 0.0
        }
    }

    /**
     * Returns the amount that needs to be spent to reach the minimum.
     */
    fun getAmountToMinimum(): Double {
        return when {
            minimumGoal > 0.0 && currentSpending < minimumGoal -> minimumGoal - currentSpending
            else -> 0.0
        }
    }
}

/**
 * Summary of spending progress for the entire month across all categories.
 */
data class MonthProgressSummary(
    val monthKey: String,
    val totalSpent: Double,
    val minimumGoal: Double,
    val maximumGoal: Double,
    val categoryItems: List<ProgressVisualizationItem>,
    val overBudgetCount: Int,
    val underBudgetCount: Int,
    val onTrackCount: Int
) {
    fun getOverallStatus(): SpendingStatus {
        return when {
            maximumGoal > 0.0 && totalSpent > maximumGoal -> SpendingStatus.OVER_MAX
            minimumGoal > 0.0 && totalSpent < minimumGoal -> SpendingStatus.UNDER_MIN
            else -> SpendingStatus.WITHIN_GOALS
        }
    }

    fun getOverallProgressPercent(): Float {
        return if (maximumGoal > 0.0) {
            ((totalSpent / maximumGoal) * 100).toFloat().coerceIn(0f, 100f)
        } else {
            0f
        }
    }
}
