package com.example.splurge.data.local

import androidx.room.TypeConverter

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
}
