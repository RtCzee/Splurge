package com.example.splurge.data

import android.content.Context
import com.example.splurge.data.local.AppDatabase
import com.example.splurge.data.local.BudgetGoalEntity
import com.example.splurge.data.local.CategoryEntity
import com.example.splurge.data.local.CategorySpendTotal
import com.example.splurge.data.local.ExpenseEntity
import com.example.splurge.data.local.ExpenseListItem
import com.example.splurge.data.local.SavingsGoalEntity

class FinanceRepository private constructor(context: Context) {

    private val database = AppDatabase.getInstance(context)

    fun addCategory(name: String): Boolean {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return false
        }
        return database.categoryDao().insert(CategoryEntity(name = trimmedName)) != -1L
    }

    fun getCategories(): List<CategoryEntity> {
        return database.categoryDao().getAllCategories()
    }

    fun getCategoryCount(): Int {
        return database.categoryDao().getCategoryCount()
    }

    fun saveMonthlyGoal(monthKey: String, minimumGoal: Double, maximumGoal: Double) {
        database.budgetGoalDao().upsert(
            BudgetGoalEntity(
                monthKey = monthKey,
                minimumGoal = minimumGoal,
                maximumGoal = maximumGoal
            )
        )
    }

    fun getMonthlyGoal(monthKey: String): BudgetGoalEntity? {
        return database.budgetGoalDao().getGoalForMonth(monthKey)
    }

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

    fun getExpensesForPeriod(startDate: String, endDate: String): List<ExpenseListItem> {
        return database.expenseDao().getExpensesForPeriod(startDate, endDate)
    }

    fun getCategoryTotalsForPeriod(startDate: String, endDate: String): List<CategorySpendTotal> {
        return database.expenseDao().getCategoryTotalsForPeriod(startDate, endDate)
    }

    fun getTotalSpentForPeriod(startDate: String, endDate: String): Double {
        return database.expenseDao().getTotalSpentForPeriod(startDate, endDate)
    }

    fun getExpenseCountForPeriod(startDate: String, endDate: String): Int {
        return database.expenseDao().getExpenseCountForPeriod(startDate, endDate)
    }

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

    fun getSavingsGoals(): List<SavingsGoalEntity> {
        return database.savingsGoalDao().getAllGoals()
    }

    fun getTotalSavedAcrossGoals(): Double {
        return database.savingsGoalDao().getTotalSaved()
    }

    fun getTotalGoalTarget(): Double {
        return database.savingsGoalDao().getTotalTarget()
    }

    companion object {
        @Volatile
        private var INSTANCE: FinanceRepository? = null

        fun getInstance(context: Context): FinanceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FinanceRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

