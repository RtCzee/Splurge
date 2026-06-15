package com.example.splurge.data.local

import androidx.room.TypeConverter
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTransactionType(value: TransactionType): String {
        return value.name
    }

    @TypeConverter
    fun toTransactionType(value: String): TransactionType {
        return TransactionType.valueOf(value)
    }

    @TypeConverter
    fun fromBillRecurrence(value: BillRecurrence): String {
        return value.name
    }

    @TypeConverter
    fun toBillRecurrence(value: String): BillRecurrence {
        return BillRecurrence.valueOf(value)
    }

    @TypeConverter
    fun fromBillStatus(value: BillStatus): String {
        return value.name
    }

    @TypeConverter
    fun toBillStatus(value: String): BillStatus {
        return BillStatus.valueOf(value)
    }

    // ========== DATE CONVERTERS for GoalEntity and GoalHistoryEntity ==========

    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}