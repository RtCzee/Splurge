package com.example.splurge.data

import android.content.Context
import com.example.splurge.data.local.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.*

class GoalRepository(private val context: Context) {

    private val database = AppDatabase.getInstance(context)
    private val goalDao = database.goalDao()
    private val historyDao = database.goalHistoryDao()

    fun getActiveGoals(userId: Long): Flow<List<GoalEntity>> {  // Changed to Long
        return goalDao.getActiveGoals(userId, Date()).map { goals ->
            goals.map { goal ->
                if (goal.deadline < Date() && goal.status == "IN_PROGRESS") {
                    updateExpiredGoal(goal)
                }
                goal
            }
        }
    }

    suspend fun createGoal(
        userId: Long,  // Changed to Long
        name: String,
        targetAmount: Double,
        category: String,
        notes: String = "",
        deadline: Date = getEndOfMonth()
    ): Long {
        val goal = GoalEntity(
            userId = userId,
            name = name,
            targetAmount = targetAmount,
            category = category,
            notes = notes,
            deadline = deadline
        )
        return goalDao.insertGoal(goal)
    }

    suspend fun updateProgress(goalId: Long, userId: Long, amount: Double): ProgressUpdateResult {  // Changed to Long
        val goal = goalDao.getGoalById(goalId, userId) ?: return ProgressUpdateResult.GoalNotFound

        val newAmount = goal.currentAmount + amount
        val progressPercentage = (newAmount / goal.targetAmount) * 100

        goalDao.addProgress(goalId, amount, Date())

        return if (progressPercentage >= 100) {
            completeGoal(goal, newAmount)
            ProgressUpdateResult.Completed(progressPercentage)
        } else {
            ProgressUpdateResult.Updated(progressPercentage, getMotivationalMessage(progressPercentage))
        }
    }

    // Add this method
    suspend fun getGoalById(goalId: Long, userId: Long): GoalEntity? {  // Changed to Long
        return goalDao.getGoalById(goalId, userId)
    }

    private suspend fun completeGoal(goal: GoalEntity, finalAmount: Double) {
        goalDao.updateGoalStatus(goal.id, "COMPLETED")

        val history = GoalHistoryEntity(
            goalId = goal.id,
            userId = goal.userId,
            name = goal.name,
            targetAmount = goal.targetAmount,
            finalAmount = finalAmount,
            resultStatus = "COMPLETED",
            completedDate = Date(),
            category = goal.category,
            notes = goal.notes
        )
        historyDao.insertHistory(history)
    }

    private suspend fun updateExpiredGoal(goal: GoalEntity): GoalEntity {
        if (goal.deadline < Date() && goal.status == "IN_PROGRESS") {
            goalDao.updateGoalStatus(goal.id, "MISSED")

            val history = GoalHistoryEntity(
                goalId = goal.id,
                userId = goal.userId,
                name = goal.name,
                targetAmount = goal.targetAmount,
                finalAmount = goal.currentAmount,
                resultStatus = "MISSED",
                completedDate = goal.deadline,
                category = goal.category,
                notes = goal.notes
            )
            historyDao.insertHistory(history)

            return goal.copy(status = "MISSED")
        }
        return goal
    }

    suspend fun updateExpiredGoals(userId: Long) {  // Changed to Long
        val expiredGoals = goalDao.getExpiredGoals(userId, Date())
        expiredGoals.forEach { goal ->
            if (goal.status == "IN_PROGRESS") {
                updateExpiredGoal(goal)
            }
        }
    }

    suspend fun getMonthlyReview(userId: Long): MonthlyReview {  // Changed to Long
        val startOfMonth = getStartOfMonth()
        val endOfMonth = getEndOfMonth()

        val history = historyDao.getHistoryForMonth(userId, startOfMonth, endOfMonth)
        val completed = history.count { it.resultStatus == "COMPLETED" }
        val missed = history.count { it.resultStatus == "MISSED" }
        val total = completed + missed

        val successRate = if (total > 0) (completed.toFloat() / total) * 100 else 0f
        val bestCategory = historyDao.getBestPerformingCategory(userId)

        return MonthlyReview(
            totalGoals = total,
            completed = completed,
            missed = missed,
            successRate = successRate,
            bestCategory = bestCategory?.category,
            message = getMonthlyMessage(successRate)
        )
    }

    suspend fun deleteGoal(goalId: Long, userId: Long) {  // Changed to Long
        val goal = goalDao.getGoalById(goalId, userId)
        goal?.let {
            goalDao.deleteGoal(it)
        }
    }

    private fun getMotivationalMessage(progressPercentage: Double): String {
        return when {
            progressPercentage >= 100 -> "🎉 Amazing! Goal completed! 🎉"
            progressPercentage >= 90 -> "🔥 Almost there! You've got this!"
            progressPercentage >= 75 -> "⭐ Incredible progress! Keep pushing!"
            progressPercentage >= 50 -> "💪 Halfway there! You're doing great!"
            progressPercentage >= 25 -> "🌟 Good start! Stay consistent!"
            else -> "🚀 Let's get started! Every step counts!"
        }
    }

    private fun getMonthlyMessage(successRate: Float): String {
        return when {
            successRate >= 80 -> "Outstanding month! You crushed your goals! 🏆"
            successRate >= 60 -> "Great work! You're building excellent habits! 🌟"
            successRate >= 40 -> "Good effort! Next month will be even better! 💪"
            successRate >= 20 -> "Keep going! Every goal gets you closer! 📈"
            else -> "New month, fresh start! Let's make it count! 🚀"
        }
    }

    private fun getStartOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        return calendar.time
    }

    private fun getEndOfMonth(): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        return calendar.time
    }
}

sealed class ProgressUpdateResult {
    object GoalNotFound : ProgressUpdateResult()
    data class Updated(val percentage: Double, val message: String) : ProgressUpdateResult()
    data class Completed(val percentage: Double) : ProgressUpdateResult()
}

data class MonthlyReview(
    val totalGoals: Int,
    val completed: Int,
    val missed: Int,
    val successRate: Float,
    val bestCategory: String?,
    val message: String
)