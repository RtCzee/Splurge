package com.example.splurge.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

/**
 * Database operations for the unified transactions table.
 */
@Dao
interface TransactionDao {

    @Insert
    fun insert(transaction: TransactionEntity): Long

    @Query(
        """
        SELECT
            transactions.id AS id,
            transactions.amount AS amount,
            transactions.date AS date,
            transactions.description AS description,
            transactions.type AS type,
            transactions.photoUri AS photoUri,
            categories.id AS categoryId,
            categories.name AS categoryName
        FROM transactions
        INNER JOIN categories ON categories.id = transactions.categoryId
        WHERE transactions.date BETWEEN :startDate AND :endDate
        ORDER BY transactions.date DESC, transactions.id DESC
        """
    )
    fun getTransactionsForPeriod(startDate: String, endDate: String): List<TransactionListItem>

    @Query("SELECT COUNT(*) FROM transactions WHERE date BETWEEN :startDate AND :endDate")
    fun getTransactionCountForPeriod(startDate: String, endDate: String): Int
}
