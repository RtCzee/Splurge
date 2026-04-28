package com.example.splurge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Database operations used to save expenses and build reporting views.
 */
@Dao
interface ExpenseDao {

    /** Inserts one expense row. */
    @Insert
    fun insert(expense: ExpenseEntity): Long

    /** Returns detailed expense items joined with their category metadata. */
    @Query(
        """
        SELECT
            expenses.id AS id,
            expenses.amount AS amount,
            expenses.date AS date,
            expenses.startTime AS startTime,
            expenses.endTime AS endTime,
            expenses.description AS description,
            expenses.photoUri AS photoUri,
            categories.id AS categoryId,
            categories.name AS categoryName
        FROM expenses
        INNER JOIN categories ON categories.id = expenses.categoryId
        WHERE expenses.date BETWEEN :startDate AND :endDate
        ORDER BY expenses.date DESC, expenses.startTime DESC, expenses.id DESC
        """
    )
    fun getExpensesForPeriod(startDate: String, endDate: String): List<ExpenseListItem>

    /** Returns one spending summary row per category for the selected dates. */
    @Query(
        """
        SELECT
            categories.id AS categoryId,
            categories.name AS categoryName,
            COALESCE(SUM(expenses.amount), 0) AS totalAmount,
            COUNT(expenses.id) AS entryCount
        FROM categories
        LEFT JOIN expenses
            ON expenses.categoryId = categories.id
            AND expenses.date BETWEEN :startDate AND :endDate
        GROUP BY categories.id, categories.name
        ORDER BY totalAmount DESC, categories.name ASC
        """
    )
    fun getCategoryTotalsForPeriod(startDate: String, endDate: String): List<CategorySpendTotal>

    /** Returns the total amount spent between the given dates. */
    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getTotalSpentForPeriod(startDate: String, endDate: String): Double

    /** Returns the number of expense rows stored between the given dates. */
    @Query("SELECT COUNT(*) FROM expenses WHERE date BETWEEN :startDate AND :endDate")
    fun getExpenseCountForPeriod(startDate: String, endDate: String): Int
}

