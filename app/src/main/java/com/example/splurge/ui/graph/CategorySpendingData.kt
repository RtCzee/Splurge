package com.example.splurge.ui.graph

//Data class that combines category spending totals with budget goals for display in the graph.

data class CategorySpendingData(
    val categoryId: Long,
    val categoryName: String,
    val totalSpent: Double,
    val entryCount: Int,
    val minGoal: Double? = null,
    val maxGoal: Double? = null
)