package com.example.splurge.data

import android.content.Context
import com.example.splurge.data.local.AppDatabase
import com.example.splurge.data.local.BudgetGoalEntity
import com.example.splurge.data.local.CategoryEntity
import com.example.splurge.data.local.CategorySpendTotal
import com.example.splurge.data.local.ExpenseEntity
import com.example.splurge.data.local.ExpenseListItem
import com.example.splurge.data.local.SavingsGoalEntity
import com.example.splurge.data.local.UserEntity
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.Locale

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

    /** Creates a new local account if the email address is not already registered. */
    fun registerUser(
        fullName: String,
        email: String,
        password: String
    ): RegistrationResult {
        val trimmedName = fullName.trim()
        val normalizedEmail = normalizeEmail(email)

        if (trimmedName.isEmpty() || normalizedEmail.isEmpty() || password.isEmpty()) {
            return RegistrationResult.InvalidInput
        }

        if (database.userDao().getUserByEmail(normalizedEmail) != null) {
            return RegistrationResult.EmailAlreadyExists
        }

        val salt = createSalt()
        val userId = database.userDao().insert(
            UserEntity(
                fullName = trimmedName,
                email = normalizedEmail,
                passwordHash = hashPassword(password, salt),
                passwordSalt = salt,
                createdAt = System.currentTimeMillis()
            )
        )
        return RegistrationResult.Success(userId)
    }

    /** Returns the matching user when the supplied credentials are valid. */
    fun authenticateUser(email: String, password: String): UserEntity? {
        val normalizedEmail = normalizeEmail(email)
        if (normalizedEmail.isEmpty() || password.isEmpty()) {
            return null
        }

        val user = database.userDao().getUserByEmail(normalizedEmail) ?: return null
        val computedHash = hashPassword(password, user.passwordSalt)
        return user.takeIf { it.passwordHash == computedHash }
    }

    /** Looks up a registered user by id. */
    fun getUserById(userId: Long): UserEntity? {
        return database.userDao().getUserById(userId)
    }

    private fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.ROOT)
    }

    private fun createSalt(): String {
        val saltBytes = ByteArray(SALT_LENGTH)
        SecureRandom().nextBytes(saltBytes)
        return Base64.getEncoder().encodeToString(saltBytes)
    }

    private fun hashPassword(password: String, salt: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashedBytes = digest.digest("$salt:$password".toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(hashedBytes)
    }

    companion object {
        // Volatile ensures every thread reads the most recent singleton instance.
        @Volatile
        private var INSTANCE: FinanceRepository? = null
        private const val SALT_LENGTH = 16

        /** Provides the single repository instance shared across the application. */
        fun getInstance(context: Context): FinanceRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FinanceRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    sealed class RegistrationResult {
        data class Success(val userId: Long) : RegistrationResult()
        object EmailAlreadyExists : RegistrationResult()
        object InvalidInput : RegistrationResult()
    }
}

