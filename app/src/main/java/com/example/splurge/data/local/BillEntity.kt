package com.example.splurge.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Stored bill record used for reminders, history, and payment actions.
 */
@Entity(
    tableName = "bills",
    indices = [
        Index(value = ["dueDate"]),
        Index(value = ["status"])
    ]
)
data class BillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val dueDate: String,
    val amount: Double? = null,
    val recurrence: BillRecurrence = BillRecurrence.ONE_TIME,
    val notes: String? = null,
    val status: BillStatus = BillStatus.ACTIVE,
    val snoozedUntilMillis: Long? = null,
    val createdAtMillis: Long = System.currentTimeMillis(),
    val paidAtMillis: Long? = null
)
