package com.example.splurge.data.local

data class ExpenseListItem(
    val id: Long,
    val amount: Double,
    val date: String,
    val startTime: String,
    val endTime: String,
    val description: String,
    val photoUri: String?,
    val categoryId: Long,
    val categoryName: String
)

data class CategorySpendTotal(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Double,
    val entryCount: Int
)

