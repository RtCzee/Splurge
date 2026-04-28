package com.example.splurge.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stores the minimum and maximum budget targets for a single month.
 *
 * The month is stored as a string key such as `2026-04`.
 */
@Entity(tableName = "budget_goals")
data class BudgetGoalEntity(
    @PrimaryKey val monthKey: String,
    val minimumGoal: Double,
    val maximumGoal: Double
)

