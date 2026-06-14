package com.example.splurge.data

import android.content.Context
import com.example.splurge.data.local.AppDatabase
import com.example.splurge.data.local.BillEntity
import com.example.splurge.data.local.BillRecurrence
import com.example.splurge.data.local.BillStatus
import com.example.splurge.data.local.BudgetGoalEntity
import com.example.splurge.data.local.CategoryEntity
import com.example.splurge.data.local.CategorySpendTotal
import com.example.splurge.data.local.ExpenseEntity
import com.example.splurge.data.local.ExpenseListItem
import com.example.splurge.data.local.SavingsGoalEntity
import com.example.splurge.data.local.TransactionEntity
import com.example.splurge.data.local.TransactionListItem
import com.example.splurge.data.local.TransactionType
import com.example.splurge.data.local.UserEntity
import com.example.splurge.notifications.BillReminderPreferences
import com.example.splurge.notifications.BillReminderScheduler
import java.time.LocalDate
import java.time.ZoneId
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
    private val applicationContext = context.applicationContext

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

    /** Adds a unified transaction (Income or Expense). */
    fun addTransaction(
        amount: Double,
        date: String,
        description: String,
        categoryId: Long,
        type: TransactionType,
        photoUri: String?
    ) {
        database.transactionDao().insert(
            TransactionEntity(
                amount = amount,
                date = date,
                description = description,
                categoryId = categoryId,
                type = type,
                photoUri = photoUri
            )
        )
    }

    /** Returns all transactions for a period. */
    fun getTransactionsForPeriod(startDate: String, endDate: String): List<TransactionListItem> {
        return database.transactionDao().getTransactionsForPeriod(startDate, endDate)
    }

    /** Returns count of transactions for a period. */
    fun getTransactionCountForPeriod(startDate: String, endDate: String): Int {
        return database.transactionDao().getTransactionCountForPeriod(startDate, endDate)
    }

    /** Saves a bill and schedules any reminders that should fire before its due date. */
    fun addBill(
        name: String,
        dueDate: String,
        amount: Double?,
        recurrence: BillRecurrence,
        notes: String?
    ): Long {
        val billId = database.billDao().insert(
            BillEntity(
                name = name.trim(),
                dueDate = dueDate,
                amount = amount,
                recurrence = recurrence,
                notes = notes?.trim().orEmpty().takeIf { it.isNotEmpty() }
            )
        )
        rescheduleBillReminders(billId)
        return billId
    }

    /** Returns every bill in due-date order. */
    fun getBills(): List<BillEntity> {
        return database.billDao().getAllBills()
    }

    /** Returns only the bills that still need attention. */
    fun getActiveBills(): List<BillEntity> {
        return database.billDao().getActiveBills()
    }

    /** Returns paid and archived bills for the history screen. */
    fun getHistoricBills(): List<BillEntity> {
        return database.billDao().getHistoricBills()
    }

    /** Looks up one bill by id so the UI can update or act on it safely. */
    fun getBillById(billId: Long): BillEntity? {
        return database.billDao().getBillById(billId)
    }

    /** Updates a bill row and immediately re-syncs its reminder alarms. */
    fun updateBill(bill: BillEntity) {
        database.billDao().update(bill)
        rescheduleBillReminders(bill.id)
    }

    /** Marks a bill as paid and rolls recurring bills forward to their next cycle. */
    fun markBillPaid(billId: Long) {
        val bill = database.billDao().getBillById(billId) ?: return
        val paidAtMillis = System.currentTimeMillis()
        val updatedBill = when (bill.recurrence) {
            BillRecurrence.ONE_TIME -> bill.copy(
                status = BillStatus.PAID,
                paidAtMillis = paidAtMillis,
                snoozedUntilMillis = null
            )
            else -> bill.copy(
                dueDate = calculateNextDueDate(bill.dueDate, bill.recurrence),
                paidAtMillis = paidAtMillis,
                snoozedUntilMillis = null
            )
        }

        database.billDao().update(updatedBill)
        rescheduleBillReminders(updatedBill.id)
    }

    /** Snoozes an active reminder for a bill by the requested number of days. */
    fun snoozeBill(billId: Long, snoozeDays: Int) {
        val bill = database.billDao().getBillById(billId) ?: return
        val snoozedUntil = System.currentTimeMillis() + snoozeDays.coerceAtLeast(1) * MILLIS_PER_DAY
        database.billDao().update(bill.copy(snoozedUntilMillis = snoozedUntil))
        rescheduleBillReminders(billId)
    }

    /** Archives a bill without deleting its payment history. */
    fun archiveBill(billId: Long) {
        val bill = database.billDao().getBillById(billId) ?: return
        database.billDao().update(
            bill.copy(
                status = BillStatus.ARCHIVED,
                snoozedUntilMillis = null
            )
        )
        BillReminderScheduler.cancelBill(applicationContext, billId)
    }

    /** Deletes a bill permanently when the user wants to remove it completely. */
    fun deleteBill(billId: Long) {
        val bill = database.billDao().getBillById(billId) ?: return
        database.billDao().delete(bill)
        BillReminderScheduler.cancelBill(applicationContext, billId)
    }

    /** Reschedules every reminder associated with the selected bill. */
    fun rescheduleBillReminders(billId: Long) {
        val bill = database.billDao().getBillById(billId) ?: return
        BillReminderScheduler.cancelBill(applicationContext, billId)

        if (bill.status != BillStatus.ACTIVE) {
            return
        }

        val reminderDays = BillReminderPreferences(applicationContext).getReminderIntervals()
        BillReminderScheduler.scheduleBill(applicationContext, bill, reminderDays)
    }

    /** Reschedules all bill reminders after app startup or a settings change. */
    fun rescheduleAllBillReminders() {
        val preferences = BillReminderPreferences(applicationContext)
        if (!preferences.areBillRemindersEnabled()) {
            BillReminderScheduler.cancelAll(applicationContext)
            return
        }

        val reminderDays = preferences.getReminderIntervals()
        BillReminderScheduler.rescheduleAll(applicationContext, getActiveBills(), reminderDays)
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

    private fun calculateNextDueDate(currentDueDate: String, recurrence: BillRecurrence): String {
        val dueDate = LocalDate.parse(currentDueDate)
        return when (recurrence) {
            BillRecurrence.ONE_TIME -> dueDate.toString()
            BillRecurrence.WEEKLY -> dueDate.plusWeeks(1).toString()
            BillRecurrence.MONTHLY -> dueDate.plusMonths(1).toString()
            BillRecurrence.QUARTERLY -> dueDate.plusMonths(3).toString()
            BillRecurrence.YEARLY -> dueDate.plusYears(1).toString()
        }
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
        private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

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
