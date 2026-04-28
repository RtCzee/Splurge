package com.example.splurge.data.local

/**
 * Projection used by the transactions list query so the UI receives both
 * expense data and the readable category name in one object.
 */
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

/**
 * Projection used by reporting screens to show total spend and entry count per category.
 */
data class CategorySpendTotal(
    val categoryId: Long,
    val categoryName: String,
    val totalAmount: Double,
    val entryCount: Int
)

