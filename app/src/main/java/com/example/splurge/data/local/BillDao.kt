package com.example.splurge.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

/**
 * Database operations for bill reminders and bill history.
 */
@Dao
interface BillDao {

    /** Inserts a new bill and returns its generated id. */
    @Insert
    fun insert(bill: BillEntity): Long

    /** Persists edits to an existing bill. */
    @Update
    fun update(bill: BillEntity)

    /** Removes a bill permanently when the user wants a hard delete. */
    @Delete
    fun delete(bill: BillEntity)

    /** Returns a bill by id so reminder actions can update the stored row safely. */
    @Query("SELECT * FROM bills WHERE id = :billId LIMIT 1")
    fun getBillById(billId: Long): BillEntity?

    /** Returns every bill ordered by the nearest due date first. */
    @Query("SELECT * FROM bills ORDER BY dueDate ASC, createdAtMillis DESC")
    fun getAllBills(): List<BillEntity>

    /** Returns only the active bills that still need payment. */
    @Query("SELECT * FROM bills WHERE status = 'ACTIVE' ORDER BY dueDate ASC, createdAtMillis DESC")
    fun getActiveBills(): List<BillEntity>

    /** Returns bill history entries such as paid or archived bills. */
    @Query("SELECT * FROM bills WHERE status != 'ACTIVE' ORDER BY COALESCE(paidAtMillis, createdAtMillis) DESC")
    fun getHistoricBills(): List<BillEntity>
}
