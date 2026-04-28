package com.example.splurge.data

import android.content.Context
import com.example.splurge.data.local.AppDatabase
import com.example.splurge.data.local.BudgetGoalEntity
import com.example.splurge.data.local.CategoryEntity
import com.example.splurge.data.local.CategorySpendTotal
import com.example.splurge.data.local.ExpenseEntity
import com.example.splurge.data.local.ExpenseListItem
import com.example.splurge.data.local.SavingsGoalEntity

/**
 * Central data access layer for the app.
 *
 * The UI talks to this repository instead of calling Room DAOs directly so the
 * screen code can stay focused on presentation and validation.
 */
class FinanceRepository private constructor(context: Context) {

    // Keep a single database connection for the lifetime of the repository.
    private val database = AppDatabase.getInstance(context)

    /** Saves a category if the supplied name is not blank. */
    fun addCategory(name: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return false
        }
        return database.categoryDao().insert(CategoryEntity(name = trimmedName)) != -1L
    }

    /** Returns all saved categories in alphabetical order. */
    fun getCategories(): List<CategoryEntity> {
        return database.categoryDao().getAllCategories()
    }

    /** Returns the total number of categories so the UI can gate expense creation. */
    fun getCategoryCount(): Int {
        return database.categoryDao().getCategoryCount()
    }

    /** Creates or updates the budget goal associated with a specific month. */
    fun saveMonthlyGoal(monthKey: String, minimumGoal: Double, maximumGoal: Double) {
        database.budgetGoalDao().upsert(
            BudgetGoalEntity(
                monthKey = monthKey,
                minimumGoal = minimumGoal,
                maximumGoal = maximumGoal
            )
        )
    }

    /** Looks up the budget goal previously saved for the requested month. */
    fun getMonthlyGoal(monthKey: String): BudgetGoalEntity? {
        return database.budgetGoalDao().getGoalForMonth(monthKey)
    }

    /** Persists one expense entry, including the optional URI of an attached photo. */
    fun addExpense(
        amount: Double,
        date: String,
        startTime: String,
        endTime: String,
        description: String,
        categoryId: Long,
        photoUri: String?
    ) {
        database.expenseDao().insert(
            ExpenseEntity(
                amount = amount,
                date = date,
                startTime = startTime,
                endTime = endTime,
                description = description.trim(),
                categoryId = categoryId,
                photoUri = photoUri
            )
        )
    }

    /** Returns expense rows joined with category data for a date range. */
    fun getExpensesForPeriod(startDate: String, endDate: String): List<ExpenseListItem> {
        return database.expenseDao().getExpensesForPeriod(startDate, endDate)
    }

    /** Aggregates spending totals per category for the selected period. */
    fun getCategoryTotalsForPeriod(startDate: String, endDate: String): List<CategorySpendTotal> {
        return database.expenseDao().getCategoryTotalsForPeriod(startDate, endDate)
    }

    /** Sums every expense amount between the given start and end dates. */
    fun getTotalSpentForPeriod(startDate: String, endDate: String): Double {
        return database.expenseDao().getTotalSpentForPeriod(startDate, endDate)
    }

    /** Counts how many expense entries exist in the requested period. */
    fun getExpenseCountForPeriod(startDate: String, endDate: String): Int {
        return database.expenseDao().getExpenseCountForPeriod(startDate, endDate)
    }

    /** Persists a savings goal after the UI has validated the numeric inputs. */
    fun addSavingsGoal(
        title: String,
        targetAmount: Double,
        currentAmount: Double,
        targetDate: String
    ) {
        database.savingsGoalDao().insert(
            SavingsGoalEntity(
                title = title.trim(),
                targetAmount = targetAmount,
                currentAmount = currentAmount,
                targetDate = targetDate
            )
        )
    }

    /** Returns all savings goals sorted by their target date. */
    fun getSavingsGoals(): List<SavingsGoalEntity> {
        return database.savingsGoalDao().getAllGoals()
    }

    /** Returns the combined amount already saved across every savings goal. */
    fun getTotalSavedAcrossGoals(): Double {
        return database.savingsGoalDao().getTotalSaved()
    }

    /** Returns the combined target amount across every savings goal. */
    fun getTotalGoalTarget(): Double {
        return database.savingsGoalDao().getTotalTarget()
    }

    companion object {
        // Volatile ensures every thread reads the most recent singleton instance.
        @Volatile
        private var INSTANCE: FinanceRepository? = null

        /** Provides the single repository instance shared across the application. */
        fun getInstance(context: Context): FinanceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FinanceRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

